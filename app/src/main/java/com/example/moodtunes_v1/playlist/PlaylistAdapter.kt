package com.example.moodtunes_v1.playlist

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.moodtunes_v1.R

class PlaylistAdapter(
    private var playlistList: List<Playlist>,
    private var metadataMap: Map<String, Pair<String, String>>, // playlistId → (url, title)
    private val onItemClick: (Playlist) -> Unit
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {

    inner class PlaylistViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val genreTextView: TextView = view.findViewById(R.id.tvPlaylistGenre)
        val playlistTitleTextView: TextView = view.findViewById(R.id.tvPlaylistTitle)
        val thumbnailImageView: ImageView = view.findViewById(R.id.ivPlaylistImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val playlist = playlistList[position]
        holder.genreTextView.text = playlist.genre

        val playlistId = YouTubeFetcher.extractPlaylistId(playlist.url)
        val (firstVideoUrl, playlistTitle) = metadataMap[playlistId] ?: return

        val firstVideoId = firstVideoUrl.substringAfter("watch?v=").substringBefore("&list")
        val thumbnailUrl = YouTubeFetcher.getThumbnailUrl(firstVideoId)


        holder.playlistTitleTextView.text = playlistTitle

        Glide.with(holder.itemView.context)
            .load(thumbnailUrl)
            .placeholder(R.drawable.sample_image)
            .into(holder.thumbnailImageView)

        holder.itemView.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(firstVideoUrl))
            intent.setPackage("com.google.android.youtube")
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = playlistList.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: List<Playlist>, newMetadata: Map<String, Pair<String, String>>) {
        playlistList = newData
        metadataMap = newMetadata
        notifyDataSetChanged()
    }
}