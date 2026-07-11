package com.iamsubho.drivesync.domain.model

/** A file found under the job's local SAF tree. [relativePath] uses '/' separators and includes the file name. */
data class LocalFile(
    val name: String,
    val relativePath: String,
    val sizeBytes: Long,
    val modifiedAt: Long,
    val uri: String,
)

/** A file found under the job's Drive folder. [relativePath] mirrors [LocalFile.relativePath]. */
data class RemoteFile(
    val id: String,
    val name: String,
    val relativePath: String,
    val sizeBytes: Long,
    val modifiedAt: Long,
    val md5: String?,
    /**
     * Device timestamp stored by this app at upload (appProperties.deviceCreatedAt),
     * falling back to Drive's createdTime. Null for legacy/unreadable entries.
     */
    val deviceCreatedAt: Long? = null,
)

data class RemoteFolder(
    val id: String,
    val name: String,
)
