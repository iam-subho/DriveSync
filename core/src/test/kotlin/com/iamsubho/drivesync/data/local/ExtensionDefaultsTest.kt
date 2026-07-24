package com.iamsubho.drivesync.data.local

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ExtensionDefaultsTest {

    @Test
    fun `image defaults contain all required extensions`() {
        assertThat(ExtensionDefaults.IMAGES).containsExactly(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif", "dng", "raw",
            "cr2", "nef", "arw", "rw2", "orf", "raf", "srw", "tif", "tiff", "ico",
        )
    }

    @Test
    fun `video defaults contain requirement list plus camera formats deduped`() {
        assertThat(ExtensionDefaults.VIDEOS).containsExactly(
            "mp4", "3gp", "mov", "avi", "mkv", "webm", "mpeg", "mpg", "m4v", "flv",
            "wmv", "mts", "m2ts", "ts", "rmvb", "vob", "asf", "f4v", "ogv", "dv",
            "hevc", "h265",
        )
    }

    @Test
    fun `document defaults contain all required extensions`() {
        assertThat(ExtensionDefaults.DOCUMENTS).containsExactly(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "rtf",
            "odt", "ods", "odp", "epub", "mobi", "xml", "json", "html", "htm", "md",
            "log",
        )
    }

    @Test
    fun `audio and archive defaults are non-empty`() {
        assertThat(ExtensionDefaults.AUDIO).containsAtLeast("mp3", "wav", "flac", "m4a", "ogg")
        assertThat(ExtensionDefaults.ARCHIVES).containsExactly("zip", "rar", "7z", "tar", "gz")
    }

    @Test
    fun `no list has duplicates and all entries are lowercase`() {
        listOf(
            ExtensionDefaults.IMAGES, ExtensionDefaults.VIDEOS, ExtensionDefaults.DOCUMENTS,
            ExtensionDefaults.AUDIO, ExtensionDefaults.ARCHIVES,
        ).forEach { list ->
            assertThat(list).containsNoDuplicates()
            list.forEach { ext -> assertThat(ext).isEqualTo(ext.lowercase()) }
        }
    }
}
