package com.iamsubho.drivesync.domain.repository

import com.iamsubho.drivesync.domain.model.DriveAccount
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun observeAccounts(): Flow<List<DriveAccount>>

    /**
     * Runs the full add-account flow: Google sign-in, Drive scope authorization, quota fetch,
     * then persists the account. [activityContext] must be an Activity context (Credential
     * Manager shows UI). Throws [DriveConsentRequiredException] when the Drive consent screen
     * must be launched by the caller first.
     */
    suspend fun addAccount(activityContext: android.content.Context): DriveAccount

    /** Completes account setup after the user resolves a [DriveConsentRequiredException]. */
    suspend fun finishAddAccount(email: String, displayName: String, photoUrl: String?): DriveAccount

    /**
     * Fallback sign-in for devices where Credential Manager self-cancels: an Intent for the
     * classic Google account chooser, and the matching result handler.
     */
    fun legacySignInIntent(): android.content.Intent
    suspend fun addAccountFromLegacyResult(data: android.content.Intent?): DriveAccount

    suspend fun removeAccount(email: String)
    suspend fun refreshStorage(email: String)
}

/** Thrown when Drive authorization needs user consent; launch [pendingIntent] and retry. */
class DriveConsentRequiredException(
    val pendingIntent: android.app.PendingIntent,
    val email: String,
    val displayName: String,
    val photoUrl: String?,
) : Exception("Drive consent required")
