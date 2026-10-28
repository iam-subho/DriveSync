package com.iamsubho.drivesync.data.remote

import android.content.Context
import com.google.api.client.http.HttpExecuteInterceptor
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds (and caches) a Drive client per account. Access tokens come from the same
 * AuthorizationClient flow used to grant Drive access, without requiring an Android
 * system AccountManager entry for the signed-in account.
 */
@Singleton
class DriveServiceFactory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authManager: GoogleAuthManager,
) {
    private val cache = ConcurrentHashMap<String, Drive>()

    fun driveFor(email: String): Drive = cache.getOrPut(email) {
        Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance()) { request ->
            request.interceptor = HttpExecuteInterceptor { outgoing ->
                val token = runBlocking { authManager.driveAccessToken(email) }
                outgoing.headers.authorization = "Bearer $token"
            }
        }
            .setApplicationName("DriveSync")
            .build()
    }

    fun evict(email: String) {
        cache.remove(email)
    }
}
