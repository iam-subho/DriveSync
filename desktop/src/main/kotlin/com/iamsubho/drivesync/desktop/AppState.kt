package com.iamsubho.drivesync.desktop

import com.iamsubho.drivesync.desktop.auth.DesktopAuth
import com.iamsubho.drivesync.desktop.config.AppConfig
import com.iamsubho.drivesync.desktop.config.ConfigStore
import com.iamsubho.drivesync.desktop.config.DesktopAccount
import com.iamsubho.drivesync.desktop.config.SyncPairConfig
import com.iamsubho.drivesync.desktop.drive.DesktopDriveProvider
import com.iamsubho.drivesync.desktop.sync.DesktopSyncEngine
import com.iamsubho.drivesync.domain.model.SyncProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PairRuntime(
    val running: Boolean = false,
    val progress: SyncProgress? = null,
    val lastMessage: String? = null,
    val error: Boolean = false,
)

/** Application-wide state + orchestration for the desktop client. */
class AppState {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val configStore = ConfigStore()
    val auth = DesktopAuth(configStore.baseDir)
    private val engine = DesktopSyncEngine(configStore)

    val config: StateFlow<AppConfig> get() = configStore.config

    private val _provider = MutableStateFlow<DesktopDriveProvider?>(null)
    val provider: StateFlow<DesktopDriveProvider?> = _provider

    private val _statuses = MutableStateFlow<Map<String, PairRuntime>>(emptyMap())
    val statuses: StateFlow<Map<String, PairRuntime>> = _statuses

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    /** Google consent URL of an in-flight sign-in — shown in the UI as a manual fallback. */
    private val _authUrl = MutableStateFlow<String?>(null)
    val authUrl: StateFlow<String?> = _authUrl

    private var signInJob: Job? = null
    private val runningJobs = mutableMapOf<String, Job>()

    init {
        com.iamsubho.drivesync.desktop.DesktopLog.init(configStore.baseDir)
        // Restore a previous session silently.
        val oauth = config.value.oauth
        if (oauth.isConfigured) {
            scope.launch(Dispatchers.IO) {
                val credential = auth.loadExisting(oauth.clientId, oauth.clientSecret)
                if (credential != null) _provider.value = DesktopDriveProvider(credential)
            }
        }
        // Scheduler: check due pairs every minute while the app runs.
        scope.launch {
            while (true) {
                delay(60_000)
                tickSchedule()
            }
        }
    }

    fun saveOAuthConfig(clientId: String, clientSecret: String) {
        configStore.update {
            it.copy(oauth = it.oauth.copy(clientId = clientId.trim(), clientSecret = clientSecret.trim()))
        }
    }

    fun signIn() {
        val oauth = config.value.oauth
        if (!oauth.isConfigured || _busy.value) return
        signInJob = scope.launch(Dispatchers.IO) {
            _busy.value = true
            _authUrl.value = null
            try {
                com.iamsubho.drivesync.desktop.DesktopLog.log("signIn: starting OAuth flow")
                val credential = auth.signIn(oauth.clientId, oauth.clientSecret) { url ->
                    _authUrl.value = url
                }
                val provider = DesktopDriveProvider(credential)
                val email = provider.accountEmail()
                _provider.value = provider
                configStore.update { it.copy(account = DesktopAccount(email = email)) }
                showNotice("Connected $email")
                com.iamsubho.drivesync.desktop.DesktopLog.log("signIn: connected $email")
            } catch (e: Exception) {
                com.iamsubho.drivesync.desktop.DesktopLog.log("signIn: failed", e)
                showNotice("Sign-in failed: ${e.message ?: e.javaClass.simpleName}")
            } finally {
                _busy.value = false
                _authUrl.value = null
            }
        }
    }

    /** Aborts a pending sign-in (stops the loopback listener so the coroutine unblocks). */
    fun cancelSignIn() {
        auth.cancel()
        signInJob?.cancel()
        signInJob = null
        _busy.value = false
        _authUrl.value = null
    }

    fun signOut() {
        runningJobs.values.forEach { it.cancel() }
        runningJobs.clear()
        auth.signOut()
        _provider.value = null
        configStore.update { it.copy(account = null) }
    }

    fun addOrUpdatePair(pair: SyncPairConfig) {
        configStore.update { cfg ->
            val exists = cfg.pairs.any { it.id == pair.id }
            cfg.copy(
                pairs = if (exists) cfg.pairs.map { if (it.id == pair.id) pair else it }
                else cfg.pairs + pair,
            )
        }
    }

    fun deletePair(pairId: String) {
        runningJobs.remove(pairId)?.cancel()
        configStore.update { cfg -> cfg.copy(pairs = cfg.pairs.filterNot { it.id == pairId }) }
        configStore.clearLogs(pairId)
        _statuses.update { it - pairId }
    }

    fun togglePaused(pairId: String) {
        configStore.updatePair(pairId) { it.copy(paused = !it.paused) }
    }

    fun runNow(pairId: String) {
        val pair = config.value.pairs.firstOrNull { it.id == pairId } ?: return
        startRun(pair)
    }

    fun stopRun(pairId: String) {
        runningJobs.remove(pairId)?.cancel()
        _statuses.update { it + (pairId to PairRuntime(lastMessage = "Stopped")) }
    }

    fun syncAllNow() {
        config.value.pairs.filterNot { it.paused }.forEach { startRun(it) }
    }

    private fun tickSchedule() {
        val now = System.currentTimeMillis()
        for (pair in config.value.pairs) {
            if (pair.paused) continue
            if (runningJobs[pair.id]?.isActive == true) continue
            val due = pair.lastSyncAt == null ||
                now - pair.lastSyncAt >= pair.intervalHours * 3_600_000L
            if (due) startRun(pair)
        }
    }

    private fun startRun(pair: SyncPairConfig) {
        val provider = _provider.value ?: run {
            showNotice("Sign in with Google first")
            return
        }
        if (runningJobs[pair.id]?.isActive == true) return
        _statuses.update { it + (pair.id to PairRuntime(running = true)) }
        runningJobs[pair.id] = scope.launch(Dispatchers.IO) {
            val result = engine.runPair(pair, provider) { progress ->
                _statuses.update { it + (pair.id to PairRuntime(running = true, progress = progress)) }
            }
            val runtime = when (result) {
                is DesktopSyncEngine.Result.Success -> PairRuntime(
                    lastMessage = if (result.failed == 0) "Up to date" else "Done — ${result.failed} failed",
                    error = result.failed > 0,
                )
                is DesktopSyncEngine.Result.Error -> PairRuntime(lastMessage = result.message, error = true)
            }
            _statuses.update { it + (pair.id to runtime) }
        }
    }

    fun showNotice(message: String) {
        _notice.value = message
    }

    fun consumeNotice() {
        _notice.value = null
    }
}
