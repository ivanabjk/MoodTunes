package com.example.moodtunes_v1.favorites

import android.util.Log
import com.example.moodtunes_v1.playlist.Playlist
import com.example.moodtunes_v1.playlist.PlaylistDao
import com.example.moodtunes_v1.playlist.YouTubeFetcher
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

class FavoritesRepository(
    private val playlistDao: PlaylistDao
) {
    private val firestore = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    suspend fun toggleFavorite(playlist: Playlist) {
        playlistDao.updateFavoriteStatus(playlist.url, playlist.isFavorite)

        val email = auth.currentUser?.email ?: return
        val playlistId = YouTubeFetcher.extractPlaylistId(playlist.url)
        Log.d("Favorites", "Saving to FireStore using ID: $playlistId")

        val docRef = firestore
            .collection("user_favorites")
            .document(email)
            .collection("favorites")
            .document(playlistId)

        if (playlist.isFavorite) {
            try {
                docRef.set(playlist).await()
                docRef.update("addedAt", com.google.firebase.Timestamp.now()).await()
                Log.d("Favorites", "Favorites updated with timestamp.")
            } catch (e: Exception) {
                Log.e("Favorites", "Failed to set playlist: ${e.message}")
            }
        } else {
            try {
                docRef.delete().await()
                Log.d("Favorites", "Favorites deleted.")
            } catch (e: Exception) {
                Log.e("Favorites", "Failed to delete playlist: ${e.message}")
            }
        }
    }

    suspend fun getLocalFavorites(): List<Playlist> {
        return playlistDao.getFavoritePlaylists()
    }

    suspend fun getPlaylistsByMood(mood: String): List<Playlist> {
        return playlistDao.getPlaylistsByMood(mood)
    }

    suspend fun getFavoritesFromFireStore(): List<Playlist> {
        val email = auth.currentUser?.email ?: return emptyList()
        val snapshot = firestore
            .collection("user_favorites")
            .document(email)
            .collection("favorites")
            .orderBy("addedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toObject(Playlist::class.java) }
    }
    suspend fun enrichWithFavorites(playlists: List<Playlist>): List<Playlist> {
        val favoriteIds = playlistDao.getAllFavoritePlaylistIds() // returns List<String> or Set<String>
        return playlists.map { it.copy(isFavorite = it.url in favoriteIds) }
    }
}