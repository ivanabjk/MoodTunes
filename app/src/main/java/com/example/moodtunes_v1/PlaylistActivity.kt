package com.example.moodtunes_v1

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.moodtunes_v1.YouTubeFetcher.extractPlaylistId
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
class PlaylistActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist)

        val mood = intent.getStringExtra("MOOD") ?: "Neutral"
        val moodTitle = findViewById<TextView>(R.id.tvMoodTitle)
        val playlistContainer = findViewById<LinearLayout>(R.id.playlistContainer)

        moodTitle.text = "Here are some playlists for $mood mood"

        fetchPlaylists(mood, playlistContainer)
    }
    @SuppressLint("SetTextI18n")
    private fun fetchPlaylists(mood: String, playlistContainer: LinearLayout) {
        val db = MoodTunesDatabase.getDatabase(this)
        val playlistDao = db.playlistDao()


        GlobalScope.launch(Dispatchers.Main) {
            val playlists = playlistDao.getPlaylistsByMood(mood)

            playlists.forEach { playlist ->
                val playlistId = extractPlaylistId(playlist.url)
                val firstVideoUrl = YouTubeFetcher.getFirstVideoUrl(playlistId)
                val firstVideoId = firstVideoUrl.substringAfter("watch?v=").substringBefore("&list")
                val thumbnailUrl = YouTubeFetcher.getThumbnailUrl(firstVideoId)

                // Fetching playlist title
                val playlistTitle = YouTubeFetcher.getPlaylistTitle(playlistId)

                val previewLayout = LinearLayout(this@PlaylistActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(20, 20, 20, 20)

                    // Genre TextView
                    val genreTextView = TextView(this@PlaylistActivity).apply {
                        text = playlist.genre
                        textSize = 22f
                        setTypeface(null, Typeface.BOLD)
                        setPadding(0, 0, 0, 10)
                    }

                    // Playlist Title TextView
                    val playlistTitleTextView = TextView(this@PlaylistActivity).apply {
                        text = playlistTitle
                        textSize = 16f // Smaller text size
                        setPadding(0, 0, 0, 10)
                    }

                    val thumbnailImageView = ImageView(this@PlaylistActivity).apply {
                        layoutParams = LinearLayout.LayoutParams(500, 400)
                        Glide.with(this@PlaylistActivity)
                            .load(thumbnailUrl)
                            .into(this) // Loads the actual thumbnail

                        setOnClickListener {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(firstVideoUrl))
                            intent.setPackage("com.google.android.youtube")
                            startActivity(intent)
                        }
                    }

                    addView(genreTextView)
                    addView(playlistTitleTextView)
                    addView(thumbnailImageView)
                }

                playlistContainer.addView(previewLayout)
            }
        }
    }

}
