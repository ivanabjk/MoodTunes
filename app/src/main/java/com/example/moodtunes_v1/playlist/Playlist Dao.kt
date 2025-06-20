package com.example.moodtunes_v1.playlist

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PlaylistDao {
    @Insert
    suspend fun insertPlaylist(playlist: Playlist)

    @Query("SELECT * FROM playlists WHERE mood = :mood")
    suspend fun getPlaylistsByMood(mood: String): List<Playlist>

    // Get all favorite playlists
    @Query("SELECT * FROM playlists WHERE isFavorite = 1")
    suspend fun getFavoritePlaylists(): List<Playlist>

    // Get favorite playlists filtered by mood
    @Query("SELECT * FROM playlists WHERE isFavorite = 1 AND mood = :mood")
    suspend fun getFavoritePlaylistsByMood(mood: String): List<Playlist>

    // Get favorite playlists filtered by genre
    @Query("SELECT * FROM playlists WHERE isFavorite = 1 AND genre = :genre")
    suspend fun getFavoritePlaylistsByGenre(genre: String): List<Playlist>

    // Update favorite status for a playlist
    @Query("UPDATE playlists SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Int, isFavorite: Boolean)
}