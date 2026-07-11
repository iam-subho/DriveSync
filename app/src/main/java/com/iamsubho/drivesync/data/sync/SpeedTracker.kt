package com.iamsubho.drivesync.data.sync

import java.util.ArrayDeque

/** Rolling-window transfer speed estimator. Not thread-safe; used from the engine's single loop. */
class SpeedTracker(private val windowMs: Long = 5_000L) {

    private data class Sample(val atMs: Long, val bytes: Long)

    private val samples = ArrayDeque<Sample>()

    fun onBytes(bytes: Long, nowMs: Long) {
        samples.addLast(Sample(nowMs, bytes))
        evict(nowMs)
    }

    fun speedBytesPerSec(nowMs: Long): Long {
        evict(nowMs)
        if (samples.isEmpty()) return 0L
        val total = samples.sumOf { it.bytes }
        return total * 1000 / windowMs
    }

    private fun evict(nowMs: Long) {
        while (samples.isNotEmpty() && samples.first().atMs < nowMs - windowMs) {
            samples.removeFirst()
        }
    }
}
