package com.iamsubho.drivesync.data.sync

import android.net.Uri
import com.iamsubho.drivesync.data.local.dao.SyncJobDao
import com.iamsubho.drivesync.data.local.dao.TransferLogDao
import com.iamsubho.drivesync.data.local.dao.TransferQueueDao
import com.iamsubho.drivesync.data.local.entity.TransferQueueEntity
import com.iamsubho.drivesync.data.local.entity.toEntity
import com.iamsubho.drivesync.data.remote.DriveAuthException
import com.iamsubho.drivesync.data.remote.DriveFolderMissingException
import com.iamsubho.drivesync.data.remote.DriveQuotaException
import com.iamsubho.drivesync.domain.cloud.CloudStorageProvider
import com.iamsubho.drivesync.domain.model.LocalFile
import com.iamsubho.drivesync.domain.model.RemoteFile
import com.iamsubho.drivesync.domain.model.SyncProgress
import com.iamsubho.drivesync.domain.model.SyncStatus
import com.iamsubho.drivesync.domain.model.TransferDirection
import com.iamsubho.drivesync.domain.model.TransferLogEntry
import com.iamsubho.drivesync.domain.model.TransferResult
import com.iamsubho.drivesync.domain.repository.ExtensionRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DriveSyncEngine"

/**
 * Runs one sync pass for a job: snapshot both sides, plan the diff, persist the plan as a
 * durable queue, then execute transfers with streaming, retries, and live progress.
 */
