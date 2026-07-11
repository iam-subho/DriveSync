package com.iamsubho.drivesync.presentation.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.iamsubho.drivesync.domain.model.SyncStatus
import com.iamsubho.drivesync.domain.model.TransferDirection
import com.iamsubho.drivesync.domain.model.TransferLogEntry
import com.iamsubho.drivesync.domain.model.TransferResult
import com.iamsubho.drivesync.presentation.common.ConfirmDialog
import com.iamsubho.drivesync.presentation.common.DsCard
import com.iamsubho.drivesync.presentation.common.Formatters
import com.iamsubho.drivesync.presentation.common.HeaderIconButton
import com.iamsubho.drivesync.presentation.common.InfoRowsCard
import com.iamsubho.drivesync.presentation.common.PrimaryButton
import com.iamsubho.drivesync.presentation.common.StatusChip
import com.iamsubho.drivesync.presentation.common.ThinProgressBar
import com.iamsubho.drivesync.presentation.common.TonalButton
import com.iamsubho.drivesync.presentation.common.statusChipStyle
import com.iamsubho.drivesync.presentation.home.label
import com.iamsubho.drivesync.presentation.theme.DsColors
import com.iamsubho.drivesync.presentation.wizard.typeLabel
import com.iamsubho.drivesync.domain.model.FileTypeCategory
import com.iamsubho.drivesync.domain.model.LimitMode

