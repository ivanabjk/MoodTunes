package com.example.moodtunes_v1.user_auth

import com.google.firebase.FirebaseOptions

object DefaultFirebaseOptions {

    val android: FirebaseOptions by lazy {
        FirebaseOptions.Builder()
            .setApiKey("AIzaSyBSewoJuiaKacb4kmIOvuuprrvyZuCf1LE")
            .setApplicationId("1:906492811736:android:e880ca987ab8cb01897aad")
            .setProjectId("moodtunes-c1347")
            .setStorageBucket("moodtunes-c1347.firebasestorage.app")
            .setGcmSenderId("906492811736")
            .build()
    }
}
