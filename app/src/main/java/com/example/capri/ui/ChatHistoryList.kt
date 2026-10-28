package com.example.capri.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.capri.model.ChatMessage
import com.example.capri.model.MessageType
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberDark
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.CyberYellow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPink
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatHistoryList(
    messages: List<ChatMessage>,
    isLoadingAi: Boolean,
    onReplayAudio: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isLoadingAi) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        items(messages, key = { it.id }) { msg ->
            when (msg.type) {
                MessageType.USER -> UserMessageItem(msg)
                MessageType.CAPRI_AI, MessageType.CAPRI_COMMAND -> CapriMessageItem(msg, onReplayAudio)
                MessageType.SYSTEM_ALERT -> SystemAlertItem(msg)
            }
        }

        if (isLoadingAi) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = NeonCyan
                    )
                    Text(
                        text = "CAPRI NETLINK: Processing through global neural subnet...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = NeonCyan
                    )
                }
            }
        }
    }
}

@Composable
private fun UserMessageItem(msg: ChatMessage) {
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Text(
                text = "USER",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = NeonCyan
            )
            Text(
                text = "• $timeStr",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = CyberTextMuted
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 2.dp, bottomStart = 12.dp, bottomEnd = 12.dp))
                .background(CyberSurfaceElevated)
                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(topStart = 12.dp, topEnd = 2.dp, bottomStart = 12.dp, bottomEnd = 12.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = msg.text,
                fontFamily = FontFamily.SansSerif,
                fontSize = 14.sp,
                color = CyberTextPrimary,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun CapriMessageItem(
    msg: ChatMessage,
    onReplayAudio: (String) -> Unit
) {
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp))
    val isCommand = msg.type == MessageType.CAPRI_COMMAND

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Capri thumbnail
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .border(1.dp, NeonMagenta, CircleShape)
        ) {
            Image(
                painter = painterResource(id = R.drawable.capri_user),
                contentDescription = "Capri",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(32.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                Text(
                    text = "CAPRI",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonMagenta
                )
                if (isCommand && msg.commandExecuted != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(CyberBlack)
                            .border(1.dp, if (msg.isSuccess) CyberGreen else CyberYellow, RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "[HARDWARE: ${msg.commandExecuted}]",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (msg.isSuccess) CyberGreen else CyberYellow
                        )
                    }
                }
                Text(
                    text = "• $timeStr",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = CyberTextMuted
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp))
                    .background(CyberSurface)
                    .border(1.dp, NeonMagenta.copy(alpha = 0.4f), RoundedCornerShape(topStart = 2.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = msg.text,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 14.sp,
                        color = CyberTextPrimary,
                        lineHeight = 20.sp,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = { onReplayAudio(msg.text) },
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("replay_audio_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Speak line",
                            tint = NeonMagenta.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemAlertItem(msg: ChatMessage) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(CyberBlack)
            .border(1.dp, CyberYellow.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "◆ ${msg.text}",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = CyberYellow,
            lineHeight = 16.sp
        )
    }
}