@Composable
fun DetailsScreen(
    onBack: () -> Unit,
    viewModel: DetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val job = state.job

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
            Text(
                text = job?.name.orEmpty(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = DsColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
            )
            if (job != null) {
                StatusChip(job.status, modifier = Modifier.padding(end = 8.dp))
            }
        }

        // Tabs
        Row(modifier = Modifier.fillMaxWidth()) {
            DetailsTabItem("Overview", state.activeTab == DetailsTab.OVERVIEW) {
                viewModel.selectTab(DetailsTab.OVERVIEW)
            }
            DetailsTabItem("Logs", state.activeTab == DetailsTab.LOGS) {
                viewModel.selectTab(DetailsTab.LOGS)
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(DsColors.FooterBorder),
        )

        if (job == null) return@Column

        when (state.activeTab) {
            DetailsTab.OVERVIEW -> OverviewTab(state, viewModel)
            DetailsTab.LOGS -> LogsTab(state, viewModel)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.DetailsTabItem(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (active) DsColors.Primary else DsColors.TextSecondary,
            modifier = Modifier.padding(top = 12.dp, bottom = 10.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(if (active) DsColors.Primary else androidx.compose.ui.graphics.Color.Transparent),
        )
    }
}

// ---------- Overview tab ----------

@Composable
private fun OverviewTab(state: DetailsUiState, viewModel: DetailsViewModel) {
    val job = state.job ?: return
    val isSyncing = job.status == SyncStatus.SYNCING

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Status card
        DsCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (isSyncing && state.progress != null) {
                    Text(
                        text = "Syncing — ${state.progress.currentFile}",
                        fontSize = 13.sp,
                        color = DsColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    ThinProgressBar(
                        fraction = state.progress.percent / 100f,
                        height = 8.dp,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = Formatters.formatSpeed(state.progress.speedBytesPerSec),
                            fontSize = 12.sp, color = DsColors.TextTertiary,
                        )
                        Text(
                            text = state.progress.etaSeconds?.let(Formatters::formatEta) ?: "",
                            fontSize = 12.sp, color = DsColors.TextTertiary,
                        )
                    }
                } else {
                    val chip = statusChipStyle(job.status)
                    val statusLine = when (job.status) {
                        SyncStatus.UP_TO_DATE -> "Everything is in sync."
                        SyncStatus.PAUSED -> "Sync is paused. ${job.pendingCount} files pending."
                        SyncStatus.NEVER_SYNCED -> "This job has not run yet."
                        SyncStatus.SYNCING -> "Preparing sync…"
                        SyncStatus.ERROR -> job.errorMessage ?: "Sync failed."
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        chip.icon?.let {
                            Icon(it, contentDescription = null, tint = chip.fg, modifier = Modifier.size(22.dp))
                        }
                        Text(
                            text = statusLine,
                            fontSize = 14.sp,
                            color = DsColors.TextSecondary,
                            modifier = Modifier.padding(start = 10.dp),
                        )
                    }
                }

                Row(modifier = Modifier.padding(top = 14.dp)) {
                    if (!isSyncing) {
                        PrimaryButton(
                            text = "Start sync",
                            icon = Icons.Outlined.PlayArrow,
                            onClick = viewModel::start,
                        )
                    } else {
                        TonalButton(
                            text = "Pause",
                            icon = Icons.Outlined.Pause,
                            onClick = viewModel::pause,
                            bg = DsColors.GrayButton,
                            fg = DsColors.TextPrimary,
                            height = 40.dp,
                        )
                    }
                }
            }
        }

        // 2×2 stat tiles
        val pendingShown = state.progress?.pending ?: job.pendingCount
        val stats = listOf(
            Triple("Files pending", pendingShown.toString(), DsColors.TextPrimary),
            Triple("Files uploaded", "%,d".format(job.uploadedCount), DsColors.Primary),
            Triple("Files downloaded", "%,d".format(job.downloadedCount), DsColors.ChipSuccessFg),
            Triple(
                "Files failed", job.failedCount.toString(),
                if (job.failedCount > 0) DsColors.Error else DsColors.TextPrimary,
            ),
        )
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(stats[0])
                StatTile(stats[2])
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(stats[1])
                StatTile(stats[3])
            }
        }

        // Info rows
        val typeNames = if (FileTypeCategory.ALL in job.fileTypes) {
            "All files"
        } else {
            job.fileTypes.joinToString(", ") { typeLabel(it) }.ifEmpty { "—" }
        }

        fun limitLabel(mb: Int?, mode: LimitMode) = when {
            mb == null -> "No limit"
            mode == LimitMode.SKIP_ABOVE -> "$mb MB — skip above"
            else -> "$mb MB — only above"
        }

        InfoRowsCard(
            rows = listOf(
                "Drive account" to job.accountEmail,
                "Drive folder" to "My Drive/${job.driveFolderName}",
                "Local folder" to Formatters.decodeTreePath(job.localFolderUri),
                "Direction" to job.direction.label,
                "File types" to typeNames,
                "Upload limit" to limitLabel(job.uploadLimitMb, job.uploadLimitMode),
                "Download limit" to limitLabel(job.downloadLimitMb, job.downloadLimitMode),
                "Storage used" to (
                    state.account?.let {
                        "${Formatters.formatGb(it.storageUsedBytes)} of ${Formatters.formatGb(it.storageTotalBytes)}"
                    } ?: "—"
                    ),
                "Last sync" to (job.lastSyncAt?.let(Formatters::formatTimestamp) ?: "Never"),
                "Next sync" to "Every ${job.syncIntervalHours} hours (automatic)",
                "After upload" to if (job.deleteAfterUpload) "Delete local file" else "Keep local file",
                "Created" to Formatters.formatDate(job.createdAt),
            ),
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

@Composable
private fun StatTile(stat: Triple<String, String, androidx.compose.ui.graphics.Color>) {
    DsCard(cornerRadius = 14.dp, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = stat.second,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = stat.third,
            )
            Text(
                text = stat.first,
                fontSize = 12.sp,
                color = DsColors.TextTertiary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

// ---------- Logs tab ----------

@Composable
private fun LogsTab(state: DetailsUiState, viewModel: DetailsViewModel) {
    val logs = viewModel.logs.collectAsLazyPagingItems()
    var confirmClear by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${state.logCount} entries · newest first",
                fontSize = 12.sp,
                color = DsColors.TextTertiary,
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .border(1.dp, DsColors.InputBorder, RoundedCornerShape(17.dp))
                    .clickable { confirmClear = true }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.DeleteSweep, contentDescription = null,
                    tint = DsColors.Error, modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "Clear logs",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = DsColors.Error,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }

        if (logs.itemCount == 0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = null,
                    tint = DsColors.TextTertiary, modifier = Modifier.size(34.dp),
                )
                Text(
                    text = "No transfer logs yet.",
                    fontSize = 13.sp,
                    color = DsColors.TextTertiary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(count = logs.itemCount) { index ->
                    logs[index]?.let { LogRow(it) }
                }
            }
        }
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "Clear logs?",
            text = "All transfer log entries for this job will be deleted.",
            confirmLabel = "Clear",
            onConfirm = {
                viewModel.clearLogs()
                confirmClear = false
            },
            onDismiss = { confirmClear = false },
        )
    }
}

@Composable
private fun LogRow(log: TransferLogEntry) {
    val (chipBg, chipFg, chipLabel) = when (log.result) {
        TransferResult.SUCCESS -> Triple(DsColors.ChipSuccessBg, DsColors.ChipSuccessFg, "Success")
        TransferResult.FAILED -> Triple(DsColors.ChipErrorBg, DsColors.ChipErrorFg, "Failed")
        TransferResult.SKIPPED -> Triple(DsColors.ChipNeutralBg, DsColors.ChipNeutralFg, "Skipped")
    }
    DsCard(cornerRadius = 12.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (log.direction == TransferDirection.UPLOAD) Icons.Outlined.Upload else Icons.Outlined.Download,
                contentDescription = null,
                tint = DsColors.TextTertiary,
                modifier = Modifier.size(18.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp, end = 10.dp),
            ) {
                Text(
                    text = log.fileName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = DsColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val meta = buildString {
                    append(Formatters.formatTimestamp(log.timestamp))
                    append(" · ")
                    append(Formatters.formatBytes(log.sizeBytes))
                    log.reason?.let { append(" · $it") }
                }
                Text(
                    text = meta,
                    fontSize = 11.sp,
                    color = DsColors.TextTertiary,
                    modifier = Modifier.padding(top = 1.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(chipBg)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text = chipLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = chipFg,
                )
            }
        }
    }
}
