package com.iamsubho.drivesync.domain.model

data class SyncJob(
    val id: Long = 0,
    val name: String,
    val accountEmail: String,
    val driveFolderId: String,
    val driveFolderName: String,
    val localFolderUri: String,
    val localFolderName: String,
    val direction: SyncDirection,
    val fileTypes: Set<FileTypeCategory>,
    val customExtensions: List<String>,
    val uploadLimitMb: Int?,
    val uploadLimitMode: LimitMode,
    val downloadLimitMb: Int?,
    val downloadLimitMode: LimitMode,
    /** Delete each local file after its upload succeeds (never for DOWNLOAD_ONLY jobs). */
    val deleteAfterUpload: Boolean = false,
    /** Automatic re-run interval; default 24h. */
    val syncIntervalHours: Int = 24,
    val status: SyncStatus,
    val lastSyncAt: Long?,
    val createdAt: Long,
    val pendingCount: Int = 0,
    val uploadedCount: Int = 0,
    val downloadedCount: Int = 0,
    val failedCount: Int = 0,
    val errorMessage: String? = null,
)
