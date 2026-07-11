package com.iamsubho.drivesync.presentation.browser

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iamsubho.drivesync.domain.model.DriveAccount
import com.iamsubho.drivesync.domain.repository.AccountRepository
import com.iamsubho.drivesync.domain.repository.SyncJobRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** One Drive folder that at least one sync job of this account uses. */
data class SyncedFolderUi(
    val folderId: String,
    val folderName: String,
    val jobNames: List<String>,
)

data class BrowserUiState(
    val email: String = "",
    val account: DriveAccount? = null,
    val folders: List<SyncedFolderUi> = emptyList(),
)

@HiltViewModel
class BrowserViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    jobRepository: SyncJobRepository,
    accountRepository: AccountRepository,
) : ViewModel() {

    private val email: String = checkNotNull(savedStateHandle["email"])

    val uiState: StateFlow<BrowserUiState> = combine(
        jobRepository.observeJobs(),
        accountRepository.observeAccounts(),
    ) { jobs, accounts ->
        val folders = jobs
            .filter { it.accountEmail == email }
            .groupBy { it.driveFolderId }
            .map { (folderId, jobsForFolder) ->
                SyncedFolderUi(
                    folderId = folderId,
                    folderName = jobsForFolder.first().driveFolderName,
                    jobNames = jobsForFolder.map { it.name },
                )
            }
            .sortedBy { it.folderName.lowercase() }
        BrowserUiState(
            email = email,
            account = accounts.firstOrNull { it.email == email },
            folders = folders,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BrowserUiState(email = email))
}
