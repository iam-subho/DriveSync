package com.iamsubho.drivesync.desktop.auth

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.drive.DriveScopes
import com.iamsubho.drivesync.desktop.DesktopLog
import java.awt.Desktop
import java.io.File
import java.net.URI

/**
 * Google installed-app OAuth: [signIn] surfaces the consent URL (so the UI can always show
 * it), opens the system browser as robustly as possible, and waits on a loopback port.
 * Tokens persist under tokens/ and refresh silently afterwards.
 */
class DesktopAuth(baseDir: File) {

    private val tokenDir = File(baseDir, "tokens")

    @Volatile
    private var activeReceiver: LocalServerReceiver? = null

    private fun flow(clientId: String, clientSecret: String): GoogleAuthorizationCodeFlow =
        GoogleAuthorizationCodeFlow.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            clientId,
            clientSecret,
            listOf(DriveScopes.DRIVE),
        )
            .setDataStoreFactory(FileDataStoreFactory(tokenDir))
            .setAccessType("offline")
            .build()

    /**
     * Interactive sign-in (blocks until the browser round-trip completes or [cancel] is
     * called). [onAuthUrl] receives the consent URL the moment it is known.
     */
    fun signIn(clientId: String, clientSecret: String, onAuthUrl: (String) -> Unit): Credential {
        val receiver = LocalServerReceiver.Builder().setPort(-1).build()
        activeReceiver = receiver
        try {
            DesktopLog.log("auth: flow built, starting loopback receiver + authorize")
            val app = object : AuthorizationCodeInstalledApp(flow(clientId, clientSecret), receiver) {
                override fun onAuthorization(authorizationUrl: AuthorizationCodeRequestUrl) {
                    val url = authorizationUrl.build()
                    DesktopLog.log("auth: consent URL ready")
                    onAuthUrl(url)
                    openBrowser(url)
                }
            }
            return app.authorize("user")
        } finally {
            activeReceiver = null
        }
    }

    /** Unblocks a pending [signIn] by stopping the loopback receiver. */
    fun cancel() {
        try {
            activeReceiver?.stop()
        } catch (e: Exception) {
            DesktopLog.log("auth: cancel failed", e)
        }
    }

    fun loadExisting(clientId: String, clientSecret: String): Credential? =
        try {
            flow(clientId, clientSecret).loadCredential("user")
        } catch (e: Exception) {
            DesktopLog.log("auth: loadExisting failed", e)
            null
        }

    fun signOut() {
        tokenDir.deleteRecursively()
    }

    companion object {
        /** AWT Desktop first; Windows shell fallback. Failure is non-fatal — the UI shows the URL. */
        fun openBrowser(url: String) {
            try {
                if (Desktop.isDesktopSupported() &&
                    Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)
                ) {
                    Desktop.getDesktop().browse(URI(url))
                    return
                }
            } catch (e: Exception) {
                DesktopLog.log("auth: Desktop.browse failed", e)
            }
            try {
                if (System.getProperty("os.name", "").startsWith("Windows")) {
                    Runtime.getRuntime().exec(arrayOf("rundll32", "url.dll,FileProtocolHandler", url))
                }
            } catch (e: Exception) {
                DesktopLog.log("auth: rundll32 fallback failed", e)
            }
        }
    }
}
