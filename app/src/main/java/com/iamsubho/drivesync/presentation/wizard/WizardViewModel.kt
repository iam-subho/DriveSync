package com.iamsubho.drivesync.presentation.wizard

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iamsubho.drivesync.domain.cloud.CloudStorageProvider
import com.iamsubho.drivesync.domain.model.FileTypeCategory
import com.iamsubho.drivesync.domain.model.LimitMode
import com.iamsubho.drivesync.domain.model.RemoteFolder
import com.iamsubho.drivesync.domain.model.SyncDirection
import com.iamsubho.drivesync.domain.model.SyncJob
import com.iamsubho.drivesync.domain.model.SyncStatus
import com.iamsubho.drivesync.domain.repository.AccountRepository
import com.iamsubho.drivesync.domain.repository.SyncJobRepository
import com.iamsubho.drivesync.presentation.common.SnackBus
import com.iamsubho.drivesync.presentation.util.BatteryOptimization
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WizardViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    accountRepository: AccountRepository,
    private val jobRepository: SyncJobRepository,
    private val cloudProvider: CloudStorageProvider,
    private val scanner: com.iamsubho.drivesync.data.sync.LocalFolderScanner,
    private val snackBus: SnackBus,
) : ViewModel() {

    private val _state = MutableStateFlow(WizardUiState())
    val uiState: StateFlow<WizardUiState> = _state

    private var folderLoadJob: Job? = null

    init {
        viewModelScope.launch {
            accountRepository.observeAccounts().collect { accounts ->
                _state.update { it.copy(accounts = accounts) }
            }
        }
    }

    // ---- navigation ----

    fun next() {
        val s = _state.value
        if (!s.canNext || s.step >= WizardUiState.LAST_STEP) return
        _state.update { it.copy(step = it.step + 1) }
        if (_state.value.step == 4) refreshBattery()
    }

    /** @return false when already on step 1 — caller should exit the wizard. */
    fun back(): Boolean {
        val s = _state.value
        if (s.step <= 1) return false
        _state.update { it.copy(step = it.step - 1) }
        return true
    }

    // ---- step 1: account ----

    fun pickAccount(email: String) {
        val changed = _state.value.selectedAccountEmail != email
        _state.update {
            it.copy(
                selectedAccountEmail = email,
                selectedFolderId = if (changed) null else it.selectedFolderId,
                selectedFolderName = if (changed) null else it.selectedFolderName,
            )
        }
        if (changed) loadDriveFolders()
    }

    // ---- step 2: drive folder ----

    fun loadDriveFolders() {
        val email = _state.value.selectedAccountEmail ?: return
        folderLoadJob?.cancel()
        folderLoadJob = viewModelScope.launch {
            _state.update { it.copy(foldersLoading = true, folderError = null) }
            try {
                val folders = cloudProvider.listFolders(email, parentId = null)
                _state.update { it.copy(driveFolders = folders, foldersLoading = false) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        foldersLoading = false,
                        folderError = e.message ?: "Could not load Drive folders",
                    )
                }
            }
        }
    }

    fun pickFolder(folder: RemoteFolder) {
        _state.update { it.copy(selectedFolderId = folder.id, selectedFolderName = folder.name) }
    }

    fun createFolder(name: String) {
        val email = _state.value.selectedAccountEmail ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(foldersLoading = true, folderError = null) }
            try {
                val folder = cloudProvider.createFolder(email, parentId = null, name = name.trim())
                _state.update {
                    it.copy(
                        driveFolders = it.driveFolders + folder,
                        selectedFolderId = folder.id,
                        selectedFolderName = folder.name,
                        foldersLoading = false,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        foldersLoading = false,
                        folderError = e.message ?: "Could not create folder",
                    )
                }
            }
        }
    }

    // ---- step 3: local folder ----

    fun onLocalPicked(uri: Uri) {
        appContext.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        )
        val doc = DocumentFile.fromTreeUri(appContext, uri)
        _state.update {
            it.copy(
                localFolderUri = uri.toString(),
                localFolderName = doc?.name ?: "Selected folder",
                localFolderPath = com.iamsubho.drivesync.presentation.common.Formatters
                    .decodeTreePath(uri.toString()),
                localSubfolders = emptyList(),
                excludedFolders = emptySet(),
            )
        }
        // Listing subfolders touches the ContentResolver — keep it off the main thread.
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val subfolders = scanner.listSubfolders(uri)
            _state.update { it.copy(localSubfolders = subfolders) }
        }
    }

    fun toggleExcludedFolder(name: String) {
        _state.update {
            val excluded = if (name in it.excludedFolders) it.excludedFolders - name
            else it.excludedFolders + name
            it.copy(excludedFolders = excluded)
        }
    }

    // ---- step 4: battery ----

    fun refreshBattery() {
        _state.update {
            it.copy(batteryOk = BatteryOptimization.isIgnoringBatteryOptimizations(appContext))
        }
    }

    // ---- step 5: file types ----

    fun toggleType(category: FileTypeCategory) {
        _state.update { s ->
            // "All files" dims the others; they can't be toggled while it is on.
            if (s.allFilesSelected && category != FileTypeCategory.ALL) return@update s
            val types = if (category in s.types) s.types - category else s.types + category
            s.copy(types = types)
        }
    }

    fun setCustomExtensions(value: String) {
        _state.update { it.copy(customExtensions = value) }
    }

    // ---- step 6: size limits ----

    fun setUploadLimit(transform: (LimitUi) -> LimitUi) {
        _state.update { it.copy(upLimit = transform(it.upLimit)) }
    }

    fun setDownloadLimit(transform: (LimitUi) -> LimitUi) {
        _state.update { it.copy(downLimit = transform(it.downLimit)) }
    }

    // ---- step 7: direction ----

    fun setDirection(direction: SyncDirection) {
        _state.update { it.copy(direction = direction) }
    }

    // ---- step 8: schedule + cleanup ----

    fun setIntervalPreset(hours: Int) {
        _state.update { it.copy(intervalHours = hours, intervalCustom = false) }
    }

    fun setIntervalCustom() {
        _state.update { it.copy(intervalCustom = true) }
    }

    fun setCustomHours(raw: String) {
        _state.update { it.copy(customHours = raw.filter { c -> c.isDigit() }.take(4)) }
    }

    fun toggleDeleteAfterUpload() {
        _state.update { it.copy(deleteAfterUpload = !it.deleteAfterUpload) }
    }

    // ---- step 9: review + create ----

    fun setName(name: String) {
        _state.update { it.copy(jobName = name) }
    }

    fun create(onDone: () -> Unit) {
        val s = _state.value
        if (!s.canNext || s.creating) return
        viewModelScope.launch {
            _state.update { it.copy(creating = true) }
            try {
                val job = SyncJob(
                    name = s.jobName.trim(),
                    accountEmail = s.selectedAccountEmail!!,
                    driveFolderId = s.selectedFolderId!!,
                    driveFolderName = s.selectedFolderName!!,
                    localFolderUri = s.localFolderUri!!,
                    localFolderName = s.localFolderName ?: "Local folder",
                    direction = s.direction,
                    fileTypes = s.types,
                    customExtensions = parseCustomExtensions(s.customExtensions),
                    excludedFolders = s.excludedFolders.toList().sorted(),
                    uploadLimitMb = s.upLimit.mbValue,
                    uploadLimitMode = s.upLimit.mode,
                    downloadLimitMb = s.downLimit.mbValue,
                    downloadLimitMode = s.downLimit.mode,
                    deleteAfterUpload = s.effectiveDeleteAfterUpload,
                    syncIntervalHours = s.effectiveIntervalHours,
                    status = SyncStatus.NEVER_SYNCED,
                    lastSyncAt = null,
                    createdAt = System.currentTimeMillis(),
                )
                jobRepository.createJob(job)
                snackBus.post("Sync job \"${job.name}\" created")
                onDone()
            } finally {
                _state.update { it.copy(creating = false) }
            }
        }
    }

    private fun parseCustomExtensions(raw: String): List<String> =
        raw.split(',')
            .map { it.trim().lowercase().removePrefix(".") }
            .filter { it.isNotBlank() }
            .distinct()

    companion object {
        val LIMIT_MODES = listOf(
            LimitMode.SKIP_ABOVE to "Skip files above this size",
            LimitMode.ONLY_ABOVE to "Only sync files above this size",
        )
    }
}
