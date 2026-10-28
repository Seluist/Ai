package com.example.capri.viewmodel

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.capri.ai.AiService
import com.example.capri.ai.CapriBrain
import com.example.capri.audio.TtsManager
import com.example.capri.audio.VoiceManager
import com.example.capri.device.DeviceControlManager
import android.graphics.Bitmap
import com.example.capri.model.AvatarManager
import com.example.capri.model.ChatMessage
import com.example.capri.model.MessageType
import com.example.capri.tool.CapriToolRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CapriViewModel(application: Application) : AndroidViewModel(application) {

    val deviceControlManager = DeviceControlManager(application)
    val ttsManager = TtsManager(application)
    val toolRegistry = CapriToolRegistry(deviceControlManager, ttsManager)
    val brain = CapriBrain(toolRegistry)
    val aiService = AiService(application)

    private val userPrefs = application.getSharedPreferences("capri_user_prefs", android.content.Context.MODE_PRIVATE)

    private val _hasCompletedOnboarding = MutableStateFlow(userPrefs.getBoolean("has_completed_onboarding", false))
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    private val _operatorName = MutableStateFlow(userPrefs.getString("operator_name", "Seluist") ?: "Seluist")
    val operatorName: StateFlow<String> = _operatorName.asStateFlow()

    val customAvatarBitmap: StateFlow<Bitmap?> = AvatarManager.customAvatarBitmap

    private val _isCornerMode = MutableStateFlow(false)
    val isCornerMode: StateFlow<Boolean> = _isCornerMode.asStateFlow()

    private val _isCoolArt = MutableStateFlow(true)
    val isCoolArt: StateFlow<Boolean> = _isCoolArt.asStateFlow()

    private val _isOverlayOnLeaveEnabled = MutableStateFlow(true)
    val isOverlayOnLeaveEnabled: StateFlow<Boolean> = _isOverlayOnLeaveEnabled.asStateFlow()

    private val _isContinuousVoiceMode = MutableStateFlow(false)
    val isContinuousVoiceMode: StateFlow<Boolean> = _isContinuousVoiceMode.asStateFlow()

    private val _showOverlayPermissionDialog = MutableStateFlow(false)
    val showOverlayPermissionDialog: StateFlow<Boolean> = _showOverlayPermissionDialog.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "Welcome back, ${userPrefs.getString("operator_name", "Seluist")}! Capri conversational neural matrix online. Ready to talk, open your apps (Discord, YouTube, Chrome), adjust hardware, or float outside the app!",
                type = MessageType.CAPRI_AI
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoadingAi = MutableStateFlow(false)
    val isLoadingAi: StateFlow<Boolean> = _isLoadingAi.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _showWakeWordDialog = MutableStateFlow(false)
    val showWakeWordDialog: StateFlow<Boolean> = _showWakeWordDialog.asStateFlow()

    private val _permissionWarning = MutableStateFlow<String?>(null)
    val permissionWarning: StateFlow<String?> = _permissionWarning.asStateFlow()

    val isTorchOn: StateFlow<Boolean> = deviceControlManager.isTorchOn
    val volumePercent: StateFlow<Int> = deviceControlManager.volumePercent
    val screenBrightness: StateFlow<Float> = deviceControlManager.screenBrightness

    val isSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking
    val isMuted: StateFlow<Boolean> = ttsManager.isMuted

    var currentActivity: Activity? = null

    val voiceManager = VoiceManager(application) { recognizedSpeech ->
        if (recognizedSpeech.isNotBlank()) {
            handleUserInput(recognizedSpeech, currentActivity)
        }
    }

    val isListening: StateFlow<Boolean> = voiceManager.isListening
    val micRmsDb: StateFlow<Float> = voiceManager.rmsDb
    val partialVoiceText: StateFlow<String> = voiceManager.partialText

    init {
        AvatarManager.init(application)
        if (_hasCompletedOnboarding.value) {
            viewModelScope.launch {
                kotlinx.coroutines.delay(800)
                ttsManager.speak("Welcome back, ${_operatorName.value}! Capri online and standing by. What's the job today?")
            }
        }
    }

    fun handleUserInput(rawText: String, activity: Activity? = currentActivity) {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return

        // 1. Add user message
        val userMsg = ChatMessage(text = trimmed, type = MessageType.USER)
        _messages.value = _messages.value + userMsg

        // 2. Stop listening if currently active
        voiceManager.stopListening()

        // 3. Process with Capri Brain
        val commandResult = brain.processInput(trimmed, activity)

        if (commandResult.handledLocally) {
            val replyMsg = ChatMessage(
                text = commandResult.responseText,
                type = MessageType.CAPRI_COMMAND,
                commandExecuted = commandResult.commandType,
                isSuccess = commandResult.isSuccess
            )
            _messages.value = _messages.value + replyMsg

            if (commandResult.commandType != "STOP_SPEAKING") {
                ttsManager.speak(commandResult.responseText) {
                    if (_isContinuousVoiceMode.value) {
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(400)
                            startListening()
                        }
                    }
                }
            }
        } else if (commandResult.shouldPassToOnlineAi) {
            // Send to Online AI
            viewModelScope.launch {
                _isLoadingAi.value = true
                val aiResult = try {
                    if (aiService.hasValidApiKey()) {
                        aiService.queryOnlineAi(
                            userPrompt = trimmed,
                            recentHistory = _messages.value,
                            toolRegistry = toolRegistry,
                            activity = activity
                        )
                    } else {
                        com.example.capri.ai.AiQueryResult(
                            replyText = brain.getOfflineFallbackResponse(trimmed)
                        )
                    }
                } catch (e: Exception) {
                    Log.e("CapriViewModel", "Error fetching online AI: ${e.message}")
                    com.example.capri.ai.AiQueryResult(
                        replyText = brain.getOfflineFallbackResponse(trimmed)
                    )
                } finally {
                    _isLoadingAi.value = false
                }

                val replyMsg = ChatMessage(
                    text = aiResult.replyText,
                    type = if (aiResult.toolResult != null) MessageType.CAPRI_COMMAND else MessageType.CAPRI_AI,
                    commandExecuted = aiResult.toolResult?.toolName,
                    isSuccess = aiResult.toolResult?.isSuccess ?: true
                )
                _messages.value = _messages.value + replyMsg

                ttsManager.speak(aiResult.replyText) {
                    if (_isContinuousVoiceMode.value) {
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(400)
                            startListening()
                        }
                    }
                }
            }
        }
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    fun toggleContinuousVoiceMode() {
        val next = !_isContinuousVoiceMode.value
        _isContinuousVoiceMode.value = next
        val status = if (next) "Continuous follow-up listening enabled." else "Continuous listening disabled."
        ttsManager.speak(status)
    }

    fun onAvatarTapped() {
        val wittyLines = listOf(
            "Hands off the chrome, choom! Just kidding, need me to run a diagnostic?",
            "Checking my optical sensors... yep, looking sharp as ever, Seluist.",
            "I'm operating at 99.8% neural bandwidth. Don't worry, plenty of juice left for you.",
            "Nova! You tapped my neural node. What's the next mission, boss?",
            "Careful! That collar is pure Arasaka-grade cyberware. Ready when you are."
        )
        val line = wittyLines.random()
        val msg = ChatMessage(text = line, type = MessageType.CAPRI_AI)
        _messages.value = _messages.value + msg
        ttsManager.speak(line)
    }

    fun toggleTorch() {
        val (success, message) = deviceControlManager.toggleTorch()
        val msg = ChatMessage(
            text = message,
            type = MessageType.CAPRI_COMMAND,
            commandExecuted = "TORCH_TOGGLE",
            isSuccess = success
        )
        _messages.value = _messages.value + msg
        ttsManager.speak(message)
    }

    fun volumeUp() {
        val (success, message) = deviceControlManager.volumeUp()
        val msg = ChatMessage(
            text = message,
            type = MessageType.CAPRI_COMMAND,
            commandExecuted = "VOLUME_UP",
            isSuccess = success
        )
        _messages.value = _messages.value + msg
        ttsManager.speak(message)
    }

    fun volumeDown() {
        val (success, message) = deviceControlManager.volumeDown()
        val msg = ChatMessage(
            text = message,
            type = MessageType.CAPRI_COMMAND,
            commandExecuted = "VOLUME_DOWN",
            isSuccess = success
        )
        _messages.value = _messages.value + msg
        ttsManager.speak(message)
    }

    fun setScreenBrightness(percent: Int, activity: Activity? = currentActivity) {
        val (success, message) = deviceControlManager.setScreenBrightness(activity, percent)
        val msg = ChatMessage(
            text = message,
            type = MessageType.CAPRI_COMMAND,
            commandExecuted = "BRIGHTNESS_SET",
            isSuccess = success
        )
        _messages.value = _messages.value + msg
        ttsManager.speak(message)
    }

    fun checkBattery() {
        val status = deviceControlManager.getBatteryStatus()
        val msg = ChatMessage(
            text = status,
            type = MessageType.CAPRI_COMMAND,
            commandExecuted = "BATTERY_STATUS",
            isSuccess = true
        )
        _messages.value = _messages.value + msg
        ttsManager.speak(status)
    }

    fun toggleMute() {
        ttsManager.toggleMute()
    }

    fun repeatMessage(text: String) {
        ttsManager.speak(text)
    }

    fun startListening() {
        ttsManager.stop()
        voiceManager.startListening()
    }

    fun stopListening() {
        voiceManager.stopListening()
    }

    fun clearHistory() {
        _messages.value = listOf(
            ChatMessage(
                text = "Memory registers purged. Ready for fresh Net chatter.",
                type = MessageType.SYSTEM_ALERT
            )
        )
        ttsManager.speak("Neural logs cleared, choom.")
    }

    fun setShowSettings(show: Boolean) {
        _showSettingsDialog.value = show
    }

    fun setShowWakeWordDialog(show: Boolean) {
        _showWakeWordDialog.value = show
    }

    fun toggleCornerMode() {
        val newMode = !_isCornerMode.value
        _isCornerMode.value = newMode
        if (newMode) {
            ttsManager.speak("Taking my post in the corner, Seluist! Tap me anytime to issue voice commands.")
        } else {
            ttsManager.speak("Center stage unlocked. What's the mission, Seluist?")
        }
    }

    fun toggleArtStyle() {
        val next = !_isCoolArt.value
        _isCoolArt.value = next
        val styleName = if (next) "1000x Cooler Anime Edition" else "Original Cyberpunk Sketch"
        ttsManager.speak("Visual style recalibrated to $styleName, Seluist.")
    }

    fun onCornerAvatarClicked() {
        if (isListening.value) {
            stopListening()
        } else {
            startListening()
        }
    }

    fun completeOnboarding(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank()) {
            _operatorName.value = trimmed
            _hasCompletedOnboarding.value = true
            userPrefs.edit()
                .putString("operator_name", trimmed)
                .putBoolean("has_completed_onboarding", true)
                .apply()

            val greeting = "Neural link established with $trimmed! Capri online and fully operational. What's the mission today?"
            _messages.value = listOf(
                ChatMessage(
                    text = greeting,
                    type = MessageType.CAPRI_AI
                )
            )
            viewModelScope.launch {
                kotlinx.coroutines.delay(400)
                ttsManager.speak(greeting)
            }
        }
    }

    fun setOperatorName(name: String) {
        if (name.isNotBlank()) {
            val trimmed = name.trim()
            _operatorName.value = trimmed
            userPrefs.edit().putString("operator_name", trimmed).apply()
        }
    }

    fun importCustomAvatar(uri: android.net.Uri): Boolean {
        val success = AvatarManager.saveCustomAvatar(getApplication(), uri)
        if (success) {
            ttsManager.speak("Custom avatar picture loaded and synced across all decks, ${_operatorName.value}!")
        } else {
            ttsManager.speak("Failed to process the chosen picture. Make sure it's a valid image.")
        }
        return success
    }

    fun resetAvatarToDefault() {
        AvatarManager.resetToDefault(getApplication())
        ttsManager.speak("Restored Capri's original cyberpunk visual identity.")
    }

    fun toggleOverlayOnLeave(context: android.content.Context) {
        val next = !_isOverlayOnLeaveEnabled.value
        _isOverlayOnLeaveEnabled.value = next
        if (next) {
            ttsManager.speak("Overlay companion enabled! I'll float on your screen when you leave the app, ${_operatorName.value}.")
            if (!android.provider.Settings.canDrawOverlays(context)) {
                _showOverlayPermissionDialog.value = true
            }
        } else {
            ttsManager.speak("Overlay companion disabled. I'll stay inside the cyberdeck.")
            context.stopService(android.content.Intent(context, com.example.capri.service.CapriOverlayService::class.java))
        }
    }

    fun setShowOverlayPermissionDialog(show: Boolean) {
        _showOverlayPermissionDialog.value = show
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.destroy()
        ttsManager.shutdown()
    }
}
