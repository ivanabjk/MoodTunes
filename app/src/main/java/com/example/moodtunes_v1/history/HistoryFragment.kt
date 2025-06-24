package com.example.moodtunes_v1.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        lifecycleScope.launchWhenStarted {
            viewModel.historyEntries.collect { entries ->
                historyAdapter.updateData(entries)
                binding.tvEmpty.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}