package com.example.moodtunes_v1.playlist

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moodtunes_v1.databinding.FragmentPlaylistBinding
import com.example.moodtunes_v1.favorites.FavoritesRepository
import com.example.moodtunes_v1.history.HistoryEntry
import com.example.moodtunes_v1.history.HistorySessionViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PlaylistFragment : Fragment() {

    private var _binding: FragmentPlaylistBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: PlaylistViewModel
    private lateinit var playlistAdapter: PlaylistAdapter

    private val sessionViewModel: HistorySessionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val db = MoodTunesDatabase.getDatabase(requireContext())
        val playlistDao = db.playlistDao()
        val repository = FavoritesRepository(playlistDao)
        val factory = PlaylistViewModelFactory(repository)

        viewModel = ViewModelProvider(this, factory)[PlaylistViewModel::class.java]

        val userInput = arguments?.getString("USER_INPUT").orEmpty()
        val mood = arguments?.getString("MOOD").orEmpty()

        binding.tvMoodTitle.text = "For your $mood mood!"


        playlistAdapter = PlaylistAdapter(
            emptyList(),
            emptyMap(),
            onItemClick = { playlist ->
                val playlistId = YouTubeFetcher.extractPlaylistId(playlist.url)
                val firstVideoUrl = viewModel.playlistInfo.value[playlistId]?.first ?: return@PlaylistAdapter
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(firstVideoUrl)).apply {
                    setPackage("com.google.android.youtube")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
                }
                startActivity(intent)
            },
            favoriteToggleListener = object : OnFavoriteToggleListener {
                override fun onFavoriteToggled(playlist: Playlist) {
                    viewModel.toggleFavorite(playlist)
                }
            }
        )

        binding.rvPlaylists.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlaylists.adapter = playlistAdapter


        val restored = sessionViewModel.selectedPlaylists.value
        Log.d("PlaylistFragment", "Restored playlists count: ${restored.size}")
        if (restored.isNotEmpty()) {
            viewModel.setPlaylists(restored)
            binding.tvMoodTitle.text = "For your $mood mood!"

//            val ids = restored.map { YouTubeFetcher.extractPlaylistId(it.url) }
            viewModel.fetchPlaylistMetadata(restored)

            sessionViewModel.clear()
        }else{
            fetchPlaylists(mood)

        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(viewModel.playlists, viewModel.playlistInfo) { playlists, metadata ->
                    playlists to metadata
                }.collect { (playlists, metadata) ->
                    playlistAdapter.updateData(playlists, metadata)
                }
            }
        }

    }

    @SuppressLint("SetTextI18n")
    private fun fetchPlaylists(mood: String) {
        val db = MoodTunesDatabase.getDatabase(requireContext())
        val playlistDao = db.playlistDao()

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            val playlists = playlistDao.getPlaylistsByMood(mood)
            if (playlists.isEmpty()) {
                binding.tvMoodTitle.text = "No playlists found for this mood 😢"
                return@launch
            } else{
                val userInput = arguments?.getString("USER_INPUT").orEmpty()
                val detectedMood = mood // already passed in
                val timestamp = System.currentTimeMillis()

                val historyEntry = HistoryEntry(
                    userInput = userInput,
                    detectedMood = detectedMood,
                    timestamp = timestamp,
                    playlists = playlists // your DAO result
                )

                val email = FirebaseAuth.getInstance().currentUser?.email ?: return@launch

                val historyRef = FirebaseFirestore.getInstance()
                    .collection("user_history")
                    .document(email)
                    .collection("history")
                    .document() // auto-ID

                historyRef.set(historyEntry)
                    .addOnSuccessListener {
                        Log.d("History", "Saved history successfully for $email with input: $userInput")
                    }
                    .addOnFailureListener {
                        Log.e("History", "Failed to save history", it)
                    }



            }

            viewModel.setPlaylists(playlists)

//            val ids = playlists.map { YouTubeFetcher.extractPlaylistId(it.url) }
            viewModel.fetchPlaylistMetadata(playlists)



        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(userInput: String, mood: String) = PlaylistFragment().apply {
            arguments = Bundle().apply {
                putString("USER_INPUT", userInput)
                putString("MOOD", mood)
            }
        }
    }

}