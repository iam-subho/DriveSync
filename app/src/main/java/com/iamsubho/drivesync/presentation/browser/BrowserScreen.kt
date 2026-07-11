package com.iamsubho.drivesync.presentation.browser

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iamsubho.drivesync.presentation.common.DsCard
import com.iamsubho.drivesync.presentation.common.EmptyStateCard
import com.iamsubho.drivesync.presentation.common.HeaderIconButton
import com.iamsubho.drivesync.presentation.common.SectionHeader
import com.iamsubho.drivesync.presentation.theme.DsColors

/** "See Files" — the Drive folders this account's sync jobs use. */
@Composable
fun BrowserScreen(
    onBack: () -> Unit,
    onOpenFolder: (folderId: String, folderName: String) -> Unit,
    viewModel: BrowserViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
            HeaderIconButton(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack)
            Column(modifier = Modifier.padding(start = 4.dp)) {
                Text(
                    text = "See Files",
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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        ) {
            item { SectionHeader("SYNCED DRIVE FOLDERS") }

            if (state.folders.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Outlined.FolderOff,
                        title = "No synced folders",
                        subtitle = "Create a sync job with this account to see its Drive folders here.",
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
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
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
                                text = "Synced by ${folder.jobNames.joinToString(", ")}",
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
                    }
                }
            }
        }
    }
}
