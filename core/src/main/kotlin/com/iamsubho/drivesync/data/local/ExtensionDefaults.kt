package com.iamsubho.drivesync.data.local

import com.iamsubho.drivesync.domain.model.FileTypeCategory

/**
 * Default extension lists from the product requirement document (full lists — the design
 * prototype shows trimmed subsets for display only).
 */
object ExtensionDefaults {
    val IMAGES = listOf(
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif", "dng", "raw",
        "cr2", "nef", "arw", "rw2", "orf", "raf", "srw", "tif", "tiff", "ico",
    )

    // Requirement list + Android camera formats (mp4/3gp already present; hevc/h265 added).
    val VIDEOS = listOf(
        "mp4", "3gp", "mov", "avi", "mkv", "webm", "mpeg", "mpg", "m4v", "flv",
        "wmv", "mts", "m2ts", "ts", "rmvb", "vob", "asf", "f4v", "ogv", "dv",
        "hevc", "h265",
    )

    // Requirement's document list; archive formats live in ARCHIVES (their own wizard category).
    val DOCUMENTS = listOf(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv", "rtf",
        "odt", "ods", "odp", "epub", "mobi", "xml", "json", "html", "htm", "md",
        "log",
    )

    val AUDIO = listOf(
        "mp3", "wav", "flac", "m4a", "ogg", "aac", "wma", "opus", "amr", "mid",
    )

    val ARCHIVES = listOf("zip", "rar", "7z", "tar", "gz")

    fun forCategory(category: FileTypeCategory): List<String> = when (category) {
        FileTypeCategory.IMAGES -> IMAGES
        FileTypeCategory.VIDEOS -> VIDEOS
        FileTypeCategory.DOCUMENTS -> DOCUMENTS
        FileTypeCategory.AUDIO -> AUDIO
        FileTypeCategory.ARCHIVES -> ARCHIVES
        else -> emptyList()
    }
}
