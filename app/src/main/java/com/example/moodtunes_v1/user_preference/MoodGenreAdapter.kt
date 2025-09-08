package com.example.moodtunes_v1.user_preference

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.moodtunes_v1.R
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class MoodGenreAdapter(
    private var moodGenreList: List<MoodGenres>,
    private val onGenreChanged: (String, List<String>) -> Unit,
    private val onGenreRemoved: (String, String) -> Unit
) : RecyclerView.Adapter<MoodGenreAdapter.MoodGenreViewHolder>() {

    inner class MoodGenreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val moodTextView: TextView = view.findViewById(R.id.tvMood)
        val genresChipGroup: ChipGroup = view.findViewById(R.id.genresChipGroup)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoodGenreViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mood_genres, parent, false)
        return MoodGenreViewHolder(view)
    }

    @SuppressLint("NotifyDataSetChanged", "ResourceAsColor")
    override fun onBindViewHolder(holder: MoodGenreViewHolder, position: Int) {
        val moodGenre = moodGenreList[position]
        holder.moodTextView.text = moodGenre.mood
        holder.genresChipGroup.removeAllViews()

        moodGenre.genres.forEachIndexed { index, genre ->
            val chip = Chip(holder.itemView.context).apply {
                text = genre
                isCloseIconVisible = true
                chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.lightOrange)
                )

                setTextColor(Color.BLACK)

                isFocusableInTouchMode = true
                inputType = InputType.TYPE_CLASS_TEXT
                imeOptions = EditorInfo.IME_ACTION_DONE

                setOnClickListener {
                    requestFocus()
                    showKeyboard(this)
                }

                setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        val updatedGenres = moodGenre.genres.toMutableList()
                        updatedGenres[index] = this.text.toString().trim()
                        onGenreChanged(moodGenre.mood, updatedGenres) // Save changes when clicking outside
                    }
                }

                setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        val updatedGenres = moodGenre.genres.toMutableList()
                        updatedGenres[index] = this.text.toString().trim()

                        onGenreChanged(moodGenre.mood, updatedGenres) // Save to FireStore
                        notifyDataSetChanged() // Refresh RecyclerView
                        true
                    } else false
                }


                setOnCloseIconClickListener {
                    removeGenreFromUI(moodGenre.mood, genre)
                }
            }
            holder.genresChipGroup.addView(chip)
        }
        // Create and add the "+" chip at the end
        val addChip = Chip(holder.itemView.context).apply {
            text = "+"
            isCloseIconVisible = false // No remove option
            chipBackgroundColor = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.orange)
            )

            setTextColor(Color.WHITE)

            setOnClickListener {
                val newGenreName = "genre${moodGenre.genres.size + 1}" // Generate genre name dynamically
                val updatedGenres = moodGenre.genres.toMutableList().apply { add(newGenreName) }

                onGenreChanged(moodGenre.mood, updatedGenres) // Persist the new genre in FireStore
                notifyDataSetChanged() // Refresh RecyclerView to reflect changes
            }
        }

        holder.genresChipGroup.addView(addChip) // Add the "+" chip at the end

    }

    private fun removeGenreFromUI(mood: String, genre: String) {
        onGenreRemoved(mood, genre) // Notify ProfileActivity to handle FireStore update
    }

    override fun getItemCount(): Int = moodGenreList.size
    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: List<MoodGenres>) {
        moodGenreList = newData
        notifyDataSetChanged() // Ensure RecyclerView updates properly
    }

    private fun showKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

}