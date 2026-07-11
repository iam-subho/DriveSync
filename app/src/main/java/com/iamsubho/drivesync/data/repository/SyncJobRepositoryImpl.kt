package com.iamsubho.drivesync.data.repository

import com.iamsubho.drivesync.data.local.dao.SyncJobDao
import com.iamsubho.drivesync.data.local.dao.TransferQueueDao
import com.iamsubho.drivesync.data.local.entity.toEntity
import com.iamsubho.drivesync.domain.model.SyncJob
import com.iamsubho.drivesync.domain.model.SyncStatus
import com.iamsubho.drivesync.domain.repository.SyncController
import com.iamsubho.drivesync.domain.repository.SyncJobRepository
import dagger.Lazy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncJobRepositoryImpl @Inject constructor(
    private val jobDao: SyncJobDao,
    private val queueDao: TransferQueueDao,
    private val syncController: Lazy<SyncController>,
) : SyncJobRepository {

    override fun observeJobs(): Flow<List<SyncJob>> =
        jobDao.observeAll().map { jobs -> jobs.map { it.toDomain() } }

    override fun observeJob(id: Long): Flow<SyncJob?> =
        jobDao.observeById(id).map { it?.toDomain() }

    override suspend fun getJob(id: Long): SyncJob? = jobDao.get(id)?.toDomain()

    override suspend fun createJob(job: SyncJob): Long {
        val id = jobDao.insert(job.toEntity())
        // Every job re-runs automatically on its configured interval (default 24h).
        syncController.get().schedulePeriodic(id, job.syncIntervalHours)
        return id
    }

    override suspend fun deleteJob(id: Long) {
        syncController.get().cancelPeriodic(id)
        queueDao.deleteForJob(id)
        jobDao.delete(id)
    }

    override suspend fun updateStatus(id: Long, status: SyncStatus, errorMessage: String?) {
        jobDao.updateStatus(id, status.name, errorMessage)
    }
}
