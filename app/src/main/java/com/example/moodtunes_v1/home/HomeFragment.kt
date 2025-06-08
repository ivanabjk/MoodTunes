package com.example.moodtunes_v1.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.moodtunes_v1.R
import com.example.moodtunes_v1.databinding.FragmentHomeBinding
import com.example.moodtunes_v1.playlist.PlaylistActivity
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
            SavedStateHandle()
        )
        viewModel = ViewModelProvider(this, factory).get(HomeViewModel::class.java)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentHomeBinding.bind(view)

        viewLifecycleOwner.lifecycleScope.launch {
            setupListeners()  // Ensures UI setup happens asynchronously
            observeViewModel()
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

    private fun observeViewModel() {
        viewModel.emotionsLiveData.observe(viewLifecycleOwner) { emotions ->
            binding.tvEmotionList.text = emotions
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
            if (mood.isNotEmpty()) {
                showPlaylist(mood)
                binding.tvEmotionList.append("\n\nDetected Mood: $mood")

            }
        }

        viewModel.userText.observe(viewLifecycleOwner) { text ->
            if (binding.etMoodInput.text.toString() != text) { // Prevents unnecessary re-setting
                binding.etMoodInput.setText(text)
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

    private fun showPlaylist(detectedMood: String) {
        binding.btnSeePlaylists.setOnClickListener {
            val intent = Intent(requireContext(), PlaylistActivity::class.java)
            intent.putExtra("MOOD", detectedMood)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}