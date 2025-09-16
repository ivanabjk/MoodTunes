package com.example.moodtunes_v1.user_auth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.moodtunes_v1.MainActivity
import com.example.moodtunes_v1.R
import com.example.moodtunes_v1.databinding.FragmentProfileBinding
import com.example.moodtunes_v1.playlist.MoodTunesDatabase
import com.example.moodtunes_v1.user_preference.MoodGenreAdapter
import com.example.moodtunes_v1.user_preference.MoodGenres
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var moodGenreAdapter: MoodGenreAdapter
    private lateinit var authService: AuthService
    private var debounceJob: Job? = null
    private val pendingDeletions = mutableMapOf<String, MutableSet<String>>() // mood → genres
    private var deletionSyncJob: Job? = null

    private val dao by lazy { MoodTunesDatabase.getDatabase(requireContext()).playlistDao() }

    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(authService, FirebaseFirestore.getInstance())
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            imageUri?.let {
                SharedPref(requireContext()).setProfileImageUri(it.toString())

                Glide.with(requireContext())
                    .load(it)
                    .placeholder(R.drawable.default_profile_pic)
                    .circleCrop()
                    .into(binding.profileImage)

                val bitmap =
                    MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, imageUri)
                val outputStream = java.io.ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 60, outputStream)
                val base64String = android.util.Base64.encodeToString(
                    outputStream.toByteArray(),
                    android.util.Base64.DEFAULT
                )

                val userId = authService.getEmailFromFireStoreAuth()
                val userDocRef = FirebaseFirestore.getInstance()
                    .collection("user_preferences")
                    .document(userId ?: return@let)

                userDocRef.get().addOnSuccessListener { document ->
                    if (document.exists()) {
                        userDocRef.update("profileImageBase64", base64String)
                            .addOnSuccessListener {
                                Log.d("ProfileUpload", "Base64 image saved to Firestore")
                            }
                            .addOnFailureListener {
                                Log.e("ProfileUpload", "Failed to save Base64 image", it)
                            }
                    } else {
                        Log.w("ProfileUpload", "User preferences not initialized yet—skipping image save")
                    }
                }
            }
        }
    }

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProfileBinding.bind(view)

        authService = AuthService(requireContext())
        val userId = authService.getEmailFromFireStoreAuth()

        viewModel.userEmail.observe(viewLifecycleOwner) { email ->
            binding.emailText.text = email
        }

        binding.cameraIcon.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }

        if (userId != null) {
            lifecycleScope.launch {
                withContext(Dispatchers.Main) {
                    binding.loadingSpinner.visibility = View.VISIBLE
                }
                val initialized = initializeUserPreferences(userId).await()
                withContext(Dispatchers.Main) {
                    binding.loadingSpinner.visibility = View.GONE
                    if (!initialized) {
                        Toast.makeText(requireContext(), "Failed to initialize preferences", Toast.LENGTH_SHORT).show()
                    }
                }

            }


            val userDocRef =
                FirebaseFirestore.getInstance().collection("user_preferences").document(userId)
            userDocRef.get().addOnSuccessListener { document ->
                val base64String = document.getString("profileImageBase64")
                if (!base64String.isNullOrEmpty()) {
                    val imageBytes =
                        android.util.Base64.decode(base64String, android.util.Base64.DEFAULT)
                    Glide.with(requireContext())
                        .load(imageBytes)
                        .placeholder(R.drawable.default_profile_pic)
                        .circleCrop()
                        .into(binding.profileImage)
                }
            }

            moodGenreAdapter = MoodGenreAdapter(
                emptyList(),
                { mood, updatedGenres ->
                    val currentList = viewModel.moodGenres.value ?: return@MoodGenreAdapter
                    val updatedList = currentList.map {
                        if (it.mood == mood) MoodGenres(mood, updatedGenres) else it
                    }
                    viewModel.setMoodGenres(updatedList)
                    debounceJob?.cancel()
                    debounceJob = lifecycleScope.launch {
                        delay(300)
                        updateUserPreferences(userId, mood, updatedGenres)
                    }
                },
                { mood, genre ->
                    val currentList = viewModel.moodGenres.value ?: return@MoodGenreAdapter
                    val updatedList = currentList.map {
                        if (it.mood == mood) MoodGenres(mood, it.genres.filter { g -> g != genre }) else it
                    }
                    viewModel.setMoodGenres(updatedList)

                    val genresToDelete = pendingDeletions.getOrPut(mood) { mutableSetOf() }
                    genresToDelete.add(genre)

                    deletionSyncJob?.cancel()
                    deletionSyncJob = lifecycleScope.launch {
                        delay(500)
                        syncAllPendingDeletionsToFirestore(userId)
                    }
                }

            )


            binding.rvMoodGenres.layoutManager = LinearLayoutManager(requireContext())
            binding.rvMoodGenres.adapter = moodGenreAdapter

            viewModel.fetchUserPreferences(userId)

            val moodOrder = listOf("Happy", "Sad", "Angry")
            viewModel.moodGenres.observe(viewLifecycleOwner) { genres ->
                val sorted = moodOrder.mapNotNull { mood ->
                    genres.find { it.mood == mood }
                }
                moodGenreAdapter.updateData(sorted)
            }
        } else {
            Log.e("ProfileFragment", "User ID is null, cannot fetch data.")
        }


        binding.btnLogout.setOnClickListener {
            authService.logout {
                Handler(Looper.getMainLooper()).postDelayed({
                    requireActivity().run {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                }, 300)

            }
        }
    }

    private fun initializeUserPreferences(userId: String): Deferred<Boolean> {
        return lifecycleScope.async(Dispatchers.IO) {
            val userDocRef = db.collection("user_preferences").document(userId)
            val snapshot = userDocRef.get().await()

            if (!snapshot.exists()) {
                val moods = listOf("Happy", "Sad", "Angry")
                val moodGenres = moods.associateWith { mood ->
                    dao.getPlaylistsByMoodForUser(mood, userId).map { it.genre }.distinct()
                }

                val initialData = mapOf(
                    "email" to authService.getEmail(),
                    "moodGenres" to moodGenres
                )

                userDocRef.set(initialData).await()
                Log.d("FireStore", "Initialized default genres")
                return@async true
            }

            return@async true
        }
    }

    private fun updateUserPreferences(userId: String, mood: String, updatedGenres: List<String>) {
        lifecycleScope.launch(Dispatchers.IO) {
            try{
                withContext(Dispatchers.Main) {
                    binding.loadingSpinner.visibility = View.VISIBLE
                }

                FirebaseFirestore.getInstance()
                    .collection("user_preferences")
                    .document(userId)
                    .update(mapOf("moodGenres.$mood" to updatedGenres))
                    .await()

                withContext(Dispatchers.Main) {
                    binding.loadingSpinner.visibility = View.GONE
                }

            }catch (e: Exception) {
                Log.e("FireStore", "Error updating preferences", e)
                withContext(Dispatchers.Main) {
                    binding.loadingSpinner.visibility = View.GONE
                    Toast.makeText(requireContext(), "Failed to update genres", Toast.LENGTH_SHORT).show()
                }
            }

        }
    }


    private suspend fun syncAllPendingDeletionsToFirestore(userId: String) {
        val docRef = FirebaseFirestore.getInstance()
            .collection("user_preferences")
            .document(userId)

        try {
            val snapshot = docRef.get().await()
            val data = snapshot.data ?: emptyMap()
            val moodGenres = data["moodGenres"] as? Map<*, *> ?: emptyMap<Any, Any>()

            val updates = mutableMapOf<String, Any>()

            for ((mood, genresToRemove) in pendingDeletions) {
                val currentGenres = (moodGenres[mood] as? List<*>)?.mapNotNull { it as? String } ?: listOf()
                val finalGenres = currentGenres.filter { it !in genresToRemove }
                updates["moodGenres.$mood"] = finalGenres
            }

            docRef.update(updates).await()
            Log.d("FireStore", "Synced deletions: $updates")

            pendingDeletions.clear()
        } catch (e: Exception) {
            Log.e("FireStore", "Failed to sync deletions", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Failed to sync deletions", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}