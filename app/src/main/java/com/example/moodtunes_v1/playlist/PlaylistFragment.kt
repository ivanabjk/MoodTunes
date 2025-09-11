package com.example.moodtunes_v1.playlist

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.moodtunes_v1.R
import com.example.moodtunes_v1.databinding.FragmentPlaylistBinding
import com.example.moodtunes_v1.favorites.FavoritesRepository
import com.example.moodtunes_v1.history.HistoryEntry
import com.example.moodtunes_v1.history.HistorySessionViewModel
import com.example.moodtunes_v1.history.generateIdFromInput
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                val firstVideoUrl =
                    viewModel.playlistInfo.value[playlistId]?.first ?: return@PlaylistAdapter
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

        fetchPlaylists(mood)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(viewModel.playlists, viewModel.playlistInfo) { playlists, metadata ->
                    playlists to metadata
                }.collect { (playlists, metadata) ->
                    playlistAdapter.updateData(playlists, metadata)
                }
            }
        }

        requireView().findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

    }

    @SuppressLint("SetTextI18n")
    private fun fetchPlaylists(mood: String) {
        val db = MoodTunesDatabase.getDatabase(requireContext())
        val playlistDao = db.playlistDao()


        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {

            val email = FirebaseAuth.getInstance().currentUser?.email ?: ""

            val rawGenres = viewModel.getPreferredGenresForMood(FirebaseFirestore.getInstance(), email, mood)
            val normalizedGenres = rawGenres.map { it.trim().lowercase() }

            Log.d("GenreFilter", "Normalized user genres for $mood: $normalizedGenres")


            val youtubePlaylists = mutableListOf<Playlist>()

            for (genre in normalizedGenres) {
                val urls = YouTubeFetcher.searchPlaylistsByGenre(genre, 2)
                for (url in urls) {

                    val existing = playlistDao.getAllPlaylists().find { it.url == url }

                    val playlistId = YouTubeFetcher.extractPlaylistId(url)
                    val title = try {
                        YouTubeFetcher.getPlaylistTitle(playlistId)
                    } catch (e: Exception) {
                        Log.e("PlaylistFetch", "Failed to fetch title for $playlistId", e)
                        "Unknown Title"
                    }


                    Log.d("PlaylistFetch", "Fetched title for $playlistId: $title")



                    youtubePlaylists.add(
                        Playlist(
                            url = url,
                            title = title,
                            mood = mood,
                            genre = genre,
                            userEmail = email,
                            isFavorite = existing?.isFavorite == true
                        )
                    )
                }

            }

            playlistDao.insertPlaylists(youtubePlaylists)


            if (youtubePlaylists.isEmpty()) {
                withContext(Dispatchers.Main) {
                    binding.tvMoodTitle.text = "No playlists found for this mood 😢"
                }
                return@launch
            } else {
                val userInput = arguments?.getString("USER_INPUT").orEmpty()
                val detectedMood = mood // already passed in
                val timestamp = System.currentTimeMillis()

                if(email != ""){
                    val historyEntry = HistoryEntry(
                        userInput = userInput,
                        detectedMood = detectedMood,
                        timestamp = timestamp,
                        playlists = youtubePlaylists // your DAO result
                    )

                    val docId = generateIdFromInput(historyEntry.userInput)
                    val historyRef = FirebaseFirestore.getInstance()
                        .collection("user_history")
                        .document(email)
                        .collection("history")
                        .document(docId) // auto-ID

                    historyRef.set(historyEntry)
                        .addOnSuccessListener {
                            Log.d(
                                "History",
                                "Saved history successfully for $email with input: $userInput"
                            )
                        }
                        .addOnFailureListener {
                            Log.e("History", "Failed to save history", it)
                        }
                }

            }
            viewModel.setPlaylists(youtubePlaylists)
            viewModel.fetchPlaylistMetadata(youtubePlaylists)

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