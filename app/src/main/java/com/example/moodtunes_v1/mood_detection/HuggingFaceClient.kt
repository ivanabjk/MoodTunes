package com.example.moodtunes_v1.mood_detection

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Interceptor adds your API token to every request
class AuthInterceptor(private val token: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val newRequest = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        return chain.proceed(newRequest)
    }
}

object HuggingFaceClient {
    private const val BASE_URL = "https://api-inference.huggingface.co/"

    // TODO: Replace with your actual Hugging Face API token here
    private const val HF_TOKEN = "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"

    private val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(HF_TOKEN))
        .build()

    val api: HuggingFaceApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HuggingFaceApi::class.java)
    }
}