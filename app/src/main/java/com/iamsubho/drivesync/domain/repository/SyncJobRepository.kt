package com.iamsubho.drivesync.domain.repository

import com.iamsubho.drivesync.domain.model.SyncJob
import com.iamsubho.drivesync.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

interface SyncJobRepository {
    fun observeJobs(): Flow<List<SyncJob>>
    fun observeJob(id: Long): Flow<SyncJob?>
    suspend fun getJob(id: Long): SyncJob?
    suspend fun createJob(job: SyncJob): Long
    suspend fun deleteJob(id: Long)
    suspend fun updateStatus(id: Long, status: SyncStatus, errorMessage: String? = null)
}
