package com.example.moodtunes_v1

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mood: String,
    val genre: String,
    val url: String
)
