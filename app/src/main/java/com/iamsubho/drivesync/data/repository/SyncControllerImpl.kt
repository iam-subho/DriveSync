package com.iamsubho.drivesync.data.repository

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.iamsubho.drivesync.data.local.dao.SyncJobDao
import com.iamsubho.drivesync.data.local.dao.TransferQueueDao
import com.iamsubho.drivesync.data.sync.SyncProgressBus
import com.iamsubho.drivesync.domain.model.SyncProgress
import com.iamsubho.drivesync.domain.model.SyncStatus
import com.iamsubho.drivesync.domain.repository.SyncController
import com.iamsubho.drivesync.worker.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val jobDao: SyncJobDao,
    private val queueDao: TransferQueueDao,
    private val progressBus: SyncProgressBus,
) : SyncController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun observeProgress(jobId: Long): Flow<SyncProgress?> = progressBus.flow(jobId)

    override fun start(jobId: Long) {
        scope.launch {
            jobDao.updateStatus(jobId, SyncStatus.SYNCING.name, null)
        }
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInputData(workDataOf(SyncWorker.KEY_JOB_ID to jobId))
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            SyncWorker.uniqueName(jobId),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    override fun pause(jobId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(SyncWorker.uniqueName(jobId))
        scope.launch {
            queueDao.resetRunning(jobId)
            jobDao.updateStatus(jobId, SyncStatus.PAUSED.name, null)
            progressBus.update(jobId, null)
        }
    }

    override fun schedulePeriodic(jobId: Long, intervalHours: Int) {
        val request = androidx.work.PeriodicWorkRequestBuilder<SyncWorker>(
            intervalHours.coerceAtLeast(1).toLong(), TimeUnit.HOURS,
        )
            .setInputData(
                workDataOf(
                    SyncWorker.KEY_JOB_ID to jobId,
                    SyncWorker.KEY_IS_PERIODIC to true,
                ),
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SyncWorker.periodicName(jobId),
            androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    override fun cancelPeriodic(jobId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(SyncWorker.periodicName(jobId))
    }
}
