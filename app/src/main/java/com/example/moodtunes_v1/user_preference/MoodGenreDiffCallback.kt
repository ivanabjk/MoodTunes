package com.example.moodtunes_v1.user_preference

import androidx.recyclerview.widget.DiffUtil

class MoodGenreDiffCallback(
    private val oldList: List<MoodGenres>,
    private val newList: List<MoodGenres>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size
    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].mood == newList[newItemPosition].mood
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].genres == newList[newItemPosition].genres
    }
}