package com.iamsubho.drivesync.data.remote

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds (and caches) a Drive client per account. GoogleAccountCredential silently mints
 * and refreshes OAuth access tokens through Play Services — safe to use inside workers
 * with no UI, as long as the user granted the Drive scope during account setup.
 */
@Singleton
class DriveServiceFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val cache = ConcurrentHashMap<String, Drive>()

    fun driveFor(email: String): Drive = cache.getOrPut(email) {
        val credential = GoogleAccountCredential
            .usingOAuth2(context, listOf(DriveScopes.DRIVE))
            .apply { selectedAccountName = email }
        Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
            .setApplicationName("DriveSync")
            .build()
    }

    fun evict(email: String) {
        cache.remove(email)
    }
}
