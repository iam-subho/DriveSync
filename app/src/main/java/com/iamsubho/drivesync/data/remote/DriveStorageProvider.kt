package com.iamsubho.drivesync.data.remote

import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.googleapis.media.MediaHttpDownloader
import com.google.api.client.googleapis.media.MediaHttpUploader
import com.google.api.client.http.InputStreamContent
import com.google.api.client.util.DateTime
import com.iamsubho.drivesync.domain.cloud.CloudStorageProvider
import com.iamsubho.drivesync.domain.model.LocalFile
import com.iamsubho.drivesync.domain.model.RemoteFile
import com.iamsubho.drivesync.domain.model.RemoteFolder
import com.iamsubho.drivesync.domain.model.StorageQuota
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton
import com.google.api.services.drive.model.File as DriveFile

private const val FOLDER_MIME = "application/vnd.google-apps.folder"
private const val FILE_FIELDS = "id,name,size,modifiedTime,md5Checksum"
private const val PAGE_FIELDS =
    "nextPageToken,files(id,name,mimeType,size,modifiedTime,md5Checksum,createdTime,appProperties)"

/** appProperties key holding the device file's timestamp at upload (epoch millis). */
private const val PROP_DEVICE_CREATED_AT = "deviceCreatedAt"
private const val UPLOAD_CHUNK = 8 * MediaHttpUploader.MINIMUM_CHUNK_SIZE // 2 MB

/** Google Drive v3 implementation of [CloudStorageProvider]. */
@Singleton
class DriveStorageProvider @Inject constructor(
    private val serviceFactory: DriveServiceFactory,
) : CloudStorageProvider {

    override suspend fun listFolders(accountEmail: String, parentId: String?): List<RemoteFolder> =
        runDrive {
            val drive = serviceFactory.driveFor(accountEmail)
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

    override suspend fun createFolder(accountEmail: String, parentId: String?, name: String): RemoteFolder =
        runDrive {
            val drive = serviceFactory.driveFor(accountEmail)
            val meta = DriveFile().apply {
                this.name = name
                mimeType = FOLDER_MIME
                parents = listOf(parentId ?: "root")
            }
            val created = drive.files().create(meta).setFields("id,name").execute()
            RemoteFolder(created.id, created.name)
        }

    override suspend fun listAllFiles(accountEmail: String, rootFolderId: String): List<RemoteFile> =
        runDrive {
            val drive = serviceFactory.driveFor(accountEmail)
            val results = mutableListOf<RemoteFile>()
            // BFS over sub-folders, building relative paths as we go.
            val queue = ArrayDeque<Pair<String, String>>() // folderId to pathPrefix
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
                            // Files without a size are Google-native docs; they can't be
                            // byte-synced and are ignored.
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

    override suspend fun upload(
        accountEmail: String,
        folderIdForPath: suspend (String) -> String,
        file: LocalFile,
        overwriteFileId: String?,
        openStream: () -> InputStream,
        onProgress: (bytesSent: Long) -> Unit,
    ): RemoteFile {
        val parentDir = file.relativePath.substringBeforeLast('/', "")
        val parentId = folderIdForPath(parentDir)
        return runDrive {
            val drive = serviceFactory.driveFor(accountEmail)
            openStream().use { stream ->
                val content = InputStreamContent(null, stream).setLength(file.sizeBytes)
                // The device file's timestamp travels with the Drive file so the browser's
                // date search can filter on it (SAF exposes last-modified only).
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
                        parents = listOf(parentId)
                        modifiedTime = DateTime(file.modifiedAt)
                        createdTime = DateTime(file.modifiedAt)
                        appProperties = deviceProps
                    }
                    drive.files().create(meta, content)
                }
                request.mediaHttpUploader.apply {
                    isDirectUploadEnabled = false
                    chunkSize = UPLOAD_CHUNK
                    setProgressListener { uploader -> onProgress(uploader.numBytesUploaded) }
                }
                val uploaded = request.setFields(FILE_FIELDS).execute()
                RemoteFile(
                    id = uploaded.id,
                    name = uploaded.name,
                    relativePath = file.relativePath,
                    sizeBytes = uploaded.getSize()?.toLong() ?: file.sizeBytes,
                    modifiedAt = uploaded.modifiedTime?.value ?: file.modifiedAt,
                    md5 = uploaded.md5Checksum,
                )
            }
        }
    }

    override suspend fun download(
        accountEmail: String,
        file: RemoteFile,
        openSink: () -> OutputStream,
        onProgress: (bytesReceived: Long) -> Unit,
    ) {
        runDrive {
            val drive = serviceFactory.driveFor(accountEmail)
            openSink().use { sink ->
                val request = drive.files().get(file.id)
                request.mediaHttpDownloader.apply {
                    isDirectDownloadEnabled = false
                    setProgressListener { downloader: MediaHttpDownloader ->
                        onProgress(downloader.numBytesDownloaded)
                    }
                }
                request.executeMediaAndDownloadTo(sink)
            }
        }
    }

    override suspend fun quota(accountEmail: String): StorageQuota = runDrive {
        val drive = serviceFactory.driveFor(accountEmail)
        val about = drive.about().get().setFields("storageQuota").execute()
        val quota = about.storageQuota
        StorageQuota(
            usedBytes = quota?.usage ?: 0L,
            totalBytes = quota?.limit ?: 0L, // 0 = unlimited plan; UI guards division
        )
    }

    override suspend fun delete(accountEmail: String, fileId: String) {
        runDrive {
            serviceFactory.driveFor(accountEmail).files().delete(fileId).execute()
        }
    }

    override suspend fun rename(accountEmail: String, fileId: String, newName: String): RemoteFile =
        runDrive {
            val drive = serviceFactory.driveFor(accountEmail)
            val updated = drive.files()
                .update(fileId, DriveFile().apply { name = newName })
                .setFields(FILE_FIELDS)
                .execute()
            RemoteFile(
                id = updated.id,
                name = updated.name,
                relativePath = updated.name,
                sizeBytes = updated.getSize()?.toLong() ?: 0L,
                modifiedAt = updated.modifiedTime?.value ?: 0L,
                md5 = updated.md5Checksum,
            )
        }

    /** Runs a Drive call on IO and translates API failures into domain exceptions. */
    private suspend fun <T> runDrive(block: () -> T): T = withContext(Dispatchers.IO) {
        try {
            block()
        } catch (e: UserRecoverableAuthIOException) {
            throw DriveAuthException(e)
        } catch (e: GoogleJsonResponseException) {
            val reason = e.details?.errors?.firstOrNull()?.reason
            throw when {
                e.statusCode == 401 -> DriveAuthException(e)
                e.statusCode == 403 && reason in setOf("storageQuotaExceeded", "quotaExceeded") ->
                    DriveQuotaException(e)
                e.statusCode == 404 -> DriveFolderMissingException(e)
                else -> e
            }
        }
    }
}
