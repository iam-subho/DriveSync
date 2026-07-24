package com.iamsubho.drivesync.desktop.drive

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.media.MediaHttpUploader
import com.google.api.client.http.InputStreamContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.drive.Drive
import com.iamsubho.drivesync.domain.model.LocalFile
import com.iamsubho.drivesync.domain.model.RemoteFile
import com.iamsubho.drivesync.domain.model.RemoteFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.ArrayDeque
import com.google.api.services.drive.model.File as DriveFile

private const val FOLDER_MIME = "application/vnd.google-apps.folder"
private const val FILE_FIELDS = "id,name,size,modifiedTime,md5Checksum"
private const val PAGE_FIELDS =
    "nextPageToken,files(id,name,mimeType,size,modifiedTime,md5Checksum,createdTime,appProperties)"
private const val PROP_DEVICE_CREATED_AT = "deviceCreatedAt"
private const val UPLOAD_CHUNK = 8 * MediaHttpUploader.MINIMUM_CHUNK_SIZE

/** JVM Drive v3 access for the desktop app (single signed-in account). */
class DesktopDriveProvider(credential: Credential) {

    private val drive: Drive = Drive.Builder(
        NetHttpTransport(), GsonFactory.getDefaultInstance(), credential,
    ).setApplicationName("DriveSync Desktop").build()

    suspend fun accountEmail(): String = withContext(Dispatchers.IO) {
        drive.about().get().setFields("user").execute().user?.emailAddress ?: "unknown"
    }

    suspend fun listFolders(parentId: String?): List<RemoteFolder> = withContext(Dispatchers.IO) {
        val parent = parentId ?: "root"
        val folders = mutableListOf<RemoteFolder>()
        var pageToken: String? = null
        do {
            val page = drive.files().list()
                .setQ("'$parent' in parents and mimeType = '$FOLDER_MIME' and trashed = false")
                .setFields("nextPageToken,files(id,name)")
                .setOrderBy("name")
                .setPageSize(1000)
                .setPageToken(pageToken)
                .execute()
            page.files?.forEach { folders += RemoteFolder(it.id, it.name) }
            pageToken = page.nextPageToken
        } while (pageToken != null)
        folders
    }

    suspend fun createFolder(parentId: String?, name: String): RemoteFolder =
        withContext(Dispatchers.IO) {
            val meta = DriveFile().apply {
                this.name = name
                mimeType = FOLDER_MIME
                parents = listOf(parentId ?: "root")
            }
            val created = drive.files().create(meta).setFields("id,name").execute()
            RemoteFolder(created.id, created.name)
        }

    suspend fun listAllFiles(rootFolderId: String): List<RemoteFile> = withContext(Dispatchers.IO) {
        val results = mutableListOf<RemoteFile>()
        val queue = ArrayDeque<Pair<String, String>>()
        queue.add(rootFolderId to "")
        while (queue.isNotEmpty()) {
            val (folderId, prefix) = queue.poll()
            var pageToken: String? = null
            do {
                val page = drive.files().list()
                    .setQ("'$folderId' in parents and trashed = false")
                    .setFields(PAGE_FIELDS)
                    .setPageSize(1000)
                    .setPageToken(pageToken)
                    .execute()
                page.files?.forEach { f ->
                    if (f.mimeType == FOLDER_MIME) {
                        queue.add(f.id to "$prefix${f.name}/")
                    } else if (f.getSize() != null) {
                        results += RemoteFile(
                            id = f.id,
                            name = f.name,
                            relativePath = "$prefix${f.name}",
                            sizeBytes = f.getSize().toLong(),
                            modifiedAt = f.modifiedTime?.value ?: 0L,
                            md5 = f.md5Checksum,
                            deviceCreatedAt = f.appProperties?.get(PROP_DEVICE_CREATED_AT)?.toLongOrNull()
                                ?: f.createdTime?.value,
                        )
                    }
                }
                pageToken = page.nextPageToken
            } while (pageToken != null)
        }
        results
    }

    suspend fun upload(
        parentFolderId: String,
        file: LocalFile,
        overwriteFileId: String?,
        openStream: () -> InputStream,
        onProgress: (Long) -> Unit,
    ): Unit = withContext(Dispatchers.IO) {
        openStream().use { stream ->
            val content = InputStreamContent(null, stream).setLength(file.sizeBytes)
            val deviceProps = mapOf(PROP_DEVICE_CREATED_AT to file.modifiedAt.toString())
            val request = if (overwriteFileId != null) {
                val meta = DriveFile().apply {
                    modifiedTime = DateTime(file.modifiedAt)
                    appProperties = deviceProps
                }
                drive.files().update(overwriteFileId, meta, content)
            } else {
                val meta = DriveFile().apply {
                    name = file.name
                    parents = listOf(parentFolderId)
                    modifiedTime = DateTime(file.modifiedAt)
                    createdTime = DateTime(file.modifiedAt)
                    appProperties = deviceProps
                }
                drive.files().create(meta, content)
            }
            request.mediaHttpUploader.apply {
                isDirectUploadEnabled = false
                chunkSize = UPLOAD_CHUNK
                setProgressListener { onProgress(it.numBytesUploaded) }
            }
            request.setFields(FILE_FIELDS).execute()
        }
    }

    suspend fun download(
        fileId: String,
        openSink: () -> OutputStream,
        onProgress: (Long) -> Unit,
    ): Unit = withContext(Dispatchers.IO) {
        openSink().use { sink ->
            val request = drive.files().get(fileId)
            request.mediaHttpDownloader.apply {
                isDirectDownloadEnabled = false
                setProgressListener { onProgress(it.numBytesDownloaded) }
            }
            request.executeMediaAndDownloadTo(sink)
        }
    }
}
