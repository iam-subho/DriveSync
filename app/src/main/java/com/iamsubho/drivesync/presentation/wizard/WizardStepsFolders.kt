package com.iamsubho.drivesync.presentation.wizard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import com.iamsubho.drivesync.presentation.common.AccountAvatar
import com.iamsubho.drivesync.presentation.common.PillButtonOutlined
import com.iamsubho.drivesync.presentation.common.RadioCard
import com.iamsubho.drivesync.presentation.home.accountColor
import com.iamsubho.drivesync.presentation.theme.DsColors

@Composable
fun RadioIcon(selected: Boolean, modifier: Modifier = Modifier) {
    Icon(
        imageVector = if (selected) Icons.Outlined.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
        contentDescription = null,
        tint = if (selected) DsColors.Primary else DsColors.DisabledFg,
        modifier = modifier.size(22.dp),
    )
}

// ---------- Step 1: Choose Google Drive account ----------

@Composable
fun Step1Account(state: WizardUiState, viewModel: WizardViewModel) {
    StepHeading(
        title = "Choose Google Drive account",
        subtitle = "Files will sync with this account's Drive.",
    )
    Column {
        state.accounts.forEach { account ->
            val selected = state.selectedAccountEmail == account.email
            RadioCard(
                selected = selected,
                onClick = { viewModel.pickAccount(account.email) },
                modifier = Modifier.padding(bottom = 10.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AccountAvatar(
                        name = account.displayName,
                        photoUrl = account.photoUrl,
                        color = accountColor(account.email),
                        size = 40.dp,
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 14.dp),
                    ) {
                        Text(
                            text = account.displayName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = DsColors.TextPrimary,
                        )
                        Text(text = account.email, fontSize = 13.sp, color = DsColors.TextSecondary)
                    }
                    RadioIcon(selected)
                }
            }
        }
    }
}

// ---------- Step 2: Choose Drive folder ----------

@Composable
fun Step2DriveFolder(state: WizardUiState, viewModel: WizardViewModel) {
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.selectedAccountEmail) {
        if (state.driveFolders.isEmpty() && !state.foldersLoading) viewModel.loadDriveFolders()
    }

    Text(
        text = "Choose Drive folder",
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium,
        color = DsColors.TextPrimary,
    )
    Text(
        text = "Tap a folder to select it as the sync destination. Use the arrow to browse inside.",
        fontSize = 13.sp,
        color = DsColors.TextSecondary,
        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = { viewModel.navigateDriveFolderPath(state.driveFolderPath.size - 1) },
            enabled = state.driveFolderPath.isNotEmpty() && !state.foldersLoading,
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Go to parent folder",
                tint = if (state.driveFolderPath.isNotEmpty()) DsColors.Primary else DsColors.DisabledFg,
            )
        }
        Text(
            text = (listOf("My Drive") + state.driveFolderPath.map { it.name }).joinToString(" / "),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = DsColors.TextPrimary,
            modifier = Modifier.weight(1f),
        )
    }

    if (state.foldersLoading) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
            CircularProgressIndicator(color = DsColors.Primary, modifier = Modifier.size(28.dp))
        }
    }

    state.folderError?.let { error ->
        Text(
            text = error,
            fontSize = 13.sp,
            color = DsColors.Error,
            modifier = Modifier.padding(bottom = 10.dp),
        )
    }

    Column {
        if (!state.foldersLoading && state.driveFolders.isEmpty()) {
            Text(
                text = "No folders in this location. You can create one here.",
                fontSize = 13.sp,
                color = DsColors.TextTertiary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        state.driveFolders.forEach { folder ->
            val selected = state.selectedFolderId == folder.id
            RadioCard(
                selected = selected,
                onClick = { viewModel.pickFolder(folder) },
                cornerRadius = 14.dp,
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Row(
                    modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Folder, contentDescription = null,
                        tint = DsColors.TextTertiary, modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = folder.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = DsColors.TextPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                    )
                    RadioIcon(selected, modifier = Modifier.size(20.dp))
                    IconButton(
                        onClick = { viewModel.openDriveFolder(folder) },
                        enabled = !state.foldersLoading,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = "Browse inside ${folder.name}",
                            tint = DsColors.Primary,
                        )
                    }
                }
            }
        }
    }

    PillButtonOutlined(
        icon = Icons.Outlined.CreateNewFolder,
        text = "Create new folder",
        onClick = { showCreateDialog = true },
        height = 42.dp,
        modifier = Modifier.padding(top = 4.dp),
    )

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = DsColors.Card,
            title = {
                Text(
                    "Create new folder",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                )
            },
            text = {
                Column {
                    Text(
                        text = "Inside ${(listOf("My Drive") + state.driveFolderPath.map { it.name }).joinToString(" / ")}",
                        fontSize = 13.sp,
                        color = DsColors.TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { Text("Folder name") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createFolder(name)
                        showCreateDialog = false
                    },
                    enabled = name.isNotBlank(),
                ) { Text("Create", color = DsColors.Primary, fontWeight = FontWeight.Medium) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = DsColors.TextSecondary)
                }
            },
        )
    }
}

