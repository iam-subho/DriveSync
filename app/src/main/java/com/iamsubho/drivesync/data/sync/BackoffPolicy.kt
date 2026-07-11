package com.iamsubho.drivesync.data.sync

/** Exponential backoff schedule for failed transfers: 30s, 1m, 2m, 4m… capped at 30 minutes. */
object BackoffPolicy {
    const val MAX_ATTEMPTS = 5
    private const val BASE_DELAY_MS = 30_000L
    private const val MAX_DELAY_MS = 30L * 60_000L

    fun nextDelayMs(attemptCount: Int): Long {
        val shift = attemptCount.coerceIn(0, 20)
        return (BASE_DELAY_MS shl shift).coerceAtMost(MAX_DELAY_MS)
    }
}
