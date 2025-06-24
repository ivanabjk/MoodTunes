package com.example.moodtunes_v1.history

import com.example.moodtunes_v1.playlist.Playlist

data class HistoryEntry(
    val userInput: String = "",
    val detectedMood: String = "",
    val timestamp: Long = 0L,
    val playlists: List<Playlist> = emptyList()
)
