package com.iamsubho.drivesync.presentation.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iamsubho.drivesync.domain.model.SyncDirection
import com.iamsubho.drivesync.presentation.common.DsCard
import com.iamsubho.drivesync.presentation.theme.DsColors

// ---------- Step 8: Schedule & cleanup ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Step8Schedule(state: WizardUiState, viewModel: WizardViewModel) {
    StepHeading(
        title = "Sync schedule",
        subtitle = "The job re-runs automatically on this interval. You can still start it manually anytime.",
    )

    DsCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Schedule, contentDescription = null,
                    tint = DsColors.Primary, modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Run this job every",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = DsColors.TextPrimary,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            FlowRow(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WizardUiState.INTERVAL_PRESETS.forEach { hours ->
                    ScheduleChip(
                        text = "${hours}h",
                        selected = !state.intervalCustom && state.intervalHours == hours,
                        onClick = { viewModel.setIntervalPreset(hours) },
                    )
                }
                ScheduleChip(
                    text = "Custom",
                    selected = state.intervalCustom,
                    onClick = { viewModel.setIntervalCustom() },
                )
            }
            if (state.intervalCustom) {
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = state.customHours,
                        onValueChange = viewModel::setCustomHours,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = wizardFieldColors(),
                        modifier = Modifier.width(110.dp),
                    )
                    Text(
                        text = "hours between runs",
                        fontSize = 14.sp,
                        color = DsColors.TextSecondary,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            Text(
                text = "Default is every 24 hours.",
                fontSize = 12.sp,
                color = DsColors.TextTertiary,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }

    // Delete-after-upload never applies to download-only jobs (nothing is uploaded).
    if (state.direction != SyncDirection.DOWNLOAD_ONLY) {
        val checked = state.deleteAfterUpload
        DsCard(
            cornerRadius = 14.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            onClick = viewModel::toggleDeleteAfterUpload,
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
                        text = "Delete local files after upload",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = DsColors.TextPrimary,
                    )
                    Text(
                        text = "Each file is removed from this device once it is safely on Drive. " +
                            "Files that exist only on Drive are never downloaded back.",
                        fontSize = 12.sp,
                        color = DsColors.TextTertiary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleChip(text: String, selected: Boolean, onClick: () -> Unit) {
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
