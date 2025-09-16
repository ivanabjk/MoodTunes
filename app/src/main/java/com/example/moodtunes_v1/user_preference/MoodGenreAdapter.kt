package com.example.moodtunes_v1.user_preference

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.moodtunes_v1.R
import com.google.android.flexbox.FlexboxLayout
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

            val genreChip = Chip(holder.itemView.context).apply {
                text = genre
                isCloseIconVisible = true
                chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.lightOrange)
                )
                setTextColor(Color.BLACK)
                chipStrokeWidth = 2f
                chipStrokeColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.lightOrange))
                elevation = 6f

                setOnClickListener {
                    showTopDialog(holder.itemView.context, moodGenre.mood, genre) { newText ->
                        val updatedGenres = moodGenre.genres.toMutableList()
                        updatedGenres[index] = newText
                        onGenreChanged(moodGenre.mood, updatedGenres)
//                        notifyDataSetChanged()
                        notifyItemChanged(position)
                    }
                }

                setOnCloseIconClickListener {
                    onGenreRemoved(moodGenre.mood, genre)
                }
            }
            holder.genresChipGroup.addView(genreChip)

        }
        // Create and add the "+" chip at the end
        val addChip = Chip(holder.itemView.context).apply {
            text = "+"
            isCloseIconVisible = false // No remove option
            chipBackgroundColor = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.orange)
            )

            chipStrokeWidth = 2f
            chipStrokeColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.orange))
            elevation = 6f

            setTextColor(Color.WHITE)

            setOnClickListener {
                showTopDialog(holder.itemView.context, moodGenre.mood,"genre${moodGenre.genres.size + 1}") { newText ->
                    val trimmed = newText.trim()
                    val currentGenres = moodGenre.genres
                    val isDuplicate = currentGenres.any { it.equals(trimmed, ignoreCase = true) }

                    if (isDuplicate) {
                        Toast.makeText(holder.itemView.context, "Genre already exists", Toast.LENGTH_SHORT).show()
                        return@showTopDialog
                    }

                    val updatedGenres = currentGenres.toMutableList().apply { add(trimmed) }
                    onGenreChanged(moodGenre.mood, updatedGenres)
                    notifyItemChanged(position)

                }
            }
        }

        holder.genresChipGroup.addView(addChip) // Add the "+" chip at the end

    }

    override fun getItemCount(): Int = moodGenreList.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newData: List<MoodGenres>) {
//        val oldList = moodGenreList // capture BEFORE overwriting
//        moodGenreList = newData
//
//        val diff = oldList.zip(newData).mapIndexedNotNull { index, (old, new) ->
//            if (old.genres != new.genres || old.mood != new.mood) index else null
//        }
//
//        if (diff.isEmpty()) {
//            notifyDataSetChanged() // fallback if no diff detected
//        } else {
//            diff.forEach { notifyItemChanged(it) }
//        }

        val diffCallback = MoodGenreDiffCallback(moodGenreList, newData)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        moodGenreList = newData
        diffResult.dispatchUpdatesTo(this)


//        moodGenreList = newData
//        notifyDataSetChanged() // Ensure RecyclerView updates properly

    }

    private fun showTopDialog(context: Context, mood: String, initialText: String, onSave: (String) -> Unit) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_genre, null)
        val editText = dialogView.findViewById<EditText>(R.id.editGenreInput)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val previewChip = dialogView.findViewById<Chip>(R.id.previewChip)

        previewChip.text = initialText

        editText.setText(initialText)
        editText.setSelection(initialText.length)

        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                previewChip.text = s?.toString()?.trim()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        editText.filters = arrayOf(InputFilter.LengthFilter(20))

        editText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                btnSave.performClick()
                true
            } else {
                false
            }
        }

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.attributes?.windowAnimations = R.style.DialogSlideAnimation

        btnSave.setOnClickListener {
            val newText = editText.text.toString().trim()

            if (newText.isEmpty()) {
                editText.error = "Genre cannot be empty"
                return@setOnClickListener
            }
            val currentMoodGenres = moodGenreList.find { it.mood == mood }?.genres ?: emptyList()
            val isDuplicate = currentMoodGenres.any {
                it.equals(newText, ignoreCase = true) && !it.equals(initialText, ignoreCase = true)
            }

            if (isDuplicate) {
                editText.error = "Genre already exists"
                return@setOnClickListener
            }

            onSave(newText)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setGravity(Gravity.TOP)
        dialog.show()
    }


}