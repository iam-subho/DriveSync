package com.iamsubho.drivesync.data.sync

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.iamsubho.drivesync.domain.model.LocalFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

private const val TMP_SUFFIX = ".drivesync-tmp"

/** All SAF access for sync jobs: tree walking, streams, temp-file downloads, hashing. */
@Singleton
class LocalFolderScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** True when the persisted read/write permission for [treeUri] is still valid. */
    fun hasPermission(treeUri: Uri): Boolean {
        val persisted = context.contentResolver.persistedUriPermissions.any {
            it.uri == treeUri && it.isReadPermission && it.isWritePermission
        }
        if (!persisted) return false
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return false
        return root.exists() && root.canRead()
    }

    fun scan(treeUri: Uri): List<LocalFile> {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        val results = mutableListOf<LocalFile>()
        walk(root, "", results)
        return results
    }

    private fun walk(dir: DocumentFile, prefix: String, out: MutableList<LocalFile>) {
        for (child in dir.listFiles()) {
            val name = child.name ?: continue
            when {
                child.isDirectory -> walk(child, "$prefix$name/", out)
                child.isFile && !name.endsWith(TMP_SUFFIX) -> out += LocalFile(
                    name = name,
                    relativePath = "$prefix$name",
                    sizeBytes = child.length(),
                    modifiedAt = child.lastModified(),
                    uri = child.uri.toString(),
                )
            }
        }
    }

    fun openIn(uri: String): InputStream =
        context.contentResolver.openInputStream(Uri.parse(uri))
            ?: throw IOException("Cannot open local file: $uri")

    /** Deletes a single file (used by delete-after-upload). @return true on success. */
    fun deleteFile(uri: String): Boolean =
        DocumentFile.fromSingleUri(context, Uri.parse(uri))?.delete() ?: false

    /** Streams the file's MD5 (lowercase hex) without loading it into memory. */
    fun md5(uri: String): String? = try {
        val digest = MessageDigest.getInstance("MD5")
        DigestInputStream(openIn(uri), digest).use { stream ->
            val buffer = ByteArray(64 * 1024)
            @Suppress("ControlFlowWithEmptyBody")
            while (stream.read(buffer) != -1) {
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    } catch (e: IOException) {
        null
    }

    /**
     * Prepares a download destination: parent folders are created, content is written to a
     * temp file, and [PendingDownload.commit] atomically replaces the final file.
     */
    fun prepareDownload(treeUri: Uri, relativePath: String): PendingDownload {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: throw IOException("Local folder unavailable: $treeUri")
        val dirPath = relativePath.substringBeforeLast('/', "")
        val fileName = relativePath.substringAfterLast('/')
        val parent = ensureDirs(root, dirPath)
        val tempName = "$fileName$TMP_SUFFIX"
        parent.findFile(tempName)?.delete() // stale temp from an interrupted run
        val temp = parent.createFile("application/octet-stream", tempName)
            ?: throw IOException("Cannot create file in ${parent.uri}")
        return PendingDownload(parent, temp, fileName)
    }

    inner class PendingDownload(
        private val parent: DocumentFile,
        private val temp: DocumentFile,
        private val finalName: String,
    ) {
        fun openSink(): OutputStream =
            context.contentResolver.openOutputStream(temp.uri, "w")
                ?: throw IOException("Cannot open output stream: ${temp.uri}")

        fun commit() {
            parent.findFile(finalName)?.delete()
            if (!temp.renameTo(finalName)) {
                throw IOException("Cannot rename downloaded file to $finalName")
            }
        }

        fun abort() {
            temp.delete()
        }
    }

    private fun ensureDirs(root: DocumentFile, relativeDir: String): DocumentFile {
        if (relativeDir.isEmpty()) return root
        var current = root
        for (segment in relativeDir.split('/')) {
            val existing = current.findFile(segment)
            current = when {
                existing != null && existing.isDirectory -> existing
                else -> current.createDirectory(segment)
                    ?: throw IOException("Cannot create local folder: $segment")
            }
        }
        return current
    }

    @Suppress("unused")
    private fun mimeFor(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
            ?: "application/octet-stream"
    }
}
