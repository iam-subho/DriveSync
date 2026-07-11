package com.iamsubho.drivesync.worker

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.iamsubho.drivesync.DriveSyncApp
import com.iamsubho.drivesync.MainActivity
import com.iamsubho.drivesync.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private fun contentIntent(): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun progressNotification(jobName: String, currentFile: String?, percent: Int): Notification =
        NotificationCompat.Builder(context, DriveSyncApp.SYNC_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("Syncing $jobName")
            .setContentText(currentFile ?: "Preparing…")
            .setProgress(100, percent.coerceIn(0, 100), currentFile == null)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent())
            .build()

    fun completionNotification(jobName: String, success: Boolean, detail: String?) {
        if (!canNotify()) return
        val notification = NotificationCompat.Builder(context, DriveSyncApp.SYNC_CHANNEL_ID)
            .setSmallIcon(
                if (success) android.R.drawable.stat_sys_upload_done
                else android.R.drawable.stat_notify_error,
            )
            .setContentTitle(if (success) "Sync complete" else "Sync failed")
            .setContentText(detail ?: jobName)
            .setAutoCancel(true)
            .setContentIntent(contentIntent())
            .build()
        NotificationManagerCompat.from(context)
            .notify(COMPLETION_ID_OFFSET + jobName.hashCode(), notification)
    }

    private fun canNotify(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED || android.os.Build.VERSION.SDK_INT < 33

    companion object {
        private const val COMPLETION_ID_OFFSET = 100_000
        fun progressNotificationId(jobId: Long): Int = jobId.toInt()
    }
}
