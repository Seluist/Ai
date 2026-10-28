package com.example.capri.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.capri.model.AvatarManager
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDark
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.CyberYellow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta

@Composable
fun OnboardingDialog(
    initialName: String = "Seluist",
    onConfirm: (String) -> Unit
) {
    var nameInput by remember { mutableStateOf(initialName) }
    var isError by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val infiniteTransition = rememberInfiniteTransition(label = "onboarding_avatar_float")
    val hoverOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatar_float"
    )

    Dialog(
        onDismissRequest = { /* Force explicit name entry on first boot */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CyberDark)
                .border(2.dp, NeonCyan, RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cyberpunk Header Tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = CyberYellow,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "// NEURAL LINK INITIALIZATION",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan,
                        letterSpacing = 1.2.sp
                    )
                }

                Text(
                    text = "IDENTIFY YOURSELF, NETRUNNER",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = CyberTextPrimary,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = "Before connecting to Capri's neural matrix, declare your call-sign. Capri will address you by this name across all voice, device, and neural operations.",
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 12.5.sp,
                    color = CyberTextSecondary,
                    lineHeight = 17.sp
                )

                // Animated Avatar Preview Box
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(120.dp)
                        .offset(y = hoverOffset.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CyberBlack)
                        .border(1.5.dp, NeonMagenta, RoundedCornerShape(16.dp))
                        .padding(6.dp)
                ) {
                    val customBitmap = AvatarManager.getAvatarBitmap(context)
                    if (customBitmap != null) {
                        Image(
                            bitmap = customBitmap.asImageBitmap(),
                            contentDescription = "Capri Avatar",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(108.dp)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.capri_user),
                            contentDescription = "Capri Avatar",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(108.dp)
                        )
                    }
                }

                // Operator Name Input
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = {
                        nameInput = it
                        if (it.isNotBlank()) isError = false
                    },
                    label = { Text("Operator Call-Sign", color = CyberTextMuted, fontSize = 12.sp) },
                    placeholder = { Text("Enter your name or codename...", color = CyberTextMuted) },
                    singleLine = true,
                    isError = isError,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Badge,
                            contentDescription = null,
                            tint = NeonCyan
                        )
                    },
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
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_name_input")
                )

                if (isError) {
                    Text(
                        text = "Call-sign cannot be empty, choom!",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = CyberYellow
                    )
                }

                // Confirm Button
                Button(
                    onClick = {
                        val trimmed = nameInput.trim()
                        if (trimmed.isEmpty()) {
                            isError = true
                        } else {
                            onConfirm(trimmed)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("onboarding_jack_in_button")
                ) {
                    Text(
                        text = "JACK IN // ESTABLISH NEURAL LINK",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Black,
                        color = CyberBlack,
                        letterSpacing = 0.8.sp
                    )
                }

                Text(
                    text = "Lead Architect & Design: Seluist",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.5.sp,
                    color = CyberTextMuted
                )
            }
        }
    }
}
