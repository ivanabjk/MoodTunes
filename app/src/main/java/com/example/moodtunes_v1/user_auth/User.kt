package com.example.moodtunes_v1.user_auth

data class User(
    val email: String,
    val password: String,
    val name: String = "",
    val profileImageUrl: String = "",
    val moodGenres: Map<String, List<String>> = emptyMap()
)
