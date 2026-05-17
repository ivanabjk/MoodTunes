package com.example.moodtunes_v1

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.util.Locale

class VoiceToTextParser(
    private val context: Context
) : RecognitionListener {

    private val _state = MutableStateFlow(VoiceToTextParserState())

    private var recognizer: SpeechRecognizer? = null
    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(this@VoiceToTextParser)
            }
        } else {
            _state.update { it.copy(error = "Speech recognition not available") }
        }
    }
    val state: MutableStateFlow<VoiceToTextParserState> get() = _state
    val stateFlow: StateFlow<VoiceToTextParserState> get() = _state

    fun startListening(languageCode: String = "en-US") {
        _state.update { VoiceToTextParserState() }

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _state.update {
                it.copy(
                    error = "Recognition not available."
                )
            }
            return
        }

        val languageCode = Locale.getDefault().toLanguageTag() // e.g. "mk-MK"

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
        }

        recognizer?.setRecognitionListener(this)
        recognizer?.startListening(intent)

//        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
//            setRecognitionListener(this@VoiceToTextParser)
////            startListening(intent)
//        }
//        recognizer?.startListening(intent)


        _state.update {
            it.copy(
                isSpeaking = true
            )
        }
//        Handler(Looper.getMainLooper()).postDelayed({
//            if (_state.value.isSpeaking) {
//                Log.d("VoiceDebug", "Timeout reached, forcing stop")
//                stopListening()
//            }
//        }, 6000)

    }

    fun stopListening() {
        _state.update { it.copy(isSpeaking = false) }
        recognizer?.stopListening()
        recognizer?.cancel()
        recognizer = null
    }

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d("VoiceDebug", "Ready for speech")
        _state.update {
            it.copy(
                error = null
            )
        }
    }

    override fun onBeginningOfSpeech(){
        Log.d("VoiceDebug", "Speech started")

    }

    override fun onRmsChanged(rmsdB: Float) = Unit

    override fun onBufferReceived(buffer: ByteArray?) = Unit

    override fun onEndOfSpeech() {
        Log.d("VoiceDebug", "End of speech")

        _state.update {
            it.copy(
                isSpeaking = false
            )
        }
//        stopListening()
    }

    override fun onError(error: Int) {

        _state.update { it.copy(isSpeaking = false) }

        val message = when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> "No match"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_AUDIO -> "Audio error"
            SpeechRecognizer.ERROR_CLIENT -> "Client error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission error"
            else -> "Unknown error"
        }

        Log.e("VoiceDebug", "Recognizer error code: $message")

        _state.update { it.copy(error = message) }
    }

    override fun onResults(results: Bundle?) {
        val resultList = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        Log.d("VoiceDebug", "Raw results: $resultList")
        val result = resultList?.getOrNull(0)
        if (result != null) {
            _state.update { it.copy(spokenText = result) }
        } else {
            _state.update { it.copy(error = "No speech recognized") }
        }

    }

    override fun onPartialResults(partialResults: Bundle?) = Unit

    override fun onEvent(eventType: Int, params: Bundle?) = Unit

}

data class VoiceToTextParserState(
    val spokenText: String = "",
    val isSpeaking: Boolean = false,
    val error: String? = null
)
