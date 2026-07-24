package com.iamsubho.drivesync.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.iamsubho.drivesync.domain.model.FileTypeCategory
import com.iamsubho.drivesync.domain.model.LimitMode
import com.iamsubho.drivesync.domain.model.SyncDirection
import com.iamsubho.drivesync.domain.model.SyncJob
import com.iamsubho.drivesync.domain.model.SyncStatus

@Entity(
    tableName = "sync_jobs",
    foreignKeys = [
        ForeignKey(
            entity = DriveAccountEntity::class,
            parentColumns = ["email"],
            childColumns = ["accountEmail"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("accountEmail")],
)
data class SyncJobEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val accountEmail: String,
    val driveFolderId: String,
    val driveFolderName: String,
    val localFolderUri: String,
    val localFolderName: String,
    val direction: String,
    val fileTypes: String,          // comma-joined FileTypeCategory names
    val customExtensions: String,   // comma-joined lowercase extensions
    val excludedFolders: String = "", // newline-joined relative folder paths
    val uploadLimitMb: Int?,
    val uploadLimitMode: String,
    val downloadLimitMb: Int?,
    val downloadLimitMode: String,
    val deleteAfterUpload: Boolean = false,
    val syncIntervalHours: Int = 24,
    val status: String,
    val lastSyncAt: Long?,
    val createdAt: Long,
    val pendingCount: Int = 0,
    val uploadedCount: Int = 0,
    val downloadedCount: Int = 0,
    val failedCount: Int = 0,
    val errorMessage: String? = null,
) {
    fun toDomain() = SyncJob(
        id = id,
        name = name,
        accountEmail = accountEmail,
        driveFolderId = driveFolderId,
        driveFolderName = driveFolderName,
        localFolderUri = localFolderUri,
        localFolderName = localFolderName,
        direction = SyncDirection.valueOf(direction),
        fileTypes = fileTypes.split(',').filter { it.isNotBlank() }
            .map { FileTypeCategory.valueOf(it) }.toSet(),
        customExtensions = customExtensions.split(',').filter { it.isNotBlank() },
        excludedFolders = excludedFolders.split('\n').filter { it.isNotBlank() },
        uploadLimitMb = uploadLimitMb,
        uploadLimitMode = LimitMode.valueOf(uploadLimitMode),
        downloadLimitMb = downloadLimitMb,
        downloadLimitMode = LimitMode.valueOf(downloadLimitMode),
        deleteAfterUpload = deleteAfterUpload,
        syncIntervalHours = syncIntervalHours,
        status = SyncStatus.valueOf(status),
        lastSyncAt = lastSyncAt,
        createdAt = createdAt,
        pendingCount = pendingCount,
        uploadedCount = uploadedCount,
        downloadedCount = downloadedCount,
        failedCount = failedCount,
        errorMessage = errorMessage,
    )
}

fun SyncJob.toEntity() = SyncJobEntity(
    id = id,
    name = name,
    accountEmail = accountEmail,
    driveFolderId = driveFolderId,
    driveFolderName = driveFolderName,
    localFolderUri = localFolderUri,
    localFolderName = localFolderName,
    direction = direction.name,
    fileTypes = fileTypes.joinToString(",") { it.name },
    customExtensions = customExtensions.joinToString(","),
    excludedFolders = excludedFolders.joinToString("\n"),
    uploadLimitMb = uploadLimitMb,
    uploadLimitMode = uploadLimitMode.name,
    downloadLimitMb = downloadLimitMb,
    downloadLimitMode = downloadLimitMode.name,
    deleteAfterUpload = deleteAfterUpload,
    syncIntervalHours = syncIntervalHours,
    status = status.name,
    lastSyncAt = lastSyncAt,
    createdAt = createdAt,
    pendingCount = pendingCount,
    uploadedCount = uploadedCount,
    downloadedCount = downloadedCount,
    failedCount = failedCount,
    errorMessage = errorMessage,
)
