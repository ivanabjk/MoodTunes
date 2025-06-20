package com.example.moodtunes_v1.favorites

import android.util.Log
import com.example.moodtunes_v1.playlist.Playlist
import com.example.moodtunes_v1.playlist.PlaylistDao
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
        val updatedFavorite = !playlist.isFavorite
        playlistDao.updateFavoriteStatus(playlist.id, updatedFavorite)

        val email = auth.currentUser?.email ?: return
        val docRef = firestore
            .collection("user_favorites")
            .document(email)
            .collection("favorites")
            .document(playlist.id.toString())

        if (updatedFavorite) {
            val playlistWithTimestamp = playlist.copy(isFavorite = true)
            docRef.set(playlistWithTimestamp)
                .addOnSuccessListener {
                    docRef.update("addedAt", com.google.firebase.Timestamp.now())
                    Log.e("Favorites", "Favorites updated with timestamp.")
                }
                .addOnFailureListener { e ->
                    Log.e("Favorites", "Failed to set playlist: ${e.message}")
                }
        }
        else {
            docRef.delete()
            Log.e("Favorites", "Favorites deleted.")
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
}