package com.iamsubho.drivesync.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.iamsubho.drivesync.data.local.entity.TransferLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferLogDao {
    @Query("SELECT * FROM transfer_logs WHERE jobId = :jobId ORDER BY timestamp DESC, id DESC")
    fun pagingSource(jobId: Long): PagingSource<Int, TransferLogEntity>

    @Query("SELECT COUNT(*) FROM transfer_logs WHERE jobId = :jobId")
    fun observeCount(jobId: Long): Flow<Int>

    @Insert
    suspend fun insert(log: TransferLogEntity)

    @Query("DELETE FROM transfer_logs WHERE jobId = :jobId")
    suspend fun clear(jobId: Long)
}
