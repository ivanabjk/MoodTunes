package com.example.moodtunes_v1.playlist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey val url: String,
    var mood: String,
    var genre: String,
    var isFavorite: Boolean = false
){
    constructor() : this("", "", "", false)
}
