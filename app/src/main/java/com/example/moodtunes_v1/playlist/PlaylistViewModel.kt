package com.example.moodtunes_v1.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlaylistViewModel : ViewModel() {

    private val _playlistInfo = MutableStateFlow<Map<String, Pair<String, String>>>(emptyMap())
    val playlistInfo: StateFlow<Map<String, Pair<String, String>>> = _playlistInfo

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
}