package com.iamsubho.drivesync.domain.model

data class DriveAccount(
    val email: String,
    val displayName: String,
    val photoUrl: String?,
    val storageUsedBytes: Long,
    val storageTotalBytes: Long,
    val addedAt: Long,
)

data class StorageQuota(
    val usedBytes: Long,
    val totalBytes: Long,
)
