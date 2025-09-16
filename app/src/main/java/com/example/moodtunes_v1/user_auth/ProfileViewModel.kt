package com.example.moodtunes_v1.user_auth

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.moodtunes_v1.user_preference.MoodGenres
import com.google.firebase.firestore.FirebaseFirestore

class ProfileViewModel(private val authService: AuthService, private val db: FirebaseFirestore) : ViewModel() {

    private val _userEmail = MutableLiveData<String>()
    val userEmail: LiveData<String> get() = _userEmail

    private val _moodGenres = MutableLiveData<List<MoodGenres>>()
    val moodGenres: LiveData<List<MoodGenres>> get() = _moodGenres

    init {
        _userEmail.value = authService.getEmail()
    }

    fun setMoodGenres(updated: List<MoodGenres>) {
        _moodGenres.postValue(updated)
    }

    fun fetchUserPreferences(userId: String) {
        db.collection("user_preferences").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val moodGenresMap = document.get("moodGenres") as? Map<String, List<String>> ?: emptyMap()
                    val convertedMoodGenres = moodGenresMap.map { MoodGenres(it.key, it.value) }

                    Log.d("FirestoreData", "Retrieved: $convertedMoodGenres") // Debug log

//                    _moodGenres.value = convertedMoodGenres // Update LiveData
                    _moodGenres.postValue(convertedMoodGenres)
                } else {
                    Log.e("FirestoreData", "No document found for user $userId")
                }
            }
            .addOnFailureListener { Log.e("FirestoreError", "Error fetching data", it) }
    }

}