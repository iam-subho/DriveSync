package com.iamsubho.drivesync.presentation.browser

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iamsubho.drivesync.domain.cloud.CloudStorageProvider
import com.iamsubho.drivesync.domain.model.RemoteFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

data class BrowserFilesUiState(
    val email: String = "",
    val folderName: String = "",
    val loading: Boolean = true,
    val error: String? = null,
    val allFiles: List<RemoteFile> = emptyList(),
    val query: String = "",
    /** UTC-midnight millis from the Material 3 date picker; null = no date filter. */
    val selectedDayUtcMillis: Long? = null,
    val filtered: List<RemoteFile> = emptyList(),
    /** Drive file ids ticked for deletion. */
    val selectedIds: Set<String> = emptySet(),
    val deleting: Boolean = false,
    /** Transient outcome message (e.g. after a delete). */
    val notice: String? = null,
)

@HiltViewModel
class BrowserFilesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val cloudProvider: CloudStorageProvider,
) : ViewModel() {

    private val email: String = checkNotNull(savedStateHandle["email"])
    private val folderId: String = checkNotNull(savedStateHandle["folderId"])
    private val folderName: String = savedStateHandle["folderName"] ?: "Drive folder"

    private val _state = MutableStateFlow(
        BrowserFilesUiState(email = email, folderName = folderName),
    )
    val uiState: StateFlow<BrowserFilesUiState> = _state

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val files = cloudProvider.listAllFiles(email, folderId)
                    .sortedByDescending { it.deviceCreatedAt ?: it.modifiedAt }
                _state.update { applyFilter(it.copy(loading = false, allFiles = files)) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, error = e.message ?: "Could not load files")
                }
            }
        }
    }

    fun setQuery(query: String) {
        _state.update { applyFilter(it.copy(query = query)) }
    }

    fun setDay(utcMidnightMillis: Long?) {
        _state.update { applyFilter(it.copy(selectedDayUtcMillis = utcMidnightMillis)) }
    }

    fun toggleSelect(fileId: String) {
        _state.update {
            val selected = if (fileId in it.selectedIds) it.selectedIds - fileId else it.selectedIds + fileId
            it.copy(selectedIds = selected)
        }
    }

    fun clearSelection() {
        _state.update { it.copy(selectedIds = emptySet()) }
    }

    fun consumeNotice() {
        _state.update { it.copy(notice = null) }
    }

    /** Deletes the ticked files from Google Drive. */
    fun deleteSelected() {
        val ids = _state.value.selectedIds
        if (ids.isEmpty() || _state.value.deleting) return
        viewModelScope.launch {
            _state.update { it.copy(deleting = true) }
            val failed = mutableSetOf<String>()
            for (id in ids) {
                try {
                    cloudProvider.delete(email, id)
                } catch (e: Exception) {
                    failed += id
                }
            }
            val deleted = ids - failed
            _state.update { s ->
                applyFilter(
                    s.copy(
                        allFiles = s.allFiles.filterNot { it.id in deleted },
                        selectedIds = failed,
                        deleting = false,
                        notice = when {
                            failed.isEmpty() -> "${deleted.size} file(s) deleted from Drive"
                            else -> "Deleted ${deleted.size}, failed ${failed.size} — check connection and retry"
                        },
                    ),
                )
            }
        }
    }

    private fun applyFilter(state: BrowserFilesUiState): BrowserFilesUiState {
        val (start, end) = state.selectedDayUtcMillis?.let(::localDayBounds) ?: (null to null)
        return state.copy(
            filtered = FileSearchFilter.filter(state.allFiles, state.query, start, end),
        )
    }

    /**
     * The date picker returns midnight UTC of the chosen calendar date. Files carry local
     * epoch timestamps, so convert that calendar date to the device timezone's day bounds.
     */
    private fun localDayBounds(utcMidnightMillis: Long): Pair<Long, Long> {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMidnightMillis
        }
        val local = Calendar.getInstance().apply {
            clear()
            set(
                utc.get(Calendar.YEAR),
                utc.get(Calendar.MONTH),
                utc.get(Calendar.DAY_OF_MONTH),
            )
        }
        val start = local.timeInMillis
        local.add(Calendar.DAY_OF_MONTH, 1)
        return start to local.timeInMillis
    }
}
