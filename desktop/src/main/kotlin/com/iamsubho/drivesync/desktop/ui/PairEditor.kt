package com.iamsubho.drivesync.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iamsubho.drivesync.desktop.AppState
import com.iamsubho.drivesync.desktop.config.SyncPairConfig
import com.iamsubho.drivesync.desktop.sync.LocalFsScanner
import com.iamsubho.drivesync.domain.model.RemoteFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.swing.JFileChooser

private val FILE_TYPE_OPTIONS = listOf(
    "ALL" to "All files", "IMAGES" to "Images", "VIDEOS" to "Videos",
    "DOCUMENTS" to "Documents", "AUDIO" to "Audio", "ARCHIVES" to "Archives",
)

private suspend fun pickDirectory(): String? = withContext(Dispatchers.Swing) {
    val chooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        dialogTitle = "Choose folder to sync"
    }
    if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
        chooser.selectedFile.absolutePath
    } else {
        null
    }
}

@Composable
fun PairEditorDialog(
    appState: AppState,
    existing: SyncPairConfig?,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val provider by appState.provider.collectAsState()

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var localPath by remember { mutableStateOf(existing?.localPath ?: "") }
    var driveFolderId by remember { mutableStateOf(existing?.driveFolderId) }
    var driveFolderName by remember { mutableStateOf(existing?.driveFolderName ?: "") }
    var direction by remember { mutableStateOf(existing?.direction ?: "TWO_WAY") }
    var types by remember { mutableStateOf(existing?.fileTypes?.toSet() ?: setOf("ALL")) }
    var intervalHours by remember { mutableStateOf((existing?.intervalHours ?: 24).toString()) }
    var deleteAfterUpload by remember { mutableStateOf(existing?.deleteAfterUpload ?: false) }
    var excluded by remember { mutableStateOf(existing?.excludedFolders?.toSet() ?: emptySet()) }
    var subfolders by remember { mutableStateOf<List<String>>(emptyList()) }
    var driveFolders by remember { mutableStateOf<List<RemoteFolder>>(emptyList()) }
    var loadingFolders by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loadingFolders = true
        try {
            driveFolders = provider?.listFolders(null) ?: emptyList()
        } catch (_: Exception) {
        }
        loadingFolders = false
    }
    LaunchedEffect(localPath) {
        subfolders = if (localPath.isNotBlank()) {
            withContext(Dispatchers.IO) { LocalFsScanner.listSubfolders(File(localPath)) }
        } else {
            emptyList()
        }
    }

    val valid = name.isNotBlank() && localPath.isNotBlank() && driveFolderId != null &&
        types.isNotEmpty() && (intervalHours.toIntOrNull() ?: 0) >= 1

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DsColors.Card,
        modifier = Modifier.widthIn(min = 560.dp, max = 680.dp),
        title = {
            Text(
                if (existing == null) "New sync pair" else "Edit sync pair",
                fontSize = 18.sp, fontWeight = FontWeight.Medium, color = DsColors.TextPrimary,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Local folder
                SectionLabel("Local folder")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        localPath.ifBlank { "Not selected" },
                        fontSize = 13.sp,
                        color = if (localPath.isBlank()) DsColors.TextTertiary else DsColors.TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    DsButton("Browse…") {
                        scope.launch {
                            pickDirectory()?.let { localPath = it }
                        }
                    }
                }

                // Drive folder
                SectionLabel("Drive folder (My Drive)")
                if (loadingFolders) {
                    Text("Loading folders…", fontSize = 12.5.sp, color = DsColors.TextTertiary)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    driveFolders.forEach { folder ->
                        val selected = driveFolderId == folder.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    driveFolderId = folder.id
                                    driveFolderName = folder.name
                                }
                                .padding(vertical = 5.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (selected) Icons.Outlined.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
                                null,
                                tint = if (selected) DsColors.Primary else DsColors.DisabledFg,
                                modifier = Modifier.size(16.dp),
                            )
                            Icon(
                                Icons.Outlined.Folder, null, tint = DsColors.TextTertiary,
                                modifier = Modifier.padding(start = 8.dp).size(15.dp),
                            )
                            Text(
                                folder.name, fontSize = 13.sp, color = DsColors.TextPrimary,
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    }
                }

                // Direction
                SectionLabel("Direction")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "TWO_WAY" to "Two-way", "UPLOAD_ONLY" to "Upload only", "DOWNLOAD_ONLY" to "Download only",
                    ).forEach { (value, label) ->
                        SelectChip(label, direction == value) { direction = value }
                    }
                }

                // File types
                SectionLabel("File types")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FILE_TYPE_OPTIONS.take(3).forEach { (value, label) ->
                        SelectChip(label, value in types) { types = toggleType(types, value) }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    FILE_TYPE_OPTIONS.drop(3).forEach { (value, label) ->
                        SelectChip(label, value in types) { types = toggleType(types, value) }
                    }
                }

                // Schedule + cleanup
                SectionLabel("Schedule")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = intervalHours,
                        onValueChange = { intervalHours = it.filter { c -> c.isDigit() }.take(4) },
                        singleLine = true,
                        modifier = Modifier.width(90.dp),
                    )
                    Text(
                        "hours between runs (runs while the app is open)",
                        fontSize = 12.5.sp, color = DsColors.TextSecondary,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                if (direction != "DOWNLOAD_ONLY") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { deleteAfterUpload = !deleteAfterUpload }
                            .padding(vertical = 4.dp),
                    ) {
                        Icon(
                            if (deleteAfterUpload) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                            null,
                            tint = if (deleteAfterUpload) DsColors.Primary else DsColors.DisabledFg,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "Delete local files after upload (Drive-only files are never downloaded back)",
                            fontSize = 12.5.sp, color = DsColors.TextPrimary,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }

                // Exclusions
                if (subfolders.isNotEmpty()) {
                    SectionLabel("Exclude subfolders")
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        subfolders.forEach { sub ->
                            val checked = sub in excluded
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        excluded = if (checked) excluded - sub else excluded + sub
                                    }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    if (checked) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                                    null,
                                    tint = if (checked) DsColors.Primary else DsColors.DisabledFg,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    sub, fontSize = 13.sp,
                                    color = if (checked) DsColors.TextTertiary else DsColors.TextPrimary,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    appState.addOrUpdatePair(
                        SyncPairConfig(
                            id = existing?.id ?: UUID.randomUUID().toString(),
                            name = name.trim(),
                            localPath = localPath,
                            driveFolderId = driveFolderId!!,
                            driveFolderName = driveFolderName,
                            direction = direction,
                            fileTypes = types.toList(),
                            customExtensions = existing?.customExtensions ?: emptyList(),
                            excludedFolders = excluded.toList().sorted(),
                            uploadLimitMb = existing?.uploadLimitMb,
                            uploadLimitMode = existing?.uploadLimitMode ?: "SKIP_ABOVE",
                            downloadLimitMb = existing?.downloadLimitMb,
                            downloadLimitMode = existing?.downloadLimitMode ?: "SKIP_ABOVE",
                            intervalHours = intervalHours.toIntOrNull() ?: 24,
                            deleteAfterUpload = deleteAfterUpload && direction != "DOWNLOAD_ONLY",
                            lastSyncAt = existing?.lastSyncAt,
                            paused = existing?.paused ?: false,
                        ),
                    )
                    onDismiss()
                },
            ) {
                Text(
                    if (existing == null) "Create" else "Save",
                    color = if (valid) DsColors.Primary else DsColors.DisabledFg,
                    fontWeight = FontWeight.Medium,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = DsColors.TextSecondary) }
        },
    )
}

private fun toggleType(types: Set<String>, value: String): Set<String> {
    val next = if (value in types) types - value else types + value
    return when {
        value == "ALL" && "ALL" in next -> setOf("ALL")
        next.isEmpty() -> setOf("ALL")
        else -> next - if (value != "ALL") "ALL" else ""
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DsColors.TextSecondary,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
    )
}

@Composable
private fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(if (selected) DsColors.Tonal else DsColors.Card)
            .border(
                1.dp,
                if (selected) DsColors.Tonal else DsColors.InputBorder,
                RoundedCornerShape(15.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            color = if (selected) DsColors.TonalFg else DsColors.TextSecondary,
        )
    }
}
