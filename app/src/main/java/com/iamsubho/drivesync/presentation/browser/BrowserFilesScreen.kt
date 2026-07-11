package com.iamsubho.drivesync.presentation.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iamsubho.drivesync.data.local.ExtensionDefaults
import com.iamsubho.drivesync.domain.model.RemoteFile
import com.iamsubho.drivesync.presentation.common.DsCard
import com.iamsubho.drivesync.presentation.common.EmptyStateCard
import com.iamsubho.drivesync.presentation.common.Formatters
import com.iamsubho.drivesync.presentation.common.HeaderIconButton
import com.iamsubho.drivesync.presentation.common.PrimaryButton
import com.iamsubho.drivesync.presentation.theme.DsColors
import com.iamsubho.drivesync.presentation.util.DriveAppLauncher
import com.iamsubho.drivesync.presentation.wizard.wizardFieldColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** All files inside one synced Drive folder — searchable, opens files in the Drive app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserFilesScreen(
    onBack: () -> Unit,
    viewModel: BrowserFilesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var showInstallDrive by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DsColors.Bg),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderIconButton(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
            ) {
                Text(
                    text = state.folderName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = DsColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (state.selectedIds.isEmpty()) state.email
                    else "${state.selectedIds.size} selected",
                    fontSize = 12.sp,
                    color = if (state.selectedIds.isEmpty()) DsColors.TextSecondary else DsColors.Primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (state.selectedIds.isNotEmpty()) {
                if (state.deleting) {
                    CircularProgressIndicator(
                        color = DsColors.Error,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(22.dp),
                    )
                } else {
                    HeaderIconButton(Icons.Outlined.Close, "Clear selection", viewModel::clearSelection)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { confirmDelete = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Delete, contentDescription = "Delete selected files",
                            tint = DsColors.Error, modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }

        // Search row: name query + date chip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text("Search files", fontSize = 14.sp, color = DsColors.DisabledFg) },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Search, contentDescription = null,
                        tint = DsColors.TextTertiary, modifier = Modifier.size(20.dp),
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = wizardFieldColors(),
                modifier = Modifier.weight(1f),
            )
            DateChip(
                selectedDayUtcMillis = state.selectedDayUtcMillis,
                onClick = { showDatePicker = true },
                onClear = { viewModel.setDay(null) },
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        state.notice?.let { notice ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DsColors.Snack)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(text = notice, fontSize = 13.sp, color = DsColors.SnackFg)
            }
        }

        when {
            state.loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = DsColors.Primary, modifier = Modifier.size(32.dp))
            }

            state.error != null -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = state.error.orEmpty(),
                    fontSize = 13.sp,
                    color = DsColors.Error,
                )
                PrimaryButton(
                    text = "Retry",
                    onClick = viewModel::load,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            state.filtered.isEmpty() -> Box(modifier = Modifier.padding(16.dp)) {
                EmptyStateCard(
                    icon = Icons.Outlined.Search,
                    title = if (state.allFiles.isEmpty()) "No files yet" else "No matches",
                    subtitle = if (state.allFiles.isEmpty()) {
                        "Run a sync job to upload files into this folder."
                    } else {
                        "Try a different name or date."
                    },
                )
            }

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(count = state.filtered.size, key = { state.filtered[it].id }) { index ->
                    val file = state.filtered[index]
                    FileRow(
                        file = file,
                        selected = file.id in state.selectedIds,
                        onToggleSelect = { viewModel.toggleSelect(file.id) },
                        onClick = {
                            if (!DriveAppLauncher.openFileInDriveApp(context, file.id)) {
                                showInstallDrive = true
                            }
                        },
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        com.iamsubho.drivesync.presentation.common.ConfirmDialog(
            title = "Delete from Drive?",
            text = "${state.selectedIds.size} file(s) will be permanently deleted from Google Drive. " +
                "This can't be undone. Copies on this device are not affected.",
            confirmLabel = "Delete",
            onConfirm = {
                confirmDelete = false
                viewModel.deleteSelected()
            },
            onDismiss = { confirmDelete = false },
        )
    }

    state.notice?.let { notice ->
        androidx.compose.runtime.LaunchedEffect(notice) {
            kotlinx.coroutines.delay(2_800)
            viewModel.consumeNotice()
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.selectedDayUtcMillis,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setDay(datePickerState.selectedDateMillis)
                        showDatePicker = false
                    },
                ) { Text("Apply", color = DsColors.Primary, fontWeight = FontWeight.Medium) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = DsColors.TextSecondary)
                }
            },
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }

    if (showInstallDrive) {
        AlertDialog(
            onDismissRequest = { showInstallDrive = false },
            containerColor = DsColors.Card,
            title = {
                Text(
                    "Google Drive app required",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = DsColors.TextPrimary,
                )
            },
            text = {
                Text(
                    text = "File previews open in the official Google Drive app, which is not " +
                        "installed on this device.\n\n" +
                        "Please install Google Drive from the Play Store, then sign in with the " +
                        "same account this folder belongs to:\n\n${state.email}\n\n" +
                        "Once Google Drive is installed and authenticated with that email, come " +
                        "back and tap the file again to preview it.",
                    fontSize = 14.sp,
                    color = DsColors.TextSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        DriveAppLauncher.openPlayStoreForDrive(context)
                        showInstallDrive = false
                    },
                ) { Text("Get Google Drive", color = DsColors.Primary, fontWeight = FontWeight.Medium) }
            },
            dismissButton = {
                TextButton(onClick = { showInstallDrive = false }) {
                    Text("Not now", color = DsColors.TextSecondary)
                }
            },
        )
    }
}

