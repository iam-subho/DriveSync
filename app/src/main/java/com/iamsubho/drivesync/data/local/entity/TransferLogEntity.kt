package com.iamsubho.drivesync.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.iamsubho.drivesync.domain.model.TransferDirection
import com.iamsubho.drivesync.domain.model.TransferLogEntry
import com.iamsubho.drivesync.domain.model.TransferResult

@Entity(
    tableName = "transfer_logs",
    foreignKeys = [
        ForeignKey(
            entity = SyncJobEntity::class,
            parentColumns = ["id"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["jobId", "timestamp"])],
)
data class TransferLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobId: Long,
    val timestamp: Long,
    val direction: String,
    val fileName: String,
    val sizeBytes: Long,
    val result: String,
    val reason: String?,
) {
    fun toDomain() = TransferLogEntry(
        id = id,
        jobId = jobId,
        timestamp = timestamp,
        direction = TransferDirection.valueOf(direction),
        fileName = fileName,
        sizeBytes = sizeBytes,
        result = TransferResult.valueOf(result),
        reason = reason,
    )
}

fun TransferLogEntry.toEntity() = TransferLogEntity(
    id = id,
    jobId = jobId,
    timestamp = timestamp,
    direction = direction.name,
    fileName = fileName,
    sizeBytes = sizeBytes,
    result = result.name,
    reason = reason,
)
