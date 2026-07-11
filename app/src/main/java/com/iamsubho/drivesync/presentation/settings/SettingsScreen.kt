package com.iamsubho.drivesync.presentation.settings

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iamsubho.drivesync.domain.model.DriveAccount
import com.iamsubho.drivesync.domain.repository.DriveConsentRequiredException
import com.iamsubho.drivesync.presentation.common.AccountAvatar
import com.iamsubho.drivesync.presentation.common.ConfirmDialog
import com.iamsubho.drivesync.presentation.common.DsCard
import com.iamsubho.drivesync.presentation.common.Formatters
import com.iamsubho.drivesync.presentation.common.HeaderIconButton
import com.iamsubho.drivesync.presentation.common.SectionHeader
import com.iamsubho.drivesync.presentation.common.TonalButton
import com.iamsubho.drivesync.presentation.home.accountColor
import com.iamsubho.drivesync.presentation.theme.DsColors
import com.iamsubho.drivesync.presentation.wizard.wizardFieldColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var removeCandidate by remember { mutableStateOf<String?>(null) }
    var pendingConsent by remember { mutableStateOf<DriveConsentRequiredException?>(null) }

    val consentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val request = pendingConsent
        pendingConsent = null
        if (result.resultCode == Activity.RESULT_OK && request != null) {
            viewModel.finishConsent(request)
        }
    }

    val legacySignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        viewModel.onLegacySignInResult(result.data)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HeaderIconButton(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack)
                Text(
                    text = "Settings",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = DsColors.TextPrimary,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            ) {
                SectionHeader("GOOGLE DRIVE ACCOUNTS")
                DsCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        state.accounts.forEach { account ->
                            AccountRow(
                                account = account,
                                onRefresh = { viewModel.refreshStorage(account.email) },
                                onRemove = { removeCandidate = account.email },
                            )
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(DsColors.RowDivider),
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.addAccount(context) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.Add, contentDescription = null,
                                tint = DsColors.Primary, modifier = Modifier.size(20.dp),
                            )
                            Text(
                                text = "Add account",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = DsColors.Primary,
                                modifier = Modifier.padding(start = 10.dp),
                            )
                        }
                    }
                }

                state.sections.forEach { section ->
                    SectionHeader(section.title, modifier = Modifier.padding(top = 14.dp))
                    DsCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                section.extensions.forEach { ext ->
                                    ExtensionChip(
                                        name = ext,
                                        onRemove = { viewModel.removeExtension(section.category, ext) },
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.padding(top = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                OutlinedTextField(
                                    value = section.input,
                                    onValueChange = { viewModel.setInput(section.category, it) },
                                    placeholder = { Text("Add extension", fontSize = 13.sp, color = DsColors.DisabledFg) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = wizardFieldColors(),
                                    modifier = Modifier.weight(1f),
                                )
                                TonalButton(
                                    text = "Add",
                                    onClick = { viewModel.addExtension(section.category) },
                                    height = 38.dp,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .padding(top = 10.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.restoreDefaults(section.category) }
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Outlined.RestartAlt, contentDescription = null,
                                    tint = DsColors.Primary, modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "Restore defaults",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = DsColors.Primary,
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        state.snackMessage?.let { message ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DsColors.Snack)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(text = message, fontSize = 14.sp, color = DsColors.SnackFg)
            }
        }
    }

    removeCandidate?.let { email ->
        ConfirmDialog(
            title = "Remove account?",
            text = "$email will be disconnected and its sync jobs deleted. Files stay untouched.",
            confirmLabel = "Remove",
            onConfirm = {
                viewModel.removeAccount(email)
                removeCandidate = null
            },
            onDismiss = { removeCandidate = null },
        )
    }
}

@Composable
private fun AccountRow(
    account: DriveAccount,
    onRefresh: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AccountAvatar(
            name = account.displayName,
            photoUrl = account.photoUrl,
            color = accountColor(account.email),
            size = 38.dp,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp),
        ) {
            Text(
                text = account.email,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = DsColors.TextPrimary,
            )
            Text(
                text = Formatters.formatStorageLabel(account.storageUsedBytes, account.storageTotalBytes),
                fontSize = 12.sp,
                color = DsColors.TextTertiary,
            )
        }
        SmallIconButton(Icons.Outlined.Refresh, "Refresh storage", DsColors.TextSecondary, onRefresh)
        SmallIconButton(Icons.Outlined.Close, "Remove account", DsColors.TextTertiary, onRemove)
    }
}

@Composable
private fun SmallIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun ExtensionChip(name: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .height(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DsColors.LightBlueBg)
            .padding(start = 12.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = DsColors.TonalFg,
        )
        Box(
            modifier = Modifier
                .padding(start = 4.dp)
                .size(21.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Close, contentDescription = "Remove $name",
                tint = DsColors.TextSecondary, modifier = Modifier.size(15.dp),
            )
        }
    }
}
