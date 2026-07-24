package com.iamsubho.drivesync.desktop.config

import kotlinx.serialization.Serializable

@Serializable
data class OAuthClientConfig(
    val clientId: String = "",
    val clientSecret: String = "",
) {
    val isConfigured: Boolean get() = clientId.isNotBlank() && clientSecret.isNotBlank()
}

@Serializable
data class DesktopAccount(
    val email: String,
    val displayName: String = "",
)

/** One local-folder ↔ Drive-folder sync configuration. */
@Serializable
data class SyncPairConfig(
    val id: String,
    val name: String,
    val localPath: String,
    val driveFolderId: String,
    val driveFolderName: String,
    val direction: String = "TWO_WAY",              // SyncDirection name
    val fileTypes: List<String> = listOf("ALL"),    // FileTypeCategory names
    val customExtensions: List<String> = emptyList(),
    val excludedFolders: List<String> = emptyList(),
    val uploadLimitMb: Int? = null,
    val uploadLimitMode: String = "SKIP_ABOVE",     // LimitMode name
    val downloadLimitMb: Int? = null,
    val downloadLimitMode: String = "SKIP_ABOVE",
    val intervalHours: Int = 24,
    val deleteAfterUpload: Boolean = false,
    val lastSyncAt: Long? = null,
    val paused: Boolean = false,
)

@Serializable
data class LogEntry(
    val timestamp: Long,
    val direction: String,   // UPLOAD | DOWNLOAD
    val fileName: String,
    val sizeBytes: Long,
    val result: String,      // SUCCESS | FAILED | SKIPPED
    val reason: String? = null,
)

@Serializable
data class AppConfig(
    val oauth: OAuthClientConfig = OAuthClientConfig(),
    val account: DesktopAccount? = null,
    val pairs: List<SyncPairConfig> = emptyList(),
)
