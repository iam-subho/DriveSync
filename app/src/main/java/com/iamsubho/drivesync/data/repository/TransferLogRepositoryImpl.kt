package com.iamsubho.drivesync.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.iamsubho.drivesync.data.local.dao.TransferLogDao
import com.iamsubho.drivesync.data.local.entity.toEntity
import com.iamsubho.drivesync.domain.model.TransferLogEntry
import com.iamsubho.drivesync.domain.repository.TransferLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferLogRepositoryImpl @Inject constructor(
    private val logDao: TransferLogDao,
) : TransferLogRepository {

    override fun pagedLogs(jobId: Long): Flow<PagingData<TransferLogEntry>> =
        Pager(
            config = PagingConfig(pageSize = 50, enablePlaceholders = false),
            pagingSourceFactory = { logDao.pagingSource(jobId) },
        ).flow.map { paging -> paging.map { it.toDomain() } }

    override fun observeCount(jobId: Long): Flow<Int> = logDao.observeCount(jobId)

    override suspend fun add(entry: TransferLogEntry) {
        logDao.insert(entry.toEntity())
    }

    override suspend fun clear(jobId: Long) {
        logDao.clear(jobId)
    }
}
