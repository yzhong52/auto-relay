package com.autorelay.app.data

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

object RelayLog {
    private const val TAG = "AutoRelay"
    private const val FILE_NAME = "relay_log.json"
    private const val MAX_AGE_MS = 14L * 24 * 60 * 60 * 1000

    private val _entries = MutableLiveData<List<LogEntry>>(emptyList())
    val entries: LiveData<List<LogEntry>> = _entries

    private val entriesList = mutableListOf<LogEntry>()
    private var nextId = 0L
    private lateinit var appContext: Context
    private val gson = Gson()

    fun init(context: Context) {
        appContext = context.applicationContext
        Thread { loadFromDisk() }.start()
    }

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
        entriesList.add(0, entry)
        _entries.postValue(entriesList.toList())
        saveToDisk()
    }

    @Synchronized
    fun clear() {
        entriesList.clear()
        _entries.postValue(emptyList())
        saveToDisk()
    }

    @Synchronized
    private fun loadFromDisk() {
        val file = File(appContext.filesDir, FILE_NAME)
        if (!file.exists()) return
        try {
            val type = object : TypeToken<List<LogEntry>>() {}.type
            val all: List<LogEntry> = gson.fromJson(file.readText(), type) ?: emptyList()
            val cutoff = System.currentTimeMillis() - MAX_AGE_MS
            val recent = all.filter { it.timestamp > cutoff }
            entriesList.clear()
            entriesList.addAll(recent)
            nextId = (recent.maxOfOrNull { it.id } ?: -1L) + 1
            _entries.postValue(recent)
            if (recent.size != all.size) saveToDisk()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load relay log from disk", e)
        }
    }

    private fun saveToDisk() {
        try {
            File(appContext.filesDir, FILE_NAME).writeText(gson.toJson(entriesList))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save relay log to disk", e)
        }
    }
}