@Singleton
class SyncEngine @Inject constructor(
    private val jobDao: SyncJobDao,
    private val queueDao: TransferQueueDao,
    private val logDao: TransferLogDao,
    private val extensionRepository: ExtensionRepository,
    private val provider: CloudStorageProvider,
    private val scanner: LocalFolderScanner,
) {
    sealed interface Result {
        data object Success : Result

        /** Some transfers failed transiently; the worker should retry after backoff. */
        data object Retry : Result
        data object AuthNeeded : Result
        data class Error(val message: String) : Result
    }

    suspend fun run(jobId: Long, onProgress: (SyncProgress) -> Unit): Result {
        val job = jobDao.get(jobId)?.toDomain()
            ?: return Result.Error("Sync job not found")
        val treeUri = Uri.parse(job.localFolderUri)
        android.util.Log.i(TAG, "run: job=$jobId '${job.name}' dir=${job.direction} uri=${job.localFolderUri}")

        if (!scanner.hasPermission(treeUri)) {
            android.util.Log.e(TAG, "run: SAF permission missing/lost for $treeUri")
            log(jobId, TransferDirection.UPLOAD, job.localFolderName, 0, TransferResult.FAILED,
                "Folder permission lost — re-select the local folder")
            return Result.Error("Local folder permission lost")
        }

        try {
            // Resume shortcut: if a previous run left pending operations, execute them
            // without re-planning.
            val pendingBefore = queueDao.countPending(jobId)
            android.util.Log.i(TAG, "run: pending ops before planning = $pendingBefore")
            if (pendingBefore == 0) {
                planAndEnqueue(jobId, treeUri)
            }
            return execute(jobId, treeUri, onProgress)
        } catch (e: DriveAuthException) {
            android.util.Log.e(TAG, "run: auth failure", e)
            jobDao.updateStatus(jobId, SyncStatus.ERROR.name, "Account needs re-authorization")
            return Result.AuthNeeded
        } catch (e: DriveQuotaException) {
            android.util.Log.e(TAG, "run: Drive quota exceeded", e)
            jobDao.updateStatus(jobId, SyncStatus.ERROR.name, "Drive storage full")
            return Result.Error("Drive storage full")
        } catch (e: DriveFolderMissingException) {
            android.util.Log.e(TAG, "run: Drive folder missing", e)
            jobDao.updateStatus(jobId, SyncStatus.ERROR.name, "Drive folder missing")
            return Result.Error("Drive folder missing")
        } catch (e: IOException) {
            // Snapshot-phase network failure: WorkManager retries the whole run.
            android.util.Log.e(TAG, "run: IO failure during snapshot/planning — will retry", e)
            return Result.Retry
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e // pause/stop — handled by the worker
        } catch (e: Exception) {
            // Anything unexpected must surface as a visible job error, never a stuck "Syncing".
            android.util.Log.e(TAG, "run: unexpected failure", e)
            jobDao.updateStatus(jobId, SyncStatus.ERROR.name, e.message ?: e.javaClass.simpleName)
            return Result.Error(e.message ?: e.javaClass.simpleName)
        }
    }

    // ---- planning ----

    private suspend fun planAndEnqueue(jobId: Long, treeUri: Uri) {
        val job = jobDao.get(jobId)!!.toDomain()
        val remotes = provider.listAllFiles(job.accountEmail, job.driveFolderId)
        android.util.Log.i(TAG, "plan: ${remotes.size} remote files listed")
        val locals = scanner.scan(treeUri)
        android.util.Log.i(TAG, "plan: ${locals.size} local files scanned")
        val filter = FileFilter(
            fileTypes = job.fileTypes,
            extensionsByCategory = extensionRepository.snapshotAll(),
            customExtensions = job.customExtensions.toSet(),
        )
        val entries = SyncPlanner().plan(
            direction = job.direction,
            locals = locals,
            remotes = remotes,
            filter = filter,
            uploadLimitMb = job.uploadLimitMb,
            uploadLimitMode = job.uploadLimitMode,
            downloadLimitMb = job.downloadLimitMb,
            downloadLimitMode = job.downloadLimitMode,
            deleteAfterUpload = job.deleteAfterUpload,
            localMd5 = { local -> scanner.md5(local.uri) },
        )

        val ops = mutableListOf<TransferQueueEntity>()
        for (entry in entries) {
            when (entry) {
                is PlanEntry.Skip -> log(
                    jobId, entry.direction, entry.fileName, entry.sizeBytes,
                    TransferResult.SKIPPED, entry.reason,
                )
                is PlanEntry.Upload -> ops += TransferQueueEntity(
                    jobId = jobId,
                    opType = TransferQueueEntity.OP_UPLOAD,
                    fileName = entry.local.name,
                    relativePath = entry.local.relativePath,
                    localUri = entry.local.uri,
                    remoteFileId = entry.overwriteRemoteId,
                    sizeBytes = entry.local.sizeBytes,
                    md5 = null,
                    modifiedAt = entry.local.modifiedAt,
                )
                is PlanEntry.Download -> ops += TransferQueueEntity(
                    jobId = jobId,
                    opType = TransferQueueEntity.OP_DOWNLOAD,
                    fileName = entry.remote.name,
                    relativePath = entry.remote.relativePath,
                    localUri = null,
                    remoteFileId = entry.remote.id,
                    sizeBytes = entry.remote.sizeBytes,
                    md5 = entry.remote.md5,
                    modifiedAt = entry.remote.modifiedAt,
                )
            }
        }
        if (ops.isNotEmpty()) queueDao.insertAll(ops)
        jobDao.updatePending(jobId, ops.size)
        android.util.Log.i(
            TAG,
            "plan: ${entries.size} entries -> ${ops.size} transfers, " +
                "${entries.count { it is PlanEntry.Skip }} skips",
        )
    }

    // ---- execution ----

    private suspend fun execute(
        jobId: Long,
        treeUri: Uri,
        onProgress: (SyncProgress) -> Unit,
    ): Result {
        val job = jobDao.get(jobId)!!.toDomain()
        val folderIdCache = mutableMapOf("" to job.driveFolderId)
        val speed = SpeedTracker()
        var hadTransientFailures = false

        val ready = queueDao.readyForJob(jobId, System.currentTimeMillis())
        android.util.Log.i(TAG, "execute: ${ready.size} ops ready")
        val totalBytes = ready.sumOf { it.sizeBytes }.coerceAtLeast(1)
        var doneBytes = 0L

        for ((index, op) in ready.withIndex()) {
            currentCoroutineContext().ensureActive()
            queueDao.update(op.copy(state = TransferQueueEntity.STATE_RUNNING))

            val emitProgress: (Long) -> Unit = { currentBytes ->
                val now = System.currentTimeMillis()
                val speedBps = speed.speedBytesPerSec(now)
                val remaining = (totalBytes - doneBytes - currentBytes).coerceAtLeast(0)
                onProgress(
                    SyncProgress(
                        jobId = jobId,
                        currentFile = op.fileName,
                        percent = ((doneBytes + currentBytes) * 100 / totalBytes).toInt().coerceIn(0, 100),
                        speedBytesPerSec = speedBps,
                        etaSeconds = if (speedBps > 0) remaining / speedBps else null,
                        pending = ready.size - index,
                    ),
                )
            }

            try {
                var lastBytes = 0L
                val trackBytes: (Long) -> Unit = { bytes ->
                    speed.onBytes(bytes - lastBytes, System.currentTimeMillis())
                    lastBytes = bytes
                    emitProgress(bytes)
                }
                emitProgress(0)

                when (op.opType) {
                    TransferQueueEntity.OP_UPLOAD -> executeUpload(job.accountEmail, op, folderIdCache, trackBytes)
                    TransferQueueEntity.OP_DOWNLOAD -> executeDownload(job.accountEmail, treeUri, op, trackBytes)
                }

                doneBytes += op.sizeBytes
                queueDao.update(op.copy(state = TransferQueueEntity.STATE_DONE))
                if (op.opType == TransferQueueEntity.OP_UPLOAD) jobDao.incrementUploaded(jobId)
                else jobDao.incrementDownloaded(jobId)

                var successReason: String? = null
                if (op.opType == TransferQueueEntity.OP_UPLOAD && job.deleteAfterUpload && op.localUri != null) {
                    successReason = if (scanner.deleteFile(op.localUri)) {
                        "Uploaded and deleted from device"
                    } else {
                        "Uploaded — could not delete local copy"
                    }
                }
                log(
                    jobId,
                    if (op.opType == TransferQueueEntity.OP_UPLOAD) TransferDirection.UPLOAD else TransferDirection.DOWNLOAD,
                    op.fileName, op.sizeBytes, TransferResult.SUCCESS, successReason,
                )
                jobDao.updatePending(jobId, queueDao.countPending(jobId))
            } catch (e: DriveQuotaException) {
                throw e // fatal for the whole run
            } catch (e: DriveAuthException) {
                throw e
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive() // don't treat cancellation as failure
                android.util.Log.e(TAG, "execute: ${op.opType} failed for ${op.fileName}", e)
                val attempts = op.attemptCount + 1
                if (attempts < BackoffPolicy.MAX_ATTEMPTS) {
                    hadTransientFailures = true
                    queueDao.update(
                        op.copy(
                            state = TransferQueueEntity.STATE_PENDING,
                            attemptCount = attempts,
                            nextRetryAt = System.currentTimeMillis() + BackoffPolicy.nextDelayMs(attempts - 1),
                        ),
                    )
                    log(
                        jobId,
                        if (op.opType == TransferQueueEntity.OP_UPLOAD) TransferDirection.UPLOAD else TransferDirection.DOWNLOAD,
                        op.fileName, op.sizeBytes, TransferResult.FAILED,
                        "${e.message ?: "Transfer failed"} — retrying with backoff",
                    )
                } else {
                    queueDao.update(op.copy(state = TransferQueueEntity.STATE_FAILED, attemptCount = attempts))
                    jobDao.incrementFailed(jobId)
                    log(
                        jobId,
                        if (op.opType == TransferQueueEntity.OP_UPLOAD) TransferDirection.UPLOAD else TransferDirection.DOWNLOAD,
                        op.fileName, op.sizeBytes, TransferResult.FAILED,
                        e.message ?: "Transfer failed after ${BackoffPolicy.MAX_ATTEMPTS} attempts",
                    )
                    jobDao.updatePending(jobId, queueDao.countPending(jobId))
                }
            }
        }

        val stillPending = queueDao.countPending(jobId)
        android.util.Log.i(TAG, "execute: done; stillPending=$stillPending transientFailures=$hadTransientFailures")
        return if (stillPending > 0 || hadTransientFailures) {
            jobDao.updatePending(jobId, stillPending)
            Result.Retry
        } else {
            queueDao.clearFinished(jobId)
            jobDao.markCompleted(jobId, SyncStatus.UP_TO_DATE.name, System.currentTimeMillis())
            Result.Success
        }
    }

    private suspend fun executeUpload(
        accountEmail: String,
        op: TransferQueueEntity,
        folderIdCache: MutableMap<String, String>,
        onBytes: (Long) -> Unit,
    ) {
        val local = LocalFile(op.fileName, op.relativePath, op.sizeBytes, op.modifiedAt, op.localUri!!)
        provider.upload(
            accountEmail = accountEmail,
            folderIdForPath = { dir -> resolveFolderId(accountEmail, dir, folderIdCache) },
            file = local,
            overwriteFileId = op.remoteFileId,
            openStream = { scanner.openIn(op.localUri) },
            onProgress = onBytes,
        )
    }

    private suspend fun executeDownload(
        accountEmail: String,
        treeUri: Uri,
        op: TransferQueueEntity,
        onBytes: (Long) -> Unit,
    ) {
        val remote = RemoteFile(op.remoteFileId!!, op.fileName, op.relativePath, op.sizeBytes, op.modifiedAt, op.md5)
        val pending = scanner.prepareDownload(treeUri, op.relativePath)
        try {
            provider.download(accountEmail, remote, { pending.openSink() }, onBytes)
            pending.commit()
        } catch (e: Exception) {
            pending.abort()
            throw e
        }
    }

    /** Resolves (creating when missing) the Drive folder id for a relative directory path. */
    private suspend fun resolveFolderId(
        accountEmail: String,
        relativeDir: String,
        cache: MutableMap<String, String>,
    ): String {
        if (relativeDir.isEmpty()) return cache.getValue("")
        cache[relativeDir]?.let { return it }
        val parentDir = relativeDir.substringBeforeLast('/', "")
        val name = relativeDir.substringAfterLast('/')
        val parentId = resolveFolderId(accountEmail, parentDir, cache)
        val existing = provider.listFolders(accountEmail, parentId).firstOrNull { it.name == name }
        val id = existing?.id ?: provider.createFolder(accountEmail, parentId, name).id
        cache[relativeDir] = id
        return id
    }

    /** Resets any RUNNING rows back to PENDING (crash/cancel recovery). */
    suspend fun resetInterrupted(jobId: Long) {
        queueDao.resetRunning(jobId)
    }

    private suspend fun log(
        jobId: Long,
        direction: TransferDirection,
        fileName: String,
        sizeBytes: Long,
        result: TransferResult,
        reason: String?,
    ) {
        logDao.insert(
            TransferLogEntry(
                jobId = jobId,
                timestamp = System.currentTimeMillis(),
                direction = direction,
                fileName = fileName,
                sizeBytes = sizeBytes,
                result = result,
                reason = reason,
            ).toEntity(),
        )
    }
}
