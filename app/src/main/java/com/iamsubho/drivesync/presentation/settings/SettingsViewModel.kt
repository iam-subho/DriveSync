package com.iamsubho.drivesync.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iamsubho.drivesync.domain.model.DriveAccount
import com.iamsubho.drivesync.domain.model.FileTypeCategory
import com.iamsubho.drivesync.domain.repository.AccountRepository
import com.iamsubho.drivesync.domain.repository.DriveConsentRequiredException
import com.iamsubho.drivesync.domain.repository.ExtensionRepository
import com.iamsubho.drivesync.presentation.common.SnackBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExtensionSectionUi(
    val category: FileTypeCategory,
    val title: String,
    val extensions: List<String>,
    val input: String,
)

data class SettingsUiState(
    val accounts: List<DriveAccount> = emptyList(),
    val sections: List<ExtensionSectionUi> = emptyList(),
    val snackMessage: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val extensionRepository: ExtensionRepository,
    private val snackBus: SnackBus,
) : ViewModel() {

    private val editableSections = listOf(
        FileTypeCategory.IMAGES to "IMAGE EXTENSIONS",
        FileTypeCategory.VIDEOS to "VIDEO EXTENSIONS",
        FileTypeCategory.DOCUMENTS to "DOCUMENT EXTENSIONS",
    )

    private val inputs = MutableStateFlow(editableSections.associate { it.first to "" })
    private val snackMessage = MutableStateFlow<String?>(null)

    /** Drive consent PendingIntents that the Activity must launch (same flow as Home). */
    private val _consentRequests = MutableSharedFlow<DriveConsentRequiredException>(extraBufferCapacity = 1)
    val consentRequests: SharedFlow<DriveConsentRequiredException> = _consentRequests

    /** Classic sign-in Intents to launch when Credential Manager self-cancels. */
    private val _legacySignInRequests = MutableSharedFlow<android.content.Intent>(extraBufferCapacity = 1)
    val legacySignInRequests: SharedFlow<android.content.Intent> = _legacySignInRequests

    val uiState: StateFlow<SettingsUiState> = combine(
        accountRepository.observeAccounts(),
        extensionRepository.observe(FileTypeCategory.IMAGES),
        extensionRepository.observe(FileTypeCategory.VIDEOS),
        extensionRepository.observe(FileTypeCategory.DOCUMENTS),
        combine(inputs, snackMessage) { i, s -> i to s },
    ) { accounts, images, videos, documents, (inputMap, snack) ->
        val lists = mapOf(
            FileTypeCategory.IMAGES to images,
            FileTypeCategory.VIDEOS to videos,
            FileTypeCategory.DOCUMENTS to documents,
        )
        SettingsUiState(
            accounts = accounts,
            sections = editableSections.map { (category, title) ->
                ExtensionSectionUi(
                    category = category,
                    title = title,
                    extensions = lists.getValue(category),
                    input = inputMap.getValue(category),
                )
            },
            snackMessage = snack,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun addAccount(activityContext: Context) {
        viewModelScope.launch {
            try {
                val account = accountRepository.addAccount(activityContext)
                showSnack("Connected ${account.email}")
            } catch (e: DriveConsentRequiredException) {
                _consentRequests.tryEmit(e)
            } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                // Fall back to the classic account chooser (see HomeViewModel for rationale).
                _legacySignInRequests.tryEmit(accountRepository.legacySignInIntent())
            } catch (e: Exception) {
                android.util.Log.e(
                    com.iamsubho.drivesync.data.remote.GoogleAuthManager.TAG,
                    "addAccount failed", e,
                )
                showSnack(com.iamsubho.drivesync.data.remote.GoogleAuthManager.friendlyMessage(e))
            }
        }
    }

    fun finishConsent(request: DriveConsentRequiredException) {
        viewModelScope.launch {
            try {
                val account = accountRepository.finishAddAccount(
                    request.email, request.displayName, request.photoUrl,
                )
                showSnack("Connected ${account.email}")
            } catch (e: Exception) {
                showSnack("Drive access was not granted")
            }
        }
    }

    fun onLegacySignInResult(data: android.content.Intent?) {
        viewModelScope.launch {
            try {
                val account = accountRepository.addAccountFromLegacyResult(data)
                showSnack("Connected ${account.email}")
            } catch (e: DriveConsentRequiredException) {
                _consentRequests.tryEmit(e)
            } catch (e: com.google.android.gms.common.api.ApiException) {
                if (e.statusCode != 12501) {
                    showSnack(com.iamsubho.drivesync.data.remote.GoogleAuthManager.friendlyMessage(e))
                }
            } catch (e: Exception) {
                showSnack(com.iamsubho.drivesync.data.remote.GoogleAuthManager.friendlyMessage(e))
            }
        }
    }

    fun removeAccount(email: String) {
        viewModelScope.launch {
            accountRepository.removeAccount(email)
            showSnack("Removed $email")
        }
    }

    fun refreshStorage(email: String) {
        viewModelScope.launch {
            try {
                accountRepository.refreshStorage(email)
                showSnack("Storage refreshed")
            } catch (e: Exception) {
                showSnack(e.message ?: "Could not refresh storage")
            }
        }
    }

    fun setInput(category: FileTypeCategory, value: String) {
        inputs.update { it + (category to value) }
    }

    fun addExtension(category: FileTypeCategory) {
        val raw = inputs.value[category].orEmpty()
        if (raw.isBlank()) return
        viewModelScope.launch {
            extensionRepository.add(category, raw)
            inputs.update { it + (category to "") }
        }
    }

    fun removeExtension(category: FileTypeCategory, ext: String) {
        viewModelScope.launch { extensionRepository.remove(category, ext) }
    }

    fun restoreDefaults(category: FileTypeCategory) {
        viewModelScope.launch { extensionRepository.restoreDefaults(category) }
    }

    private fun showSnack(message: String) {
        snackMessage.value = message
    }

    fun consumeSnack() {
        snackMessage.value = null
    }
}
