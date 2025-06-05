package com.example.moodtunes_v1.user_auth

import MoodGenreAdapter
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moodtunes_v1.R
import com.example.moodtunes_v1.databinding.ActivityProfileBinding
import com.example.moodtunes_v1.playlist.MoodTunesDatabase
import com.example.moodtunes_v1.user_preference.MoodGenres
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var authService: AuthService

    private val PICK_IMAGE_REQUEST = 1
    private var imageUri: Uri? = null
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
        }
    }

    // Recycler view for genres

    private lateinit var moodRecyclerView: RecyclerView
    private lateinit var moodGenreAdapter: MoodGenreAdapter
    private val moodGenreList = listOf(
        MoodGenres("Happy", listOf("Pop", "Rock")),
        MoodGenres("Sad", listOf("Indie", "Acoustic")),
        MoodGenres("Calm", listOf("Lo-Fi", "Jazz")),
        MoodGenres("Angry", listOf("Metal", "Techno"))
    )

    private val defaultMoodGenres = moodGenreList.associate { it.mood to it.genres }

    // Firestore DB
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Auth Service
        authService = AuthService(this)
        val userId = authService.getUserId()

        // Set email dynamically (replace with actual data retrieval logic)
        val userEmail = authService.getEmail()
        binding.emailText.text = userEmail

        // Image picker
        binding.cameraIcon.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
//            startActivityForResult(intent, PICK_IMAGE_REQUEST)
            pickImageLauncher.launch(intent)
        }

        //Logout
        binding.btnLogout.setOnClickListener {
            authService.logout()
            finish()
        }

        // Testing db connection
        db.collection("test").document("sample")
            .set(mapOf("testField" to "Hello Firebase!"))
            .addOnSuccessListener { Log.d("Firestore", "Success!") }
            .addOnFailureListener { Log.e("Firestore", "Error", it) }

        // Firebase connection with db
        if (userId != null) {
            initializeUserPreferences(userId)

            binding.rvMoodGenres.layoutManager = LinearLayoutManager(this)
            moodGenreAdapter = MoodGenreAdapter(
                emptyList(),
                { mood, updatedGenres -> updateUserPreferences(userId, mood, updatedGenres) }, // Editing a genre
                { mood, genre -> removeGenreFromFirestore(userId, mood, genre) } // Removing a genre
            )

            binding.rvMoodGenres.adapter = moodGenreAdapter

            getUserPreferences(userId) { moodGenres ->
                moodGenreAdapter.updateData(moodGenres) // Dynamically update adapter data
            }
        }

    }

    fun initializeUserPreferences(userId: String) {
        val userDocRef = db.collection("user_preferences").document(userId)

        userDocRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
//                val initialData = mapOf("email" to userId, "moodGenres" to defaultMoodGenres)
                val initialData = mapOf("email" to authService.getEmail(), "moodGenres" to defaultMoodGenres)
                userDocRef.set(initialData)
                    .addOnSuccessListener { Log.d("Firestore", "Initialized default genres!") }
                    .addOnFailureListener { Log.e("Firestore", "Error initializing preferences", it) }
            }
        }

    }

    fun getUserPreferences(userId: String, callback: (List<MoodGenres>) -> Unit) {
        db.collection("user_preferences").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val moodGenresMap = document.get("moodGenres") as? Map<*, *> ?: defaultMoodGenres
                val convertedMoodGenres = moodGenresMap.mapNotNull { (key, value) ->
                    if (key is String && value is List<*>) {
                        MoodGenres(mood = key, genres = value.filterIsInstance<String>())
                    } else {
                        null
                    }
                }

                callback(convertedMoodGenres)
            }
            .addOnFailureListener { Log.e("Firestore", "Error fetching data", it) }
    }

    fun updateUserPreferences(userId: String, mood: String, updatedGenres: List<String>) {
        val userDocRef = db.collection("user_preferences").document(userId)

        userDocRef.update(mapOf("moodGenres.$mood" to updatedGenres))
            .addOnSuccessListener { Log.d("Firestore", "Preferences updated for $mood: $updatedGenres") }
            .addOnFailureListener { Log.e("Firestore", "Error saving preferences", it) }

        getUserPreferences(userId) { newData ->
            moodGenreAdapter.updateData(newData) // **RecyclerView updates automatically**
        }

    }
    fun removeGenreFromFirestore(userId: String, mood: String, genre: String) {
        val userDocRef = db.collection("user_preferences").document(userId)

        userDocRef.get().addOnSuccessListener { document ->
            val moodGenres = document.get("moodGenres") as? Map<String, List<String>> ?: emptyMap()
            val updatedGenres = moodGenres[mood]?.filter { it != genre } ?: listOf()

            userDocRef.update(mapOf("moodGenres.$mood" to updatedGenres))
                .addOnSuccessListener {
                    Log.d("Firestore", "Removed $genre from $mood")

                    // **Refresh RecyclerView after deletion**
                    getUserPreferences(userId) { moodGenreAdapter.updateData(it) }
                }
                .addOnFailureListener { Log.e("Firestore", "Error removing genre", it) }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // If user taps outside EditText, reset focus
        currentFocus?.let {
            if (it is EditText) {
                it.clearFocus()
                window.decorView.rootView.hideKeyboard() // Hide keyboard
            }
        }
        return super.dispatchTouchEvent(event)
    }

    // Helper Function to Hide Keyboard
    fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

}

