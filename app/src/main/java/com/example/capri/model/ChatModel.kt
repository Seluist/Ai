package com.example.capri.model

import java.util.UUID

enum class MessageType {
    USER,
    CAPRI_AI,
    CAPRI_COMMAND,
    SYSTEM_ALERT
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val type: MessageType,
    val timestamp: Long = System.currentTimeMillis(),
    val commandExecuted: String? = null,
    val isSuccess: Boolean = true
) {
    val isFromUser: Boolean
        get() = type == MessageType.USER
}
