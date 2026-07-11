package com.iamsubho.drivesync.presentation.common

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FormattersTest {

    @Test
    fun `formatBytes renders MB with one decimal`() {
        assertThat(Formatters.formatBytes(3_355_443)) .isEqualTo("3.2 MB")
        assertThat(Formatters.formatBytes(193_986_560)).isEqualTo("185.0 MB")
    }

    @Test
    fun `formatBytes renders KB below one MB`() {
        assertThat(Formatters.formatBytes(860_160)).isEqualTo("840 KB")
        assertThat(Formatters.formatBytes(512)).isEqualTo("1 KB")
    }

    @Test
    fun `formatBytes renders GB above 1024 MB`() {
        assertThat(Formatters.formatBytes(2L * 1024 * 1024 * 1024)).isEqualTo("2.0 GB")
    }

    @Test
    fun `formatStorageLabel matches prototype copy`() {
        val gb = 1024L * 1024 * 1024
        assertThat(Formatters.formatStorageLabel(10 * gb, 15 * gb))
            .isEqualTo("10 GB of 15 GB used")
    }

    @Test
    fun `formatSpeed renders MB per second`() {
        assertThat(Formatters.formatSpeed(4_404_019)).isEqualTo("4.2 MB/s")
        assertThat(Formatters.formatSpeed(0)).isEqualTo("0.0 MB/s")
    }

    @Test
    fun `formatEta matches prototype copy`() {
        assertThat(Formatters.formatEta(180)).isEqualTo("~3 min remaining")
        assertThat(Formatters.formatEta(59)).isEqualTo("~1 min remaining")
        assertThat(Formatters.formatEta(3_700)).isEqualTo("~62 min remaining")
    }

    @Test
    fun `accountColorIndex is deterministic`() {
        val a = Formatters.accountColorIndex("john@gmail.com", 5)
        val b = Formatters.accountColorIndex("john@gmail.com", 5)
        assertThat(a).isEqualTo(b)
        assertThat(a).isAtLeast(0)
        assertThat(a).isLessThan(5)
    }
}
