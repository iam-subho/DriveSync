package com.iamsubho.drivesync.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.iamsubho.drivesync.data.sync.JobLocks
import com.iamsubho.drivesync.data.sync.SyncEngine
import com.iamsubho.drivesync.data.sync.SyncProgressBus
import com.iamsubho.drivesync.domain.model.SyncStatus
import com.iamsubho.drivesync.domain.repository.SyncJobRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Executes one sync run for one job as foreground work. Unique work name: "sync-job-{id}".
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val engine: SyncEngine,
    private val progressBus: SyncProgressBus,
    private val notifications: SyncNotifications,
    private val jobRepository: SyncJobRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val jobId = inputData.getLong(KEY_JOB_ID, -1L)
        if (jobId == -1L) return Result.failure()
        val job = jobRepository.getJob(jobId) ?: return Result.failure()
        val isPeriodic = inputData.getBoolean(KEY_IS_PERIODIC, false)

        // Scheduled runs respect a user-initiated pause; only Start lifts it.
        if (isPeriodic && job.status == SyncStatus.PAUSED) return Result.success()

        // Manual and periodic runs are separate unique works — never sync the same job twice.
        if (!JobLocks.tryAcquire(jobId)) return Result.success()

        jobRepository.updateStatus(jobId, SyncStatus.SYNCING)

        // Android 12+ forbids foreground promotion while the app is in the background
        // (e.g. a WorkManager retry firing later). The worker may still run un-promoted
        // for a while, so log and continue instead of crashing into a stuck "Syncing".
        try {
            setForeground(createForegroundInfo(jobId, job.name, null, 0))
        } catch (e: Exception) {
            android.util.Log.w(TAG, "setForeground rejected (app in background?) — continuing without it", e)
        }

        return try {
            val result = engine.run(jobId) { progress ->
                progressBus.update(jobId, progress)
                runCatching {
                    setForegroundAsync(
                        createForegroundInfo(jobId, job.name, progress.currentFile, progress.percent),
                    )
                }
            }
            android.util.Log.i(TAG, "job $jobId finished with $result")
            when (result) {
                is SyncEngine.Result.Success -> {
                    notifications.completionNotification(job.name, success = true, detail = null)
                    Result.success()
                }
                is SyncEngine.Result.Retry -> Result.retry()
                is SyncEngine.Result.AuthNeeded -> {
                    notifications.completionNotification(
                        job.name, success = false, detail = "Account needs re-authorization",
                    )
                    Result.failure()
                }
                is SyncEngine.Result.Error -> {
                    notifications.completionNotification(job.name, success = false, detail = result.message)
                    Result.failure()
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e // pause / system stop
        } catch (e: Throwable) {
            // Last line of defense: surface the crash as a visible job error.
            android.util.Log.e(TAG, "job $jobId crashed", e)
            jobRepository.updateStatus(jobId, SyncStatus.ERROR, e.message ?: e.javaClass.simpleName)
            notifications.completionNotification(job.name, success = false, detail = e.message)
            Result.failure()
        } finally {
            // Runs on success, failure, and cancellation (pause/constraint loss):
            // release interrupted rows and clear the live progress card.
            JobLocks.release(jobId)
            withContext(NonCancellable) {
                engine.resetInterrupted(jobId)
                progressBus.update(jobId, null)
            }
        }
    }

    private fun createForegroundInfo(
        jobId: Long,
        jobName: String,
        currentFile: String?,
        percent: Int,
    ): ForegroundInfo {
        val notification = notifications.progressNotification(jobName, currentFile, percent)
        val id = SyncNotifications.progressNotificationId(jobId)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(id, notification)
        }
    }

    companion object {
        private const val TAG = "DriveSyncEngine"
        const val KEY_JOB_ID = "job_id"
        const val KEY_IS_PERIODIC = "is_periodic"
        fun uniqueName(jobId: Long) = "sync-job-$jobId"
        fun periodicName(jobId: Long) = "sync-job-periodic-$jobId"
    }
}
