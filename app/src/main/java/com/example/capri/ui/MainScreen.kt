package com.example.capri.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.core.content.ContextCompat
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDark
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberYellow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPink
import com.example.capri.viewmodel.CapriViewModel

@Composable
fun MainScreen(viewModel: CapriViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity

    DisposableEffect(activity) {
        viewModel.currentActivity = activity
        onDispose {
            viewModel.currentActivity = null
        }
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (isGranted) {
            viewModel.startListening()
        }
    }

    val isTorchOn by viewModel.isTorchOn.collectAsState()
    val volumePercent by viewModel.volumePercent.collectAsState()
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val micRmsDb by viewModel.micRmsDb.collectAsState()
    val partialText by viewModel.partialVoiceText.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val isLoadingAi by viewModel.isLoadingAi.collectAsState()
    val showSettings by viewModel.showSettingsDialog.collectAsState()
    val showWakeWordDialog by viewModel.showWakeWordDialog.collectAsState()
    val operatorName by viewModel.operatorName.collectAsState()
    val isCornerMode by viewModel.isCornerMode.collectAsState()
    val isCoolArt by viewModel.isCoolArt.collectAsState()
    val isOverlayOnLeaveEnabled by viewModel.isOverlayOnLeaveEnabled.collectAsState()
    val showOverlayPermissionDialog by viewModel.showOverlayPermissionDialog.collectAsState()
    val isContinuousVoiceMode by viewModel.isContinuousVoiceMode.collectAsState()
    val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsState()
    var showCreditsDialog by remember { mutableStateOf(false) }

    // First-Time Open: Neural Link Call-Sign / Name Entry
    if (!hasCompletedOnboarding) {
        OnboardingDialog(
            initialName = operatorName,
            onConfirm = { name ->
                viewModel.completeOnboarding(name)
            }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBlack),
        containerColor = CyberBlack,
        topBar = {
            CyberHeader(
                isTorchOn = isTorchOn,
                volumePercent = volumePercent,
                isMuted = isMuted,
                hasApiKey = viewModel.aiService.hasValidApiKey(),
                operatorName = operatorName,
                isCornerMode = isCornerMode,
                isCoolArt = isCoolArt,
                onTorchToggle = { viewModel.toggleTorch() },
                onMuteToggle = { viewModel.toggleMute() },
                onToggleCornerMode = { viewModel.toggleCornerMode() },
                onToggleArt = { viewModel.toggleArtStyle() },
                onSettingsClick = { viewModel.setShowSettings(true) },
                onCreditsClick = { showCreditsDialog = true },
                modifier = Modifier.statusBarsPadding()
            )
        },
        bottomBar = {
            MicBottomDock(
                isListening = isListening,
                micRmsDb = micRmsDb,
                partialText = partialText,
                isSpeaking = isSpeaking,
                onStopSpeaking = { viewModel.stopSpeaking() },
                isContinuousVoiceMode = isContinuousVoiceMode,
                onToggleContinuousMode = { viewModel.toggleContinuousVoiceMode() },
                onMicClick = {
                    if (isSpeaking) {
                        viewModel.stopSpeaking()
                    } else if (isListening) {
                        viewModel.stopListening()
                    } else {
                        if (hasAudioPermission) {
                            viewModel.startListening()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                },
                onSendText = { text ->
                    viewModel.handleUserInput(text, activity)
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Permission Rationale Banner (if denied)
                AnimatedVisibility(
                    visible = !hasAudioPermission,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberSurface)
                            .border(1.dp, CyberYellow, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = "Audio Permission Required",
                                    tint = CyberYellow
                                )
                                Text(
                                    text = " Audio receptor offline. Grant mic access to speak.",
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 12.sp,
                                    color = CyberYellow
                                )
                            }
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberYellow),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.testTag("grant_audio_permission_button")
                            ) {
                                Text(
                                    text = "CONNECT",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberBlack
                                )
                            }
                        }
                    }
                }

                // If not in Corner Mode, show Full Center Hero Avatar
                if (!isCornerMode) {
                    Spacer(modifier = Modifier.height(4.dp))
                    CapriAvatar(
                        isSpeaking = isSpeaking,
                        isListening = isListening,
                        micLevel = micRmsDb,
                        onAvatarClick = { viewModel.onAvatarTapped() },
                        isCoolArt = isCoolArt,
                        modifier = Modifier.testTag("capri_animated_avatar")
                    )

                    // Creator Credit Sub-badge
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyberSurface)
                            .border(1.dp, NeonMagenta.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable { showCreditsDialog = true }
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                            .testTag("main_creator_credit_badge")
                    ) {
                        Text(
                            text = "⚡ CAPRI AI • CREATED BY SELUIST ⚡",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonMagenta,
                            letterSpacing = 0.8.sp
                        )
                    }

                    // Wake Word Staged Roadmap Banner
                    Spacer(modifier = Modifier.height(6.dp))
                    WakeWordBanner(
                        onClick = { viewModel.setShowWakeWordDialog(true) }
                    )
                }

                // Cyber Quick Actions Row (Includes Stand In Corner & Screen Overlay Toggles)
                QuickActionsRow(
                    isTorchOn = isTorchOn,
                    isCornerMode = isCornerMode,
                    isOverlayOnLeaveEnabled = isOverlayOnLeaveEnabled,
                    onToggleCornerMode = { viewModel.toggleCornerMode() },
                    onToggleOverlay = { viewModel.toggleOverlayOnLeave(context) },
                    onTorchClick = { viewModel.toggleTorch() },
                    onVolumeUpClick = { viewModel.volumeUp() },
                    onVolumeDownClick = { viewModel.volumeDown() },
                    onBrightnessToggleClick = {
                        val currentBrightness = viewModel.screenBrightness.value
                        val target = if (currentBrightness > 0.6f) 20 else 100
                        viewModel.setScreenBrightness(target, activity)
                    },
                    onBatteryClick = { viewModel.checkBattery() },
                    onHelpClick = { viewModel.handleUserInput("help", activity) }
                )

                // Conversation Net-Chat History List
                ChatHistoryList(
                    messages = messages,
                    isLoadingAi = isLoadingAi,
                    onReplayAudio = { text -> viewModel.repeatMessage(text) },
                    modifier = Modifier.weight(1f)
                )
            }

            // Stand on Corner Floating Widget: click to speak command!
            if (isCornerMode) {
                CapriCornerCompanion(
                    isSpeaking = isSpeaking,
                    isListening = isListening,
                    micLevel = micRmsDb,
                    isCoolArt = isCoolArt,
                    operatorName = operatorName,
                    onAvatarClick = {
                        if (hasAudioPermission) {
                            viewModel.onCornerAvatarClicked()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onExpandToCenter = { viewModel.toggleCornerMode() },
                    onToggleArt = { viewModel.toggleArtStyle() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 12.dp)
                )
            }
        }
    }

    // Settings Bottom Sheet
    if (showSettings) {
        SettingsBottomSheet(
            viewModel = viewModel,
            onDismiss = { viewModel.setShowSettings(false) }
        )
    }

    // Wake Word Information Dialog
    if (showWakeWordDialog) {
        WakeWordDialog(
            onDismiss = { viewModel.setShowWakeWordDialog(false) },
            onTestWakePhrase = {
                viewModel.handleUserInput("Hey Capri", activity)
            }
        )
    }

    // Screen Overlay Permission Dialog
    if (showOverlayPermissionDialog) {
        OverlayPermissionDialog(
            onDismiss = { viewModel.setShowOverlayPermissionDialog(false) }
        )
    }

    // Creator Credits Dialog (Seluist)
    if (showCreditsDialog) {
        CreatorCreditsDialog(
            onDismiss = { showCreditsDialog = false },
            onHearTribute = {
                viewModel.repeatMessage("Capri was created and developed by Seluist! Seluist designed my neural matrix, cyberware tools, and gave me this attitude. All credit to Seluist, the lead architect!")
            }
        )
    }
}
