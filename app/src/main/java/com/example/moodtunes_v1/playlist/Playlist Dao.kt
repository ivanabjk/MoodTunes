package com.example.moodtunes_v1.playlist

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylists(playlists: List<Playlist>)

    @Query("SELECT EXISTS(SELECT 1 FROM playlists WHERE url = :url)")
    suspend fun exists(url: String): Boolean

    @Query("SELECT * FROM playlists")
    suspend fun getAllPlaylists(): List<Playlist>

    @Query("UPDATE playlists SET isFavorite = 0")
    suspend fun clearAllFavorites()

    @Query("UPDATE playlists SET isFavorite = 1 WHERE url IN (:urls)")
    suspend fun setFavoritesByUrls(urls: List<String>)

    // Update favorite status for a playlist
    @Query("UPDATE playlists SET isFavorite = :isFavorite WHERE url = :url")
    suspend fun updateFavoriteStatus(url: String, isFavorite: Boolean)

    @Query("SELECT * FROM playlists WHERE mood = :mood AND userEmail = :userEmail")
    suspend fun getPlaylistsByMoodForUser(mood: String, userEmail: String): List<Playlist>

    @Query("SELECT * FROM playlists WHERE mood = :mood")
    suspend fun getPlaylistsByMood(mood: String): List<Playlist>


    @Query("SELECT url FROM playlists WHERE isFavorite = 1 AND userEmail = :email")
    suspend fun getAllFavoritePlaylistIdsForUser(email: String): List<String>

    @Query(
        """
    SELECT * FROM playlists
    WHERE mood = :mood AND (userEmail = :userEmail OR userEmail IS NULL OR userEmail = '')
"""
    )
    suspend fun getPlaylistsByMoodInclusive(mood: String, userEmail: String): List<Playlist>

    @Query("""
  SELECT * FROM playlists
  WHERE mood = :mood
  AND genre IN (:genres)
  AND (userEmail = :userEmail OR userEmail IS NULL OR userEmail = '')
""")
    suspend fun getPlaylistsByMoodAndGenres(
        mood: String,
        genres: List<String>,
        userEmail: String
    ): List<Playlist>


}