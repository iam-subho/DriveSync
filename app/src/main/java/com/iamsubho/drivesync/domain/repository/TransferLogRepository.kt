package com.iamsubho.drivesync.domain.repository

import androidx.paging.PagingData
import com.iamsubho.drivesync.domain.model.TransferLogEntry
import kotlinx.coroutines.flow.Flow

interface TransferLogRepository {
    fun pagedLogs(jobId: Long): Flow<PagingData<TransferLogEntry>>
    fun observeCount(jobId: Long): Flow<Int>
    suspend fun add(entry: TransferLogEntry)
    suspend fun clear(jobId: Long)
}
