package com.autorelay.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.autorelay.app.databinding.ItemLogEntryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogAdapter : ListAdapter<LogEntry, LogAdapter.ViewHolder>(DiffCallback) {

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

    inner class ViewHolder(private val binding: ItemLogEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: LogEntry) {
            binding.tvSender.text = entry.sender
            binding.tvTimestamp.text = formatTimestamp(entry.timestamp)
            val ctx = binding.root.context
            binding.tvSource.text = when (entry.source) {
                LogEntry.Source.SMS -> ctx.getString(R.string.label_source_sms)
                LogEntry.Source.RCS -> ctx.getString(R.string.label_source_rcs)
            }
            binding.tvMessagePreview.text = entry.message
            binding.tvActions.text = if (entry.actions.isEmpty()) {
                ctx.getString(R.string.log_no_actions)
            } else {
                entry.actions.joinToString("\n") { "→ $it" }
            }
        }

        private fun formatTimestamp(millis: Long): String {
            val date = Date(millis)
            val isToday = System.currentTimeMillis() - millis < 24 * 60 * 60 * 1000L
            return if (isToday) timeFormat.format(date) else dateTimeFormat.format(date)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLogEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    private object DiffCallback : DiffUtil.ItemCallback<LogEntry>() {
        override fun areItemsTheSame(oldItem: LogEntry, newItem: LogEntry) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: LogEntry, newItem: LogEntry) = oldItem == newItem
    }
}
