package com.example.moodtunes_v1

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PlaylistDao {
    @Insert
    suspend fun insertPlaylist(playlist: Playlist)

    @Query("SELECT * FROM playlists WHERE mood = :mood")
    suspend fun getPlaylistsByMood(mood: String): List<Playlist>
}