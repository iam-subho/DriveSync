package com.iamsubho.drivesync.presentation.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.iamsubho.drivesync.domain.model.FileTypeCategory
import com.iamsubho.drivesync.domain.model.LimitMode
import com.iamsubho.drivesync.presentation.common.DsCard
import com.iamsubho.drivesync.presentation.theme.DsColors
import com.iamsubho.drivesync.presentation.util.BatteryOptimization

// ---------- Step 4: Battery optimization ----------

@Composable
fun Step4Battery(state: WizardUiState, viewModel: WizardViewModel) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Re-check when returning from the system settings screen.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshBattery()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    StepHeading(
        title = "Battery optimization",
        subtitle = "Android pauses background apps to save battery. To sync reliably, " +
            "Drive Sync needs to run unrestricted.",
    )

    if (!state.batteryOk) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DsColors.BatteryWarnBg)
                .border(1.dp, DsColors.BatteryWarnBorder, RoundedCornerShape(16.dp))
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Outlined.BatteryAlert, contentDescription = null,
                    tint = DsColors.BatteryWarnFg, modifier = Modifier.size(26.dp),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                ) {
                    Text(
                        text = "Battery optimization is on",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = DsColors.BatteryWarnTitle,
                    )
                    Text(
                        text = "Background sync may be delayed or killed by the system.",
                        fontSize = 13.sp,
                        color = DsColors.BatteryWarnBody,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(DsColors.BatteryWarnFg)
                    .clickable {
                        context.startActivity(BatteryOptimization.requestIgnoreIntent(context))
                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null,
                    tint = Color.White, modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Open system settings",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DsColors.BatteryOkBg)
                .border(1.dp, DsColors.BatteryOkBorder, RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.BatteryFull, contentDescription = null,
                tint = DsColors.BatteryOkFg, modifier = Modifier.size(26.dp),
            )
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = "Unrestricted battery use",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = DsColors.BatteryOkTitle,
                )
                Text(
                    text = "Drive Sync can run reliably in the background.",
                    fontSize = 13.sp,
                    color = DsColors.BatteryOkBody,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

// ---------- Step 5: Choose file types ----------

private data class TypeDef(val category: FileTypeCategory, val label: String, val desc: String)

private val TYPE_DEFS = listOf(
    TypeDef(FileTypeCategory.ALL, "All files", "Sync everything — other filters are ignored"),
    TypeDef(FileTypeCategory.IMAGES, "Images", "jpg, png, heic, dng, webp…"),
    TypeDef(FileTypeCategory.VIDEOS, "Videos", "mp4, mov, mkv, 3gp, webm…"),
    TypeDef(FileTypeCategory.DOCUMENTS, "Documents", "pdf, docx, xlsx, txt, md…"),
    TypeDef(FileTypeCategory.AUDIO, "Audio", "mp3, wav, flac, m4a, ogg…"),
    TypeDef(FileTypeCategory.ARCHIVES, "Archives", "zip, rar, 7z, tar, gz…"),
    TypeDef(FileTypeCategory.CUSTOM, "Custom extensions", "Comma-separated list"),
)

fun typeLabel(category: FileTypeCategory): String =
    TYPE_DEFS.first { it.category == category }.label

@Composable
fun Step5FileTypes(state: WizardUiState, viewModel: WizardViewModel) {
    StepHeading(
        title = "Choose file types",
        subtitle = "Only matching files will sync. Extension lists are editable in Settings.",
    )
    Column {
        TYPE_DEFS.forEach { def ->
            val checked = def.category in state.types
            val dimmed = state.allFilesSelected && def.category != FileTypeCategory.ALL
            DsCard(
                cornerRadius = 14.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .alpha(if (dimmed) 0.4f else 1f),
                onClick = { viewModel.toggleType(def.category) },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (checked) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                        contentDescription = null,
                        tint = if (checked) DsColors.Primary else DsColors.DisabledFg,
                        modifier = Modifier.size(22.dp),
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                    ) {
                        Text(
                            text = def.label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = DsColors.TextPrimary,
                        )
                        Text(text = def.desc, fontSize = 12.sp, color = DsColors.TextTertiary)
                    }
                }
            }
        }
    }

    if (FileTypeCategory.CUSTOM in state.types && !state.allFilesSelected) {
        OutlinedTextField(
            value = state.customExtensions,
            onValueChange = viewModel::setCustomExtensions,
            placeholder = { Text("e.g. psd, sketch, blend", color = DsColors.DisabledFg) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = wizardFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
        )
    }
}

// ---------- Step 6: File size limits ----------

@Composable
fun Step6SizeLimits(state: WizardUiState, viewModel: WizardViewModel) {
    StepHeading(
        title = "File size limits",
        subtitle = "Skip files above a size to save data and storage.",
    )
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LimitCard(
            icon = Icons.Outlined.Upload,
            title = "Upload maximum size",
            limit = state.upLimit,
            onChange = viewModel::setUploadLimit,
        )
        LimitCard(
            icon = Icons.Outlined.Download,
            title = "Download maximum size",
            limit = state.downLimit,
            onChange = viewModel::setDownloadLimit,
        )
    }
}

@Composable
private fun LimitCard(
    icon: ImageVector,
    title: String,
    limit: LimitUi,
    onChange: ((LimitUi) -> LimitUi) -> Unit,
) {
    DsCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = DsColors.Primary, modifier = Modifier.size(20.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = DsColors.TextPrimary,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LimitChip("No limit", selected = !limit.custom) {
                    onChange { it.copy(custom = false) }
                }
                LimitChip("Custom", selected = limit.custom) {
                    onChange { it.copy(custom = true) }
                }
            }
            if (limit.custom) {
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = limit.mb,
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }.take(7)
                            onChange { it.copy(mb = digits) }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = wizardFieldColors(),
                        modifier = Modifier.width(110.dp),
                    )
                    Text(
                        text = "MB maximum",
                        fontSize = 14.sp,
                        color = DsColors.TextSecondary,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                // Approved addition: what the limit means, per direction.
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    WizardViewModel.LIMIT_MODES.forEach { (mode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onChange { it.copy(mode = mode) } }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioIcon(selected = limit.mode == mode, modifier = Modifier.size(20.dp))
                            Text(
                                text = label,
                                fontSize = 14.sp,
                                color = DsColors.TextPrimary,
                                modifier = Modifier.padding(start = 10.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LimitChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) DsColors.Tonal else DsColors.Card)
            .border(
                1.dp,
                if (selected) DsColors.Tonal else DsColors.InputBorder,
                RoundedCornerShape(18.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) DsColors.TonalFg else DsColors.TextSecondary,
        )
    }
}

@Composable
fun wizardFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = DsColors.Primary,
    unfocusedBorderColor = DsColors.InputBorder,
    focusedContainerColor = DsColors.Card,
    unfocusedContainerColor = DsColors.Card,
    focusedTextColor = DsColors.TextPrimary,
    unfocusedTextColor = DsColors.TextPrimary,
    cursorColor = DsColors.Primary,
)
