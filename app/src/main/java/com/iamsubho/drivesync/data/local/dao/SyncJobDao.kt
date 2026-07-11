package com.iamsubho.drivesync.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.iamsubho.drivesync.data.local.entity.SyncJobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncJobDao {
    @Query("SELECT * FROM sync_jobs ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<SyncJobEntity>>

    @Query("SELECT * FROM sync_jobs WHERE id = :id")
    fun observeById(id: Long): Flow<SyncJobEntity?>

    @Query("SELECT * FROM sync_jobs WHERE id = :id")
    suspend fun get(id: Long): SyncJobEntity?

    @Query("SELECT * FROM sync_jobs WHERE status = :status")
    suspend fun byStatus(status: String): List<SyncJobEntity>

    @Insert
    suspend fun insert(job: SyncJobEntity): Long

    @Update
    suspend fun update(job: SyncJobEntity)

    @Query("DELETE FROM sync_jobs WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE sync_jobs SET status = :status, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, errorMessage: String?)

    @Query("UPDATE sync_jobs SET pendingCount = :pending WHERE id = :id")
    suspend fun updatePending(id: Long, pending: Int)

    @Query("UPDATE sync_jobs SET uploadedCount = uploadedCount + 1 WHERE id = :id")
    suspend fun incrementUploaded(id: Long)

    @Query("UPDATE sync_jobs SET downloadedCount = downloadedCount + 1 WHERE id = :id")
    suspend fun incrementDownloaded(id: Long)

    @Query("UPDATE sync_jobs SET failedCount = failedCount + 1 WHERE id = :id")
    suspend fun incrementFailed(id: Long)

    @Query("UPDATE sync_jobs SET status = :status, lastSyncAt = :lastSyncAt, pendingCount = 0, errorMessage = NULL WHERE id = :id")
    suspend fun markCompleted(id: Long, status: String, lastSyncAt: Long)
}
