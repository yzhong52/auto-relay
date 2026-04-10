package com.autorelay.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.autorelay.app.data.LogEntry
import com.autorelay.app.data.RelayConfig
import com.autorelay.app.data.RelayLog
import com.autorelay.app.databinding.FragmentLogBinding

class LogFragment : Fragment() {

    private var _binding: FragmentLogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = LogAdapter()
        binding.rvLog.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLog.adapter = adapter

        RelayLog.entries.observe(viewLifecycleOwner) { entries ->
            val config = RelayConfig(requireContext())
            val visible = if (config.hideUnknownSender) {
                entries.filter { it.sender != LogEntry.UNKNOWN_SENDER }
            } else {
                entries
            }
            adapter.submitList(visible)
            binding.tvEmptyLog.visibility = if (visible.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
