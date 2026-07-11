package com.iamsubho.drivesync.presentation.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iamsubho.drivesync.domain.repository.AccountRepository
import com.iamsubho.drivesync.domain.repository.DriveConsentRequiredException
import com.iamsubho.drivesync.domain.repository.SyncController
import com.iamsubho.drivesync.domain.repository.SyncJobRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val jobRepository: SyncJobRepository,
    private val syncController: SyncController,
    snackBus: com.iamsubho.drivesync.presentation.common.SnackBus,
) : ViewModel() {

    private val snackMessage = MutableStateFlow<String?>(null)
    private val addingAccount = MutableStateFlow(false)

    init {
        // Messages posted by other screens (wizard/settings) surface on Home.
        viewModelScope.launch {
            snackBus.messages.collect { snackMessage.value = it }
        }
    }

    /** Drive consent PendingIntents that the Activity must launch. */
    private val _consentRequests = MutableSharedFlow<DriveConsentRequiredException>(extraBufferCapacity = 1)
    val consentRequests: SharedFlow<DriveConsentRequiredException> = _consentRequests

    /** Classic sign-in Intents to launch when Credential Manager self-cancels. */
    private val _legacySignInRequests = MutableSharedFlow<android.content.Intent>(extraBufferCapacity = 1)
    val legacySignInRequests: SharedFlow<android.content.Intent> = _legacySignInRequests

    private val jobsWithProgress = jobRepository.observeJobs().flatMapLatest { jobs ->
        if (jobs.isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(
                jobs.map { job ->
                    syncController.observeProgress(job.id)
                },
            ) { progresses ->
                jobs.mapIndexed { index, job -> JobWithProgress(job, progresses[index]) }
            }
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        accountRepository.observeAccounts(),
        jobsWithProgress,
        snackMessage,
        addingAccount,
    ) { accounts, jobs, snack, adding ->
        HomeUiState(accounts = accounts, jobs = jobs, snackMessage = snack, addingAccount = adding)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun addAccount(activityContext: Context) {
        if (addingAccount.value) return
        viewModelScope.launch {
            addingAccount.value = true
            try {
                val account = accountRepository.addAccount(activityContext)
                showSnack("Connected ${account.email}")
            } catch (e: DriveConsentRequiredException) {
                _consentRequests.tryEmit(e)
            } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                // Play Services reports config/internal failures as "user cancelled" here.
                // Fall back to the classic account chooser, which works on stubborn OEMs
                // and surfaces the real error code if something is actually misconfigured.
                android.util.Log.w(
                    com.iamsubho.drivesync.data.remote.GoogleAuthManager.TAG,
                    "Credential Manager cancelled — falling back to classic Google sign-in",
                )
                _legacySignInRequests.tryEmit(accountRepository.legacySignInIntent())
            } catch (e: Exception) {
                android.util.Log.e(
                    com.iamsubho.drivesync.data.remote.GoogleAuthManager.TAG,
                    "addAccount failed", e,
                )
                showSnack(com.iamsubho.drivesync.data.remote.GoogleAuthManager.friendlyMessage(e))
            } finally {
                addingAccount.value = false
            }
        }
    }

    /** Called after the user approves the Drive consent screen. */
    fun finishConsent(request: DriveConsentRequiredException) {
        viewModelScope.launch {
            addingAccount.value = true
            try {
                val account = accountRepository.finishAddAccount(
                    request.email, request.displayName, request.photoUrl,
                )
                showSnack("Connected ${account.email}")
            } catch (e: DriveConsentRequiredException) {
                showSnack("Drive access was not granted")
            } catch (e: Exception) {
                showSnack(e.message ?: "Could not add account")
            } finally {
                addingAccount.value = false
            }
        }
    }

    /** Result of the classic sign-in fallback dialog. */
    fun onLegacySignInResult(data: android.content.Intent?) {
        viewModelScope.launch {
            addingAccount.value = true
            try {
                val account = accountRepository.addAccountFromLegacyResult(data)
                showSnack("Connected ${account.email}")
            } catch (e: DriveConsentRequiredException) {
                _consentRequests.tryEmit(e)
            } catch (e: com.google.android.gms.common.api.ApiException) {
                if (e.statusCode != 12501) { // 12501 = genuine user cancel
                    showSnack(com.iamsubho.drivesync.data.remote.GoogleAuthManager.friendlyMessage(e))
                }
            } catch (e: Exception) {
                android.util.Log.e(
                    com.iamsubho.drivesync.data.remote.GoogleAuthManager.TAG,
                    "legacy addAccount failed", e,
                )
                showSnack(com.iamsubho.drivesync.data.remote.GoogleAuthManager.friendlyMessage(e))
            } finally {
                addingAccount.value = false
            }
        }
    }

    fun startJob(id: Long) = syncController.start(id)

    fun pauseJob(id: Long) = syncController.pause(id)

    fun deleteJob(id: Long) {
        viewModelScope.launch {
            syncController.pause(id)
            jobRepository.deleteJob(id)
            showSnack("Sync job deleted")
        }
    }

    fun showSnack(message: String) {
        snackMessage.value = message
    }

    fun consumeSnack() {
        snackMessage.update { null }
    }
}
