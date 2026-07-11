package com.iamsubho.drivesync.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single pending transfer operation. Rows survive process death and reboot,
 * which is what makes sync jobs resumable.
 */
@Entity(
    tableName = "transfer_queue",
    foreignKeys = [
        ForeignKey(
            entity = SyncJobEntity::class,
            parentColumns = ["id"],
            childColumns = ["jobId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("jobId")],
)
data class TransferQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobId: Long,
    val opType: String,            // UPLOAD | DOWNLOAD
    val fileName: String,
    val relativePath: String,
    val localUri: String?,         // set for uploads
    val remoteFileId: String?,     // set for downloads and overwrite-uploads
    val sizeBytes: Long,
    val md5: String?,
    val modifiedAt: Long,
    val attemptCount: Int = 0,
    val nextRetryAt: Long = 0,
    val state: String = STATE_PENDING,
) {
    companion object {
        const val STATE_PENDING = "PENDING"
        const val STATE_RUNNING = "RUNNING"
        const val STATE_DONE = "DONE"
        const val STATE_FAILED = "FAILED"
        const val OP_UPLOAD = "UPLOAD"
        const val OP_DOWNLOAD = "DOWNLOAD"
    }
}
