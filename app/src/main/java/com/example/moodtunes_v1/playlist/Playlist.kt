package com.example.moodtunes_v1.playlist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    var mood: String,
    var genre: String,
    var url: String,
    var isFavorite: Boolean = false
){
    constructor() : this(0, "", "", "", false)
}
