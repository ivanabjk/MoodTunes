package com.example.moodtunes_v1.home

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moodtunes_v1.VoiceToTextParser
import com.example.moodtunes_v1.mood_detection.EmotionClassifier
import com.example.moodtunes_v1.mood_detection.HfResponseItem
import com.example.moodtunes_v1.mood_detection.MoodClassifier
import kotlinx.coroutines.launch

class HomeViewModel(
    private val app: Application,
    private val context: Context,
    private val state: SavedStateHandle
) : ViewModel() {

    private val voiceToTextParser = VoiceToTextParser(context)
    private val emotionClassifier = EmotionClassifier(context)

    private val _emotionsLiveData = MutableLiveData<String>()
    val emotionsLiveData: LiveData<String> get() = _emotionsLiveData

    private val _isSpeaking = MutableLiveData<Boolean>()
    val isSpeaking: LiveData<Boolean> get() = _isSpeaking

    private val _userText = state.getLiveData("userText", "")
    val userText: LiveData<String> get() = _userText

    private val _detectedMood = state.getLiveData("detectedMood", "")
    val detectedMood: LiveData<String> get() = _detectedMood

    val spokenTextLiveData = MutableLiveData<String>()

    init {
        viewModelScope.launch {
            voiceToTextParser.stateFlow.collect { parserState ->
                if (parserState.spokenText.isNotBlank()) {
                    spokenTextLiveData.postValue(parserState.spokenText)
                    saveUserText(parserState.spokenText)
                }
                _isSpeaking.postValue(parserState.isSpeaking)
            }
        }
    }


    fun startListening() {
        voiceToTextParser.startListening()
        _isSpeaking.value = true
    }

    fun stopListening() {
        voiceToTextParser.stopListening()
        _isSpeaking.value = false
    }

    fun saveUserText(text: String) {
        state["userText"] = text
    }

    fun saveMood(mood: String) {
        state["detectedMood"] = mood
    }


    @SuppressLint("DefaultLocale")
    fun analyzeEmotion(text: String) {
        viewModelScope.launch {
            try {
                val predictions = emotionClassifier.classify(text)
                val labels = emotionClassifier.loadLabels("labels.txt")
                val emotions = labels.mapIndexed { index, label ->
                    "$label: ${String.format("%.2f", predictions[index] * 100)}%"
                }.joinToString("\n")

                _emotionsLiveData.postValue(emotions)

                val moodResult = MoodClassifier.classifyMood(labels.mapIndexed { index, label ->
                    HfResponseItem(label, predictions[index])
                })

//                _detectedMood.postValue(moodResult.mood)
                saveMood(moodResult.mood)


            } catch (e: Exception) {
                _emotionsLiveData.postValue("Error: ${e.message}")
            }
        }
    }

    fun clearMood() {
        viewModelScope.launch {
            _emotionsLiveData.postValue("")
            _detectedMood.postValue("")
            _userText.postValue("")
        }
    }
}