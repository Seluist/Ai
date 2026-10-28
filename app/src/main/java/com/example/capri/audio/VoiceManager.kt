package com.example.capri.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceManager(
    private val context: Context,
    private val onResultRecognized: (String) -> Unit
) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _rmsDb = MutableStateFlow(0f)
    val rmsDb: StateFlow<Float> = _rmsDb.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    private fun ensureRecognizer(): SpeechRecognizer? {
        if (speechRecognizer == null && isAvailable()) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            Log.d("VoiceManager", "Ready for speech")
                            _isListening.value = true
                            _lastError.value = null
                        }

                        override fun onBeginningOfSpeech() {
                            Log.d("VoiceManager", "Speech started")
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            // rmsdB typically ranges from -2 to 10
                            _rmsDb.value = rmsdB.coerceAtLeast(0f)
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            Log.d("VoiceManager", "Speech ended")
                            _isListening.value = false
                            _rmsDb.value = 0f
                        }

                        override fun onError(error: Int) {
                            _isListening.value = false
                            _rmsDb.value = 0f
                            val message = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected in this frequency."
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Signal timeout, choom. Speak up."
                                SpeechRecognizer.ERROR_AUDIO -> "Audio hardware capture glitch."
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Audio recording permission missing."
                                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Netlink connection drop."
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Neural audio bus busy, retry."
                                else -> "Audio receptor glitch: code $error"
                            }
                            Log.w("VoiceManager", "Speech error: $message ($error)")
                            _lastError.value = message
                        }

                        override fun onResults(results: Bundle?) {
                            _isListening.value = false
                            _rmsDb.value = 0f
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                val text = matches[0].trim()
                                Log.d("VoiceManager", "Recognized text: $text")
                                _partialText.value = ""
                                onResultRecognized(text)
                            } else {
                                _lastError.value = "Signal received but no intelligible words parsed."
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                _partialText.value = matches[0]
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
            } catch (e: Exception) {
                Log.e("VoiceManager", "Failed to create SpeechRecognizer: ${e.message}")
            }
        }
        return speechRecognizer
    }

    fun startListening() {
        val recognizer = ensureRecognizer()
        if (recognizer == null) {
            _lastError.value = "Speech recognition service unavailable on device."
            return
        }

        try {
            _lastError.value = null
            _partialText.value = ""
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            recognizer.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error starting listening: ${e.message}")
            _isListening.value = false
            _lastError.value = e.message
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error stopping listening: ${e.message}")
        } finally {
            _isListening.value = false
            _rmsDb.value = 0f
        }
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e("VoiceManager", "Error destroying speech recognizer: ${e.message}")
        }
    }
}
