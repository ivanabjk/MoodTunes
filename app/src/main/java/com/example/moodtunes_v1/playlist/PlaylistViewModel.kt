package com.example.moodtunes_v1.playlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moodtunes_v1.favorites.FavoritesRepository
import com.example.moodtunes_v1.user_auth.AuthService
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlaylistViewModel(
    private val repository: FavoritesRepository
) : ViewModel() {

    private val _playlistInfo = MutableStateFlow<Map<String, Pair<String, String>>>(emptyMap())
    val playlistInfo: StateFlow<Map<String, Pair<String, String>>> = _playlistInfo

    private val _favorites = MutableStateFlow<List<Playlist>>(emptyList())
    val favorites: StateFlow<List<Playlist>> = _favorites

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists


    fun fetchPlaylistMetadata(playlistIds: List<String>) {
        viewModelScope.launch {
            val result = playlistIds.associateWith { id ->
                try {
                    val url = YouTubeFetcher.getFirstVideoUrl(id)
                    val title = YouTubeFetcher.getPlaylistTitle(id)

                    if (url.isBlank() || title.isBlank()) {
                        android.util.Log.w("MetadataFetch", "Missing data for $id — url: $url, title: $title")
                    }

                    url to title
                } catch (e: Exception) {
                    android.util.Log.e("MetadataFetch", "Error fetching metadata for $id: ${e.message}")
                    "" to "Unknown Playlist"
                }
            }
            _playlistInfo.value = result
        }
    }

    fun toggleFavorite(playlist: Playlist) {
        viewModelScope.launch {
            repository.toggleFavorite(playlist)
            _playlists.value = repository.getPlaylistsByMood(playlist.mood)
        }
    }

    fun fetchPlaylistsByMood(mood: String) {
        viewModelScope.launch {
            _playlists.value = repository.getPlaylistsByMood(mood)
        }
    }


    fun fetchFavoritesFromFireStore() {
        viewModelScope.launch {
            val remoteFavorites = repository.getFavoritesFromFireStore()
            _playlists.value = remoteFavorites

            val ids = remoteFavorites.map { YouTubeFetcher.extractPlaylistId(it.url) }
            fetchPlaylistMetadata(ids)

        }
    }
}