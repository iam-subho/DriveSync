package com.iamsubho.drivesync.presentation.browser

import android.app.PendingIntent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iamsubho.drivesync.domain.cloud.CloudStorageProvider
import com.iamsubho.drivesync.data.remote.DriveAuthException
import com.iamsubho.drivesync.domain.model.DriveAccount
import com.iamsubho.drivesync.domain.model.RemoteFolder
import com.iamsubho.drivesync.domain.model.SavedDriveFolder
import com.iamsubho.drivesync.domain.repository.AccountRepository
import com.iamsubho.drivesync.domain.repository.SavedDriveFolderRepository
import com.iamsubho.drivesync.domain.repository.SyncJobRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SyncedFolderUi(
    val folderId: String,
    val folderName: String,
    val jobNames: List<String>,
)

data class BrowserUiState(
    val email: String = "",
    val account: DriveAccount? = null,
    val folders: List<SyncedFolderUi> = emptyList(),
    val browsing: Boolean = false,
    val browsingPath: List<RemoteFolder> = emptyList(),
    val driveFolders: List<RemoteFolder> = emptyList(),
    val foldersLoading: Boolean = false,
    val folderError: String? = null,
)

@HiltViewModel
class BrowserViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    jobRepository: SyncJobRepository,
    accountRepository: AccountRepository,
    private val savedFolderRepository: SavedDriveFolderRepository,
    private val cloudProvider: CloudStorageProvider,
) : ViewModel() {

    private val email: String = checkNotNull(savedStateHandle["email"])
    private val browsingPath = MutableStateFlow<List<RemoteFolder>>(emptyList())
    private val browsing = MutableStateFlow(false)
    private val driveFolders = MutableStateFlow<List<RemoteFolder>>(emptyList())
    private val foldersLoading = MutableStateFlow(false)
    private val folderError = MutableStateFlow<String?>(null)
    private var folderLoadJob: Job? = null
    private var folderLoadGeneration = 0
    private var pendingFolderParentId: String? = null
    private val _driveAuthorizationRequests = MutableSharedFlow<PendingIntent>(extraBufferCapacity = 1)
    val driveAuthorizationRequests: SharedFlow<PendingIntent> = _driveAuthorizationRequests

    private val savedAndJobFolders = combine(
        jobRepository.observeJobs(),
        accountRepository.observeAccounts(),
        savedFolderRepository.observeForAccount(email),
    ) { jobs, accounts, savedFolders ->
        val groupedJobs = jobs.filter { it.accountEmail == email }.groupBy { it.driveFolderId }
        val folders = savedFolders.map { saved ->
            SyncedFolderUi(
                folderId = saved.folderId,
                folderName = saved.folderName,
                jobNames = groupedJobs[saved.folderId].orEmpty().map { it.name },
            )
        }.sortedBy { it.folderName.lowercase() }
        Pair(
            accounts.firstOrNull { it.email == email },
            folders,
        )
    }

    private val browsingState = combine(
        savedAndJobFolders,
        browsingPath,
        browsing,
        driveFolders,
        foldersLoading,
    ) { savedAndJobs, path, isBrowsing, listedDriveFolders, isLoading ->
        BrowserUiState(
            email = email,
            account = savedAndJobs.first,
            folders = savedAndJobs.second,
            browsing = isBrowsing,
            browsingPath = path,
            driveFolders = listedDriveFolders,
            foldersLoading = isLoading,
        )
    }

    val uiState: StateFlow<BrowserUiState> = combine(browsingState, folderError) { state, error ->
        state.copy(folderError = error)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BrowserUiState(email = email))

    fun beginBrowse() {
        browsing.value = true
        browsingPath.value = emptyList()
        loadDriveFolders(parentId = null)
    }

    fun onDriveAuthorizationResult(granted: Boolean) {
        val parentId = pendingFolderParentId
        pendingFolderParentId = null
        if (granted && browsing.value) {
            loadDriveFolders(parentId)
        } else if (!granted) {
            foldersLoading.value = false
            folderError.value = "Drive access was not granted. You can try browsing again."
        }
    }

    fun openDriveFolder(folder: RemoteFolder) {
        browsingPath.value = browsingPath.value + folder
        loadDriveFolders(folder.id)
    }

    fun backFromBrowser(): Boolean {
        if (!browsing.value) return false
        if (browsingPath.value.isNotEmpty()) {
            val newPath = browsingPath.value.dropLast(1)
            browsingPath.value = newPath
            loadDriveFolders(newPath.lastOrNull()?.id)
        } else {
            browsing.value = false
        }
        return true
    }

    fun saveFolder(folder: RemoteFolder) {
        viewModelScope.launch {
            try {
                val folderName = folder.name.trim()
                if (folderName.isEmpty()) {
                    folderError.value = "This Drive folder has no name and cannot be added."
                    return@launch
                }
                savedFolderRepository.save(SavedDriveFolder(email, folder.id, folderName))
                folderError.value = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e(
                    "BrowserViewModel",
                    "Could not add Drive folder id=${folder.id} name=${folder.name}",
                    e,
                )
                folderError.value = e.message ?: "Could not add folder to the app list"
            }
        }
    }

    fun removeFolder(folderId: String) {
        viewModelScope.launch {
            try {
                savedFolderRepository.remove(email, folderId)
                folderError.value = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("BrowserViewModel", "Could not remove Drive folder id=$folderId", e)
                folderError.value = e.message ?: "Could not remove folder from the app list"
            }
        }
    }

    private fun loadDriveFolders(parentId: String?) {
        folderLoadJob?.cancel()
        val generation = ++folderLoadGeneration
        folderLoadJob = viewModelScope.launch {
            foldersLoading.value = true
            folderError.value = null
            driveFolders.value = emptyList()
            try {
                driveFolders.value = cloudProvider.listFolders(email, parentId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: DriveAuthException) {
                foldersLoading.value = false
                val pendingIntent = e.pendingIntent
                if (pendingIntent != null) {
                    pendingFolderParentId = parentId
                    _driveAuthorizationRequests.emit(pendingIntent)
                } else {
                    folderError.value = "Drive access is required. Please reconnect this account and try again."
                }
            } catch (e: Exception) {
                android.util.Log.e(
                    "BrowserViewModel",
                    "Could not list Drive folders for account=$email parentId=$parentId",
                    e,
                )
                folderError.value = e.message ?: "Could not load Drive folders"
            } finally {
                if (generation == folderLoadGeneration) foldersLoading.value = false
            }
        }
    }
}
