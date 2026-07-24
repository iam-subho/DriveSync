package com.iamsubho.drivesync.data.sync

import com.google.common.truth.Truth.assertThat
import com.iamsubho.drivesync.domain.model.FileTypeCategory
import com.iamsubho.drivesync.domain.model.LimitMode
import com.iamsubho.drivesync.domain.model.LocalFile
import com.iamsubho.drivesync.domain.model.RemoteFile
import com.iamsubho.drivesync.domain.model.SyncDirection
import com.iamsubho.drivesync.domain.model.TransferDirection
import org.junit.Test

class SyncPlannerTest {

    private val planner = SyncPlanner(mtimeToleranceMs = 2000L)
    private val allFilter = FileFilter(setOf(FileTypeCategory.ALL), emptyMap(), emptySet())
    private val imagesFilter = FileFilter(
        setOf(FileTypeCategory.IMAGES),
        mapOf(FileTypeCategory.IMAGES to setOf("jpg")),
        emptySet(),
    )

    private fun local(
        path: String, size: Long = 100, mtime: Long = 10_000,
    ) = LocalFile(path.substringAfterLast('/'), path, size, mtime, "content://$path")

    private fun remote(
        path: String, size: Long = 100, mtime: Long = 10_000, md5: String? = null, id: String = "r-$path",
    ) = RemoteFile(id, path.substringAfterLast('/'), path, size, mtime, md5)

    private fun plan(
        direction: SyncDirection,
        locals: List<LocalFile>,
        remotes: List<RemoteFile>,
        filter: FileFilter = allFilter,
        upMb: Int? = null, upMode: LimitMode = LimitMode.SKIP_ABOVE,
        downMb: Int? = null, downMode: LimitMode = LimitMode.SKIP_ABOVE,
        deleteAfterUpload: Boolean = false,
        md5: (LocalFile) -> String? = { null },
    ) = planner.plan(
        direction, locals, remotes, filter, upMb, upMode, downMb, downMode,
        deleteAfterUpload, md5,
    )

    // ---- missing on one side ----

    @Test
    fun `local-only file uploads in two-way and upload-only`() {
        val locals = listOf(local("a.jpg"))
        for (dir in listOf(SyncDirection.TWO_WAY, SyncDirection.UPLOAD_ONLY)) {
            val entries = plan(dir, locals, emptyList())
            assertThat(entries).hasSize(1)
            val up = entries[0] as PlanEntry.Upload
            assertThat(up.local.relativePath).isEqualTo("a.jpg")
            assertThat(up.overwriteRemoteId).isNull()
        }
    }

    @Test
    fun `local-only file is ignored in download-only`() {
        assertThat(plan(SyncDirection.DOWNLOAD_ONLY, listOf(local("a.jpg")), emptyList())).isEmpty()
    }

    @Test
    fun `remote-only file downloads in two-way and download-only`() {
        val remotes = listOf(remote("b.jpg"))
        for (dir in listOf(SyncDirection.TWO_WAY, SyncDirection.DOWNLOAD_ONLY)) {
            val entries = plan(dir, emptyList(), remotes)
            assertThat(entries).hasSize(1)
            assertThat((entries[0] as PlanEntry.Download).remote.relativePath).isEqualTo("b.jpg")
        }
    }

    @Test
    fun `remote-only file is ignored in upload-only`() {
        assertThat(plan(SyncDirection.UPLOAD_ONLY, emptyList(), listOf(remote("b.jpg")))).isEmpty()
    }

    // ---- unchanged files ----

    @Test
    fun `same size and mtime within tolerance is ignored silently`() {
        val entries = plan(
            SyncDirection.TWO_WAY,
            listOf(local("a.jpg", size = 100, mtime = 10_000)),
            listOf(remote("a.jpg", size = 100, mtime = 11_500)),
        )
        assertThat(entries).isEmpty()
    }

    @Test
    fun `same size with mtime beyond tolerance but identical md5 skips with hash reason`() {
        val entries = plan(
            SyncDirection.TWO_WAY,
            listOf(local("a.jpg", size = 100, mtime = 50_000)),
            listOf(remote("a.jpg", size = 100, mtime = 10_000, md5 = "ABC")),
            md5 = { "abc" }, // case-insensitive match
        )
        assertThat(entries).hasSize(1)
        val skip = entries[0] as PlanEntry.Skip
        assertThat(skip.reason).isEqualTo("Identical hash on Drive")
    }

    // ---- newest wins conflicts ----

    @Test
    fun `two-way conflict with newer local uploads with overwrite id`() {
        val entries = plan(
            SyncDirection.TWO_WAY,
            listOf(local("a.jpg", size = 200, mtime = 50_000)),
            listOf(remote("a.jpg", size = 100, mtime = 10_000, id = "rid")),
        )
        assertThat(entries).hasSize(1)
        val up = entries[0] as PlanEntry.Upload
        assertThat(up.overwriteRemoteId).isEqualTo("rid")
    }

