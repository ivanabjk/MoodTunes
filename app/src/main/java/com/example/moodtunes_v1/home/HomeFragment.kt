package com.example.moodtunes_v1.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.moodtunes_v1.R
import com.example.moodtunes_v1.databinding.FragmentHomeBinding
import com.example.moodtunes_v1.playlist.PlaylistFragment
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: HomeViewModel
    private var canRecord = false

    private val editTextHint = "Type or speak how you feel..."

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val factory = HomeViewModelFactory(
            requireActivity().application,
            requireContext(),
            this,
            arguments
        )
        viewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentHomeBinding.bind(view)

        viewLifecycleOwner.lifecycleScope.launch {
            setupListeners()  // Ensures UI setup happens asynchronously
            observeViewModel()

            binding.btnSeePlaylists.setOnClickListener {
                launchPlaylist(viewModel.detectedMood.value.orEmpty())
            }

            requestPermissions()
        }


    }

    private fun setupListeners() {
        binding.fab.setOnClickListener {
            if (viewModel.isSpeaking.value == true) {
                viewModel.stopListening()
            } else if (canRecord) {
                viewModel.startListening()
            }
        }

        binding.btnClearText.setOnClickListener {
            viewModel.clearMood()
            binding.etMoodInput.setText("") // Forcefully clear EditText
            binding.etMoodInput.hint = editTextHint // Reset hint

            // Hide the mood animation
            binding.moodAnimation.cancelAnimation()
            binding.moodAnimation.visibility = View.GONE

        }

        binding.etMoodInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                viewModel.saveUserText(binding.etMoodInput.text.toString()) // Store input before navigating away
            }
        }

        binding.btnAnalyzeMood.setOnClickListener {
            val moodText = binding.etMoodInput.text.toString().trim()
            if (moodText.isNotEmpty()) {
                viewModel.analyzeEmotion(moodText)
                viewModel.saveUserText(moodText)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun observeViewModel() {
        viewModel.emotionsLiveData.observe(viewLifecycleOwner) { emotions ->
            binding.btnSeePlaylists.isEnabled = emotions.isNotEmpty()
        }

        viewModel.isSpeaking.observe(viewLifecycleOwner) { isSpeaking ->
            if (isSpeaking) {
                binding.etMoodInput.setText("Speaking...")
                binding.fab.setImageResource(R.drawable.ic_stop)
            } else {
                binding.fab.setImageResource(R.drawable.ic_mic)
                val userText = viewModel.userText.value ?: ""
                binding.etMoodInput.setText(userText)
            }
        }

        viewModel.detectedMood.observe(viewLifecycleOwner) { mood ->
            if (mood.isNotBlank()) {
                displayMoodAnimation(mood)
            }
        }


    }

    private fun requestPermissions() {
        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            canRecord = isGranted
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            canRecord = true
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        val isBackStack = requireActivity()
            .supportFragmentManager
            .backStackEntryCount > 0

        if (!isBackStack) {
            // User navigated away via bottom nav → reset mood + input
            viewModel.clearMood() // clears detected mood and user text
        }

    }

    private fun getMoodAnimationFile(mood: String): String? = when (mood.lowercase()) {
        "happy" -> "happy_emoji.json"
        "sad" -> "sad_emoji.json"
        "angry" -> "angry_emoji.json"
        "calm" -> "calm_emoji.json"
        else -> null
    }

    private fun displayMoodAnimation(mood: String) {
        val file = getMoodAnimationFile(mood)
        binding.moodAnimation.apply {
            if (file != null) {
                setAnimation(file)
                visibility = View.VISIBLE
                playAnimation()
            } else {
                cancelAnimation()
                visibility = View.GONE
            }
        }
    }

    private fun launchPlaylist(mood: String) {
        if (mood.isBlank()) return

        val fragment = PlaylistFragment.newInstance(
            userInput = viewModel.userText.value.orEmpty(),
            mood = mood
        )

        requireActivity()
            .supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onResume() {
        super.onResume()

        val isBackStack = requireActivity()
            .supportFragmentManager
            .backStackEntryCount > 0

        if (isBackStack) {
            val userText = viewModel.userText.value.orEmpty()
            binding.etMoodInput.setText(userText)

            val mood = viewModel.detectedMood.value.orEmpty()
            if (mood.isNotBlank()) {
                displayMoodAnimation(mood)
                binding.btnSeePlaylists.isEnabled = true
            }
        }
    }

}