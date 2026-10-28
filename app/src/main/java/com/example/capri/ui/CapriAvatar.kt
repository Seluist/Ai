package com.example.capri.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.example.capri.model.AvatarManager
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberYellow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanDark
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPink
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun CapriAvatar(
    isSpeaking: Boolean,
    isListening: Boolean,
    micLevel: Float, // 0f to 10f
    onAvatarClick: () -> Unit,
    isCoolArt: Boolean = true,
    modifier: Modifier = Modifier
) {
    // 1. Natural Blinking Loop: blink eye state (1f = fully open, 0f = closed)
    val eyeOpenness = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        while (true) {
            // Wait random interval between 2.5s and 5.5s
            delay(Random.nextLong(2500, 5500))
            // Quick natural blink: close in 70ms, open in 90ms
            eyeOpenness.animateTo(0f, tween(70, easing = LinearEasing))
            eyeOpenness.animateTo(1f, tween(90, easing = LinearEasing))
            // Occasional double-blink
            if (Random.nextFloat() < 0.25f) {
                delay(120)
                eyeOpenness.animateTo(0f, tween(60, easing = LinearEasing))
                eyeOpenness.animateTo(1f, tween(80, easing = LinearEasing))
            }
        }
    }

    // 2. Animated Speaking Mouth Modulation
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_motion")
    val speechMouthCycle by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speech_mouth"
    )

    // Dynamic mouth open height: when speaking oscillates between 0.2 and 1.0; when idle sits at 0.05
    val currentMouthOpen = if (isSpeaking) {
        (speechMouthCycle * 0.85f).coerceIn(0.15f, 1f)
    } else {
        0.05f
    }

    // 3. Cyber HUD Rings Rotation
    val hudRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hud_rot"
    )

    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hud_counter_rot"
    )

    // Breathing float animation
    val breathingOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Aura pulse
    val auraScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_pulse"
    )

    // Active color scheme based on state
    val activeBorderColor = when {
        isListening -> NeonCyan
        isSpeaking -> NeonMagenta
        else -> NeonCyan.copy(alpha = 0.6f)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(230.dp)
            .clickable(onClick = onAvatarClick)
    ) {
        // Background Glow Aura
        Box(
            modifier = Modifier
                .size(190.dp)
                .scale(if (isSpeaking || isListening) auraScale * 1.08f else auraScale)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            if (isSpeaking) NeonMagenta.copy(alpha = 0.35f) else NeonCyan.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        // Outer Cyber HUD Ring Canvas (with rotating ticks & brackets)
        Canvas(
            modifier = Modifier
                .size(220.dp)
                .rotate(hudRotation)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2 - 4.dp.toPx()

            // Dashed cyan orbital ring
            drawCircle(
                color = NeonCyan.copy(alpha = 0.5f),
                radius = radius,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 18f, 10f, 18f), 0f)
                )
            )

            // Accent Corner Brackets at 4 quadrants
            val bracketLen = 22.dp.toPx()
            for (angle in listOf(45f, 135f, 225f, 315f)) {
                val rad = Math.toRadians(angle.toDouble())
                val bx = center.x + (radius * cos(rad)).toFloat()
                val by = center.y + (radius * sin(rad)).toFloat()
                drawCircle(
                    color = NeonMagenta,
                    radius = 3.dp.toPx(),
                    center = Offset(bx, by)
                )
            }
        }

        // Counter-rotating Inner HUD Ring
        Canvas(
            modifier = Modifier
                .size(195.dp)
                .rotate(counterRotation)
        ) {
            val radius = size.width / 2 - 2.dp.toPx()
            drawCircle(
                color = NeonMagenta.copy(alpha = 0.4f),
                radius = radius,
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 25f), 0f)
                )
            )
        }

        // Main Character Showcase Box with breathing float
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(width = 200.dp, height = 210.dp)
                .offset(y = breathingOffset.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(CyberBlack.copy(alpha = 0.88f))
                .border(2.dp, activeBorderColor, RoundedCornerShape(20.dp))
        ) {
            val customBitmap by AvatarManager.customAvatarBitmap.collectAsState()
            val currentCustom = customBitmap ?: AvatarManager.getAvatarBitmap(LocalContext.current)
            if (currentCustom != null) {
                Image(
                    bitmap = currentCustom.asImageBitmap(),
                    contentDescription = "Capri Cyberpunk AI Assistant Avatar",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.capri_user),
                    contentDescription = "Capri Cyberpunk AI Assistant Avatar",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                )
            }

            // Dynamic Overlay Canvas for Blinking Eyes & Animated Mouth
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val w = size.width
                val h = size.height

                // EYES OVERLAY: When eyeOpenness < 1f, draw glowing cyber optics blink
                if (eyeOpenness.value < 0.95f) {
                    val blinkProgress = 1f - eyeOpenness.value

                    val leftEyeCenter = Offset(w * 0.49f, h * 0.365f)
                    val rightEyeCenter = Offset(w * 0.54f, h * 0.365f)
                    val eyeRadiusX = w * 0.035f
                    val eyeRadiusY = h * 0.02f * blinkProgress

                    val eyelidBrush = Brush.verticalGradient(
                        colors = listOf(CyberBlack.copy(alpha = 0.95f), NeonMagenta.copy(alpha = 0.85f))
                    )

                    drawOval(
                        brush = eyelidBrush,
                        topLeft = Offset(leftEyeCenter.x - eyeRadiusX, leftEyeCenter.y - eyeRadiusY),
                        size = Size(eyeRadiusX * 2, eyeRadiusY * 2)
                    )
                    drawOval(
                        brush = eyelidBrush,
                        topLeft = Offset(rightEyeCenter.x - eyeRadiusX, rightEyeCenter.y - eyeRadiusY),
                        size = Size(eyeRadiusX * 2, eyeRadiusY * 2)
                    )

                    drawLine(
                        color = NeonCyan,
                        start = Offset(leftEyeCenter.x - eyeRadiusX, leftEyeCenter.y),
                        end = Offset(leftEyeCenter.x + eyeRadiusX, leftEyeCenter.y),
                        strokeWidth = 1.8.dp.toPx()
                    )
                    drawLine(
                        color = NeonCyan,
                        start = Offset(rightEyeCenter.x - eyeRadiusX, rightEyeCenter.y),
                        end = Offset(rightEyeCenter.x + eyeRadiusX, rightEyeCenter.y),
                        strokeWidth = 1.8.dp.toPx()
                    )
                }

                // MOUTH OVERLAY: When speaking, dynamically render animated mouth pulse
                if (isSpeaking) {
                    val mouthCenter = Offset(w * 0.52f, h * 0.40f)
                    val mouthWidth = w * 0.06f
                    val mouthHeight = (h * 0.025f * currentMouthOpen).coerceAtLeast(2f)

                    drawOval(
                        color = NeonMagenta,
                        topLeft = Offset(mouthCenter.x - mouthWidth / 2, mouthCenter.y - mouthHeight / 2),
                        size = Size(mouthWidth, mouthHeight),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // Audio Spectrum / Mic Wave Arc around avatar when listening or speaking
                if (isListening || isSpeaking) {
                    val barCount = 18
                    val baseRadius = w * 0.48f
                    val level = if (isListening) (micLevel / 10f).coerceIn(0.1f, 1f) else currentMouthOpen

                    for (i in 0 until barCount) {
                        val angle = 110f + (i * (140f / barCount))
                        val rad = Math.toRadians(angle.toDouble())
                        val barHeight = (4.dp.toPx() + (level * 16.dp.toPx() * ((i % 3) + 1) / 3f))

                        val sx = (w / 2) + ((baseRadius) * cos(rad)).toFloat()
                        val sy = (h / 2) + ((baseRadius) * sin(rad)).toFloat()
                        val ex = (w / 2) + ((baseRadius + barHeight) * cos(rad)).toFloat()
                        val ey = (h / 2) + ((baseRadius + barHeight) * sin(rad)).toFloat()

                        drawLine(
                            color = if (isSpeaking) NeonMagenta else NeonCyan,
                            start = Offset(sx, sy),
                            end = Offset(ex, ey),
                            strokeWidth = 2.2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }

        // Floating Status Badge below Avatar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 10.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CyberBlack)
                .border(
                    width = 1.dp,
                    color = when {
                        isSpeaking -> NeonMagenta
                        isListening -> NeonCyan
                        else -> CyberYellow
                    },
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 3.dp)
        ) {
            val statusText = when {
                isListening -> "● LISTENING..."
                isSpeaking -> "▶ VOCALIZING..."
                else -> "◆ CAPRI // SELUIST"
            }
            val statusColor = when {
                isListening -> NeonCyan
                isSpeaking -> NeonPink
                else -> CyberYellow
            }
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp
            )
        }
    }
}
