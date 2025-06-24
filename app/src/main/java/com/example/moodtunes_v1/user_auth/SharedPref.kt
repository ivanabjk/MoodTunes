package com.example.moodtunes_v1.user_auth

import android.content.Context
import android.content.SharedPreferences

class SharedPref(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    fun setEmail(email: String?) {
        prefs.edit().putString("email", email).apply()
    }

    fun getEmail(): String? = prefs.getString("email", null)

    fun setLogged(logged: Boolean) {
        prefs.edit().putBoolean("logged", logged).apply()
    }

    fun isLogged(): Boolean = prefs.getBoolean("logged", false)

    fun setMoodGenre(mood: String, genres: List<String>) {
        prefs.edit().putStringSet(mood, genres.toSet()).apply()
    }

    fun setProfileImageUri(uri: String) {
        prefs.edit().putString("profile_image_uri", uri).apply()
    }

    fun getProfileImageUri(): String? = prefs.getString("profile_image_uri", null)
}
