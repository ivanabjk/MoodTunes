package com.example.moodtunes_v1.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.moodtunes_v1.favorites.FavoritesRepository

class PlaylistViewModelFactory(private val repository: FavoritesRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlaylistViewModel::class.java)) {
            return PlaylistViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}