package com.iamsubho.drivesync.domain.repository

import com.iamsubho.drivesync.domain.model.SyncProgress
import kotlinx.coroutines.flow.Flow

/** Starts/pauses sync jobs (backed by WorkManager) and exposes live progress. */
interface SyncController {
    fun observeProgress(jobId: Long): Flow<SyncProgress?>
    fun start(jobId: Long)
    fun pause(jobId: Long)

    /** (Re)registers the job's automatic periodic run. */
    fun schedulePeriodic(jobId: Long, intervalHours: Int)
    fun cancelPeriodic(jobId: Long)
}
