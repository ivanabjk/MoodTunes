package com.example.moodtunes_v1.user_auth

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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var moodGenreAdapter: MoodGenreAdapter
    private lateinit var authService: AuthService

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

                // Convert to Base64 and save to Firestore
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
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentProfileBinding.bind(view)

        authService = AuthService(requireContext()) // Initialize when context is available
        val userId = authService.getEmailFromFireStoreAuth()

        // Observe email updates dynamically
        viewModel.userEmail.observe(viewLifecycleOwner) { email ->
            binding.emailText.text = email
        }

        binding.cameraIcon.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }
//        val savedUri = SharedPref(requireContext()).getProfileImageUri()
//        savedUri?.let {
//            Glide.with(requireContext())
//                .load(Uri.parse(it))
//                .placeholder(R.drawable.default_profile_pic)
//                .circleCrop()
//                .into(binding.profileImage)
//        }

        //Recycler view
        if (userId != null) {
            initializeUserPreferences(userId)

            // For Profile Image
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
                { mood, updatedGenres -> updateUserPreferences(userId, mood, updatedGenres) },
                { mood, genre -> removeGenreFromFireStore(userId, mood, genre) }
            )

            binding.rvMoodGenres.layoutManager = LinearLayoutManager(requireContext())
            binding.rvMoodGenres.adapter = moodGenreAdapter

            viewModel.fetchUserPreferences(userId)
//            viewModel.moodGenres.observe(viewLifecycleOwner) { genres ->
//                moodGenreAdapter.updateData(genres)
//            }
            val moodOrder = listOf("Happy", "Sad", "Angry", "Calm")
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

    private fun initializeUserPreferences(userId: String) {
        val userDocRef = db.collection("user_preferences").document(userId)

        userDocRef.get().addOnSuccessListener { document ->

            if (!document.exists()) {
                viewLifecycleOwner.lifecycleScope.launch {
                    val moods = listOf("Happy", "Sad", "Angry", "Calm")
                    val moodGenres = moods.associateWith { mood ->
                        dao.getPlaylistsByMoodForUser(mood, userId).map { it.genre }.distinct()
                    }

                    val initialData = mapOf(
                        "email" to authService.getEmail(),
                        "moodGenres" to moodGenres
                    )

                    userDocRef.set(initialData)
                        .addOnSuccessListener {
                            Log.d(
                                "FireStore",
                                "Initialized default genres from PlaylistLoader"
                            )
                        }
                        .addOnFailureListener {
                            Log.e(
                                "FireStore",
                                "Error initializing genres",
                                it
                            )
                        }
                }
            }
        }
    }


    private fun updateUserPreferences(userId: String, mood: String, updatedGenres: List<String>) {
        FirebaseFirestore.getInstance().collection("user_preferences").document(userId)
            .update(mapOf("moodGenres.$mood" to updatedGenres))
            .addOnSuccessListener {
                Log.d("FireStore", "Updated $mood: $updatedGenres")
                viewModel.fetchUserPreferences(userId) // Refresh UI after update
            }
            .addOnFailureListener { Log.e("FireStore", "Error saving preferences", it) }
    }

    private fun removeGenreFromFireStore(userId: String, mood: String, genre: String) {
        FirebaseFirestore.getInstance().collection("user_preferences").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val moodGenres =
                    document.get("moodGenres") as? Map<String, List<String>> ?: emptyMap()
                val updatedGenres = moodGenres[mood]?.filter { it != genre } ?: listOf()

                FirebaseFirestore.getInstance().collection("user_preferences").document(userId)
                    .update(mapOf("moodGenres.$mood" to updatedGenres))
                    .addOnSuccessListener {
                        Log.d("FireStore", "Removed $genre from $mood")
                        viewModel.fetchUserPreferences(userId) // Refresh UI dynamically
                    }
                    .addOnFailureListener { Log.e("FireStore", "Error removing genre", it) }
            }
    }

//
//    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
//        currentFocus?.let {
//            if (it is EditText) {
//                it.clearFocus()
//                requireActivity().window.decorView.rootView.hideKeyboard()
//            }
//        }
//        return super.requireActivity().dispatchTouchEvent(event)
//    }
//
//    private fun View.hideKeyboard() {
//        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        imm.hideSoftInputFromWindow(windowToken, 0)
//    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Prevent memory leaks
    }


}