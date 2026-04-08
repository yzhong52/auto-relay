package com.autorelay.app

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * In-memory log of received messages and the actions triggered by them.
 * Entries are lost when the process is killed; persistence can be added later.
 */
object RelayLog {
    private val _entries = MutableLiveData<List<LogEntry>>(emptyList())
    val entries: LiveData<List<LogEntry>> = _entries

    private var nextId = 0L

    @Synchronized
    fun add(
        sender: String,
        message: String,
        source: LogEntry.Source,
        actions: List<String>
    ) {
        val entry = LogEntry(
            id = nextId++,
            timestamp = System.currentTimeMillis(),
            sender = sender,
            message = message,
            source = source,
            actions = actions
        )
        val current = _entries.value ?: emptyList()
        _entries.postValue(listOf(entry) + current)
    }
}
