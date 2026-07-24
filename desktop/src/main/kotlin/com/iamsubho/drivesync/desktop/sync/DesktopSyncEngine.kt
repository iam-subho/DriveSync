package com.iamsubho.drivesync.desktop.sync

import com.iamsubho.drivesync.data.local.ExtensionDefaults
import com.iamsubho.drivesync.data.sync.ExclusionFilter
import com.iamsubho.drivesync.data.sync.FileFilter
import com.iamsubho.drivesync.data.sync.PlanEntry
import com.iamsubho.drivesync.data.sync.SpeedTracker
import com.iamsubho.drivesync.data.sync.SyncPlanner
import com.iamsubho.drivesync.desktop.config.ConfigStore
import com.iamsubho.drivesync.desktop.config.LogEntry
import com.iamsubho.drivesync.desktop.config.SyncPairConfig
import com.iamsubho.drivesync.desktop.drive.DesktopDriveProvider
import com.iamsubho.drivesync.domain.model.FileTypeCategory
import com.iamsubho.drivesync.domain.model.LimitMode
import com.iamsubho.drivesync.domain.model.SyncDirection
import com.iamsubho.drivesync.domain.model.SyncProgress
import kotlinx.coroutines.ensureActive
import java.io.File
import kotlin.coroutines.coroutineContext

