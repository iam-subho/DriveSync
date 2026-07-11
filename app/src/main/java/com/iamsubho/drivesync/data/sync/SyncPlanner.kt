package com.iamsubho.drivesync.data.sync

import com.iamsubho.drivesync.domain.model.LimitMode
import com.iamsubho.drivesync.domain.model.LocalFile
import com.iamsubho.drivesync.domain.model.RemoteFile
import com.iamsubho.drivesync.domain.model.SyncDirection
import com.iamsubho.drivesync.domain.model.TransferDirection
import kotlin.math.abs

sealed interface PlanEntry {
    data class Upload(val local: LocalFile, val overwriteRemoteId: String?) : PlanEntry
    data class Download(val remote: RemoteFile) : PlanEntry
    data class Skip(
        val fileName: String,
        val sizeBytes: Long,
        val direction: TransferDirection,
        val reason: String,
    ) : PlanEntry
}

/**
 * Pure diffing logic: compares local and remote snapshots and produces the list of
 * operations for one sync run. Conflicts resolve via Newest Wins, constrained by the
 * job's sync direction. Files identical on both sides produce no entry at all;
 * meaningful skips (hash-identical, size limits, direction blocks) are logged.
 */
class SyncPlanner(private val mtimeToleranceMs: Long = 2000L) {

    fun plan(
        direction: SyncDirection,
        locals: List<LocalFile>,
        remotes: List<RemoteFile>,
        filter: FileFilter,
        uploadLimitMb: Int?,
        uploadLimitMode: LimitMode,
        downloadLimitMb: Int?,
        downloadLimitMode: LimitMode,
        deleteAfterUpload: Boolean = false,
        localMd5: (LocalFile) -> String?,
    ): List<PlanEntry> {
        // Delete-after-upload jobs intentionally remove local copies of uploaded files;
        // downloading remote-only files back would ping-pong them forever. Download-only
        // jobs never upload, so the flag is meaningless there and ignored.
        val skipRemoteOnly = deleteAfterUpload && direction != SyncDirection.DOWNLOAD_ONLY
        val entries = mutableListOf<PlanEntry>()
        val remotesByPath = remotes.associateBy { it.relativePath }
        val matchedRemoteIds = HashSet<String>()

        for (local in locals) {
            if (!filter.matchesType(local.name)) continue
            val remote = remotesByPath[local.relativePath]

            if (remote == null) {
                if (direction != SyncDirection.DOWNLOAD_ONLY) {
                    entries += uploadOrSkip(local, null, uploadLimitMb, uploadLimitMode)
                }
                continue
            }
            matchedRemoteIds += remote.id

            val mtimeDiff = local.modifiedAt - remote.modifiedAt
            val sameSize = local.sizeBytes == remote.sizeBytes
            if (sameSize && abs(mtimeDiff) <= mtimeToleranceMs) continue // unchanged

            if (sameSize && isHashIdentical(local, remote, localMd5)) {
                entries += PlanEntry.Skip(
                    local.name, local.sizeBytes, TransferDirection.UPLOAD, "Identical hash on Drive",
                )
                continue
            }

            // Newest Wins, constrained by direction.
            if (mtimeDiff >= 0) { // local is newer (ties favor local)
                entries += if (direction == SyncDirection.DOWNLOAD_ONLY) {
                    PlanEntry.Skip(
                        local.name, local.sizeBytes, TransferDirection.DOWNLOAD,
                        "Local copy is newer (Newest Wins)",
                    )
                } else {
                    uploadOrSkip(local, remote.id, uploadLimitMb, uploadLimitMode)
                }
            } else { // remote is newer
                entries += if (direction == SyncDirection.UPLOAD_ONLY) {
                    PlanEntry.Skip(
                        remote.name, remote.sizeBytes, TransferDirection.UPLOAD,
                        "Drive copy is newer (Newest Wins)",
                    )
                } else {
                    downloadOrSkip(remote, downloadLimitMb, downloadLimitMode)
                }
            }
        }

        for (remote in remotes) {
            if (remote.id in matchedRemoteIds) continue
            if (!filter.matchesType(remote.name)) continue
            if (direction != SyncDirection.UPLOAD_ONLY && !skipRemoteOnly) {
                entries += downloadOrSkip(remote, downloadLimitMb, downloadLimitMode)
            }
        }
        return entries
    }

    private fun isHashIdentical(
        local: LocalFile,
        remote: RemoteFile,
        localMd5: (LocalFile) -> String?,
    ): Boolean {
        val remoteMd5 = remote.md5 ?: return false
        val md5 = localMd5(local) ?: return false
        return md5.equals(remoteMd5, ignoreCase = true)
    }

    private fun uploadOrSkip(local: LocalFile, overwriteId: String?, limitMb: Int?, mode: LimitMode): PlanEntry =
        if (FileFilter.passesSizeLimit(local.sizeBytes, limitMb, mode)) {
            PlanEntry.Upload(local, overwriteId)
        } else {
            PlanEntry.Skip(
                local.name, local.sizeBytes, TransferDirection.UPLOAD,
                FileFilter.sizeLimitSkipReason(limitMb!!, mode),
            )
        }

    private fun downloadOrSkip(remote: RemoteFile, limitMb: Int?, mode: LimitMode): PlanEntry =
        if (FileFilter.passesSizeLimit(remote.sizeBytes, limitMb, mode)) {
            PlanEntry.Download(remote)
        } else {
            PlanEntry.Skip(
                remote.name, remote.sizeBytes, TransferDirection.DOWNLOAD,
                FileFilter.sizeLimitSkipReason(limitMb!!, mode),
            )
        }
}
