package com.example.moodtunes_v1

import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

// Data class for API request body
data class HfRequest(val inputs: String)

// Data class for response (example for Hugging Face text classification output)
data class HfResponseItem(
    val label: String,
    val score: Float
)

// Hugging Face API interface
interface HuggingFaceApi {
    @Headers("Content-Type: application/json")
    @POST("models/borisn70/bert-43-multilabel-emotion-detection")
    suspend fun analyzeEmotion(@Body request: HfRequest): List<List<HfResponseItem>>
}
