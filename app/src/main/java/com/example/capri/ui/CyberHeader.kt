package com.example.capri.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberBorderCyan
import com.example.ui.theme.CyberDark
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberYellow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPink

@Composable
fun CyberHeader(
    isTorchOn: Boolean,
    volumePercent: Int,
    isMuted: Boolean,
    hasApiKey: Boolean,
    operatorName: String,
    isCornerMode: Boolean,
    isCoolArt: Boolean,
    onTorchToggle: () -> Unit,
    onMuteToggle: () -> Unit,
    onToggleCornerMode: () -> Unit,
    onToggleArt: () -> Unit,
    onSettingsClick: () -> Unit,
    onCreditsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CyberDark)
            .border(width = 1.dp, color = CyberBorder, shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Title & Creator Credit Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onCreditsClick() }
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(NeonCyan)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "CAPRI",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 19.sp,
                    letterSpacing = 1.8.sp,
                    color = NeonCyan
                )
                Text(
                    text = ".AI",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp,
                    color = NeonMagenta
                )
                Spacer(modifier = Modifier.width(6.dp))
                // Creator Badge: BY SELUIST
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyberBlack)
                        .border(1.dp, NeonMagenta.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "BY SELUIST",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.6.sp,
                        color = NeonMagenta
                    )
                }
            }

            // Top Action Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Developer Credits Info Button
                IconButton(
                    onClick = onCreditsClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("header_credits_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Developer Credits: Seluist",
                        tint = NeonMagenta,
                        modifier = Modifier.size(19.dp)
                    )
                }
                // Stand in Corner / Center Toggle Button
                IconButton(
                    onClick = onToggleCornerMode,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("corner_mode_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isCornerMode) Icons.Filled.AspectRatio else Icons.Filled.PictureInPictureAlt,
                        contentDescription = if (isCornerMode) "Return to Center" else "Stand Capri in Corner",
                        tint = if (isCornerMode) NeonCyan else CyberTextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Art Style Switcher
                IconButton(
                    onClick = onToggleArt,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("header_toggle_art_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.SwapHoriz,
                        contentDescription = "Toggle 1000x Cooler / Original Art",
                        tint = if (isCoolArt) NeonMagenta else NeonCyan,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Torch status button
                IconButton(
                    onClick = onTorchToggle,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("header_torch_button")
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                        contentDescription = "Flashlight Toggle",
                        tint = if (isTorchOn) CyberYellow else CyberTextMuted,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Mute status button
                IconButton(
                    onClick = onMuteToggle,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("header_mute_button")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Mute Voice Toggle",
                        tint = if (isMuted) NeonMagenta else NeonCyan,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Settings button
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("header_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = CyberTextPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        // Bottom Telemetry Ticker featuring Creator Seluist
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onCreditsClick() }
            ) {
                Text(
                    text = "CREATED BY SELUIST",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonMagenta
                )
                Text(
                    text = " • OP: ${operatorName.uppercase()}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan.copy(alpha = 0.85f)
                )
            }
            Text(
                text = if (isCornerMode) "[CORNER DOCK ACTIVE]" else "CHROME VOL: $volumePercent%",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = if (isCornerMode) CyberYellow else NeonMagenta.copy(alpha = 0.8f)
            )
        }
    }
}
