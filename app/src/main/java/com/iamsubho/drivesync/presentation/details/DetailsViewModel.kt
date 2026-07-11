package com.iamsubho.drivesync.presentation.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.iamsubho.drivesync.domain.model.DriveAccount
import com.iamsubho.drivesync.domain.model.SyncJob
import com.iamsubho.drivesync.domain.model.SyncProgress
import com.iamsubho.drivesync.domain.model.TransferLogEntry
import com.iamsubho.drivesync.domain.repository.AccountRepository
import com.iamsubho.drivesync.domain.repository.SyncController
import com.iamsubho.drivesync.domain.repository.SyncJobRepository
import com.iamsubho.drivesync.domain.repository.TransferLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailsUiState(
    val job: SyncJob? = null,
    val account: DriveAccount? = null,
    val progress: SyncProgress? = null,
    val logCount: Int = 0,
    val activeTab: DetailsTab = DetailsTab.OVERVIEW,
)

enum class DetailsTab { OVERVIEW, LOGS }

@HiltViewModel
class DetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val jobRepository: SyncJobRepository,
    private val logRepository: TransferLogRepository,
    private val syncController: SyncController,
    accountRepository: AccountRepository,
) : ViewModel() {

    private val jobId: Long = checkNotNull(savedStateHandle["jobId"])

    private val activeTab = MutableStateFlow(DetailsTab.OVERVIEW)

    val logs: Flow<PagingData<TransferLogEntry>> =
        logRepository.pagedLogs(jobId).cachedIn(viewModelScope)

    val uiState: StateFlow<DetailsUiState> = combine(
        jobRepository.observeJob(jobId),
        accountRepository.observeAccounts(),
        syncController.observeProgress(jobId),
        logRepository.observeCount(jobId),
        activeTab,
    ) { job, accounts, progress, logCount, tab ->
        DetailsUiState(
            job = job,
            account = accounts.firstOrNull { it.email == job?.accountEmail },
            progress = progress,
            logCount = logCount,
            activeTab = tab,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailsUiState())

    fun selectTab(tab: DetailsTab) {
        activeTab.value = tab
    }

    fun start() = syncController.start(jobId)

    fun pause() = syncController.pause(jobId)

    fun clearLogs() {
        viewModelScope.launch { logRepository.clear(jobId) }
    }
}
