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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.example.capri.model.AvatarManager
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberBorderCyan
import com.example.ui.theme.CyberDark
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberYellow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPink

@Composable
fun CapriCornerCompanion(
    isSpeaking: Boolean,
    isListening: Boolean,
    micLevel: Float,
    isCoolArt: Boolean,
    operatorName: String,
    onAvatarClick: () -> Unit,
    onExpandToCenter: () -> Unit,
    onToggleArt: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "corner_avatar_motion")

    // Gentle hover float animation
    val hoverOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "corner_hover"
    )

    // Holographic pedestal rotation
    val platformRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "platform_rot"
    )

    // Pulse scale when listening
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "corner_pulse"
    )

    val imageRes = R.drawable.capri_user

    Column(
        horizontalAlignment = Alignment.End,
        modifier = modifier.padding(end = 12.dp, bottom = 4.dp)
    ) {
        // Floating speech / status indicator above her head
        Box(
            modifier = Modifier
                .padding(bottom = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CyberBlack)
                .border(
                    width = 1.dp,
                    color = when {
                        isListening -> NeonCyan
                        isSpeaking -> NeonMagenta
                        else -> CyberYellow.copy(alpha = 0.8f)
                    },
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 8.dp, vertical = 3.dp)
                .clickable { onAvatarClick() }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "Voice Command",
                    tint = if (isListening) NeonCyan else CyberYellow,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = when {
                        isListening -> "● LISTENING..."
                        isSpeaking -> "▶ VOCALIZING..."
                        else -> "CLICK TO COMMAND"
                    },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isListening -> NeonCyan
                        isSpeaking -> NeonPink
                        else -> CyberYellow
                    }
                )
            }
        }

        // Main Standing Capri Corner Avatar Card
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 130.dp, height = 150.dp)
                .offset(y = hoverOffset.dp)
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(CyberSurfaceElevated, CyberDark)
                    )
                )
                .border(
                    width = 1.5.dp,
                    color = if (isListening) NeonCyan else if (isSpeaking) NeonMagenta else NeonCyan.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onAvatarClick
                )
                .testTag("corner_avatar_button")
        ) {
            // Rotating Holographic Pedestal at bottom of card
            Canvas(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(width = 110.dp, height = 30.dp)
                    .offset(y = 4.dp)
                    .rotate(platformRotation)
            ) {
                drawOval(
                    color = NeonCyan.copy(alpha = 0.5f),
                    topLeft = Offset(0f, 0f),
                    size = size,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f), 0f)
                    )
                )
            }

            // Expanding ripple wave when listening
            if (isListening) {
                Canvas(modifier = Modifier.size(120.dp)) {
                    drawCircle(
                        color = NeonCyan.copy(alpha = 0.4f),
                        radius = (size.width / 2) * pulseScale,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }
            }

            // Capri Character Artwork
            val customBitmap by AvatarManager.customAvatarBitmap.collectAsState()
            val currentCustom = customBitmap ?: AvatarManager.getAvatarBitmap(LocalContext.current)
            if (currentCustom != null) {
                Image(
                    bitmap = currentCustom.asImageBitmap(),
                    contentDescription = "Capri standing in corner. Click to speak command.",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(width = 120.dp, height = 135.dp)
                        .align(Alignment.Center)
                )
            } else {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = "Capri standing in corner. Click to speak command.",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(width = 120.dp, height = 135.dp)
                        .align(Alignment.Center)
                )
            }

            // Top-right Expand to center button
            IconButton(
                onClick = onExpandToCenter,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp)
                    .padding(2.dp)
                    .testTag("expand_center_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Fullscreen,
                    contentDescription = "Expand Capri to Center",
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Top-left Art Style switch button
            IconButton(
                onClick = onToggleArt,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(28.dp)
                    .padding(2.dp)
                    .testTag("toggle_art_style_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.SwapHoriz,
                    contentDescription = "Toggle 1000x Cooler / Original Art",
                    tint = NeonMagenta,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Bottom name tag
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-4).dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(CyberBlack.copy(alpha = 0.85f))
                    .border(0.8.dp, NeonCyan.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = if (isCoolArt) "CAPRI • DEV: SELUIST" else "CAPRI // SELUIST",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCoolArt) NeonCyan else NeonMagenta
                )
            }
        }
    }
}
