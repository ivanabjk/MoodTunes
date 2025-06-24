package com.example.moodtunes_v1.history

import androidx.lifecycle.ViewModel
import com.example.moodtunes_v1.playlist.Playlist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HistorySessionViewModel : ViewModel() {
    private val _selectedPlaylists = MutableStateFlow<List<Playlist>>(emptyList())
    val selectedPlaylists: StateFlow<List<Playlist>> = _selectedPlaylists

    fun setSelected(playlists: List<Playlist>) {
        _selectedPlaylists.value = playlists
    }

    fun clear() {
        _selectedPlaylists.value = emptyList()
    }
}