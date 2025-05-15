package com.example.moodtunes_v1

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.widget.TextView
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var voiceToTextParser: VoiceToTextParser
    private lateinit var textView: TextView
    private lateinit var fab: FloatingActionButton
    private var canRecord = false
    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView)
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
            }
        }
    }
}
