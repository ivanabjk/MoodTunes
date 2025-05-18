package com.example.moodtunes_v1

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var voiceToTextParser: VoiceToTextParser
    private lateinit var textView: TextView
    private lateinit var emotionsListTextView: TextView
    private lateinit var btnSeePlaylists: Button
    private lateinit var fab: FloatingActionButton
    private var canRecord = false
    private val scope = MainScope()

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        preloadPlaylists()

        textView = findViewById(R.id.textView)
        emotionsListTextView = findViewById(R.id.tvEmotionList)
        fab = findViewById(R.id.fab)
        btnSeePlaylists = findViewById(R.id.btnSeePlaylists)

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

    @SuppressLint("SetTextI18n", "DefaultLocale")
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

                val topFive = emotionList.sortedByDescending { it.score }.take(5)
                val emotionsText = topFive.joinToString("\n") {
                    "${it.label}: ${String.format("%.2f", it.score * 100)}%"
                }
                emotionsListTextView.text = if (emotionsText.isNotEmpty()) {
                    "Top 5 Emotions:\n$emotionsText"
                } else {
                    ""
                }

                // Mood Detection
                val detectedMood = MoodClassifier.classifyMood(emotionList)
                emotionsListTextView.text =
                    "${emotionsListTextView.text}\n\nDetected Mood: ${detectedMood.mood} (${
                        String.format(
                            "%.2f",
                            detectedMood.confidence * 100
                        )
                    }%)"

                // Enable the button after mood detection
                btnSeePlaylists.isEnabled = true
                btnSeePlaylists.setOnClickListener {
                    val intent = Intent(this@MainActivity, PlaylistActivity::class.java)
                    intent.putExtra("MOOD", detectedMood.mood)
                    startActivity(intent)
                }


            } catch (e: Exception) {
                textView.text = "Error: ${e.message}"
                Log.e("HF_ERROR", e.toString(), e)
            }

        }
    }

    private fun preloadPlaylists() {
        val db = MoodTunesDatabase.getDatabase(this)
        val playlistDao = db.playlistDao()

        scope.launch {
            // Check if playlists already exist to avoid duplicates
            var existingPlaylists = playlistDao.getPlaylistsByMood("Happy")
            if (existingPlaylists.isEmpty()) {
                playlistDao.insertPlaylist(
                    Playlist(
                        mood = "Happy",
                        genre = "90s Pop Hits",
                        url = "https://www.youtube.com/playlist?list=PL4C44E2875308A280"
                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        mood = "Happy",
                        genre = "Upbeat Songs",
                        url = "https://www.youtube.com/playlist?list=PLx2Jv96o522ORh69HaDKClrz2Midqj8AE"
                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        mood = "Happy",
                        genre = "Rock Classics",
                        url = "https://www.youtube.com/playlist?list=PLlLxrl-tbz-z3RqzMCOc_uQqdsbsAMGAq"
                    )
                )
            }
            existingPlaylists = playlistDao.getPlaylistsByMood("Sad")
            if (existingPlaylists.isEmpty()) {

                playlistDao.insertPlaylist(
                    Playlist(
                        mood = "Sad",
                        genre = "Melancholy Tunes",
                        url = "https://www.youtube.com/playlist?list=PLRJriok9d2H8eNwmsdh2MSYsBXFlzZU8H"
                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        mood = "Sad",
                        genre = "Soft Acoustic",
                        url = "https://www.youtube.com/playlist?list=PLJH9QWrouDvTIqgnrB1XtwOkw4ZgQjaZq"
                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        mood = "Sad",
                        genre = "Emo Rock",
                        url = "https://www.youtube.com/playlist?list=PLWwVW5BHHeYVM4rAKmNOjKAXn21R6XAKV"
                    )
                )
            }
            existingPlaylists = playlistDao.getPlaylistsByMood("Angry")
            if (existingPlaylists.isEmpty()) {
                playlistDao.insertPlaylist(
                    Playlist(
                        mood = "Angry",
                        genre = "Heavy Metal",
                        url = "https://www.youtube.com/playlist?list=PLqrHHabBzX0nY0NU5xFJ6NDYR1R-jopi0"
                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        mood = "Angry",
                        genre = "Techno Rage",
                        url = "https://www.youtube.com/playlist?list=PLfF4wIXCvi2NJOY808YLGmcd3AABPs1__"
                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        mood = "Angry",
                        genre = "Hardcore Punk",
                        url = "https://www.youtube.com/playlist?list=PLYFbDTv39GlnLFu6rJeNRCAlP2dTuiP3C"
                    )
                )
            }
            existingPlaylists = playlistDao.getPlaylistsByMood("Calm")
            if (existingPlaylists.isEmpty()) {
                playlistDao.insertPlaylist(
                    Playlist(
                        mood = "Calm",
                        genre = "Ambient Sounds",
                        url = "https://www.youtube.com/playlist?list=PLQ_PIlf6OzqIq5aQe0uTHBmli1Nc1HTpB"
                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        mood = "Calm",
                        genre = "Lo-Fi Beats",
                        url = "https://www.youtube.com/playlist?list=PLOzDu-MXXLlj7croDcwz33c-a5rpNEBNe"
                    )
                )
                playlistDao.insertPlaylist(
                    Playlist(
                        mood = "Calm",
                        genre = "Classical Relaxation",
                        url = "https://www.youtube.com/playlist?list=PLW68_wbsDJYmg51TTOrpPWG0NfZL8SU6I"
                    )
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
