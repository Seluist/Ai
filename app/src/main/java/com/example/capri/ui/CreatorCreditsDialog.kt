package com.example.capri.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDark
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.CyberYellow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPink

@Composable
fun CreatorCreditsDialog(
    onDismiss: () -> Unit,
    onHearTribute: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, NeonMagenta, RoundedCornerShape(16.dp))
                .testTag("creator_credits_dialog"),
            colors = CardDefaults.cardColors(containerColor = CyberDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(NeonCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SYSTEM CREDITS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            letterSpacing = 1.2.sp,
                            color = NeonCyan
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("close_credits_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close Credits",
                            tint = CyberTextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Creator Badge & Avatar Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CyberBlack)
                        .border(1.5.dp, NeonMagenta, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Code,
                        contentDescription = "Creator",
                        tint = NeonMagenta,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Creator Name
                Text(
                    text = "SELUIST",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    letterSpacing = 2.sp,
                    color = NeonCyan
                )

                Text(
                    text = "CREATOR & LEAD ARCHITECT",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = NeonMagenta
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Badges Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CreditTag(text = "ORIGINAL CREATOR", color = NeonCyan)
                    CreditTag(text = "CYBERPUNK ENGINE", color = CyberYellow)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Specification Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberSurface)
                        .border(1.dp, CyberBorder, RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Capri is a bespoke cyberpunk AI companion engineered by Seluist. Built with high-octane Edgerunners styling, offline hardware control pipelines, and multimodal Gemini AI intelligence.",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        color = CyberTextSecondary,
                        lineHeight = 17.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    CreditFeatureItem(
                        label = "Neural Interface",
                        detail = "Designed by Seluist"
                    )
                    CreditFeatureItem(
                        label = "Device Cyberware",
                        detail = "Flashlight, Sound, Screen, Apps"
                    )
                    CreditFeatureItem(
                        label = "Global Netlink",
                        detail = "Gemini 2.5 Flash + Custom Tools"
                    )
                    CreditFeatureItem(
                        label = "Companion HUD",
                        detail = "Corner Dock & Overlay Screen Companion"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Capri Audio Tribute Button
                Button(
                    onClick = onHearTribute,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceElevated),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, NeonCyan, RoundedCornerShape(8.dp))
                        .testTag("hear_creator_tribute_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Speak tribute",
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "HEAR CAPRI'S TRIBUTE TO SELUIST",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dismiss Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dismiss_credits_button")
                ) {
                    Text(
                        text = "ACKNOWLEDGE ARCHITECT",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = CyberBlack
                    )
                }
            }
        }
    }
}

@Composable
private fun CreditTag(text: String, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(CyberBlack)
            .border(1.dp, color.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun CreditFeatureItem(label: String, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = CyberTextPrimary
        )
        Text(
            text = detail,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = NeonPink
        )
    }
}
