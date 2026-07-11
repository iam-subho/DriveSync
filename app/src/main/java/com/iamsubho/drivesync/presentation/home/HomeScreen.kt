package com.iamsubho.drivesync.presentation.home

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iamsubho.drivesync.domain.model.DriveAccount
import com.iamsubho.drivesync.domain.model.SyncDirection
import com.iamsubho.drivesync.domain.model.SyncStatus
import com.iamsubho.drivesync.domain.repository.DriveConsentRequiredException
import com.iamsubho.drivesync.presentation.common.AccountAvatar
import com.iamsubho.drivesync.presentation.common.ConfirmDialog
import com.iamsubho.drivesync.presentation.common.DsCard
import com.iamsubho.drivesync.presentation.common.EmptyStateCard
import com.iamsubho.drivesync.presentation.common.Formatters
import com.iamsubho.drivesync.presentation.common.HeaderIconButton
import com.iamsubho.drivesync.presentation.common.PillButtonOutlined
import com.iamsubho.drivesync.presentation.common.SectionHeader
import com.iamsubho.drivesync.presentation.common.StatusChip
import com.iamsubho.drivesync.presentation.common.ThinProgressBar
import com.iamsubho.drivesync.presentation.common.TonalButton
import com.iamsubho.drivesync.presentation.theme.DsColors
import kotlinx.coroutines.delay

val SyncDirection.label: String
    get() = when (this) {
        SyncDirection.TWO_WAY -> "Two-way sync"
        SyncDirection.UPLOAD_ONLY -> "Upload only"
        SyncDirection.DOWNLOAD_ONLY -> "Download only"
    }

val SyncDirection.icon: ImageVector
    get() = when (this) {
        SyncDirection.TWO_WAY -> Icons.Outlined.SyncAlt
        SyncDirection.UPLOAD_ONLY -> Icons.Outlined.Upload
        SyncDirection.DOWNLOAD_ONLY -> Icons.Outlined.Download
    }

fun accountColor(email: String): Color =
    DsColors.AccountColors[Formatters.accountColorIndex(email, DsColors.AccountColors.size)]

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenWizard: () -> Unit,
    onOpenDetails: (Long) -> Unit,
    onOpenBrowser: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var deleteCandidate by remember { mutableStateOf<Long?>(null) }
    var pendingConsent by remember { mutableStateOf<DriveConsentRequiredException?>(null) }
    var jobAwaitingPermission by remember { mutableStateOf<Long?>(null) }

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val request = pendingConsent
        pendingConsent = null
        if (result.resultCode == Activity.RESULT_OK && request != null) {
            viewModel.finishConsent(request)
        } else {
            viewModel.showSnack("Drive access was not granted")
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        jobAwaitingPermission?.let(viewModel::startJob)
        jobAwaitingPermission = null
    }

    val legacySignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        viewModel.onLegacySignInResult(result.data)
    }

    fun startJobWithPermission(jobId: Long) {
        val needsPermission = Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            jobAwaitingPermission = jobId
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.startJob(jobId)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.consentRequests.collect { request ->
            pendingConsent = request
            consentLauncher.launch(IntentSenderRequest.Builder(request.pendingIntent.intentSender).build())
        }
    }

    LaunchedEffect(Unit) {
        viewModel.legacySignInRequests.collect { intent ->
            legacySignInLauncher.launch(intent)
        }
    }

    LaunchedEffect(state.snackMessage) {
        if (state.snackMessage != null) {
            delay(2_800)
            viewModel.consumeSnack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DsColors.Bg),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header: logo box + title + settings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DsColors.Primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.CloudSync, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = "Drive Sync",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = DsColors.TextPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                )
                HeaderIconButton(Icons.Outlined.Settings, "Settings", onOpenSettings)
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
            ) {
                item { SectionHeader("GOOGLE DRIVE ACCOUNTS") }

                if (state.accounts.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Outlined.CloudOff,
                            title = "No accounts connected",
                            subtitle = "Add a Google Drive account to start syncing.",
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                }

                items(count = state.accounts.size, key = { "acct-" + state.accounts[it].email }) { index ->
                    val account = state.accounts[index]
                    AccountCard(
                        account = account,
                        onSeeFiles = { onOpenBrowser(account.email) },
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }

                item {
                    PillButtonOutlined(
                        icon = Icons.Outlined.Add,
                        text = "Add Google Drive",
                        onClick = { viewModel.addAccount(context) },
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                item { SectionHeader("SYNC JOBS", modifier = Modifier.padding(top = 14.dp)) }

                if (state.jobs.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Outlined.Sync,
                            title = "No sync jobs yet",
                            subtitle = "Create a sync job to link a local folder with Drive.",
                        )
                    }
                }

                items(count = state.jobs.size, key = { "job-" + state.jobs[it].job.id }) { index ->
                    val item = state.jobs[index]
                    JobCard(
                        item = item,
                        onOpen = { onOpenDetails(item.job.id) },
                        onStart = { startJobWithPermission(item.job.id) },
                        onPause = { viewModel.pauseJob(item.job.id) },
                        onDelete = { deleteCandidate = item.job.id },
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                }
            }
        }

        // Extended FAB — disabled gray variant until an account exists.
        val fabEnabled = state.accounts.isNotEmpty()
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 20.dp)
                .then(
                    if (fabEnabled) {
                        Modifier.shadow(8.dp, RoundedCornerShape(16.dp))
                    } else Modifier,
                )
                .clip(RoundedCornerShape(16.dp))
                .background(if (fabEnabled) DsColors.Primary else DsColors.GrayButton)
                .then(if (fabEnabled) Modifier.clickable(onClick = onOpenWizard) else Modifier)
                .height(56.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val fg = if (fabEnabled) Color.White else DsColors.DisabledFg
            Icon(Icons.Outlined.Add, contentDescription = null, tint = fg, modifier = Modifier.size(24.dp))
            Text(
                text = "Create Sync Job",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = fg,
                modifier = Modifier.padding(start = 10.dp),
            )
        }

        // Snackbar (prototype's dark rounded bar above the FAB)
        state.snackMessage?.let { message ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, bottom = 88.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DsColors.Snack)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(text = message, fontSize = 14.sp, color = DsColors.SnackFg)
            }
        }
    }

    deleteCandidate?.let { jobId ->
        ConfirmDialog(
            title = "Delete sync job?",
            text = "The job and its transfer logs will be removed. Synced files stay untouched.",
            confirmLabel = "Delete",
            onConfirm = {
                viewModel.deleteJob(jobId)
                deleteCandidate = null
            },
            onDismiss = { deleteCandidate = null },
        )
    }
}

