package com.iamsubho.drivesync.presentation.wizard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iamsubho.drivesync.domain.model.FileTypeCategory
import com.iamsubho.drivesync.domain.model.SyncDirection
import com.iamsubho.drivesync.presentation.common.InfoRowsCard
import com.iamsubho.drivesync.presentation.common.RadioCard
import com.iamsubho.drivesync.presentation.home.icon
import com.iamsubho.drivesync.presentation.home.label
import com.iamsubho.drivesync.presentation.theme.DsColors

// ---------- Step 7: Sync direction ----------

private val DIRECTION_DESCRIPTIONS = mapOf(
    SyncDirection.TWO_WAY to "Changes on either side copy to the other. Newest wins on conflict.",
    SyncDirection.UPLOAD_ONLY to "Local files copy to Drive. Drive changes are ignored.",
    SyncDirection.DOWNLOAD_ONLY to "Drive files copy to this device. Local changes are ignored.",
)

@Composable
fun Step7Direction(state: WizardUiState, viewModel: WizardViewModel) {
    StepHeading(
        title = "Sync direction",
        subtitle = "How changes move between this device and Drive.",
    )
    Column {
        SyncDirection.entries.forEach { direction ->
            val selected = state.direction == direction
            RadioCard(
                selected = selected,
                onClick = { viewModel.setDirection(direction) },
                modifier = Modifier.padding(bottom = 10.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DsColors.Tonal),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            direction.icon, contentDescription = null,
                            tint = DsColors.Primary, modifier = Modifier.size(22.dp),
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 14.dp, end = 8.dp),
                    ) {
                        Text(
                            text = direction.label,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = DsColors.TextPrimary,
                        )
                        Text(
                            text = DIRECTION_DESCRIPTIONS.getValue(direction),
                            fontSize = 12.sp,
                            color = DsColors.TextTertiary,
                        )
                    }
                    RadioIcon(selected)
                }
            }
        }
    }
}

// ---------- Step 9: Review and create ----------

@Composable
fun StepReview(state: WizardUiState, viewModel: WizardViewModel) {
    StepHeading(
        title = "Review and create",
        subtitle = "Give the job a name and confirm its configuration.",
    )

    OutlinedTextField(
        value = state.jobName,
        onValueChange = viewModel::setName,
        placeholder = { Text("Job name", color = DsColors.DisabledFg) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = wizardFieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )

    val typeNames = if (state.allFilesSelected) {
        "All files"
    } else {
        state.types
            .filter { it != FileTypeCategory.ALL }
            .joinToString(", ") { typeLabel(it) }
            .ifEmpty { "—" }
    }

    InfoRowsCard(
        rows = listOf(
            "Account" to (state.selectedAccountEmail ?: "—"),
            "Drive folder" to ("My Drive/" + (state.selectedFolderName ?: "—")),
            "Local folder" to (state.localFolderPath ?: "—"),
            "Direction" to state.direction.label,
            "File types" to typeNames,
            "Excluded" to if (state.excludedFolders.isEmpty()) "None"
            else state.excludedFolders.sorted().joinToString(", "),
            "Upload limit" to state.upLimit.summaryLabel(),
            "Download limit" to state.downLimit.summaryLabel(),
            "Schedule" to "Every ${state.effectiveIntervalHours} hours",
            "After upload" to if (state.effectiveDeleteAfterUpload) "Delete local file" else "Keep local file",
            "Conflicts" to "Newest Wins",
        ),
        labelWidth = 108.dp,
        modifier = Modifier.padding(top = 12.dp),
    )
}