/** One sync pass for one desktop sync pair: snapshot → core planner → sequential transfers. */
class DesktopSyncEngine(
    private val configStore: ConfigStore,
) {
    sealed interface Result {
        data class Success(val transferred: Int, val failed: Int) : Result
        data class Error(val message: String) : Result
    }

    suspend fun runPair(
        pair: SyncPairConfig,
        provider: DesktopDriveProvider,
        onProgress: (SyncProgress?) -> Unit,
    ): Result {
        val root = File(pair.localPath)
        if (!root.isDirectory) {
            log(pair.id, "UPLOAD", pair.localPath, 0, "FAILED", "Local folder not found")
            return Result.Error("Local folder not found: ${pair.localPath}")
        }

        try {
            com.iamsubho.drivesync.desktop.DesktopLog.log(
                "sync[${pair.name}]: start local='${pair.localPath}' driveFolder=${pair.driveFolderId} " +
                    "excluded=${pair.excludedFolders}",
            )
            val remotes = provider.listAllFiles(pair.driveFolderId)
                .filterNot { ExclusionFilter.isExcluded(it.relativePath, pair.excludedFolders) }
            val locals = LocalFsScanner.scan(root, pair.excludedFolders)
            com.iamsubho.drivesync.desktop.DesktopLog.log(
                "sync[${pair.name}]: ${remotes.size} remote, ${locals.size} local files; " +
                    "local samples=${locals.take(3).map { it.relativePath }}",
            )

            val direction = SyncDirection.valueOf(pair.direction)
            val filter = FileFilter(
                fileTypes = pair.fileTypes.map { FileTypeCategory.valueOf(it) }.toSet(),
                extensionsByCategory = mapOf(
                    FileTypeCategory.IMAGES to ExtensionDefaults.IMAGES.toSet(),
                    FileTypeCategory.VIDEOS to ExtensionDefaults.VIDEOS.toSet(),
                    FileTypeCategory.DOCUMENTS to ExtensionDefaults.DOCUMENTS.toSet(),
                    FileTypeCategory.AUDIO to ExtensionDefaults.AUDIO.toSet(),
                    FileTypeCategory.ARCHIVES to ExtensionDefaults.ARCHIVES.toSet(),
                ),
                customExtensions = pair.customExtensions.toSet(),
            )
            val entries = SyncPlanner().plan(
                direction = direction,
                locals = locals,
                remotes = remotes,
                filter = filter,
                uploadLimitMb = pair.uploadLimitMb,
                uploadLimitMode = LimitMode.valueOf(pair.uploadLimitMode),
                downloadLimitMb = pair.downloadLimitMb,
                downloadLimitMode = LimitMode.valueOf(pair.downloadLimitMode),
                deleteAfterUpload = pair.deleteAfterUpload,
                localMd5 = { local -> LocalFsScanner.md5(File(local.uri)) },
            )

            val transfers = entries.filter { it !is PlanEntry.Skip }
            entries.filterIsInstance<PlanEntry.Skip>().forEach { skip ->
                log(pair.id, skip.direction.name, skip.fileName, skip.sizeBytes, "SKIPPED", skip.reason)
            }

            val folderIdCache = mutableMapOf("" to pair.driveFolderId)
            val speed = SpeedTracker()
            val totalBytes = transfers.sumOf { transferBytes(it) }.coerceAtLeast(1)
            var doneBytes = 0L
            var failed = 0

            for ((index, entry) in transfers.withIndex()) {
                coroutineContext.ensureActive()
                val (fileName, sizeBytes) = when (entry) {
                    is PlanEntry.Upload -> entry.local.name to entry.local.sizeBytes
                    is PlanEntry.Download -> entry.remote.name to entry.remote.sizeBytes
                    is PlanEntry.Skip -> continue
                }
                var lastBytes = 0L
                val track: (Long) -> Unit = { bytes ->
                    speed.onBytes(bytes - lastBytes, System.currentTimeMillis())
                    lastBytes = bytes
                    val now = System.currentTimeMillis()
                    val bps = speed.speedBytesPerSec(now)
                    val remaining = (totalBytes - doneBytes - bytes).coerceAtLeast(0)
                    onProgress(
                        SyncProgress(
                            jobId = 0,
                            currentFile = fileName,
                            percent = ((doneBytes + bytes) * 100 / totalBytes).toInt().coerceIn(0, 100),
                            speedBytesPerSec = bps,
                            etaSeconds = if (bps > 0) remaining / bps else null,
                            pending = transfers.size - index,
                        ),
                    )
                }
                track(0)
                try {
                    when (entry) {
                        is PlanEntry.Upload -> {
                            val parentDir = entry.local.relativePath.substringBeforeLast('/', "")
                            val parentId = resolveFolderId(provider, parentDir, folderIdCache)
                            com.iamsubho.drivesync.desktop.DesktopLog.log(
                                "sync[${pair.name}]: upload '${entry.local.relativePath}' " +
                                    "parentDir='$parentDir' parentId=$parentId",
                            )
                            provider.upload(
                                parentFolderId = parentId,
                                file = entry.local,
                                overwriteFileId = entry.overwriteRemoteId,
                                openStream = { File(entry.local.uri).inputStream() },
                                onProgress = track,
                            )
                            var reason: String? = null
                            if (pair.deleteAfterUpload) {
                                reason = if (File(entry.local.uri).delete()) {
                                    "Uploaded and deleted from this PC"
                                } else {
                                    "Uploaded — could not delete local copy"
                                }
                            }
                            log(pair.id, "UPLOAD", fileName, sizeBytes, "SUCCESS", reason)
                        }
                        is PlanEntry.Download -> {
                            val target = File(root, entry.remote.relativePath.replace('/', File.separatorChar))
                            target.parentFile?.mkdirs()
                            val temp = File(target.parentFile, target.name + ".drivesync-tmp")
                            try {
                                provider.download(entry.remote.id, { temp.outputStream() }, track)
                                if (target.exists()) target.delete()
                                if (!temp.renameTo(target)) throw java.io.IOException("Could not rename temp file")
                                target.setLastModified(entry.remote.modifiedAt)
                            } catch (e: Exception) {
                                temp.delete()
                                throw e
                            }
                            log(pair.id, "DOWNLOAD", fileName, sizeBytes, "SUCCESS", null)
                        }
                        is PlanEntry.Skip -> Unit
                    }
                    doneBytes += sizeBytes
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    failed++
                    log(
                        pair.id,
                        if (entry is PlanEntry.Upload) "UPLOAD" else "DOWNLOAD",
                        fileName, sizeBytes, "FAILED", e.message ?: e.javaClass.simpleName,
                    )
                }
            }

            onProgress(null)
            configStore.updatePair(pair.id) { it.copy(lastSyncAt = System.currentTimeMillis()) }
            return Result.Success(transferred = transfers.size - failed, failed = failed)
        } catch (e: kotlinx.coroutines.CancellationException) {
            onProgress(null)
            throw e
        } catch (e: Exception) {
            onProgress(null)
            log(pair.id, "UPLOAD", pair.name, 0, "FAILED", e.message ?: e.javaClass.simpleName)
            return Result.Error(e.message ?: e.javaClass.simpleName)
        }
    }

    private fun transferBytes(entry: PlanEntry): Long = when (entry) {
        is PlanEntry.Upload -> entry.local.sizeBytes
        is PlanEntry.Download -> entry.remote.sizeBytes
        is PlanEntry.Skip -> 0
    }

    private suspend fun resolveFolderId(
        provider: DesktopDriveProvider,
        relativeDir: String,
        cache: MutableMap<String, String>,
    ): String {
        if (relativeDir.isEmpty()) return cache.getValue("")
        cache[relativeDir]?.let { return it }
        val parentDir = relativeDir.substringBeforeLast('/', "")
        val name = relativeDir.substringAfterLast('/')
        val parentId = resolveFolderId(provider, parentDir, cache)
        val existing = provider.listFolders(parentId).firstOrNull { it.name == name }
        val id = if (existing != null) {
            existing.id
        } else {
            val created = provider.createFolder(parentId, name)
            com.iamsubho.drivesync.desktop.DesktopLog.log(
                "sync: created Drive folder '$name' (id=${created.id}) under $parentId",
            )
            created.id
        }
        cache[relativeDir] = id
        return id
    }

    private fun log(
        pairId: String,
        direction: String,
        fileName: String,
        sizeBytes: Long,
        result: String,
        reason: String?,
    ) {
        configStore.appendLog(
            pairId,
            LogEntry(
                timestamp = System.currentTimeMillis(),
                direction = direction,
                fileName = fileName,
                sizeBytes = sizeBytes,
                result = result,
                reason = reason,
            ),
        )
    }
}
