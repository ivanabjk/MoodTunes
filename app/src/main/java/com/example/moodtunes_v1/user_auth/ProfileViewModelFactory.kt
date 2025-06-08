package com.example.moodtunes_v1.user_auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.firestore.FirebaseFirestore

class ProfileViewModelFactory(private val authService: AuthService, private val db: FirebaseFirestore) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            return ProfileViewModel(authService, db) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}