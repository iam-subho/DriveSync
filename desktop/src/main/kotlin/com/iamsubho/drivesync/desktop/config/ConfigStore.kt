package com.iamsubho.drivesync.desktop.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

private const val MAX_LOG_ENTRIES = 500

/**
 * All persistent desktop state lives under %APPDATA%\DriveSync:
 * config.json (settings + sync pairs), logs-<pairId>.json, tokens\ (OAuth store).
 */
class ConfigStore {

    val baseDir: File = File(System.getenv("APPDATA") ?: System.getProperty("user.home"), "DriveSync")
        .apply { mkdirs() }

    private val configFile = File(baseDir, "config.json")
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val _config = MutableStateFlow(load())
    val config: StateFlow<AppConfig> = _config

    private fun load(): AppConfig = try {
        if (configFile.exists()) json.decodeFromString(configFile.readText()) else AppConfig()
    } catch (e: Exception) {
        AppConfig()
    }

    @Synchronized
    fun update(transform: (AppConfig) -> AppConfig) {
        val next = transform(_config.value)
        _config.value = next
        configFile.writeText(json.encodeToString(next))
    }

    fun updatePair(pairId: String, transform: (SyncPairConfig) -> SyncPairConfig) {
        update { cfg ->
            cfg.copy(pairs = cfg.pairs.map { if (it.id == pairId) transform(it) else it })
        }
    }

    // ---- per-pair logs ----

    private fun logFile(pairId: String) = File(baseDir, "logs-$pairId.json")

    fun readLogs(pairId: String): List<LogEntry> = try {
        val file = logFile(pairId)
        if (file.exists()) json.decodeFromString(file.readText()) else emptyList()
    } catch (e: Exception) {
        emptyList()
    }

    @Synchronized
    fun appendLog(pairId: String, entry: LogEntry) {
        val entries = (listOf(entry) + readLogs(pairId)).take(MAX_LOG_ENTRIES)
        logFile(pairId).writeText(json.encodeToString(entries))
    }

    fun clearLogs(pairId: String) {
        logFile(pairId).delete()
    }
}
