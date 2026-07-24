package com.iamsubho.drivesync.domain.model

data class SyncProgress(
    val jobId: Long,
    val currentFile: String,
    val percent: Int,
    val speedBytesPerSec: Long,
    val etaSeconds: Long?,
    val pending: Int,
)
