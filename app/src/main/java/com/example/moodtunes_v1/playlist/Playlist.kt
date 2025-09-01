package com.example.moodtunes_v1.playlist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey val url: String,
    val title: String,
    val mood: String,
    val genre: String,
    var isFavorite: Boolean = false,
    val userEmail: String = ""
){
    constructor() : this("", "", "", "", false)
}
