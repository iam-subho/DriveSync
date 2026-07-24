package com.iamsubho.drivesync.desktop.auth

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.drive.DriveScopes
import java.io.File

/**
 * Google installed-app OAuth: [signIn] opens the system browser and waits on a loopback
 * port; tokens are persisted under tokens/ and refresh silently afterwards.
 */
class DesktopAuth(baseDir: File) {

    private val tokenDir = File(baseDir, "tokens")

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

    /** Interactive sign-in (blocks; call from a background dispatcher). */
    fun signIn(clientId: String, clientSecret: String): Credential {
        val receiver = LocalServerReceiver.Builder().setPort(-1).build() // any free port
        return AuthorizationCodeInstalledApp(flow(clientId, clientSecret), receiver).authorize("user")
    }

    /** Previously stored credential, or null when the user never signed in. */
    fun loadExisting(clientId: String, clientSecret: String): Credential? =
        try {
            flow(clientId, clientSecret).loadCredential("user")
        } catch (e: Exception) {
            null
        }

    fun signOut() {
        tokenDir.deleteRecursively()
    }
}
