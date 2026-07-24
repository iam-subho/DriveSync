package com.iamsubho.drivesync.domain.cloud

import com.iamsubho.drivesync.domain.model.LocalFile
import com.iamsubho.drivesync.domain.model.RemoteFile
import com.iamsubho.drivesync.domain.model.RemoteFolder
import com.iamsubho.drivesync.domain.model.StorageQuota
import java.io.InputStream
import java.io.OutputStream

/**
 * Abstraction over a cloud storage backend. Google Drive is the v1 implementation;
 * OneDrive/Dropbox/Box can be added later by implementing this interface.
 */
interface CloudStorageProvider {

    /** Lists sub-folders of [parentId] (null = storage root). */
    suspend fun listFolders(accountEmail: String, parentId: String?): List<RemoteFolder>

    suspend fun createFolder(accountEmail: String, parentId: String?, name: String): RemoteFolder

    /** Recursively lists every file under [rootFolderId] with paths relative to it. */
    suspend fun listAllFiles(accountEmail: String, rootFolderId: String): List<RemoteFile>

    /**
     * Streams [file] to the cloud. [folderIdForPath] resolves (and creates if needed) the remote
     * folder for a relative directory path (empty string = the job's root folder).
     * When [overwriteFileId] is set, the existing remote file's content is replaced.
     */
    suspend fun upload(
        accountEmail: String,
        folderIdForPath: suspend (String) -> String,
        file: LocalFile,
        overwriteFileId: String?,
        openStream: () -> InputStream,
        onProgress: (bytesSent: Long) -> Unit,
    ): RemoteFile

    /** Streams [file] content into [openSink]. */
    suspend fun download(
        accountEmail: String,
        file: RemoteFile,
        openSink: () -> OutputStream,
        onProgress: (bytesReceived: Long) -> Unit,
    )

    suspend fun quota(accountEmail: String): StorageQuota

    /** files.delete — reserved for deletion sync (future). */
    suspend fun delete(accountEmail: String, fileId: String)

    /** files.update(name) — reserved for rename sync (future). */
    suspend fun rename(accountEmail: String, fileId: String, newName: String): RemoteFile
}
