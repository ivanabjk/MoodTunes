package com.example.moodtunes_v1.history

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SearchView
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moodtunes_v1.R
import com.example.moodtunes_v1.databinding.FragmentHistoryBinding
import com.example.moodtunes_v1.playlist.PlaylistFragment
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HistoryViewModel
    private lateinit var historyAdapter: HistoryAdapter

    private val sessionViewModel: HistorySessionViewModel by activityViewModels()

    private var pendingDeletion: HistoryEntry? = null
    private var pendingSnackbar: Snackbar? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this)[HistoryViewModel::class.java]

        var currentSearchQuery = ""

        historyAdapter = HistoryAdapter(
            emptyList(),
            onViewPlaylistsClick = { playlists, mood ->
                sessionViewModel.setSelected(playlists)
                val fragment = PlaylistFragment.newInstance(
                    userInput = "",
                    mood = mood,
                    fromHome = false
                )
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            onDeleteClick = ::deleteEntryWithUndo
        )

        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }


        binding.btnDeleteAll.setOnClickListener {
            showClearHistoryConfirmation()
        }

        val filterHeader = binding.filterHeader
        val filterContainer = binding.filterContainer

        filterHeader.setOnClickListener {
            if (filterContainer.visibility == View.VISIBLE) {
                filterContainer.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction { filterContainer.visibility = View.GONE }
            } else {
                filterContainer.alpha = 0f
                filterContainer.visibility = View.VISIBLE
                filterContainer.animate().alpha(1f).setDuration(200).start()
            }
        }

        var currentMood = "All"

        binding.searchView.addTextChangedListener {
            val query = it.toString()
            currentSearchQuery = query
            applyFilters(currentMood, currentSearchQuery)
        }

//        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
//            override fun onQueryTextSubmit(query: String?): Boolean = false
//
//            override fun onQueryTextChange(newText: String?): Boolean {
//                currentSearchQuery = newText.orEmpty()
//                applyFilters(currentMood, currentSearchQuery)
//                return true
//            }
//        })

        setupMoodFilter { selectedMood ->
            currentMood = selectedMood
            applyFilters(currentMood, currentSearchQuery)
            viewModel.historyEntries.value.let { entries ->
                val filtered = if (selectedMood == "All") entries else entries.filter { it.detectedMood == selectedMood }
                historyAdapter.updateData(filtered)
                binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.historyEntries.collect { entries ->
                    val moods = entries.map { it.detectedMood }.distinct()

                    // If currentMood is no longer present, reset to "All"
                    if (currentMood != "All" && !moods.contains(currentMood)) {
                        currentMood = "All"
                    }

                    updateMoodOptions(moods, currentMood)

                    val filtered = if (currentMood == "All") entries
                    else entries.filter { it.detectedMood == currentMood }

//                    historyAdapter.updateData(filtered)

                    // Apply mood + search filters together
                    applyFilters(currentMood, currentSearchQuery)

                    binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE

                }
            }
        }
    }

    private fun setupMoodFilter(onMoodSelected: (String) -> Unit) {
        val spinner = view?.findViewById<Spinner>(R.id.spinnerMoodFilter) ?: return

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selected = spinner.selectedItem.toString()
                onMoodSelected(selected)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    private fun showClearHistoryConfirmation() {
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Clear History")
            .setMessage("Are you sure you want to delete your history?")
            .setPositiveButton("Delete") { _, _ ->
                clearAllHistory()
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }
    private fun clearAllHistory() {
        viewModel.clearAllHistory(
            onSuccess = {
                Snackbar.make(binding.root, "All history deleted.", Snackbar.LENGTH_LONG).show()
            },
            onFailure = {
                Snackbar.make(binding.root, "Failed to delete history.", Snackbar.LENGTH_LONG).show()
            }
        )
    }

    private fun applyFilters(mood: String, query: String) {
        val entries = viewModel.historyEntries.value
        val filtered = entries.filter {
            (mood == "All" || it.detectedMood == mood) &&
                    it.userInput.contains(query, ignoreCase = true)
        }

        historyAdapter.updateData(filtered)
        binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun deleteEntryWithUndo(entry: HistoryEntry) {
        val originalList = viewModel.historyEntries.value
        val position = originalList.indexOf(entry)

        // Commit previous pending deletion if any
        pendingSnackbar?.dismiss()
        pendingDeletion?.let { viewModel.deleteEntry(it) }

        // Temporarily remove from adapter
        val updatedList = originalList.toMutableList().apply {
            remove(entry)
        }
        historyAdapter.updateData(updatedList)

        pendingDeletion = entry

        // Show Snackbar with Undo
        val preview = if (entry.userInput.length > 30) {
            entry.userInput.take(27) + "..."
        } else entry.userInput

        val snackbar = Snackbar.make(binding.root, "Deleted \"$preview\"", Snackbar.LENGTH_LONG)
            .setAction("Undo") {
                val restoredList = updatedList.toMutableList().apply {
                    add(position.coerceAtMost(size), entry)
                }
                historyAdapter.updateData(restoredList)
                binding.rvHistory.scrollToPosition(position)
                pendingDeletion = null
            }
            .addCallback(object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    if (event != DISMISS_EVENT_ACTION) {
                        pendingDeletion?.let { viewModel.deleteEntry(it) }
                        pendingDeletion = null
                    }
                    pendingSnackbar = null
                }
            })

        val snackbarView = snackbar.view
        snackbarView.background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_snackbar)

        pendingSnackbar = snackbar
        snackbar.show()


    }

    private fun deleteEntry(entry: HistoryEntry) {
        viewModel.deleteEntry(entry)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateMoodOptions(moods: List<String>, selected: String = "All") {
        val spinner = view?.findViewById<Spinner>(R.id.spinnerMoodFilter) ?: return
        val options = listOf("All") + moods.distinct()
        spinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, options)
        spinner.setSelection(options.indexOfFirst { it == selected }.coerceAtLeast(0))
    }
}