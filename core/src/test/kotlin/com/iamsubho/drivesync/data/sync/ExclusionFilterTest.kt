package com.iamsubho.drivesync.data.sync

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ExclusionFilterTest {

    private val excluded = listOf("Sent", "Private/Temp")

    @Test
    fun `files inside an excluded folder are excluded`() {
        assertThat(ExclusionFilter.isExcluded("Sent/img.jpg", excluded)).isTrue()
        assertThat(ExclusionFilter.isExcluded("Sent/nested/deep.mp4", excluded)).isTrue()
        assertThat(ExclusionFilter.isExcluded("Private/Temp/x.txt", excluded)).isTrue()
    }

    @Test
    fun `files outside excluded folders are kept`() {
        assertThat(ExclusionFilter.isExcluded("img.jpg", excluded)).isFalse()
        assertThat(ExclusionFilter.isExcluded("Received/img.jpg", excluded)).isFalse()
        assertThat(ExclusionFilter.isExcluded("Private/keep.txt", excluded)).isFalse()
    }

    @Test
    fun `similar prefix folder names are not excluded`() {
        assertThat(ExclusionFilter.isExcluded("SentX/img.jpg", excluded)).isFalse()
        assertThat(ExclusionFilter.isExcluded("Sentimental/a.png", excluded)).isFalse()
    }

    @Test
    fun `empty exclusion list keeps everything`() {
        assertThat(ExclusionFilter.isExcluded("Sent/img.jpg", emptyList())).isFalse()
    }

    @Test
    fun `folder path itself matches for scanner subtree skipping`() {
        assertThat(ExclusionFilter.isExcludedDir("Sent", excluded)).isTrue()
        assertThat(ExclusionFilter.isExcludedDir("Sent/nested", excluded)).isTrue()
        assertThat(ExclusionFilter.isExcludedDir("Received", excluded)).isFalse()
    }
}