@Composable
private fun DateChip(
    selectedDayUtcMillis: Long?,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = selectedDayUtcMillis != null
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) DsColors.Tonal else DsColors.Card)
            .then(
                if (active) Modifier else Modifier.background(DsColors.Card),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.CalendarMonth, contentDescription = "Filter by date",
            tint = if (active) DsColors.TonalFg else DsColors.TextSecondary,
            modifier = Modifier.size(18.dp),
        )
        if (active) {
            val label = SimpleDateFormat("MMM d", Locale.US)
                .apply { timeZone = TimeZone.getTimeZone("UTC") }
                .format(Date(selectedDayUtcMillis!!))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = DsColors.TonalFg,
                modifier = Modifier.padding(start = 6.dp),
            )
            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClear),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Close, contentDescription = "Clear date filter",
                    tint = DsColors.TonalFg, modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun FileRow(
    file: RemoteFile,
    selected: Boolean,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit,
) {
    DsCard(
        cornerRadius = 12.dp,
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (selected) DsColors.Primary else DsColors.CardBorder,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 12.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleSelect),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (selected) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                    contentDescription = if (selected) "Deselect ${file.name}" else "Select ${file.name}",
                    tint = if (selected) DsColors.Primary else DsColors.DisabledFg,
                    modifier = Modifier.size(20.dp),
                )
            }
            Icon(
                imageVector = fileTypeIcon(file.name),
                contentDescription = null,
                tint = DsColors.Primary,
                modifier = Modifier.size(20.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    text = file.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = DsColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val date = file.deviceCreatedAt ?: file.modifiedAt
                Text(
                    text = "${Formatters.formatTimestamp(date)} · ${Formatters.formatBytes(file.sizeBytes)}",
                    fontSize = 11.sp,
                    color = DsColors.TextTertiary,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
    }
}

private fun fileTypeIcon(fileName: String): ImageVector {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when {
        ext in ExtensionDefaults.IMAGES -> Icons.Outlined.Image
        ext in ExtensionDefaults.VIDEOS -> Icons.Outlined.Movie
        ext in ExtensionDefaults.AUDIO -> Icons.Outlined.MusicNote
        ext in ExtensionDefaults.ARCHIVES -> Icons.Outlined.Archive
        ext in ExtensionDefaults.DOCUMENTS -> Icons.Outlined.Description
        else -> Icons.AutoMirrored.Outlined.InsertDriveFile
    }
}
