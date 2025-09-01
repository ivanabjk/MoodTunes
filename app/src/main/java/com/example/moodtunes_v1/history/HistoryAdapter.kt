package com.example.moodtunes_v1.history

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.moodtunes_v1.R
import com.example.moodtunes_v1.playlist.Playlist
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private var entries: List<HistoryEntry>,
    private val onViewPlaylistsClick: (List<Playlist>, String) -> Unit,
    private val onDeleteClick: (HistoryEntry) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUserInput = itemView.findViewById<TextView>(R.id.tvUserInput)
        val tvMood = itemView.findViewById<TextView>(R.id.tvMood)
        val tvTimestamp = itemView.findViewById<TextView>(R.id.tvTimestamp)
        val btnViewPlaylists = itemView.findViewById<ImageButton>(R.id.btnViewPlaylists)
        val btnDeleteEntry = itemView.findViewById<ImageButton>(R.id.btnDeleteEntry)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val entry = entries[position]
        holder.tvUserInput.text = entry.userInput
        holder.tvMood.text = entry.detectedMood
        holder.tvTimestamp.text = formatTimestamp(entry.timestamp)

        holder.btnViewPlaylists.setOnClickListener {
            onViewPlaylistsClick(entry.playlists, entry.detectedMood)
        }
        holder.btnDeleteEntry.setOnClickListener {
            onDeleteClick(entry)
        }

    }

    override fun getItemCount(): Int = entries.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newEntries: List<HistoryEntry>) {
        entries = newEntries.sortedByDescending { it.timestamp }
        notifyDataSetChanged()
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}