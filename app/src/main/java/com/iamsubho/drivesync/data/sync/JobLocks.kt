package com.iamsubho.drivesync.data.sync

import java.util.concurrent.ConcurrentHashMap

/**
 * In-process per-job run guard. Manual runs ("sync-job-{id}") and periodic runs
 * ("sync-job-periodic-{id}") are separate WorkManager unique works, so both could fire at
 * once — the second one bails out cleanly instead of double-syncing.
 */
object JobLocks {
    private val running = ConcurrentHashMap.newKeySet<Long>()

    /** @return true when the lock was acquired; false when the job is already running. */
    fun tryAcquire(jobId: Long): Boolean = running.add(jobId)

    fun release(jobId: Long) {
        running.remove(jobId)
    }
}
