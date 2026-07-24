package com.iamsubho.drivesync.desktop.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Same fixed palette as the Android app (dd/Drive Sync App.dc.html). */
object DsColors {
    val Primary = Color(0xFF0B57D0)
    val Bg = Color(0xFFF8F9FF)
    val Card = Color(0xFFFFFFFF)
    val CardBorder = Color(0xFFE2E5EE)
    val TextPrimary = Color(0xFF191C20)
    val TextSecondary = Color(0xFF44474E)
    val TextTertiary = Color(0xFF74777F)
    val DisabledFg = Color(0xFF9AA0AC)
    val Tonal = Color(0xFFD3E3FD)
    val TonalFg = Color(0xFF041E49)
    val TrackGray = Color(0xFFE2E5EE)
    val ChipSuccessBg = Color(0xFFE6F4EA)
    val ChipSuccessFg = Color(0xFF146C2E)
    val ChipSyncBg = Color(0xFFE8F0FE)
    val ChipSyncFg = Color(0xFF0B57D0)
    val ChipNeutralBg = Color(0xFFECEEF4)
    val ChipNeutralFg = Color(0xFF44474E)
    val ChipErrorBg = Color(0xFFFFDAD6)
    val ChipErrorFg = Color(0xFFBA1A1A)
    val Error = Color(0xFFBA1A1A)
    val Snack = Color(0xFF2F3033)
    val SnackFg = Color(0xFFF1F0F4)
    val InputBorder = Color(0xFFC4C6D0)
}

@Composable
fun DriveSyncTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
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
            outline = DsColors.InputBorder,
            error = DsColors.Error,
        ),
        typography = Typography(),
        content = content,
    )
}
