package com.iamsubho.drivesync.presentation.browser

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iamsubho.drivesync.domain.model.RemoteFolder
import com.iamsubho.drivesync.presentation.common.DsCard
import com.iamsubho.drivesync.presentation.common.EmptyStateCard
import com.iamsubho.drivesync.presentation.common.HeaderIconButton
import com.iamsubho.drivesync.presentation.common.PillButtonOutlined
import com.iamsubho.drivesync.presentation.common.SectionHeader
import com.iamsubho.drivesync.presentation.theme.DsColors

/** Drive folders saved in the app, whether or not a sync job currently uses them. */
@Composable
fun BrowserScreen(
    onBack: () -> Unit,
    onOpenFolder: (folderId: String, folderName: String) -> Unit,
    viewModel: BrowserViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val authorizationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        viewModel.onDriveAuthorizationResult(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(viewModel) {
        viewModel.driveAuthorizationRequests.collect { pendingIntent ->
            authorizationLauncher.launch(
                IntentSenderRequest.Builder(pendingIntent.intentSender).build(),
            )
        }
    }

    fun back() {
        if (!viewModel.backFromBrowser()) onBack()
    }

    BackHandler(enabled = state.browsing) { back() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DsColors.Bg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderIconButton(Icons.AutoMirrored.Outlined.ArrowBack, "Back", ::back)
            Column(modifier = Modifier.padding(start = 4.dp)) {
                Text(
                    text = if (state.browsing) "Add Drive folder" else "See Files",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = DsColors.TextPrimary,
                )
                Text(
                    text = state.email,
                    fontSize = 12.sp,
                    color = DsColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (state.browsing) {
            val currentPath = listOf("My Drive") + state.browsingPath.map { it.name }
            Text(
                text = currentPath.joinToString("  /  "),
                fontSize = 13.sp,
                color = DsColors.TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            state.folderError?.let {
                Text(
                    text = it,
                    fontSize = 13.sp,
                    color = DsColors.Error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            if (state.foldersLoading) {
                CircularProgressIndicator(
                    color = DsColors.Primary,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 12.dp)
                        .size(28.dp),
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            ) {
                if (!state.foldersLoading && state.driveFolders.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Outlined.FolderOff,
                            title = "No folders here",
                            subtitle = "Choose another folder or go back to My Drive.",
                        )
                    }
                }
                items(count = state.driveFolders.size, key = { state.driveFolders[it].id }) { index ->
                    val folder = state.driveFolders[index]
                    val alreadySaved = state.folders.any { it.folderId == folder.id }
                    DriveFolderRow(
                        folder = folder,
                        alreadySaved = alreadySaved,
                        onOpen = { viewModel.openDriveFolder(folder) },
                        onSave = { viewModel.saveFolder(folder) },
                    )
                }
            }
        } else {
            state.folderError?.let {
                Text(
                    text = it,
                    fontSize = 13.sp,
                    color = DsColors.Error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SectionHeader("DRIVE FOLDERS")
                    }
                    PillButtonOutlined(
                        icon = Icons.Outlined.Add,
                        text = "Add existing Drive folder",
                        onClick = viewModel::beginBrowse,
                        height = 42.dp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                    )
                }
                if (state.folders.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Outlined.FolderOff,
                            title = "No saved folders",
                            subtitle = "Add a folder from Drive, or create a sync job to save its folder here.",
                        )
                    }
                }
                items(count = state.folders.size, key = { state.folders[it].folderId }) { index ->
                    val folder = state.folders[index]
                    DsCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        onClick = { onOpenFolder(folder.folderId, folder.folderName) },
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.Folder, contentDescription = null,
                                tint = DsColors.Primary, modifier = Modifier.size(24.dp),
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 14.dp, end = 8.dp),
                            ) {
                                Text(
                                    text = folder.folderName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = DsColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = if (folder.jobNames.isEmpty()) {
                                        "Not linked to a sync job"
                                    } else {
                                        "Synced by ${folder.jobNames.joinToString(", ")}"
                                    },
                                    fontSize = 12.sp,
                                    color = DsColors.TextTertiary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null,
                                tint = DsColors.TextTertiary, modifier = Modifier.size(18.dp),
                            )
                            IconButton(onClick = { viewModel.removeFolder(folder.folderId) }) {
                                Icon(
                                    Icons.Outlined.RemoveCircleOutline,
                                    contentDescription = "Remove from app list",
                                    tint = DsColors.TextTertiary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DriveFolderRow(
    folder: RemoteFolder,
    alreadySaved: Boolean,
    onOpen: () -> Unit,
    onSave: () -> Unit,
) {
    DsCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onOpen)
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.Folder, contentDescription = null,
                    tint = DsColors.Primary, modifier = Modifier.size(24.dp),
                )
                Text(
                    text = folder.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = DsColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 14.dp, end = 8.dp),
                )
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "Browse folder",
                    tint = DsColors.TextTertiary, modifier = Modifier.size(18.dp),
                )
            }
            IconButton(onClick = onSave, enabled = !alreadySaved) {
                Icon(
                    if (alreadySaved) Icons.Outlined.Check else Icons.Outlined.Add,
                    contentDescription = if (alreadySaved) "Already in app list" else "Add to app list",
                    tint = DsColors.Primary,
                )
            }
        }
    }
}
