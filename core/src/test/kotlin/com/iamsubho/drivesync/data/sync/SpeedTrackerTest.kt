package com.iamsubho.drivesync.data.sync

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SpeedTrackerTest {

    @Test
    fun `empty tracker reports zero speed`() {
        val tracker = SpeedTracker(windowMs = 5_000)
        assertThat(tracker.speedBytesPerSec(nowMs = 10_000)).isEqualTo(0L)
    }

    @Test
    fun `speed reflects bytes within the window`() {
        val tracker = SpeedTracker(windowMs = 5_000)
        tracker.onBytes(1_000_000, nowMs = 1_000)
        tracker.onBytes(1_000_000, nowMs = 2_000)
        // 2 MB over a 5s window = 400 KB/s
        assertThat(tracker.speedBytesPerSec(nowMs = 5_000)).isEqualTo(400_000L)
    }

    @Test
    fun `samples older than the window are evicted`() {
        val tracker = SpeedTracker(windowMs = 5_000)
        tracker.onBytes(10_000_000, nowMs = 1_000)
        tracker.onBytes(500_000, nowMs = 9_000)
        // At t=10s the first sample (t=1s) is outside the 5s window.
        assertThat(tracker.speedBytesPerSec(nowMs = 10_000)).isEqualTo(100_000L)
    }
}
