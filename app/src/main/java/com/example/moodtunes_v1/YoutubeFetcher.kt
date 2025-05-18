package com.example.moodtunes_v1

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object YouTubeFetcher {

    private const val API_KEY = "AIzaSyBC0Qd9z8VOH7_7AooK7YowBsmy5dH7d0A" // Replace with your actual API key

    suspend fun getFirstVideoUrl(playlistId: String): String {
        val apiUrl = "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&playlistId=$playlistId&maxResults=1&key=$API_KEY"

        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(apiUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                val itemsArray = jsonResponse.optJSONArray("items")

                if (itemsArray != null && itemsArray.length() > 0) {
                    val firstItem = itemsArray.getJSONObject(0).getJSONObject("snippet")
                    val firstVideoId = firstItem.getJSONObject("resourceId").getString("videoId")

                    // Correctly format the URL to open the playlist with first video
                    return@withContext "https://www.youtube.com/watch?v=$firstVideoId&list=$playlistId"
                }
            } catch (e: Exception) {
                Log.e("YouTubeFetcher", "Error fetching video: ${e.message}")
            }

            return@withContext ""
        }
    }
    suspend fun getPlaylistTitle(playlistId: String): String {
        val apiUrl = "https://www.googleapis.com/youtube/v3/playlists?part=snippet&id=$playlistId&key=$API_KEY"

        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(apiUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = JSONObject(response)
                val itemsArray = jsonResponse.optJSONArray("items")

                if (itemsArray != null && itemsArray.length() > 0) {
                    val snippetObject = itemsArray.getJSONObject(0).getJSONObject("snippet")
                    val playlistTitle = snippetObject.getString("title") // Fetches the actual title
                    return@withContext playlistTitle
                }
            } catch (e: Exception) {
                Log.e("YouTubeFetcher", "Error fetching playlist title: ${e.message}")
            }

            return@withContext "Unknown Playlist"
        }
    }
    fun extractPlaylistId(playlistUrl: String): String {
        return playlistUrl.substringAfter("list=").substringBefore("&") // Extracts the ID
    }
    fun getThumbnailUrl(videoId: String): String {
        return "https://img.youtube.com/vi/$videoId/mqdefault.jpg"
    }
}