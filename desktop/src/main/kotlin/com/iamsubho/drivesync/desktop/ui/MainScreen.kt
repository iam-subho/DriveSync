package com.iamsubho.drivesync.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iamsubho.drivesync.desktop.AppState
import com.iamsubho.drivesync.desktop.PairRuntime
import com.iamsubho.drivesync.desktop.config.LogEntry
import com.iamsubho.drivesync.desktop.config.SyncPairConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun formatBytes(bytes: Long): String {
    val mb = 1024L * 1024
    return when {
        bytes >= mb * 1024 -> "%.1f GB".format(Locale.US, bytes.toDouble() / (mb * 1024))
        bytes >= mb -> "%.1f MB".format(Locale.US, bytes.toDouble() / mb)
        else -> "${(bytes / 1024).coerceAtLeast(1)} KB"
    }
}

private fun formatTime(ts: Long): String =
    SimpleDateFormat("MMM d, h:mm a", Locale.US).format(Date(ts))

private fun directionIcon(direction: String): ImageVector = when (direction) {
    "UPLOAD_ONLY" -> Icons.Outlined.Upload
    "DOWNLOAD_ONLY" -> Icons.Outlined.Download
    else -> Icons.Outlined.SyncAlt
}

private fun directionLabel(direction: String): String = when (direction) {
    "UPLOAD_ONLY" -> "Upload only"
    "DOWNLOAD_ONLY" -> "Download only"
    else -> "Two-way sync"
}

