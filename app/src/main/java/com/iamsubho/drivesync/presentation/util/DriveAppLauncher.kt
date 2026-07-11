package com.iamsubho.drivesync.presentation.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Opens files in the Google Drive app so previews never happen inside this app. */
object DriveAppLauncher {

    const val DRIVE_PACKAGE = "com.google.android.apps.docs"

    /** @return false when the Google Drive app is not installed. */
    fun openFileInDriveApp(context: Context, fileId: String): Boolean {
        val intent = Intent(Intent.ACTION_VIEW)
            .setData(Uri.parse("https://drive.google.com/file/d/$fileId/view"))
            .setPackage(DRIVE_PACKAGE)
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    fun openPlayStoreForDrive(context: Context) {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$DRIVE_PACKAGE"))
        try {
            context.startActivity(market)
        } catch (e: ActivityNotFoundException) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$DRIVE_PACKAGE"),
                ),
            )
        }
    }
}
