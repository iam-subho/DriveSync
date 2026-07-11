package com.iamsubho.drivesync.presentation.navigation

import android.net.Uri

object Routes {
    const val HOME = "home"
    const val WIZARD = "wizard"
    const val SETTINGS = "settings"
    const val DETAILS = "details/{jobId}"
    const val BROWSER = "browser/{email}"
    const val BROWSER_FILES = "browser/{email}/folder/{folderId}?folderName={folderName}"

    fun details(jobId: Long) = "details/$jobId"

    fun browser(email: String) = "browser/${Uri.encode(email)}"

    fun browserFiles(email: String, folderId: String, folderName: String) =
        "browser/${Uri.encode(email)}/folder/${Uri.encode(folderId)}" +
            "?folderName=${Uri.encode(folderName)}"
}
