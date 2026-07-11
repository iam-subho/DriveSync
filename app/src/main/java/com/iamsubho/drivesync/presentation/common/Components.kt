package com.iamsubho.drivesync.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.iamsubho.drivesync.domain.model.SyncStatus
import com.iamsubho.drivesync.presentation.theme.DsColors

/** Uppercase gray section label — "GOOGLE DRIVE ACCOUNTS", "SYNC JOBS", … */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(top = 8.dp, bottom = 10.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.3.sp,
        color = DsColors.TextSecondary,
    )
}

data class StatusChipStyle(val label: String, val bg: Color, val fg: Color, val icon: ImageVector?)

fun statusChipStyle(status: SyncStatus): StatusChipStyle = when (status) {
    SyncStatus.UP_TO_DATE -> StatusChipStyle("Up to date", DsColors.ChipSuccessBg, DsColors.ChipSuccessFg, Icons.Outlined.CheckCircle)
    SyncStatus.SYNCING -> StatusChipStyle("Syncing", DsColors.ChipSyncBg, DsColors.ChipSyncFg, Icons.Outlined.Sync)
    SyncStatus.PAUSED -> StatusChipStyle("Paused", DsColors.ChipNeutralBg, DsColors.ChipNeutralFg, Icons.Outlined.PauseCircle)
    SyncStatus.NEVER_SYNCED -> StatusChipStyle("Never synced", DsColors.ChipNeutralBg, DsColors.ChipNeutralFg, Icons.Outlined.Schedule)
    SyncStatus.ERROR -> StatusChipStyle("Error", DsColors.ChipErrorBg, DsColors.ChipErrorFg, Icons.Outlined.ErrorOutline)
}

@Composable
fun StatusChip(status: SyncStatus, modifier: Modifier = Modifier) {
    val style = statusChipStyle(status)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(style.bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = style.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = style.fg,
            maxLines = 1,
        )
    }
}

/** Circular avatar: photo when available, colored initial fallback (prototype style). */
@Composable
fun AccountAvatar(
    name: String,
    photoUrl: String?,
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                text = name.take(1).uppercase(),
                color = Color.White,
                fontSize = (size.value * 0.41f).sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/** White card with the prototype's 16dp radius and #E2E5EE border. */
@Composable
fun DsCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    borderColor: Color = DsColors.CardBorder,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    var m = modifier
        .clip(shape)
        .background(DsColors.Card)
        .border(borderWidth, borderColor, shape)
    if (onClick != null) m = m.clickable(onClick = onClick)
    Box(modifier = m) { content() }
}

/** Dashed-border empty state — icon, title, subtitle (prototype style). */
@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DsColors.Card)
            .drawBehind {
                val stroke = Stroke(
                    width = with(density) { 1.dp.toPx() },
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f)),
                )
                drawRoundRect(
                    color = DsColors.DashedBorder,
                    cornerRadius = CornerRadius(with(density) { 16.dp.toPx() }),
                    style = stroke,
                )
            }
            .padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = DsColors.TextTertiary, modifier = Modifier.size(36.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = DsColors.TextPrimary,
            modifier = Modifier.padding(top = 8.dp),
            textAlign = TextAlign.Center,
        )
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = DsColors.TextSecondary,
            modifier = Modifier.padding(top = 4.dp),
            textAlign = TextAlign.Center,
        )
    }
}

/** Outlined pill button with blue text + icon ("Add Google Drive", "Create new folder"). */
@Composable
fun PillButtonOutlined(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .border(1.dp, DsColors.OutlineBorder, RoundedCornerShape(height / 2))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = DsColors.Primary, modifier = Modifier.size(20.dp))
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = DsColors.Primary,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** Tonal (light blue) pill button — "Start", "Add". */
@Composable
fun TonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 36.dp,
    bg: Color = DsColors.Tonal,
    fg: Color = DsColors.TonalFg,
) {
    Row(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
            Box(Modifier.width(6.dp))
        }
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = fg)
    }
}

/** Solid blue pill button — "Start sync", wizard "Next". */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 40.dp,
) {
    val bg = if (enabled) DsColors.Primary else DsColors.GrayButton
    val fg = if (enabled) Color.White else DsColors.DisabledFg
    Row(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = if (trailingIcon != null) 26.dp else 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
            Box(Modifier.width(8.dp))
        }
        Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = fg)
        if (trailingIcon != null) {
            Box(Modifier.width(8.dp))
            Icon(trailingIcon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        }
    }
}

/** Thin rounded progress track (prototype's 6-8dp bars). */
@Composable
fun ThinProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    fillColor: Color = DsColors.Primary,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(DsColors.TrackGray),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(height / 2))
                .background(fillColor),
        )
    }
}

/** Key-value rows card (wizard summary, details overview). */
@Composable
fun InfoRowsCard(
    rows: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    labelWidth: Dp = 122.dp,
) {
    DsCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            rows.forEachIndexed { index, (label, value) ->
                Row(modifier = Modifier.padding(vertical = 11.dp)) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        color = DsColors.TextTertiary,
                        modifier = Modifier.width(labelWidth),
                    )
                    Text(
                        text = value,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = DsColors.TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (index != rows.lastIndex) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(DsColors.RowDivider),
                    )
                }
            }
        }
    }
}

/** Selection card with 2dp blue border when selected (wizard radio cards). */
@Composable
fun RadioCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    DsCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = cornerRadius,
        borderColor = if (selected) DsColors.Primary else DsColors.CardBorder,
        borderWidth = 2.dp,
        onClick = onClick,
    ) {
        content()
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DsColors.Card,
        title = { Text(title, fontSize = 18.sp, fontWeight = FontWeight.Medium, color = DsColors.TextPrimary) },
        text = { Text(text, fontSize = 14.sp, color = DsColors.TextSecondary) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = DsColors.Error, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = DsColors.Primary, fontWeight = FontWeight.Medium)
            }
        },
    )
}

/** 44dp circular ripple icon button used in headers (settings, back). */
@Composable
fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = DsColors.TextSecondary, modifier = Modifier.size(24.dp))
    }
}