    @Test
    fun `two-way conflict with newer remote downloads`() {
        val entries = plan(
            SyncDirection.TWO_WAY,
            listOf(local("a.jpg", size = 200, mtime = 10_000)),
            listOf(remote("a.jpg", size = 100, mtime = 50_000)),
        )
        assertThat(entries).hasSize(1)
        assertThat(entries[0]).isInstanceOf(PlanEntry.Download::class.java)
    }

    @Test
    fun `upload-only conflict with newer remote is skipped with newest-wins reason`() {
        val entries = plan(
            SyncDirection.UPLOAD_ONLY,
            listOf(local("a.jpg", size = 200, mtime = 10_000)),
            listOf(remote("a.jpg", size = 100, mtime = 50_000)),
        )
        assertThat(entries).hasSize(1)
        val skip = entries[0] as PlanEntry.Skip
        assertThat(skip.direction).isEqualTo(TransferDirection.UPLOAD)
        assertThat(skip.reason).isEqualTo("Drive copy is newer (Newest Wins)")
    }

    @Test
    fun `download-only conflict with newer local is skipped with newest-wins reason`() {
        val entries = plan(
            SyncDirection.DOWNLOAD_ONLY,
            listOf(local("a.jpg", size = 200, mtime = 50_000)),
            listOf(remote("a.jpg", size = 100, mtime = 10_000)),
        )
        assertThat(entries).hasSize(1)
        val skip = entries[0] as PlanEntry.Skip
        assertThat(skip.reason).isEqualTo("Local copy is newer (Newest Wins)")
    }

    // ---- filters and limits ----

    @Test
    fun `type filter excludes non-matching files on both sides silently`() {
        val entries = plan(
            SyncDirection.TWO_WAY,
            listOf(local("notes.txt")),
            listOf(remote("clip.mp4")),
            filter = imagesFilter,
        )
        assertThat(entries).isEmpty()
    }

    @Test
    fun `upload above SKIP_ABOVE limit becomes a skip entry`() {
        val fiveMb = 5L * 1024 * 1024
        val entries = plan(
            SyncDirection.UPLOAD_ONLY,
            listOf(local("big.jpg", size = fiveMb + 1)),
            emptyList(),
            upMb = 5,
        )
        assertThat(entries).hasSize(1)
        val skip = entries[0] as PlanEntry.Skip
        assertThat(skip.direction).isEqualTo(TransferDirection.UPLOAD)
        assertThat(skip.reason).isEqualTo("Above size limit (5 MB)")
    }

    @Test
    fun `download below ONLY_ABOVE threshold becomes a skip entry`() {
        val entries = plan(
            SyncDirection.DOWNLOAD_ONLY,
            emptyList(),
            listOf(remote("small.jpg", size = 10)),
            downMb = 5, downMode = LimitMode.ONLY_ABOVE,
        )
        assertThat(entries).hasSize(1)
        val skip = entries[0] as PlanEntry.Skip
        assertThat(skip.direction).isEqualTo(TransferDirection.DOWNLOAD)
        assertThat(skip.reason).isEqualTo("Below size threshold (5 MB)")
    }

    // ---- delete-after-upload safeguard ----

    @Test
    fun `delete-after-upload on two-way never downloads remote-only files`() {
        // The remote-only file was likely uploaded and then deleted locally on purpose —
        // downloading it back would ping-pong forever.
        val entries = plan(
            SyncDirection.TWO_WAY,
            listOf(local("keep.jpg")),
            listOf(remote("keep.jpg"), remote("uploaded-then-deleted.jpg")),
            deleteAfterUpload = true,
        )
        assertThat(entries.filterIsInstance<PlanEntry.Download>()).isEmpty()
    }

    @Test
    fun `delete-after-upload does not affect download-only jobs`() {
        val entries = plan(
            SyncDirection.DOWNLOAD_ONLY,
            emptyList(),
            listOf(remote("b.jpg")),
            deleteAfterUpload = true,
        )
        assertThat(entries.filterIsInstance<PlanEntry.Download>()).hasSize(1)
    }

    @Test
    fun `mixed plan handles multiple files independently`() {
        val entries = plan(
            SyncDirection.TWO_WAY,
            listOf(local("up.jpg"), local("same.jpg", size = 50, mtime = 1_000)),
            listOf(remote("down.jpg"), remote("same.jpg", size = 50, mtime = 1_000)),
        )
        assertThat(entries).hasSize(2)
        assertThat(entries.filterIsInstance<PlanEntry.Upload>()).hasSize(1)
        assertThat(entries.filterIsInstance<PlanEntry.Download>()).hasSize(1)
    }
}
