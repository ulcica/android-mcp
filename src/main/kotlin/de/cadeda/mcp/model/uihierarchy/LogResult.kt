package de.cadeda.mcp.model.uihierarchy

import de.cadeda.mcp.adb.LogEntry
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class LogResult(
    val device: String,
    val entries: List<LogEntry>,
    val count: Int,
    val filters: LogFilters,
    val timestamp: String = Instant.now().toString()
)

@Serializable
data class LogFilters(
    val packageName: String? = null,
    val maxLines: Int = 100,
    val priority: String? = null
)