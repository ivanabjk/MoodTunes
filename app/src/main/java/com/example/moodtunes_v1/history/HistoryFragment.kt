package com.example.moodtunes_v1.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moodtunes_v1.R
import com.example.moodtunes_v1.databinding.FragmentHistoryBinding
import com.example.moodtunes_v1.playlist.PlaylistFragment

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: HistoryViewModel
    private lateinit var historyAdapter: HistoryAdapter

    private val sessionViewModel: HistorySessionViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this)[HistoryViewModel::class.java]

        historyAdapter = HistoryAdapter(emptyList()) { playlists, mood ->
            sessionViewModel.setSelected(playlists)
            val fragment = PlaylistFragment().apply {
                arguments = Bundle().apply {
                    putString("MOOD", mood)
                    // You could serialize playlists and pass them if PlaylistFragment supports it
                }
            }
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
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

        setupMoodFilter { selectedMood ->
            currentMood = selectedMood
            viewModel.historyEntries.value.let { entries ->
                val filtered = if (selectedMood == "All") entries else entries.filter { it.detectedMood == selectedMood }
                historyAdapter.updateData(filtered)
                binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.historyEntries.collect { entries ->
                val moods = entries.map { it.detectedMood }.distinct()
                updateMoodOptions(moods, currentMood)

                val filtered = if (currentMood == "All") entries else entries.filter { it.detectedMood == currentMood }
                historyAdapter.updateData(filtered)
                binding.tvEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
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