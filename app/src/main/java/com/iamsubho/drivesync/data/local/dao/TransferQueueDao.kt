package com.iamsubho.drivesync.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.iamsubho.drivesync.data.local.entity.TransferQueueEntity

@Dao
interface TransferQueueDao {
    @Query("SELECT * FROM transfer_queue WHERE jobId = :jobId AND state = 'PENDING' AND nextRetryAt <= :now ORDER BY id ASC")
    suspend fun readyForJob(jobId: Long, now: Long): List<TransferQueueEntity>

    @Query("SELECT COUNT(*) FROM transfer_queue WHERE jobId = :jobId AND state = 'PENDING'")
    suspend fun countPending(jobId: Long): Int

    @Query("SELECT MIN(nextRetryAt) FROM transfer_queue WHERE jobId = :jobId AND state = 'PENDING'")
    suspend fun earliestRetryAt(jobId: Long): Long?

    @Query("UPDATE transfer_queue SET state = 'PENDING' WHERE jobId = :jobId AND state = 'RUNNING'")
    suspend fun resetRunning(jobId: Long)

    @Insert
    suspend fun insertAll(ops: List<TransferQueueEntity>)

    @Update
    suspend fun update(op: TransferQueueEntity)

    @Query("DELETE FROM transfer_queue WHERE jobId = :jobId AND state IN ('DONE', 'FAILED')")
    suspend fun clearFinished(jobId: Long)

    @Query("DELETE FROM transfer_queue WHERE jobId = :jobId")
    suspend fun deleteForJob(jobId: Long)
}
