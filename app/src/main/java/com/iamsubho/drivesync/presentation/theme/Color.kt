package com.iamsubho.drivesync.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Fixed palette copied 1:1 from the approved design prototype (dd/Drive Sync App.dc.html).
 * No dynamic color in v1 — the design mandates these exact values.
 */
object DsColors {
    val Primary = Color(0xFF0B57D0)
    val PrimaryPressed = Color(0xFF0A4CB8)
    val Bg = Color(0xFFF8F9FF)
    val Card = Color(0xFFFFFFFF)
    val CardBorder = Color(0xFFE2E5EE)
    val RowDivider = Color(0xFFF0F2F8)
    val FooterBorder = Color(0xFFE7E9F2)

    val TextPrimary = Color(0xFF191C20)
    val TextSecondary = Color(0xFF44474E)
    val TextTertiary = Color(0xFF74777F)
    val DisabledFg = Color(0xFF9AA0AC)

    val Tonal = Color(0xFFD3E3FD)
    val TonalFg = Color(0xFF041E49)
    val TonalPressed = Color(0xFFC0D7FB)
    val LightBlueBg = Color(0xFFE8F0FE)

    val TrackGray = Color(0xFFE2E5EE)
    val GrayButton = Color(0xFFE2E5EE)
    val GrayButtonPressed = Color(0xFFD5D9E4)

    val ChipSuccessBg = Color(0xFFE6F4EA)
    val ChipSuccessFg = Color(0xFF146C2E)
    val ChipSyncBg = Color(0xFFE8F0FE)
    val ChipSyncFg = Color(0xFF0B57D0)
    val ChipNeutralBg = Color(0xFFECEEF4)
    val ChipNeutralFg = Color(0xFF44474E)
    val ChipErrorBg = Color(0xFFFFDAD6)
    val ChipErrorFg = Color(0xFFBA1A1A)

    val BatteryWarnBg = Color(0xFFFFF3E4)
    val BatteryWarnBorder = Color(0xFFF0D5B0)
    val BatteryWarnFg = Color(0xFF8A5100)
    val BatteryWarnTitle = Color(0xFF2E1B00)
    val BatteryWarnBody = Color(0xFF6B5B41)
    val BatteryWarnPressed = Color(0xFF744400)
    val BatteryOkBg = Color(0xFFE6F4EA)
    val BatteryOkBorder = Color(0xFFBFE3C9)
    val BatteryOkFg = Color(0xFF146C2E)
    val BatteryOkTitle = Color(0xFF0A2F15)
    val BatteryOkBody = Color(0xFF3E5D48)

    val DashedBorder = Color(0xFFC4C6D0)
    val InputBorder = Color(0xFFC4C6D0)
    val OutlineBorder = Color(0xFF74777F)
    val Snack = Color(0xFF2F3033)
    val SnackFg = Color(0xFFF1F0F4)
    val Error = Color(0xFFBA1A1A)
    val ErrorHoverBg = Color(0xFFFFDAD6)

    /** Deterministic avatar colors used by the prototype's account pool. */
    val AccountColors = listOf(
        Color(0xFF0B57D0),
        Color(0xFF146C2E),
        Color(0xFF8A2BE2),
        Color(0xFFB3261E),
        Color(0xFF00639B),
    )
}
