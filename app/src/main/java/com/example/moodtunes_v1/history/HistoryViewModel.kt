package com.example.moodtunes_v1.history

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HistoryViewModel : ViewModel() {

    private val _historyEntries = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val historyEntries: StateFlow<List<HistoryEntry>> = _historyEntries

    init {
        fetchHistory()
    }

    private fun fetchHistory() {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        val firestore = FirebaseFirestore.getInstance()

        firestore
            .collection("user_history")
            .document(email)
            .collection("history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("HistoryViewModel", "Firestore error", error)
                    return@addSnapshotListener
                }

                val entries = snapshot?.documents?.mapNotNull {
                    it.toObject(HistoryEntry::class.java)
                } ?: emptyList()

                _historyEntries.value = entries
            }
    }
}