package com.autorelay.app.data

data class LogEntry(
    val id: Long,
    val timestamp: Long,
    val sender: String,
    val message: String,
    val source: Source,
    val actions: List<String>
) {
    enum class Source { SMS, RCS }
}
