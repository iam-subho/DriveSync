package com.iamsubho.drivesync.presentation.wizard

import com.iamsubho.drivesync.domain.model.DriveAccount
import com.iamsubho.drivesync.domain.model.FileTypeCategory
import com.iamsubho.drivesync.domain.model.LimitMode
import com.iamsubho.drivesync.domain.model.RemoteFolder
import com.iamsubho.drivesync.domain.model.SyncDirection

data class LimitUi(
    val custom: Boolean = false,
    val mb: String = "500",
    val mode: LimitMode = LimitMode.SKIP_ABOVE,
) {
    val isValid: Boolean get() = !custom || (mb.toIntOrNull() ?: 0) > 0
    val mbValue: Int? get() = if (custom) mb.toIntOrNull() else null

    fun summaryLabel(): String = when {
        !custom -> "No limit"
        mode == LimitMode.SKIP_ABOVE -> "$mb MB — skip above"
        else -> "$mb MB — only above"
    }
}

data class WizardUiState(
    val step: Int = 1,
    val accounts: List<DriveAccount> = emptyList(),
    val selectedAccountEmail: String? = null,
    val driveFolders: List<RemoteFolder> = emptyList(),
    val foldersLoading: Boolean = false,
    val folderError: String? = null,
    val selectedFolderId: String? = null,
    val selectedFolderName: String? = null,
    val localFolderUri: String? = null,
    val localFolderName: String? = null,
    val localFolderPath: String? = null,
    val batteryOk: Boolean = false,
    val types: Set<FileTypeCategory> = setOf(FileTypeCategory.IMAGES, FileTypeCategory.VIDEOS),
    val customExtensions: String = "",
    val upLimit: LimitUi = LimitUi(),
    val downLimit: LimitUi = LimitUi(),
    val direction: SyncDirection = SyncDirection.TWO_WAY,
    val intervalHours: Int = 24,
    val intervalCustom: Boolean = false,
    val customHours: String = "48",
    val deleteAfterUpload: Boolean = false,
    val jobName: String = "",
    val creating: Boolean = false,
) {
    val stepTitle: String
        get() = STEP_TITLES[step - 1]

    val canNext: Boolean
        get() = when (step) {
            1 -> selectedAccountEmail != null
            2 -> selectedFolderId != null
            3 -> localFolderUri != null
            4 -> batteryOk
            5 -> types.isNotEmpty() && !(types == setOf(FileTypeCategory.CUSTOM) && customExtensions.isBlank())
            6 -> upLimit.isValid && downLimit.isValid
            7 -> true
            8 -> !intervalCustom || (customHours.toIntOrNull() ?: 0) >= 1
            9 -> jobName.isNotBlank() && !creating
            else -> false
        }

    val allFilesSelected: Boolean get() = FileTypeCategory.ALL in types

    /** Hours the job re-runs on; falls back to 24 while a custom value is being typed. */
    val effectiveIntervalHours: Int
        get() = if (intervalCustom) customHours.toIntOrNull() ?: 24 else intervalHours

    /** Delete-after-upload never applies to download-only jobs. */
    val effectiveDeleteAfterUpload: Boolean
        get() = deleteAfterUpload && direction != SyncDirection.DOWNLOAD_ONLY

    companion object {
        val STEP_TITLES = listOf(
            "Account", "Drive folder", "Local folder", "Battery",
            "File types", "Size limits", "Direction", "Schedule", "Review",
        )
        const val LAST_STEP = 9
        val INTERVAL_PRESETS = listOf(2, 4, 8, 12, 24)
    }
}
