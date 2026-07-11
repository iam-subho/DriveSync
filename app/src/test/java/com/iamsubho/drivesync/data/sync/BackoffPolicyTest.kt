package com.iamsubho.drivesync.data.sync

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BackoffPolicyTest {

    @Test
    fun `delay doubles per attempt starting at 30 seconds`() {
        assertThat(BackoffPolicy.nextDelayMs(0)).isEqualTo(30_000L)
        assertThat(BackoffPolicy.nextDelayMs(1)).isEqualTo(60_000L)
        assertThat(BackoffPolicy.nextDelayMs(2)).isEqualTo(120_000L)
        assertThat(BackoffPolicy.nextDelayMs(3)).isEqualTo(240_000L)
    }

    @Test
    fun `delay is capped at 30 minutes`() {
        assertThat(BackoffPolicy.nextDelayMs(10)).isEqualTo(1_800_000L)
        assertThat(BackoffPolicy.nextDelayMs(30)).isEqualTo(1_800_000L)
    }

    @Test
    fun `max attempts is five`() {
        assertThat(BackoffPolicy.MAX_ATTEMPTS).isEqualTo(5)
    }
}
