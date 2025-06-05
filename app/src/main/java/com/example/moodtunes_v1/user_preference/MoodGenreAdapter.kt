import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.InputType
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.moodtunes_v1.R
import com.example.moodtunes_v1.user_preference.MoodGenres
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.internal.ViewUtils.showKeyboard

class MoodGenreAdapter(
    private var moodGenreList: List<MoodGenres>,
    private val onGenreChanged: (String, List<String>) -> Unit,
    private val onGenreRemoved: (String, String) -> Unit
) : RecyclerView.Adapter<MoodGenreAdapter.MoodGenreViewHolder>() {

    inner class MoodGenreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val moodTextView: TextView = view.findViewById(R.id.tvMood)
        //val genresContainer: LinearLayout = view.findViewById(R.id.genresContainer)
        val genresChipGroup: ChipGroup = view.findViewById(R.id.genresChipGroup)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoodGenreViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mood_genres, parent, false)
        return MoodGenreViewHolder(view)
    }

    //    override fun onBindViewHolder(holder: MoodGenreViewHolder, position: Int) {
//        val moodGenre = moodGenreList[position]
//        holder.moodTextView.text = moodGenre.mood
//        holder.genresContainer.removeAllViews()
//
//        moodGenre.genres.forEachIndexed { index, genre ->
//            val editText = EditText(holder.itemView.context).apply {
//                layoutParams = LinearLayout.LayoutParams(
//                    ViewGroup.LayoutParams.WRAP_CONTENT,
//                    ViewGroup.LayoutParams.WRAP_CONTENT
//                )
//                setText(genre)
//                hint = "Edit genre"
//
//                addTextChangedListener(object : TextWatcher {
//                    override fun afterTextChanged(s: Editable?) {
//                        try {
//                            val updatedGenres = moodGenre.genres.toMutableList()
//
//                            // Make sure the index exists before modifying
//                            if (index in updatedGenres.indices) {
//                                updatedGenres[index] = s.toString().trim()
//                                onGenreChanged(moodGenre.mood, updatedGenres)
//                            } else {
//                                Log.e("RecyclerView", "Invalid index: $index, list size: ${updatedGenres.size}")
//                            }
//                        } catch (e: Exception) {
//                            Log.e("RecyclerView", "Error updating genre", e)
//                        }
//                    }
//
//                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//                })
//            }
//            holder.genresContainer.addView(editText)
//        }
//    }
    override fun onBindViewHolder(holder: MoodGenreViewHolder, position: Int) {
        val moodGenre = moodGenreList[position]
        holder.moodTextView.text = moodGenre.mood
        holder.genresChipGroup.removeAllViews()

        moodGenre.genres.forEachIndexed { index, genre ->
            val chip = Chip(holder.itemView.context).apply {
                text = genre
                isCloseIconVisible = true
                chipBackgroundColor = ColorStateList.valueOf(Color.LTGRAY)
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

                        onGenreChanged(moodGenre.mood, updatedGenres) // Save to Firestore
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
    }

    fun removeGenreFromUI(mood: String, genre: String) {
        onGenreRemoved(mood, genre) // Notify ProfileActivity to handle Firestore update
    }

    override fun getItemCount(): Int = moodGenreList.size
    fun updateData(newData: List<MoodGenres>) {
        moodGenreList = newData
        notifyDataSetChanged() // Ensure RecyclerView updates properly
    }
    fun EditText.getOffsetForPosition(x: Float, y: Float): Int {
        val layout = layout ?: return text.length
        return layout.getOffsetForHorizontal(0, x)
    }
    fun showKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

}