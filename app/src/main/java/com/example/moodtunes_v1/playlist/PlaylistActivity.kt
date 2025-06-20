package com.example.moodtunes_v1.playlist

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.moodtunes_v1.R
import com.example.moodtunes_v1.favorites.FavoritesRepository
import com.example.moodtunes_v1.playlist.YouTubeFetcher.extractPlaylistId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlaylistActivity : AppCompatActivity() {

    private lateinit var viewModel: PlaylistViewModel
    private lateinit var playlistAdapter: PlaylistAdapter

    @SuppressLint("SetTextI18n", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist)

        val db = MoodTunesDatabase.getDatabase(this)
        val playlistDao = db.playlistDao()
        val repository = FavoritesRepository(playlistDao)

        val factory = PlaylistViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[PlaylistViewModel::class.java]

        val mood = intent.getStringExtra("MOOD") ?: "Neutral"
        val moodTitle = findViewById<TextView>(R.id.tvMoodTitle)
        moodTitle.text = "For your $mood mood!"

        val recyclerView = findViewById<RecyclerView>(R.id.rvPlaylists)
        recyclerView.layoutManager = LinearLayoutManager(this)

        playlistAdapter = PlaylistAdapter(
            emptyList(),
            emptyMap(),
            onItemClick = { playlist ->
                val playlistId = extractPlaylistId(playlist.url)
                val firstVideoUrl = viewModel.playlistInfo.value[playlistId]?.first ?: return@PlaylistAdapter
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(firstVideoUrl))
                intent.setPackage("com.google.android.youtube")
                startActivity(intent)
            },
            favoriteToggleListener = object : OnFavoriteToggleListener {
                override fun onFavoriteToggled(playlist: Playlist) {
                    viewModel.toggleFavorite(playlist)
                }
            }
        )

        recyclerView.adapter = playlistAdapter

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.playlists.collect { updated ->
                    playlistAdapter.updateData(updated, viewModel.playlistInfo.value)
                }
            }
        }

        fetchPlaylists(mood)

    }
    @SuppressLint("SetTextI18n")
    private fun fetchPlaylists(mood: String) {
        val db = MoodTunesDatabase.getDatabase(this)
        val playlistDao = db.playlistDao()


        lifecycleScope.launch(Dispatchers.Main) {
            val playlists = playlistDao.getPlaylistsByMood(mood)
            if (playlists.isEmpty()) {
                findViewById<TextView>(R.id.tvMoodTitle).text = "No playlists found for this mood 😢"
                return@launch
            }
            val playlistIds = playlists.map { extractPlaylistId(it.url) }

            viewModel.fetchPlaylistMetadata(playlistIds)

            viewModel.playlistInfo.collect { metadataMap ->
                playlistAdapter.updateData(playlists, metadataMap)
            }
        }
    }

}
