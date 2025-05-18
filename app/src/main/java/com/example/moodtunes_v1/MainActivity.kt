package com.example.moodtunes_v1

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.widget.TextView
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var voiceToTextParser: VoiceToTextParser
    private lateinit var textView: TextView
    private lateinit var emotionsListTextView: TextView
    private lateinit var fab: FloatingActionButton
    private var canRecord = false
    private val scope = MainScope()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView)
        emotionsListTextView = findViewById(R.id.tvEmotionList)
        fab = findViewById(R.id.fab)

        voiceToTextParser = VoiceToTextParser(application)

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            canRecord = isGranted
            if (!isGranted) {
                textView.text = "Permission denied."
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            canRecord = true
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        fab.setOnClickListener {
            if (voiceToTextParser.state.value.isSpeaking) {
                voiceToTextParser.stopListening()
            } else if (canRecord) {
                voiceToTextParser.startListening()
            }
        }

        scope.launch {
            voiceToTextParser.state.collect { state ->
                if (state.isSpeaking) {
                    textView.text = "Speaking..."
                    fab.setImageResource(R.drawable.ic_stop)
                } else {
                    // Wait for spokenText to update before resetting
                    if (state.spokenText.isNotEmpty()) {
                        textView.text = state.spokenText
                    } else {
                        scope.launch {
                            kotlinx.coroutines.delay(300) // Small delay before resetting
                            if (textView.text == "Speaking...") {
                                textView.text = "Click on mic to record audio."
                            }
                        }
                    }
                    fab.setImageResource(R.drawable.ic_mic)
                }
                state.error?.let {
                    textView.text = it
                }

                if (!state.isSpeaking && state.spokenText.isNotEmpty()) {
                    analyzeEmotion(state.spokenText)
                }
            }
        }
    }

    private fun analyzeEmotion(text: String) {
        scope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    HuggingFaceClient.api.analyzeEmotion(HfRequest(text))
                }
                Log.d("HF_RESPONSE", response.toString())
                val emotionList = response.firstOrNull() ?: emptyList()

                /*// for the top emotion
                val topEmotion = emotionList?.maxByOrNull { it.score }
                emotionsListTextView.text = if (topEmotion != null){
                    "Top emotion: ${topEmotion.label} (${String.format("%.2f", topEmotion.score ?: 0f)})"
                }
                else{
                    "Emotion detection failed"
                }*/

                val topFive = emotionList?.sortedByDescending { it.score }?.take(5)
                val emotionsText = topFive?.joinToString("\n") {
                    "${it.label}: ${String.format("%.2f", it.score * 100)}%"
                } ?: ""
                emotionsListTextView.text = if (emotionsText.isNotEmpty()) {
                    "Top 5 Emotions:\n$emotionsText"
                } else {
                    ""
                }

                // Mood Detection
                val detectedMood = MoodClassifier.classifyMood(emotionList)
                emotionsListTextView.text = "${emotionsListTextView.text}\n\nDetected Mood: ${detectedMood.mood} (${String.format("%.2f", detectedMood.confidence * 100)}%)"

            } catch (e: Exception) {
                textView.text = "Error: ${e.message}"
                Log.e("HF_ERROR", e.toString(), e)
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
