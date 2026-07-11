package com.iamsubho.drivesync.presentation.browser

import com.google.common.truth.Truth.assertThat
import com.iamsubho.drivesync.domain.model.RemoteFile
import org.junit.Test

class FileSearchFilterTest {

    private fun file(name: String, deviceCreatedAt: Long?, modifiedAt: Long = 999L) =
        RemoteFile("id-$name", name, name, 100, modifiedAt, null, deviceCreatedAt)

    private val files = listOf(
        file("IMG_001.jpg", deviceCreatedAt = 1_000),
        file("IMG_002.jpg", deviceCreatedAt = 5_000),
        file("Report.pdf", deviceCreatedAt = 5_500),
        file("legacy.mp4", deviceCreatedAt = null, modifiedAt = 1_500),
    )

    @Test
    fun `empty query and no date returns everything`() {
        assertThat(FileSearchFilter.filter(files, "", null, null)).hasSize(4)
    }

    @Test
    fun `name query matches case-insensitively and trims`() {
        val result = FileSearchFilter.filter(files, "  img  ", null, null)
        assertThat(result.map { it.name }).containsExactly("IMG_001.jpg", "IMG_002.jpg")
    }

    @Test
    fun `day range filters on device created date`() {
        val result = FileSearchFilter.filter(files, "", 5_000, 6_000)
        assertThat(result.map { it.name }).containsExactly("IMG_002.jpg", "Report.pdf")
    }

    @Test
    fun `day range is start-inclusive end-exclusive`() {
        assertThat(FileSearchFilter.filter(files, "", 5_000, 5_500).map { it.name })
            .containsExactly("IMG_002.jpg")
    }

    @Test
    fun `files without device date fall back to modified time`() {
        val result = FileSearchFilter.filter(files, "", 1_400, 1_600)
        assertThat(result.map { it.name }).containsExactly("legacy.mp4")
    }

    @Test
    fun `name and date combine with AND`() {
        val result = FileSearchFilter.filter(files, "report", 5_000, 6_000)
        assertThat(result.map { it.name }).containsExactly("Report.pdf")
        assertThat(FileSearchFilter.filter(files, "img", 5_500, 6_000)).isEmpty()
    }
}
