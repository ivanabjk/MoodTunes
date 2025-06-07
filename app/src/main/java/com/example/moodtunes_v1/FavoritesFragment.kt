package com.example.moodtunes_v1

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment

class FavoritesFragment : Fragment(R.layout.fragment_favourites){
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.textView).text = "Hello from Favourites"
    }
}