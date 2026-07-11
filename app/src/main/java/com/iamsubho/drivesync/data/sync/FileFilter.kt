package com.iamsubho.drivesync.data.sync

import com.iamsubho.drivesync.domain.model.FileTypeCategory
import com.iamsubho.drivesync.domain.model.LimitMode

/**
 * Decides whether a file participates in a sync job based on the job's file-type
 * selection and (statically) whether a file passes a size-limit rule.
 */
class FileFilter(
    private val fileTypes: Set<FileTypeCategory>,
    private val extensionsByCategory: Map<FileTypeCategory, Set<String>>,
    private val customExtensions: Set<String>,
) {
    fun matchesType(fileName: String): Boolean {
        if (FileTypeCategory.ALL in fileTypes) return true
        val ext = fileName.substringAfterLast('.', "").lowercase()
        if (ext.isEmpty()) return false
        return fileTypes.any { category ->
            when (category) {
                FileTypeCategory.ALL -> true
                FileTypeCategory.CUSTOM -> ext in customExtensions
                else -> ext in (extensionsByCategory[category] ?: emptySet())
            }
        }
    }

    companion object {
        private const val BYTES_PER_MB = 1024L * 1024L

        fun passesSizeLimit(sizeBytes: Long, limitMb: Int?, mode: LimitMode): Boolean {
            if (limitMb == null) return true
            val limitBytes = limitMb * BYTES_PER_MB
            return when (mode) {
                LimitMode.SKIP_ABOVE -> sizeBytes <= limitBytes
                LimitMode.ONLY_ABOVE -> sizeBytes > limitBytes
            }
        }

        fun sizeLimitSkipReason(limitMb: Int, mode: LimitMode): String = when (mode) {
            LimitMode.SKIP_ABOVE -> "Above size limit ($limitMb MB)"
            LimitMode.ONLY_ABOVE -> "Below size threshold ($limitMb MB)"
        }
    }
}
