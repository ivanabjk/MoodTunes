package com.example.moodtunes_v1.user_preference

import com.example.moodtunes_v1.playlist.PlaylistDao

data class MoodGenres(
    val mood: String,
    val genres: List<String>
)


