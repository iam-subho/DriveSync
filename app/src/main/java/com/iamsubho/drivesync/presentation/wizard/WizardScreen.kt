package com.iamsubho.drivesync.presentation.wizard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.iamsubho.drivesync.presentation.common.HeaderIconButton
import com.iamsubho.drivesync.presentation.common.PrimaryButton
import com.iamsubho.drivesync.presentation.common.ThinProgressBar
import com.iamsubho.drivesync.presentation.theme.DsColors

@Composable
fun WizardScreen(
    onExit: () -> Unit,
    viewModel: WizardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    fun goBack() {
        if (!viewModel.back()) onExit()
    }

    BackHandler { goBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DsColors.Bg),
    ) {
        // Header: back + "New sync job" + "Step X of 8 — {title}"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HeaderIconButton(Icons.AutoMirrored.Outlined.ArrowBack, "Back", ::goBack)
            Column(modifier = Modifier.padding(start = 4.dp)) {
                Text(
                    text = "New sync job",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = DsColors.TextPrimary,
                )
                Text(
                    text = "Step ${state.step} of ${WizardUiState.LAST_STEP} — ${state.stepTitle}",
                    fontSize = 12.sp,
                    color = DsColors.TextSecondary,
                )
            }
        }

        ThinProgressBar(
            fraction = state.step.toFloat() / WizardUiState.LAST_STEP,
            modifier = Modifier.padding(horizontal = 16.dp),
            height = 4.dp,
        )

        // Step content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 16.dp),
        ) {
            when (state.step) {
                1 -> Step1Account(state, viewModel)
                2 -> Step2DriveFolder(state, viewModel)
                3 -> Step3LocalFolder(state, viewModel)
                4 -> Step4Battery(state, viewModel)
                5 -> Step5FileTypes(state, viewModel)
                6 -> Step6SizeLimits(state, viewModel)
                7 -> Step7Direction(state, viewModel)
                8 -> Step8Schedule(state, viewModel)
                9 -> StepReview(state, viewModel)
            }
        }

        // Footer: Cancel/Back + Next/Create job
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DsColors.FooterBorder),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DsColors.Bg)
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .height(46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .clickable(onClick = ::goBack)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (state.step == 1) "Cancel" else "Back",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = DsColors.Primary,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                val isLast = state.step == WizardUiState.LAST_STEP
                PrimaryButton(
                    text = if (isLast) "Create job" else "Next",
                    trailingIcon = if (isLast) Icons.Outlined.Check else Icons.AutoMirrored.Outlined.ArrowForward,
                    enabled = state.canNext,
                    height = 46.dp,
                    onClick = {
                        if (isLast) viewModel.create(onDone = onExit) else viewModel.next()
                    },
                )
            }
        }
    }
}

/** Shared step heading: 20sp title + 13sp subtitle with 16dp bottom gap. */
@Composable
fun StepHeading(title: String, subtitle: String) {
    Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium,
        color = DsColors.TextPrimary,
    )
    Text(
        text = subtitle,
        fontSize = 13.sp,
        color = DsColors.TextSecondary,
        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
    )
}
