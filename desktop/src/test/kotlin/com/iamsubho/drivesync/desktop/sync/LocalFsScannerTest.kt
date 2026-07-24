package com.iamsubho.drivesync.desktop.sync

import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LocalFsScannerTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `scan builds nested relative paths with forward slashes`() {
        tmp.newFile("root.txt")
        tmp.newFolder("Sub")
        tmp.newFile("Sub/inner.jpg")
        tmp.newFolder("Sub", "Deep")
        tmp.newFile("Sub/Deep/leaf.pdf")

        val files = LocalFsScanner.scan(tmp.root, emptyList())
        val paths = files.map { it.relativePath }.sorted()

        assertThat(paths).containsExactly("Sub/Deep/leaf.pdf", "Sub/inner.jpg", "root.txt")
        // The engine derives the Drive parent folder from everything before the last '/'.
        assertThat("Sub/Deep/leaf.pdf".substringBeforeLast('/', "")).isEqualTo("Sub/Deep")
        assertThat("root.txt".substringBeforeLast('/', "")).isEmpty()
    }

    @Test
    fun `excluded subtrees are skipped entirely`() {
        tmp.newFolder("Keep")
        tmp.newFile("Keep/a.txt")
        tmp.newFolder("Skip")
        tmp.newFile("Skip/b.txt")

        val files = LocalFsScanner.scan(tmp.root, listOf("Skip"))
        assertThat(files.map { it.relativePath }).containsExactly("Keep/a.txt")
    }

    @Test
    fun `listSubfolders returns immediate directories sorted`() {
        tmp.newFolder("B")
        tmp.newFolder("A")
        tmp.newFile("file.txt")

        assertThat(LocalFsScanner.listSubfolders(tmp.root)).containsExactly("A", "B").inOrder()
    }
}
