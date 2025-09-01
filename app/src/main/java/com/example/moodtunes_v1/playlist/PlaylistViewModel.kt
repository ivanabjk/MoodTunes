package com.example.moodtunes_v1.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moodtunes_v1.favorites.FavoritesRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PlaylistViewModel(
    private val repository: FavoritesRepository
) : ViewModel() {

    private val _playlistInfo = MutableStateFlow<Map<String, Pair<String, String>>>(emptyMap())
    val playlistInfo: StateFlow<Map<String, Pair<String, String>>> = _playlistInfo

    private val _favorites = MutableStateFlow<List<Playlist>>(emptyList())
    val favorites: StateFlow<List<Playlist>> = _favorites

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists

    private val fetchedUrlsByGenre = mutableMapOf<String, MutableSet<String>>()


    fun fetchPlaylistMetadata(playlists: List<Playlist>) {
        viewModelScope.launch {
            val result = playlists.associate { playlist ->
                try {
                    val id = YouTubeFetcher.extractPlaylistId(playlist.url)
                    val firstVideoUrl = YouTubeFetcher.getFirstVideoUrl(id)
                    val title = YouTubeFetcher.getPlaylistTitle(id)

                    if (firstVideoUrl.isBlank() || title.isBlank()) {
                        android.util.Log.w("MetadataFetch", "Missing data for $id — url: $firstVideoUrl, title: $title")
                    }

                    playlist.url to (firstVideoUrl to title)
                } catch (e: Exception) {
                    val id = YouTubeFetcher.extractPlaylistId(playlist.url)
                    android.util.Log.e("MetadataFetch", "Error fetching metadata for $id: ${e.message}")
                    playlist.url to ("" to "Unknown Playlist")
                }
            }
            _playlistInfo.value = result
        }
    }

    fun toggleFavorite(playlist: Playlist) {
        viewModelScope.launch {
            val updated = playlist.copy(isFavorite = !playlist.isFavorite)
            repository.toggleFavorite(updated)
            _playlists.value = _playlists.value.map {
                if (it.url == updated.url) updated else it
            }
        }
    }


    fun fetchFavoritesFromFireStore() {
        viewModelScope.launch {
            val remoteFavorites = repository.getFavoritesFromFireStore()
            val enriched = repository.enrichWithFavorites(remoteFavorites)
            _playlists.value = enriched

            fetchPlaylistMetadata(enriched)
        }
    }

    fun setPlaylists(playlists: List<Playlist>) {
        viewModelScope.launch {
            val enriched = repository.enrichWithFavorites(playlists)
            _playlists.value = enriched
        }
    }

    fun markFetched(genre: String, urls: List<String>) {
        val set = fetchedUrlsByGenre.getOrPut(genre) { mutableSetOf() }
        set.addAll(urls)
    }

    fun hasBeenFetched(genre: String, url: String): Boolean {
        return fetchedUrlsByGenre[genre]?.contains(url) == true
    }

    suspend fun getPreferredGenresForMood(
        db: FirebaseFirestore,
        email: String,
        mood: String
    ): List<String> {
        val snapshot = db.collection("user_preferences")
            .document(email)
            .get()
            .await()

        val moodGenresMap = snapshot.get("moodGenres") as? Map<*, *> ?: emptyMap<Any, Any>()
        return (moodGenresMap[mood] as? List<*>)?.filterIsInstance<String>()
            ?.map { it.trim().lowercase() } ?: emptyList()    }

}