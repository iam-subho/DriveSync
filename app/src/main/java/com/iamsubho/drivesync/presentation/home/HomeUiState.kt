package com.iamsubho.drivesync.presentation.home

import com.iamsubho.drivesync.domain.model.DriveAccount
import com.iamsubho.drivesync.domain.model.SyncJob
import com.iamsubho.drivesync.domain.model.SyncProgress

data class HomeUiState(
    val accounts: List<DriveAccount> = emptyList(),
    val jobs: List<JobWithProgress> = emptyList(),
    val snackMessage: String? = null,
    val addingAccount: Boolean = false,
)

data class JobWithProgress(
    val job: SyncJob,
    val progress: SyncProgress?,
)
