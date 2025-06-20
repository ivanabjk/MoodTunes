package com.example.moodtunes_v1.favorites

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moodtunes_v1.R
import com.example.moodtunes_v1.playlist.MoodTunesDatabase
import com.example.moodtunes_v1.playlist.OnFavoriteToggleListener
import com.example.moodtunes_v1.playlist.Playlist
import com.example.moodtunes_v1.playlist.PlaylistAdapter
import com.example.moodtunes_v1.playlist.PlaylistViewModel
import com.example.moodtunes_v1.playlist.PlaylistViewModelFactory
import com.example.moodtunes_v1.playlist.YouTubeFetcher
import kotlinx.coroutines.launch

class FavoritesFragment : Fragment(){

    private val viewModel: PlaylistViewModel by lazy {
        val dao = MoodTunesDatabase.getDatabase(requireContext()).playlistDao()
        val repository = FavoritesRepository(dao)
        val factory = PlaylistViewModelFactory(repository)
        ViewModelProvider(this, factory)[PlaylistViewModel::class.java]
    }
    private lateinit var playlistAdapter: PlaylistAdapter

    private lateinit var allFavorites: List<Playlist>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_favourites, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.fetchFavoritesFromFireStore()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.playlistInfo.collect { metadataMap ->
                        allFavorites = viewModel.playlists.value
                        setupFilters()
                        playlistAdapter.updateData(viewModel.playlists.value, metadataMap)
                    }
                }
            }
        }

        playlistAdapter = PlaylistAdapter(
            emptyList(),
            emptyMap(),
            onItemClick = { playlist ->
                val playlistId = YouTubeFetcher.extractPlaylistId(playlist.url)
                val firstVideoUrl = viewModel.playlistInfo.value[playlistId]?.first ?: return@PlaylistAdapter
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(firstVideoUrl))
                intent.setPackage("com.google.android.youtube")
                startActivity(intent)
            },
            favoriteToggleListener = object : OnFavoriteToggleListener {
                override fun onFavoriteToggled(playlist: Playlist) {
                    viewModel.toggleFavorite(playlist)
                    viewModel.fetchFavoritesFromFireStore()
                }
            }
        )

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvPlaylists)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = playlistAdapter

        // Collect playlists
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.playlists.collect { updated ->
                    playlistAdapter.updateData(updated, viewModel.playlistInfo.value)
                }
            }
        }

        val filterHeader = view.findViewById<View>(R.id.filterHeader)
        val filterContainer = view.findViewById<LinearLayout>(R.id.filterContainer)

        filterHeader.setOnClickListener {
            if (filterContainer.visibility == View.VISIBLE) {
                filterContainer.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction {
                        filterContainer.visibility = View.GONE
                    }
            } else {
                filterContainer.alpha = 0f
                filterContainer.visibility = View.VISIBLE
                filterContainer.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
            }
        }
    }

    private fun setupFilters() {
        val moodOptions = listOf("All") + allFavorites.map { it.mood }.distinct()
        val genreOptions = listOf("All") + allFavorites.map { it.genre }.distinct()

        val moodSpinner = view?.findViewById<Spinner>(R.id.spinnerMoodFilter)
        val genreSpinner = view?.findViewById<Spinner>(R.id.spinnerGenreFilter)

        moodSpinner?.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, moodOptions)
        genreSpinner?.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, genreOptions)

        val filterAndUpdate = {
            val selectedMood = moodSpinner?.selectedItem as? String ?: "All"
            val selectedGenre = genreSpinner?.selectedItem as? String ?: "All"

            val filtered = allFavorites.filter { playlist ->
                (selectedMood == "All" || playlist.mood == selectedMood) &&
                        (selectedGenre == "All" || playlist.genre == selectedGenre)
            }

            playlistAdapter.updateData(filtered, viewModel.playlistInfo.value)
        }

        moodSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long){
                val selectedMood = moodSpinner?.selectedItem.toString()
                val genreSubset = if (selectedMood == "All") {
                    genreOptions
                } else {
                    listOf("All") + allFavorites
                        .filter { it.mood == selectedMood }
                        .map { it.genre }
                        .distinct()
                }

                genreSpinner?.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, genreSubset)
                genreSpinner?.setSelection(0)

                filterAndUpdate()

            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        genreSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) = filterAndUpdate()
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }

}