// ---------- Step 3: Choose local folder ----------

@Composable
fun Step3LocalFolder(state: WizardUiState, viewModel: WizardViewModel) {
    val pickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) viewModel.onLocalPicked(uri)
    }

    StepHeading(
        title = "Choose local folder",
        subtitle = "Opens the system folder picker (Storage Access Framework). " +
            "Access permission is saved for background sync.",
    )

    if (state.localFolderUri != null) {
        RadioCard(
            selected = true,
            onClick = { pickerLauncher.launch(null) },
            cornerRadius = 14.dp,
            modifier = Modifier.padding(bottom = 10.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Folder, contentDescription = null,
                    tint = DsColors.TextTertiary, modifier = Modifier.size(22.dp),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                ) {
                    Text(
                        text = state.localFolderName.orEmpty(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = DsColors.TextPrimary,
                    )
                    Text(
                        text = state.localFolderPath.orEmpty(),
                        fontSize = 12.sp,
                        color = DsColors.TextTertiary,
                    )
                }
                RadioIcon(true, modifier = Modifier.size(20.dp))
            }
        }
    }

    PillButtonOutlined(
        icon = Icons.Outlined.FolderOpen,
        text = if (state.localFolderUri == null) "Choose folder…" else "Choose a different folder",
        onClick = { pickerLauncher.launch(null) },
        height = 42.dp,
    )

    if (state.localFolderUri != null) {
        Text(
            text = "Subfolders",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = DsColors.TextPrimary,
            modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
        )
        Text(
            text = "Uncheck a folder to exclude it and everything inside it from sync.",
            fontSize = 13.sp,
            color = DsColors.TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        if (state.subfoldersLoading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(color = DsColors.Primary, modifier = Modifier.size(24.dp))
            }
        } else if (state.subfoldersError != null) {
            Text(
                text = state.subfoldersError,
                fontSize = 13.sp,
                color = DsColors.Error,
            )
        } else if (state.localSubfolders.isEmpty()) {
            Text(
                text = "This folder has no subfolders.",
                fontSize = 13.sp,
                color = DsColors.TextTertiary,
            )
        } else {
            state.localSubfolders.forEach { folder ->
                val parentExcluded = state.excludedSubfolders.any {
                    folder.relativePath.startsWith("$it/")
                }
                val checked = !parentExcluded && folder.relativePath !in state.excludedSubfolders
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = (folder.depth * 18).dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = checked,
                        enabled = !parentExcluded,
                        onCheckedChange = { viewModel.toggleSubfolder(folder.relativePath) },
                    )
                    Text(
                        text = folder.name,
                        fontSize = 14.sp,
                        color = if (parentExcluded) DsColors.TextTertiary else DsColors.TextPrimary,
                    )
                }
            }
        }
    }
}
