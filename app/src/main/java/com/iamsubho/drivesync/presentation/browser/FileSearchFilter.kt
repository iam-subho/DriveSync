package com.iamsubho.drivesync.presentation.browser

import com.iamsubho.drivesync.domain.model.RemoteFile

/** Pure name + day filtering for the See Files browser. */
object FileSearchFilter {

    /**
     * @param dayStartMillis inclusive start of the picked day (local time), null = no date filter
     * @param dayEndMillis exclusive end of the picked day, null = no date filter
     */
    fun filter(
        files: List<RemoteFile>,
        query: String,
        dayStartMillis: Long?,
        dayEndMillis: Long?,
    ): List<RemoteFile> {
        val trimmed = query.trim()
        return files.filter { file ->
            val nameMatches = trimmed.isEmpty() || file.name.contains(trimmed, ignoreCase = true)
            val dateMatches = if (dayStartMillis == null || dayEndMillis == null) {
                true
            } else {
                val date = file.deviceCreatedAt ?: file.modifiedAt
                date in dayStartMillis until dayEndMillis
            }
            nameMatches && dateMatches
        }
    }
}
