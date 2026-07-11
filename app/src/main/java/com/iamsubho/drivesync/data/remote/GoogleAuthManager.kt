package com.iamsubho.drivesync.data.remote

import android.accounts.Account
import android.app.PendingIntent
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.api.services.drive.DriveScopes
import com.iamsubho.drivesync.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google identity plumbing:
 *  - [signIn] shows the Credential Manager account sheet and returns the picked profile.
 *  - [authorizeDrive] requests the Drive scope for that account via AuthorizationClient;
 *    when consent UI is required the caller launches the returned PendingIntent and retries.
 * Background access tokens are minted separately by [DriveServiceFactory].
 */
@Singleton
class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    data class SignInResult(val email: String, val displayName: String, val photoUrl: String?)

    sealed interface AuthorizeResult {
        data object Granted : AuthorizeResult
        data class NeedsConsent(val pendingIntent: PendingIntent) : AuthorizeResult
    }

    companion object {
        const val TAG = "DriveSyncAuth"

        /** Maps Credential Manager failures to actionable, user-readable text. */
        fun friendlyMessage(e: Throwable): String {
            val raw = e.message.orEmpty()
            if (e is com.google.android.gms.common.api.ApiException) {
                return when (e.statusCode) {
                    12501 -> "Sign-in cancelled."
                    12500, 10 -> "Google sign-in failed (code ${e.statusCode}). In Google Cloud " +
                        "Console, register an Android OAuth client for package " +
                        "com.iamsubho.drivesync with this build's SHA-1, and use the WEB client " +
                        "ID in local.properties."
                    7 -> "Network error while contacting Google. Check the connection and try again."
                    else -> "Google sign-in failed (code ${e.statusCode})."
                }
            }
            return when {
                raw.contains("Developer console", ignoreCase = true) ||
                    raw.contains("28444") || raw.contains("10:") ->
                    "Google rejected the app's configuration. In Google Cloud Console, make sure an " +
                        "Android OAuth client exists for package com.iamsubho.drivesync with this " +
                        "build's SHA-1, and that DRIVE_SYNC_WEB_CLIENT_ID is the WEB client ID."
                e is androidx.credentials.exceptions.NoCredentialException ->
                    "No Google account is available on this device. Add one in system settings and try again."
                raw.contains("network", ignoreCase = true) ->
                    "Network error while contacting Google. Check the connection and try again."
                else -> "Google sign-in failed: ${raw.ifBlank { e.javaClass.simpleName }}"
            }
        }
    }

    suspend fun signIn(activityContext: Context): SignInResult {
        check(!BuildConfig.WEB_CLIENT_ID.startsWith("REPLACE_ME")) {
            "Google sign-in is not configured: set DRIVE_SYNC_WEB_CLIENT_ID in local.properties (see README)"
        }
        val credentialManager = CredentialManager.create(activityContext)
        // Explicit button-tap flow (full account chooser). The One Tap bottom sheet
        // (GetGoogleIdOption) self-cancels under Play Services' dismissal cooldown and on
        // some OEM skins, surfacing as TYPE_USER_CANCELED without user action.
        val signInOption = GetSignInWithGoogleOption.Builder(BuildConfig.WEB_CLIENT_ID).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        val credential = try {
            credentialManager.getCredential(activityContext, request).credential
        } catch (e: androidx.credentials.exceptions.GetCredentialException) {
            android.util.Log.e(TAG, "getCredential failed: type=${e.type} message=${e.message}", e)
            throw e
        }
        check(
            credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
        ) { "Unexpected credential type: ${credential.type}" }

        val googleId = GoogleIdTokenCredential.createFrom(credential.data)
        return SignInResult(
            email = googleId.id,
            displayName = googleId.displayName ?: googleId.id.substringBefore('@'),
            photoUrl = googleId.profilePictureUri?.toString(),
        )
    }

    /**
     * Classic Google sign-in intent (full account chooser dialog). Used as fallback when
     * Credential Manager self-cancels (Play Services masks config/internal errors as
     * TYPE_USER_CANCELED there; this API surfaces the real ApiException status code).
     * Requests the Drive scope in the same dialog, so consent happens in one step.
     */
    @Suppress("DEPRECATION")
    fun legacySignInIntent(): android.content.Intent {
        val options = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN,
        )
            .requestEmail()
            .requestProfile()
            .requestScopes(Scope(DriveScopes.DRIVE))
            .build()
        return com.google.android.gms.auth.api.signin.GoogleSignIn
            .getClient(context, options)
            .signInIntent
    }

    @Suppress("DEPRECATION")
    fun parseLegacySignInResult(data: android.content.Intent?): SignInResult {
        try {
            val account = com.google.android.gms.auth.api.signin.GoogleSignIn
                .getSignedInAccountFromIntent(data)
                .getResult(com.google.android.gms.common.api.ApiException::class.java)
            val email = account.email
                ?: throw IllegalStateException("Google account has no email")
            return SignInResult(
                email = email,
                displayName = account.displayName ?: email.substringBefore('@'),
                photoUrl = account.photoUrl?.toString(),
            )
        } catch (e: com.google.android.gms.common.api.ApiException) {
            android.util.Log.e(TAG, "Legacy sign-in failed: statusCode=${e.statusCode} status=${e.status}", e)
            throw e
        }
    }

    suspend fun authorizeDrive(email: String): AuthorizeResult {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DriveScopes.DRIVE)))
            .setAccount(Account(email, "com.google"))
            .build()
        val result = try {
            Identity.getAuthorizationClient(context).authorize(request).await()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "authorizeDrive failed: ${e.message}", e)
            throw e
        }
        val pendingIntent = result.pendingIntent
        return if (result.hasResolution() && pendingIntent != null) {
            AuthorizeResult.NeedsConsent(pendingIntent)
        } else {
            AuthorizeResult.Granted
        }
    }
}
