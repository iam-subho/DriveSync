package com.iamsubho.drivesync.data.sync

import com.google.common.truth.Truth.assertThat
import com.iamsubho.drivesync.domain.model.FileTypeCategory
import com.iamsubho.drivesync.domain.model.LimitMode
import org.junit.Test

class FileFilterTest {

    private val extensions = mapOf(
        FileTypeCategory.IMAGES to setOf("jpg", "png"),
        FileTypeCategory.VIDEOS to setOf("mp4"),
        FileTypeCategory.DOCUMENTS to setOf("pdf"),
        FileTypeCategory.AUDIO to setOf("mp3"),
        FileTypeCategory.ARCHIVES to setOf("zip"),
    )

    private fun filter(
        types: Set<FileTypeCategory>,
        custom: Set<String> = emptySet(),
    ) = FileFilter(types, extensions, custom)

    @Test
    fun `ALL matches everything including unknown and extensionless files`() {
        val f = filter(setOf(FileTypeCategory.ALL))
        assertThat(f.matchesType("photo.jpg")).isTrue()
        assertThat(f.matchesType("weird.xyz")).isTrue()
        assertThat(f.matchesType("README")).isTrue()
    }

    @Test
    fun `extension matching is case-insensitive`() {
        val f = filter(setOf(FileTypeCategory.IMAGES))
        assertThat(f.matchesType("IMG.JPG")).isTrue()
        assertThat(f.matchesType("img.jpg")).isTrue()
        assertThat(f.matchesType("clip.mp4")).isFalse()
    }

    @Test
    fun `custom extensions match only when CUSTOM selected`() {
        val withCustom = filter(setOf(FileTypeCategory.CUSTOM), setOf("psd"))
        assertThat(withCustom.matchesType("art.psd")).isTrue()
        assertThat(withCustom.matchesType("photo.jpg")).isFalse()

        val withoutCustom = filter(setOf(FileTypeCategory.IMAGES), setOf("psd"))
        assertThat(withoutCustom.matchesType("art.psd")).isFalse()
    }

    @Test
    fun `file without extension fails unless ALL`() {
        val f = filter(setOf(FileTypeCategory.IMAGES, FileTypeCategory.DOCUMENTS))
        assertThat(f.matchesType("Makefile")).isFalse()
    }

    @Test
    fun `multiple categories match union of extensions`() {
        val f = filter(setOf(FileTypeCategory.IMAGES, FileTypeCategory.VIDEOS))
        assertThat(f.matchesType("a.png")).isTrue()
        assertThat(f.matchesType("b.mp4")).isTrue()
        assertThat(f.matchesType("c.pdf")).isFalse()
    }

    @Test
    fun `size limit SKIP_ABOVE keeps files at or below the limit`() {
        val mb = 1024L * 1024L
        assertThat(FileFilter.passesSizeLimit(6 * mb, 5, LimitMode.SKIP_ABOVE)).isFalse()
        assertThat(FileFilter.passesSizeLimit(4 * mb, 5, LimitMode.SKIP_ABOVE)).isTrue()
        assertThat(FileFilter.passesSizeLimit(5 * mb, 5, LimitMode.SKIP_ABOVE)).isTrue()
    }

    @Test
    fun `size limit ONLY_ABOVE keeps files strictly above the limit`() {
        val mb = 1024L * 1024L
        assertThat(FileFilter.passesSizeLimit(6 * mb, 5, LimitMode.ONLY_ABOVE)).isTrue()
        assertThat(FileFilter.passesSizeLimit(5 * mb, 5, LimitMode.ONLY_ABOVE)).isFalse()
        assertThat(FileFilter.passesSizeLimit(4 * mb, 5, LimitMode.ONLY_ABOVE)).isFalse()
    }

    @Test
    fun `null limit always passes`() {
        assertThat(FileFilter.passesSizeLimit(Long.MAX_VALUE, null, LimitMode.SKIP_ABOVE)).isTrue()
        assertThat(FileFilter.passesSizeLimit(0, null, LimitMode.ONLY_ABOVE)).isTrue()
    }

    @Test
    fun `skip reasons name the limit`() {
        assertThat(FileFilter.sizeLimitSkipReason(500, LimitMode.SKIP_ABOVE))
            .isEqualTo("Above size limit (500 MB)")
        assertThat(FileFilter.sizeLimitSkipReason(500, LimitMode.ONLY_ABOVE))
            .isEqualTo("Below size threshold (500 MB)")
    }
}
