package com.example.moodtunes_v1.playlist

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moodtunes_v1.R
import com.example.moodtunes_v1.databinding.FragmentHomeBinding
import com.example.moodtunes_v1.databinding.FragmentProfileBinding
import com.example.moodtunes_v1.user_auth.AuthService
import com.example.moodtunes_v1.user_auth.ProfileViewModel
import com.example.moodtunes_v1.user_auth.ProfileViewModelFactory
import com.example.moodtunes_v1.user_preference.MoodGenreAdapter
import com.example.moodtunes_v1.user_preference.MoodGenres
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var moodGenreAdapter: MoodGenreAdapter
    private lateinit var authService: AuthService

    private val viewModel: ProfileViewModel by viewModels {
        ProfileViewModelFactory(authService, FirebaseFirestore.getInstance())
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri = result.data?.data
            // Handle selected image URI
        }
    }

    private val db = FirebaseFirestore.getInstance()
    private val moodGenreList = listOf(
        MoodGenres("Happy", listOf("Pop", "Rock")),
        MoodGenres("Sad", listOf("Indie", "Acoustic")),
        MoodGenres("Calm", listOf("Lo-Fi", "Jazz")),
        MoodGenres("Angry", listOf("Metal", "Techno"))
    )

    private val defaultMoodGenres = moodGenreList.associate { it.mood to it.genres }

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
        val userId = authService.getUserId()

        // Observe email updates dynamically
        viewModel.userEmail.observe(viewLifecycleOwner) { email ->
            binding.emailText.text = email
        }

        binding.cameraIcon.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }

        // Testing FireStore connection
        db.collection("test").document("sample")
            .set(mapOf("testField" to "Hello Firebase!"))
            .addOnSuccessListener { Log.d("FireStore", "Success!") }
            .addOnFailureListener { Log.e("FireStore", "Error", it) }


        //Recycler view
        if (userId != null) {
            initializeUserPreferences(userId)

            moodGenreAdapter = MoodGenreAdapter(
                emptyList(),
                { mood, updatedGenres -> updateUserPreferences(userId, mood, updatedGenres) },
                { mood, genre -> removeGenreFromFireStore(userId, mood, genre) }
            )

            binding.rvMoodGenres.layoutManager = LinearLayoutManager(requireContext())
            binding.rvMoodGenres.adapter = moodGenreAdapter

            viewModel.fetchUserPreferences(userId)
            viewModel.moodGenres.observe(viewLifecycleOwner) { genres ->
                moodGenreAdapter.updateData(genres)
            }
        } else {
            Log.e("ProfileFragment", "User ID is null, cannot fetch data.")
        }


        binding.btnLogout.setOnClickListener {
            authService.logout()
            requireActivity().finish()
        }
    }

    private fun initializeUserPreferences(userId: String) {
        val userDocRef = db.collection("user_preferences").document(userId)

        userDocRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                val initialData = mapOf("email" to authService.getEmail(), "moodGenres" to defaultMoodGenres)
                userDocRef.set(initialData)
                    .addOnSuccessListener { Log.d("FireStore", "Initialized default genres!") }
                    .addOnFailureListener { Log.e("FireStore", "Error initializing preferences", it) }
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
                val moodGenres = document.get("moodGenres") as? Map<String, List<String>> ?: emptyMap()
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