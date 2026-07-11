package com.iamsubho.drivesync.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = DsColors.Primary,
    onPrimary = Color.White,
    primaryContainer = DsColors.Tonal,
    onPrimaryContainer = DsColors.TonalFg,
    background = DsColors.Bg,
    onBackground = DsColors.TextPrimary,
    surface = DsColors.Card,
    onSurface = DsColors.TextPrimary,
    surfaceVariant = DsColors.ChipNeutralBg,
    onSurfaceVariant = DsColors.TextSecondary,
    outline = DsColors.OutlineBorder,
    outlineVariant = DsColors.CardBorder,
    error = DsColors.Error,
    errorContainer = DsColors.ChipErrorBg,
    onErrorContainer = DsColors.ChipErrorFg,
)

@Composable
fun DriveSyncTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = DriveSyncTypography,
        content = content,
    )
}
