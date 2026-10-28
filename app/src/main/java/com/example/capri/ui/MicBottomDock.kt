package com.example.capri.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberBorderCyan
import com.example.ui.theme.CyberDark
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPink
import com.example.ui.theme.CyberYellow

@Composable
fun MicBottomDock(
    isListening: Boolean,
    micRmsDb: Float,
    partialText: String,
    onMicClick: () -> Unit,
    onSendText: (String) -> Unit,
    isSpeaking: Boolean = false,
    onStopSpeaking: () -> Unit = {},
    isContinuousVoiceMode: Boolean = false,
    onToggleContinuousMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CyberDark)
            .border(
                width = 1.dp,
                color = CyberBorder,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Interruption Banner when Capri is speaking
        AnimatedVisibility(
            visible = isSpeaking,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeonPink.copy(alpha = 0.15f))
                    .border(1.dp, NeonPink, RoundedCornerShape(8.dp))
                    .clickable(onClick = onStopSpeaking)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "■ STOP TALKING ",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonPink
                    )
                    Text(
                        text = "(Tap to interrupt voice)",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 10.sp,
                        color = CyberTextMuted
                    )
                }
            }
        }

        // Live speech recognition feedback banner & Follow-up mode indicator
        AnimatedVisibility(
            visible = isListening || partialText.isNotBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CyberBlack)
                    .border(1.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (partialText.isNotBlank()) "» \"$partialText\"" else "● Audio receptor active... speak now choom",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        color = NeonCyan,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    if (isContinuousVoiceMode) {
                        Text(
                            text = "AUTO-FOLLOWUP",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = CyberYellow,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Text Input field
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = {
                    Text(
                        text = "Transmit cyber command...",
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 13.sp,
                        color = CyberTextMuted
                    )
                },
                textStyle = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp,
                    color = CyberTextPrimary
                ),
                maxLines = 2,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (textInput.isNotBlank()) {
                        onSendText(textInput)
                        textInput = ""
                        keyboardController?.hide()
                    }
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = CyberBorder,
                    focusedContainerColor = CyberSurface,
                    unfocusedContainerColor = CyberSurface,
                    cursorColor = NeonCyan
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("command_input_field")
            )

            // Send text button (if text entered) or Mic Trigger
            if (textInput.isNotBlank()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(NeonCyan)
                        .clickable {
                            onSendText(textInput)
                            textInput = ""
                            keyboardController?.hide()
                        }
                        .testTag("send_command_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Command",
                        tint = CyberBlack,
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                // Large Glowing Cyberpunk Microphone Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(56.dp)
                ) {
                    // Outer expanding pulse ripples when listening
                    if (isListening) {
                        Canvas(modifier = Modifier.size(56.dp)) {
                            drawCircle(
                                color = NeonCyan.copy(alpha = pulseAlpha),
                                radius = (size.width / 2) * pulseScale,
                                style = Stroke(width = 2.dp.toPx())
                            )
                            // Audio dB response ripple
                            val rmsRadius = (size.width / 2) * (1f + (micRmsDb / 15f))
                            drawCircle(
                                color = NeonPink.copy(alpha = 0.5f),
                                radius = rmsRadius,
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    }

                    // Main Mic Circle
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = if (isListening) {
                                        listOf(NeonCyan, NeonPink)
                                    } else {
                                        listOf(CyberSurfaceElevated, CyberSurface)
                                    }
                                )
                            )
                            .border(
                                width = 2.dp,
                                color = if (isListening) NeonCyan else NeonMagenta,
                                shape = CircleShape
                            )
                            .clickable(onClick = onMicClick)
                            .testTag("main_mic_button")
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Filled.Mic else Icons.Filled.Mic,
                            contentDescription = if (isListening) "Stop Listening" else "Start Voice Command",
                            tint = if (isListening) CyberBlack else NeonCyan,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}