@Composable
private fun AccountCard(
    account: DriveAccount,
    onSeeFiles: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AccountAvatar(
                    name = account.displayName,
                    photoUrl = account.photoUrl,
                    color = accountColor(account.email),
                    size = 44.dp,
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
                    ThinProgressBar(
                        fraction = Formatters.storageFraction(account.storageUsedBytes, account.storageTotalBytes),
                        modifier = Modifier.padding(top = 8.dp),
                        fillColor = accountColor(account.email),
                    )
                    Text(
                        text = Formatters.formatStorageLabel(account.storageUsedBytes, account.storageTotalBytes),
                        fontSize = 12.sp,
                        color = DsColors.TextTertiary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            Row(modifier = Modifier.padding(top = 10.dp)) {
                Spacer(modifier = Modifier.weight(1f))
                TonalButton(
                    text = "See Files",
                    icon = Icons.Outlined.FolderOpen,
                    onClick = onSeeFiles,
                    height = 32.dp,
                )
            }
        }
    }
}

@Composable
private fun JobCard(
    item: JobWithProgress,
    onOpen: () -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val job = item.job
    val isSyncing = job.status == SyncStatus.SYNCING
    val lastSync = job.lastSyncAt?.let(Formatters::formatTimestamp) ?: "Never"

    DsCard(modifier = modifier.fillMaxWidth(), onClick = onOpen) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DsColors.Tonal),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        job.direction.icon, contentDescription = null,
                        tint = DsColors.Primary, modifier = Modifier.size(22.dp),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 8.dp),
                ) {
                    Text(
                        text = job.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = DsColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${job.direction.label} · Last sync: $lastSync",
                        fontSize = 12.sp,
                        color = DsColors.TextSecondary,
                        modifier = Modifier.padding(top = 2.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                StatusChip(job.status)
            }

            if (isSyncing && item.progress != null) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    ThinProgressBar(fraction = item.progress.percent / 100f)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = item.progress.currentFile,
                            fontSize = 12.sp,
                            color = DsColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Text(
                            text = Formatters.formatSpeed(item.progress.speedBytesPerSec),
                            fontSize = 12.sp,
                            color = DsColors.TextSecondary,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!isSyncing) {
                    TonalButton(text = "Start", icon = Icons.Outlined.PlayArrow, onClick = onStart)
                } else {
                    TonalButton(
                        text = "Pause", icon = Icons.Outlined.Pause, onClick = onPause,
                        bg = DsColors.GrayButton, fg = DsColors.TextPrimary,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Delete, contentDescription = "Delete",
                        tint = DsColors.TextTertiary, modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
