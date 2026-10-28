package com.example.capri.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberYellow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta

@Composable
fun QuickActionsRow(
    isTorchOn: Boolean,
    isCornerMode: Boolean,
    isOverlayOnLeaveEnabled: Boolean,
    onToggleCornerMode: () -> Unit,
    onToggleOverlay: () -> Unit,
    onTorchClick: () -> Unit,
    onVolumeUpClick: () -> Unit,
    onVolumeDownClick: () -> Unit,
    onBrightnessToggleClick: () -> Unit,
    onBatteryClick: () -> Unit,
    onHelpClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Stand on Corner button
        CyberActionChip(
            icon = Icons.Filled.PictureInPictureAlt,
            label = if (isCornerMode) "CENTER STAGE" else "STAND IN CORNER",
            accentColor = if (isCornerMode) CyberYellow else NeonMagenta,
            testTag = "action_corner_mode",
            onClick = onToggleCornerMode
        )

        // Overlay on leave app toggle
        CyberActionChip(
            icon = Icons.Filled.Layers,
            label = if (isOverlayOnLeaveEnabled) "OVERLAY: ON" else "OVERLAY: OFF",
            accentColor = if (isOverlayOnLeaveEnabled) NeonCyan else CyberTextMuted,
            testTag = "action_overlay_toggle",
            onClick = onToggleOverlay
        )

        CyberActionChip(
            icon = Icons.Filled.FlashlightOn,
            label = if (isTorchOn) "LIGHT: ON" else "LIGHT: OFF",
            accentColor = if (isTorchOn) CyberYellow else NeonCyan,
            testTag = "action_torch",
            onClick = onTorchClick
        )
        CyberActionChip(
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            label = "VOL +",
            accentColor = NeonCyan,
            testTag = "action_vol_up",
            onClick = onVolumeUpClick
        )
        CyberActionChip(
            icon = Icons.AutoMirrored.Filled.VolumeDown,
            label = "VOL -",
            accentColor = NeonCyan,
            testTag = "action_vol_down",
            onClick = onVolumeDownClick
        )
        CyberActionChip(
            icon = Icons.Filled.Brightness6,
            label = "BRIGHTNESS",
            accentColor = NeonMagenta,
            testTag = "action_brightness",
            onClick = onBrightnessToggleClick
        )
        CyberActionChip(
            icon = Icons.Filled.BatteryChargingFull,
            label = "BATTERY",
            accentColor = CyberYellow,
            testTag = "action_battery",
            onClick = onBatteryClick
        )
        CyberActionChip(
            icon = Icons.AutoMirrored.Filled.HelpOutline,
            label = "COMMANDS",
            accentColor = NeonCyan,
            testTag = "action_help",
            onClick = onHelpClick
        )
    }
}

@Composable
private fun CyberActionChip(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CyberSurface)
            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = accentColor
            )
        }
    }
}
