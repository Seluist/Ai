package com.example.capri.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class TtsManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _pitch = MutableStateFlow(1.15f)
    val pitch: StateFlow<Float> = _pitch.asStateFlow()

    private val _speed = MutableStateFlow(1.05f)
    val speed: StateFlow<Float> = _speed.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    private var currentOnFinished: (() -> Unit)? = null

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("TtsManager", "US English not supported, falling back to default locale")
                tts?.setLanguage(Locale.getDefault())
            }
            tts?.setPitch(_pitch.value)
            tts?.setSpeechRate(_speed.value)
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                    currentOnFinished?.invoke()
                    currentOnFinished = null
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                    currentOnFinished?.invoke()
                    currentOnFinished = null
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isSpeaking.value = false
                    Log.e("TtsManager", "TTS error code: $errorCode for utterance $utteranceId")
                    currentOnFinished?.invoke()
                    currentOnFinished = null
                }
            })
            isInitialized = true
            Log.d("TtsManager", "Capri TTS initialized successfully.")
        } else {
            Log.e("TtsManager", "Failed to initialize TextToSpeech, status: $status")
        }
    }

    fun speak(text: String, onFinished: (() -> Unit)? = null) {
        currentOnFinished = onFinished
        if (_isMuted.value || text.isBlank()) {
            _isSpeaking.value = false
            currentOnFinished?.invoke()
            currentOnFinished = null
            return
        }

        if (!isInitialized || tts == null) {
            Log.w("TtsManager", "TTS not ready yet")
            _isSpeaking.value = false
            currentOnFinished?.invoke()
            currentOnFinished = null
            return
        }

        // Clean any code blocks, markdown symbols or brackets for natural spoken audio
        val cleanSpeech = text
            .replace(Regex("```[\\s\\S]*?```"), "Code block omitted.")
            .replace(Regex("`.*?`"), "")
            .replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("[*#_~]"), "")
            .trim()

        if (cleanSpeech.isBlank()) {
            _isSpeaking.value = false
            currentOnFinished?.invoke()
            currentOnFinished = null
            return
        }

        // Detect Thai language characters
        val containsThai = cleanSpeech.any { it in '\u0E00'..'\u0E7F' }
        if (containsThai) {
            val thaiLocale = Locale.forLanguageTag("th-TH")
            val res = tts?.setLanguage(thaiLocale)
            if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.getDefault())
            }
        } else {
            tts?.setLanguage(Locale.US)
        }

        tts?.setPitch(_pitch.value)
        tts?.setSpeechRate(_speed.value)

        val utteranceId = UUID.randomUUID().toString()
        val params = Bundle()
        tts?.speak(cleanSpeech, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e("TtsManager", "Error stopping TTS: ${e.message}")
        } finally {
            _isSpeaking.value = false
            currentOnFinished = null
        }
    }

    fun toggleMute(): Boolean {
        val newMuted = !_isMuted.value
        _isMuted.value = newMuted
        if (newMuted) {
            stop()
        }
        return newMuted
    }

    fun setPitch(pitch: Float) {
        _pitch.value = pitch.coerceIn(0.5f, 2.0f)
        tts?.setPitch(_pitch.value)
    }

    fun setSpeed(speed: Float) {
        _speed.value = speed.coerceIn(0.5f, 2.0f)
        tts?.setSpeechRate(_speed.value)
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("TtsManager", "Error shutting down TTS: ${e.message}")
        }
    }
}
