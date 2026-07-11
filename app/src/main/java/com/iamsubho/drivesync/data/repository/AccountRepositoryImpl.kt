package com.iamsubho.drivesync.data.repository

import android.content.Context
import com.iamsubho.drivesync.data.local.dao.AccountDao
import com.iamsubho.drivesync.data.local.entity.toEntity
import com.iamsubho.drivesync.data.remote.DriveServiceFactory
import com.iamsubho.drivesync.data.remote.GoogleAuthManager
import com.iamsubho.drivesync.domain.cloud.CloudStorageProvider
import com.iamsubho.drivesync.domain.model.DriveAccount
import com.iamsubho.drivesync.domain.repository.AccountRepository
import com.iamsubho.drivesync.domain.repository.DriveConsentRequiredException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
    private val authManager: GoogleAuthManager,
    private val provider: CloudStorageProvider,
    private val serviceFactory: DriveServiceFactory,
) : AccountRepository {

    override fun observeAccounts(): Flow<List<DriveAccount>> =
        accountDao.observeAll().map { accounts -> accounts.map { it.toDomain() } }

    override suspend fun addAccount(activityContext: Context): DriveAccount {
        val signIn = authManager.signIn(activityContext)
        return finishAddAccount(signIn.email, signIn.displayName, signIn.photoUrl)
    }

    /**
     * Completes account setup after sign-in: Drive consent + quota fetch + persist.
     * Also called again after the user resolves a [DriveConsentRequiredException].
     */
    override suspend fun finishAddAccount(email: String, displayName: String, photoUrl: String?): DriveAccount {
        when (val result = authManager.authorizeDrive(email)) {
            is GoogleAuthManager.AuthorizeResult.NeedsConsent ->
                throw DriveConsentRequiredException(result.pendingIntent, email, displayName, photoUrl)
            is GoogleAuthManager.AuthorizeResult.Granted -> Unit
        }
        val quota = provider.quota(email)
        val account = DriveAccount(
            email = email,
            displayName = displayName,
            photoUrl = photoUrl,
            storageUsedBytes = quota.usedBytes,
            storageTotalBytes = quota.totalBytes,
            addedAt = System.currentTimeMillis(),
        )
        accountDao.upsert(account.toEntity())
        return account
    }

    override fun legacySignInIntent(): android.content.Intent = authManager.legacySignInIntent()

    override suspend fun addAccountFromLegacyResult(data: android.content.Intent?): DriveAccount {
        val signIn = authManager.parseLegacySignInResult(data)
        return finishAddAccount(signIn.email, signIn.displayName, signIn.photoUrl)
    }

    override suspend fun removeAccount(email: String) {
        accountDao.delete(email) // sync jobs cascade via FK
        serviceFactory.evict(email)
    }

    override suspend fun refreshStorage(email: String) {
        val quota = provider.quota(email)
        accountDao.updateStorage(email, quota.usedBytes, quota.totalBytes)
    }
}