@Composable
fun MainScreen(appState: AppState, onOpenSettings: () -> Unit) {
    val config by appState.config.collectAsState()
    val statuses by appState.statuses.collectAsState()
    var editingPair by remember { mutableStateOf<SyncPairConfig?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var selectedPairId by remember { mutableStateOf<String?>(null) }
    var deleteCandidate by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DsColors.Primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.CloudSync, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text("DriveSync", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = DsColors.TextPrimary)
                Text(
                    config.account?.email ?: "",
                    fontSize = 12.sp, color = DsColors.TextSecondary,
                )
            }
            Spacer(Modifier.weight(1f))
            DsButton("Add sync pair", Icons.Outlined.Add) {
                editingPair = null
                showEditor = true
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, "Settings", tint = DsColors.TextSecondary)
            }
        }

        Row(modifier = Modifier.fillMaxSize().padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
            // Pair list
            LazyColumn(
                modifier = Modifier.weight(1.2f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
            ) {
                if (config.pairs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(DsColors.Card)
                                .border(1.dp, DsColors.CardBorder, RoundedCornerShape(16.dp))
                                .padding(32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "No sync pairs yet. Add one to link a PC folder with Drive.",
                                fontSize = 13.sp, color = DsColors.TextSecondary,
                            )
                        }
                    }
                }
                items(count = config.pairs.size, key = { config.pairs[it].id }) { index ->
                    val pair = config.pairs[index]
                    PairCard(
                        pair = pair,
                        runtime = statuses[pair.id] ?: PairRuntime(),
                        selected = selectedPairId == pair.id,
                        onSelect = { selectedPairId = pair.id },
                        onRun = { appState.runNow(pair.id) },
                        onStop = { appState.stopRun(pair.id) },
                        onPauseToggle = { appState.togglePaused(pair.id) },
                        onEdit = { editingPair = pair; showEditor = true },
                        onDelete = { deleteCandidate = pair.id },
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            // Logs panel for the selected pair
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DsColors.Card)
                    .border(1.dp, DsColors.CardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) {
                val pairId = selectedPairId
                val logs = remember(pairId, statuses) {
                    pairId?.let { appState.configStore.readLogs(it) } ?: emptyList()
                }
                Text(
                    if (pairId == null) "Logs" else "Logs — ${config.pairs.firstOrNull { it.id == pairId }?.name ?: ""}",
                    fontSize = 15.sp, fontWeight = FontWeight.Medium, color = DsColors.TextPrimary,
                )
                if (pairId == null) {
                    Text(
                        "Select a sync pair to see its transfer history.",
                        fontSize = 12.5.sp, color = DsColors.TextTertiary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else if (logs.isEmpty()) {
                    Text(
                        "No transfers logged yet.",
                        fontSize = 12.5.sp, color = DsColors.TextTertiary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(count = logs.size) { i -> LogRow(logs[i]) }
                    }
                }
            }
        }
    }

    if (showEditor) {
        PairEditorDialog(
            appState = appState,
            existing = editingPair,
            onDismiss = { showEditor = false },
        )
    }

    deleteCandidate?.let { id ->
        ConfirmDialog(
            title = "Delete sync pair?",
            text = "The pair and its logs are removed. Files stay untouched.",
            onConfirm = { appState.deletePair(id); if (selectedPairId == id) selectedPairId = null; deleteCandidate = null },
            onDismiss = { deleteCandidate = null },
        )
    }
}

@Composable
private fun PairCard(
    pair: SyncPairConfig,
    runtime: PairRuntime,
    selected: Boolean,
    onSelect: () -> Unit,
    onRun: () -> Unit,
    onStop: () -> Unit,
    onPauseToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DsColors.Card)
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) DsColors.Primary else DsColors.CardBorder,
                RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onSelect)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(DsColors.Tonal),
                contentAlignment = Alignment.Center,
            ) {
                Icon(directionIcon(pair.direction), null, tint = DsColors.Primary, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
                Text(
                    pair.name, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                    color = DsColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${pair.localPath}  ↔  My Drive/${pair.driveFolderName}",
                    fontSize = 12.sp, color = DsColors.TextSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${directionLabel(pair.direction)} · every ${pair.intervalHours}h · last: " +
                        (pair.lastSyncAt?.let(::formatTime) ?: "never"),
                    fontSize = 11.5.sp, color = DsColors.TextTertiary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            StatusChip(pair, runtime)
        }

        if (runtime.running && runtime.progress != null) {
            val p = runtime.progress
            LinearProgressIndicator(
                progress = { p.percent / 100f },
                color = DsColors.Primary,
                trackColor = DsColors.TrackGray,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
            )
            Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text(
                    p.currentFile, fontSize = 11.5.sp, color = DsColors.TextSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                Text(
                    "%.1f MB/s".format(Locale.US, p.speedBytesPerSec / (1024.0 * 1024)),
                    fontSize = 11.5.sp, color = DsColors.TextSecondary,
                )
            }
        }

        Row(modifier = Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (runtime.running) {
                DsButton("Stop", Icons.Outlined.Stop, onClick = onStop)
            } else {
                DsButton("Run now", Icons.Outlined.PlayArrow, onClick = onRun)
            }
            Spacer(Modifier.width(8.dp))
            DsButton(
                if (pair.paused) "Resume schedule" else "Pause schedule",
                Icons.Outlined.Pause,
                tonal = true,
                onClick = onPauseToggle,
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, "Edit", tint = DsColors.TextTertiary, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, "Delete", tint = DsColors.TextTertiary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun StatusChip(pair: SyncPairConfig, runtime: PairRuntime) {
    val (label, bg, fg) = when {
        runtime.running -> Triple("Syncing", DsColors.ChipSyncBg, DsColors.ChipSyncFg)
        runtime.error -> Triple("Error", DsColors.ChipErrorBg, DsColors.ChipErrorFg)
        pair.paused -> Triple("Paused", DsColors.ChipNeutralBg, DsColors.ChipNeutralFg)
        runtime.lastMessage == "Up to date" || pair.lastSyncAt != null ->
            Triple(runtime.lastMessage ?: "Up to date", DsColors.ChipSuccessBg, DsColors.ChipSuccessFg)
        else -> Triple("Never synced", DsColors.ChipNeutralBg, DsColors.ChipNeutralFg)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = fg, maxLines = 1)
    }
}

@Composable
private fun LogRow(log: LogEntry) {
    val (chipBg, chipFg) = when (log.result) {
        "SUCCESS" -> DsColors.ChipSuccessBg to DsColors.ChipSuccessFg
        "FAILED" -> DsColors.ChipErrorBg to DsColors.ChipErrorFg
        else -> DsColors.ChipNeutralBg to DsColors.ChipNeutralFg
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, DsColors.CardBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (log.direction == "UPLOAD") Icons.Outlined.Upload else Icons.Outlined.Download,
            null, tint = DsColors.TextTertiary, modifier = Modifier.size(16.dp),
        )
        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text(
                log.fileName, fontSize = 12.5.sp, fontWeight = FontWeight.Medium,
                color = DsColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                buildString {
                    append(formatTime(log.timestamp))
                    append(" · ")
                    append(formatBytes(log.sizeBytes))
                    log.reason?.let { append(" · $it") }
                },
                fontSize = 10.5.sp, color = DsColors.TextTertiary,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(chipBg)
                .padding(horizontal = 7.dp, vertical = 2.dp),
        ) {
            Text(
                log.result.lowercase().replaceFirstChar { it.uppercase() },
                fontSize = 9.5.sp, fontWeight = FontWeight.Medium, color = chipFg,
            )
        }
    }
}

@Composable
fun DsButton(
    text: String,
    icon: ImageVector? = null,
    tonal: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .height(34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(if (tonal) DsColors.ChipNeutralBg else DsColors.Tonal)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = DsColors.TonalFg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = DsColors.TonalFg)
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DsColors.Card,
        title = { Text(title, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = DsColors.TextPrimary) },
        text = { Text(text, fontSize = 13.5.sp, color = DsColors.TextSecondary) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onConfirm) {
                Text("Delete", color = DsColors.Error, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel", color = DsColors.Primary)
            }
        },
    )
}
