package com.example.capri.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDark
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.CyberYellow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPink
import com.example.capri.viewmodel.CapriViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    viewModel: CapriViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    val isTorchOn by viewModel.isTorchOn.collectAsState()
    val volumePercent by viewModel.volumePercent.collectAsState()
    val screenBrightness by viewModel.screenBrightness.collectAsState()
    val ttsPitch by viewModel.ttsManager.pitch.collectAsState()
    val ttsSpeed by viewModel.ttsManager.speed.collectAsState()
    val operatorName by viewModel.operatorName.collectAsState()
    val isCornerMode by viewModel.isCornerMode.collectAsState()
    val isCoolArt by viewModel.isCoolArt.collectAsState()
    val isOverlayOnLeaveEnabled by viewModel.isOverlayOnLeaveEnabled.collectAsState()
    val customAvatarBitmap by viewModel.customAvatarBitmap.collectAsState()

    val context = LocalContext.current

    val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.importCustomAvatar(uri)
        }
    }

    var operatorInput by remember { mutableStateOf(operatorName) }
    var apiKeyInput by remember { mutableStateOf(viewModel.aiService.getStoredApiKey()) }
    var keySavedNotification by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CyberDark,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .navigationBarsPadding()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
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
                            .background(NeonMagenta)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NETLINK SETTINGS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp,
                        color = NeonCyan
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close Settings",
                        tint = CyberTextPrimary
                    )
                }
            }

            // Section 0: Operator Identity (Seluist)
            SectionTitle(title = "OPERATOR PROFILE")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = operatorInput,
                    onValueChange = { operatorInput = it },
                    label = { Text("Cyber Operator Call-Sign", color = CyberTextMuted, fontSize = 11.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = CyberBorder,
                        focusedContainerColor = CyberSurface,
                        unfocusedContainerColor = CyberSurface,
                        cursorColor = NeonCyan
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    ),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        viewModel.setOperatorName(operatorInput)
                        viewModel.repeatMessage("Operator profile locked to $operatorInput, choom.")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "SAVE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = CyberBlack,
                        fontSize = 11.sp
                    )
                }
            }

            // Section: Developer Credits
            SectionTitle(title = "DEVELOPER CREDITS")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(CyberBlack)
                    .border(1.dp, NeonMagenta, RoundedCornerShape(10.dp))
                    .padding(14.dp)
                    .testTag("settings_developer_credits_card")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Code,
                                contentDescription = "Developer",
                                tint = NeonMagenta,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LEAD ARCHITECT & CREATOR",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonMagenta
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NeonMagenta.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "AUTHOR",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonMagenta
                            )
                        }
                    }
                    Text(
                        text = "Seluist",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Capri was conceived, created, and engineered by Seluist. Powered by real-time voice recognition, speech synthesis, local device cyberware tools, and Gemini conversational intelligence.",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        color = CyberTextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }

            // Section: Capri Companion Modes & Art
            SectionTitle(title = "CAPRI COMPANION & VISUALS")

            SettingRow(
                title = "Stand in Corner Mode",
                subtitle = "Capri docks in screen corner ready to click and command"
            ) {
                Switch(
                    checked = isCornerMode,
                    onCheckedChange = { viewModel.toggleCornerMode() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberBlack,
                        checkedTrackColor = NeonCyan,
                        uncheckedThumbColor = CyberTextMuted,
                        uncheckedTrackColor = CyberSurfaceElevated
                    ),
                    modifier = Modifier.testTag("settings_corner_mode_switch")
                )
            }

            SettingRow(
                title = "1000x Cooler Anime Art",
                subtitle = if (isCoolArt) "High-octane Edgerunners anime edition" else "Original Cyberpunk Sketch"
            ) {
                Switch(
                    checked = isCoolArt,
                    onCheckedChange = { viewModel.toggleArtStyle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberBlack,
                        checkedTrackColor = NeonMagenta,
                        uncheckedThumbColor = CyberTextMuted,
                        uncheckedTrackColor = CyberSurfaceElevated
                    ),
                    modifier = Modifier.testTag("settings_art_style_switch")
                )
            }

            SettingRow(
                title = "Appear on Screen When Leaving App",
                subtitle = "Capri floats as a cyberpunk companion over other apps & home screen"
            ) {
                Switch(
                    checked = isOverlayOnLeaveEnabled,
                    onCheckedChange = { viewModel.toggleOverlayOnLeave(context) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberBlack,
                        checkedTrackColor = NeonCyan,
                        uncheckedThumbColor = CyberTextMuted,
                        uncheckedTrackColor = CyberSurfaceElevated
                    ),
                    modifier = Modifier.testTag("settings_overlay_on_leave_switch")
                )
            }

            // Section: Custom Avatar Picture & Gallery Import
            SectionTitle(title = "AVATAR PICTURE & GALLERY IMPORT")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberBlack)
                    .border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
                    .padding(14.dp)
                    .testTag("avatar_import_card")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Thumbnail of active avatar
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(74.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyberSurfaceElevated)
                                .border(1.2.dp, NeonMagenta, RoundedCornerShape(10.dp))
                        ) {
                            val bitmap = customAvatarBitmap
                            if (bitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Active Avatar",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                    modifier = Modifier.size(64.dp)
                                )
                            } else {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.capri_user),
                                    contentDescription = "Bundled Avatar",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (customAvatarBitmap != null) "CUSTOM IMPORTED AVATAR" else "BUNDLED CYBERPUNK AVATAR",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (customAvatarBitmap != null) NeonCyan else CyberYellow
                            )
                            Text(
                                text = "Use your exact imported picture across both main deck and floating companion with swinging physics. Transparent PNG/WebP supported without stretching.",
                                fontFamily = FontFamily.SansSerif,
                                fontSize = 10.5.sp,
                                color = CyberTextSecondary,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                photoPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("select_avatar_gallery_button")
                        ) {
                            Text(
                                text = "📷 IMPORT PICTURE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberBlack
                            )
                        }

                        if (customAvatarBitmap != null) {
                            Button(
                                onClick = { viewModel.resetAvatarToDefault() },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceElevated),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .border(1.dp, NeonMagenta, RoundedCornerShape(8.dp))
                                    .testTag("reset_avatar_button")
                            ) {
                                Text(
                                    text = "RESTORE",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonMagenta
                                )
                            }
                        }
                    }
                }
            }

            // Section 1: Hardware Cyberware Controls
            SectionTitle(title = "CYBERWARE HARDWARE CONTROLS")

            // Torch Toggle Row
            SettingRow(
                title = "Photon Emitter (Flashlight)",
                subtitle = "Local camera flash control"
            ) {
                Switch(
                    checked = isTorchOn,
                    onCheckedChange = { viewModel.toggleTorch() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyberBlack,
                        checkedTrackColor = CyberYellow,
                        uncheckedThumbColor = CyberTextMuted,
                        uncheckedTrackColor = CyberSurfaceElevated
                    ),
                    modifier = Modifier.testTag("settings_torch_switch")
                )
            }

            // Volume Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Media Decibels (Volume)",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp,
                        color = CyberTextPrimary
                    )
                    Text(
                        text = "$volumePercent%",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }
                Slider(
                    value = volumePercent.toFloat(),
                    onValueChange = { viewModel.deviceControlManager.setVolume(it.toInt()) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = CyberSurfaceElevated
                    ),
                    modifier = Modifier.testTag("settings_volume_slider")
                )
            }

            // Screen Brightness Slider
            Column {
                val brightnessInt = (screenBrightness * 100).toInt().coerceIn(5, 100)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Optical Display Brightness",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp,
                        color = CyberTextPrimary
                    )
                    Text(
                        text = "$brightnessInt%",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonPink
                    )
                }
                Slider(
                    value = brightnessInt.toFloat(),
                    onValueChange = { viewModel.setScreenBrightness(it.toInt()) },
                    valueRange = 5f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonPink,
                        activeTrackColor = NeonPink,
                        inactiveTrackColor = CyberSurfaceElevated
                    ),
                    modifier = Modifier.testTag("settings_brightness_slider")
                )
            }

            // Section 2: Online AI & Netlink
            SectionTitle(title = "GLOBAL NETLINK (GEMINI API)")

            Text(
                text = "Capri operates completely offline for all device commands and banter. To unlock unlimited real-time online AI dialogue, provide your Google Gemini API key below (or configure it via Secrets in AI Studio):",
                fontFamily = FontFamily.SansSerif,
                fontSize = 12.sp,
                color = CyberTextSecondary,
                lineHeight = 18.sp
            )

            OutlinedTextField(
                value = apiKeyInput,
                onValueChange = {
                    apiKeyInput = it
                    keySavedNotification = false
                },
                placeholder = {
                    Text("Enter Gemini API Key...", color = CyberTextMuted, fontSize = 12.sp)
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = CyberBorder,
                    focusedContainerColor = CyberSurface,
                    unfocusedContainerColor = CyberSurface,
                    cursorColor = NeonCyan
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = CyberTextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("api_key_input_field")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (keySavedNotification) {
                    Text(
                        text = "✓ Key saved! Netlink active.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = CyberGreen
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        viewModel.aiService.saveCustomApiKey(apiKeyInput)
                        keySavedNotification = true
                        viewModel.repeatMessage("Netlink connection established, choom.")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("save_api_key_button")
                ) {
                    Text(
                        text = "LINK KEY",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = CyberBlack,
                        fontSize = 12.sp
                    )
                }
            }

            // Section 3: Synthetic Vocal Modulator (TTS)
            SectionTitle(title = "SYNTHETIC VOCAL MODULATOR")

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Vocal Pitch", fontSize = 13.sp, color = CyberTextPrimary)
                    Text(
                        String.format("%.2f", ttsPitch),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = NeonCyan
                    )
                }
                Slider(
                    value = ttsPitch,
                    onValueChange = { viewModel.ttsManager.setPitch(it) },
                    valueRange = 0.7f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                )
            }

            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Speech Tempo", fontSize = 13.sp, color = CyberTextPrimary)
                    Text(
                        String.format("%.2f", ttsSpeed),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = NeonPink
                    )
                }
                Slider(
                    value = ttsSpeed,
                    onValueChange = { viewModel.ttsManager.setSpeed(it) },
                    valueRange = 0.7f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = NeonPink, activeTrackColor = NeonPink)
                )
            }

            // Section 4: Maintenance
            SectionTitle(title = "MEMORY PURGE")

            Button(
                onClick = {
                    viewModel.clearHistory()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberSurfaceElevated),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberRed.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .testTag("clear_history_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Clear logs",
                    tint = CyberRed,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PURGE NEURAL CONVERSATION LOGS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberRed
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontFamily = FontFamily.Monospace,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
        color = NeonMagenta,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    control: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = FontFamily.SansSerif,
                fontSize = 14.sp,
                color = CyberTextPrimary
            )
            Text(
                text = subtitle,
                fontFamily = FontFamily.SansSerif,
                fontSize = 11.sp,
                color = CyberTextMuted
            )
        }
        control()
    }
}
