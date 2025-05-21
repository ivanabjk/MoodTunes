package com.example.moodtunes_v1

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.moodtunes_v1.mood_detection.HfRequest
import com.example.moodtunes_v1.mood_detection.HuggingFaceClient
import com.example.moodtunes_v1.mood_detection.MoodClassifier
import com.example.moodtunes_v1.mood_detection.MoodResult
import com.example.moodtunes_v1.playlist.MoodTunesDatabase
import com.example.moodtunes_v1.playlist.PlaylistActivity
import com.example.moodtunes_v1.playlist.PlaylistLoader
import com.example.moodtunes_v1.user_auth.AuthService
import com.example.moodtunes_v1.user_auth.LoginActivity
import com.example.moodtunes_v1.user_auth.ProfileActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var voiceToTextParser: VoiceToTextParser
    private lateinit var title: TextView
    private lateinit var moodInput: EditText
    private lateinit var emotionsListTextView: TextView
    private lateinit var btnSeePlaylists: Button
    private lateinit var fab: FloatingActionButton
    private lateinit var btnClearText: Button
    private lateinit var btnAnalyzeMood: Button
    private lateinit var btnLogin: Button
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var detectedMood: MoodResult
    private var canRecord = false
    private val scope = MainScope()

    private lateinit var authService: AuthService
    //private lateinit var sharedPref: SharedPref

    @SuppressLint("SetTextI18n", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Firebase authentication
        authService = AuthService(this)
        //sharedPref = SharedPref(this)

        // Room DB and Playlist Loader
        val db = MoodTunesDatabase.getDatabase(this)
        val playlistDao = db.playlistDao()

        PlaylistLoader.preloadPlaylists(scope, playlistDao)

        title = findViewById(R.id.tvTitle)
        moodInput = findViewById(R.id.etMoodInput)
        emotionsListTextView = findViewById(R.id.tvEmotionList)
        fab = findViewById(R.id.fab)
        btnSeePlaylists = findViewById(R.id.btnSeePlaylists)
        btnClearText = findViewById(R.id.btnClearText)
        btnAnalyzeMood = findViewById(R.id.btnAnalyzeMood)
        btnLogin = findViewById(R.id.btnLogin)
        detectedMood = MoodResult("Calm", 10.0F)

        voiceToTextParser = VoiceToTextParser(application)

        // Shared Preferences
        sharedPrefs = getSharedPreferences("MoodTunesPrefs", MODE_PRIVATE)

        // Retrieve saved values
        val savedMood = sharedPrefs.getString("savedMood", "") ?: ""
        val savedDetectedMood = sharedPrefs.getString("lastDetectedMood", "") ?: ""
        val savedEmotionResults = sharedPrefs.getString("emotionResults", "") ?: ""

        // Display stored values if app wasn't fully restarted
        if (savedInstanceState != null) {
            moodInput.setText(savedMood)
            emotionsListTextView.text = savedEmotionResults
            btnSeePlaylists.isEnabled =
                savedDetectedMood.isNotEmpty() // Enable button if mood was detected
        }

        //Mic recording

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            canRecord = isGranted
            if (!isGranted) {
                moodInput.hint = "Permission denied."
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
            clearMoodDetection()
            if (voiceToTextParser.state.value.isSpeaking) {
                voiceToTextParser.stopListening()
            } else if (canRecord) {
                voiceToTextParser.startListening()
            }
        }

        // Clear text button

        btnClearText.setOnClickListener {
            moodInput.setText("") // Clears input field
            clearMoodDetection()
            // Remove stored values
            sharedPrefs.edit()
                .remove("savedMood")
                .remove("lastDetectedMood")
                .remove("emotionResults")
                .apply()

        }

        //Analyze mood button

        btnAnalyzeMood.setOnClickListener {
            val moodText = moodInput.text.toString().trim()
            if (moodText.isNotEmpty()) {
                analyzeEmotion(moodText)
                btnSeePlaylists.isEnabled = true
            }
        }

        // Login button
        updateLoginButton()


        // VoiceToTextParser

        scope.launch {
            voiceToTextParser.state.collect { state ->
                when {
                    state.isSpeaking -> {
                        moodInput.setText("Speaking...")
                        fab.setImageResource(R.drawable.ic_stop)
                    }

                    state.spokenText.isNotEmpty() -> {
                        moodInput.setText(state.spokenText) // Set spoken text directly
                        moodInput.setSelection(state.spokenText.length) // Cursor at end
                        //analyzeEmotion(state.spokenText)
                    }

                    else -> {
                        moodInput.hint = getString(R.string.moodInputHint) // Reset hint
                        fab.setImageResource(R.drawable.ic_mic)
                    }
                }

                // Handle errors
                state.error?.let {
                    moodInput.setText(it)
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
                detectedMood = MoodClassifier.classifyMood(emotionList)
                emotionsListTextView.text =
                    "${emotionsListTextView.text}\n\nDetected Mood: ${detectedMood.mood} (${
                        String.format(
                            "%.2f",
                            detectedMood.confidence * 100
                        )
                    }%)"

                // Store detected mood & emotion list
                sharedPrefs.edit()
                    .putString("lastDetectedMood", detectedMood.mood)
                    .putString("emotionResults", emotionsListTextView.text as String)
                    .apply()


                // Enable the button after mood detection
                btnSeePlaylists.isEnabled = true
                showPlaylist(detectedMood.mood)


            } catch (e: Exception) {
                moodInput.hint = "Error: ${e.message}"
                Log.e("HF_ERROR", e.toString(), e)
            }

        }
    }

    private fun clearMoodDetection() {
        emotionsListTextView.text = getString(R.string.emotion_detection)
        btnSeePlaylists.isEnabled = false
    }

    private fun showPlaylist(detectedMood: String) {
        btnSeePlaylists.setOnClickListener {
            val intent = Intent(this@MainActivity, PlaylistActivity::class.java)
            intent.putExtra("MOOD", detectedMood)
            startActivity(intent)
        }
    }

    private fun updateLoginButton() {
        if (authService.getEmail() != null) {
            // User is logged in, go to Profile
            btnLogin.text = "Profile"
            btnLogin.setOnClickListener {
                startActivity(Intent(this, ProfileActivity::class.java))
            }
        } else {
            // Not logged in, go to Login
            btnLogin.text = "Login"
            btnLogin.setOnClickListener {
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("tempMood", moodInput.text.toString())
        outState.putString("emotionResults", emotionsListTextView.text.toString())
        outState.putBoolean("playlistButtonEnabled", btnSeePlaylists.isEnabled)
        outState.putString("lastDetectedMood", detectedMood.mood)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        moodInput.setText(
            savedInstanceState.getString(
                "tempMood",
                ""
            )
        ) // Restore mood input from rotation
        emotionsListTextView.text =
            savedInstanceState.getString("emotionResults", "") // Restore analysis results
        btnSeePlaylists.isEnabled =
            savedInstanceState.getBoolean("playlistButtonEnabled", false) // Restore button state

        val restoredMood = savedInstanceState.getString("lastDetectedMood", "") ?: ""
        if (restoredMood.isNotEmpty()) {
            detectedMood =
                MoodResult(restoredMood, 10.0F) // Prevent crashes due to uninitialized MoodResult
            showPlaylist(restoredMood) // Ensure playlist button works after rotation
        }
    }


}
