package com.iamsubho.drivesync.domain.model

data class TransferLogEntry(
    val id: Long = 0,
    val jobId: Long,
    val timestamp: Long,
    val direction: TransferDirection,
    val fileName: String,
    val sizeBytes: Long,
    val result: TransferResult,
    val reason: String?,
)
