package com.example.moodtunes_v1.history

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.android.material.snackbar.Snackbar
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
                    Log.e("HistoryViewModel", "FireStore error", error)
                    return@addSnapshotListener
                }

                val entries = snapshot?.documents?.mapNotNull {
                    it.toObject(HistoryEntry::class.java)
                } ?: emptyList()

                _historyEntries.value = entries
            }
    }

    fun deleteEntry(entry: HistoryEntry) {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("user_history")
            .document(email)
            .collection("history")
            .whereEqualTo("timestamp", entry.timestamp)
            .get()
            .addOnSuccessListener { snapshot ->
                for (doc in snapshot.documents) {
                    doc.reference.delete()
                }
            }
            .addOnFailureListener { error ->
                Log.e("HistoryFragment", "Failed to delete entry", error)
            }
    }

    fun clearAllHistory(
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("user_history")
            .document(email)
            .collection("history")
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { error -> onFailure(error) }
            }
            .addOnFailureListener { error -> onFailure(error) }
    }

}