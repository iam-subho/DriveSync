package com.iamsubho.drivesync.desktop.sync

import com.iamsubho.drivesync.data.sync.ExclusionFilter
import com.iamsubho.drivesync.domain.model.LocalFile
import java.io.File
import java.security.DigestInputStream
import java.security.MessageDigest

/** Plain-filesystem counterpart of the Android SAF scanner. */
object LocalFsScanner {

    fun scan(root: File, excludedFolders: Collection<String>): List<LocalFile> {
        val results = mutableListOf<LocalFile>()
        walk(root, "", excludedFolders, results)
        return results
    }

    private fun walk(dir: File, prefix: String, excluded: Collection<String>, out: MutableList<LocalFile>) {
        val children = dir.listFiles() ?: return
        for (child in children) {
            val name = child.name
            when {
                child.isDirectory -> {
                    if (!ExclusionFilter.isExcludedDir("$prefix$name", excluded)) {
                        walk(child, "$prefix$name/", excluded, out)
                    }
                }
                child.isFile -> out += LocalFile(
                    name = name,
                    relativePath = "$prefix$name",
                    sizeBytes = child.length(),
                    modifiedAt = child.lastModified(),
                    uri = child.absolutePath,
                )
            }
        }
    }

    fun listSubfolders(root: File): List<String> =
        root.listFiles()?.filter { it.isDirectory }?.map { it.name }?.sorted() ?: emptyList()

    fun md5(file: File): String? = try {
        val digest = MessageDigest.getInstance("MD5")
        DigestInputStream(file.inputStream(), digest).use { stream ->
            val buffer = ByteArray(64 * 1024)
            @Suppress("ControlFlowWithEmptyBody")
            while (stream.read(buffer) != -1) {
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
    } catch (e: Exception) {
        null
    }
}
