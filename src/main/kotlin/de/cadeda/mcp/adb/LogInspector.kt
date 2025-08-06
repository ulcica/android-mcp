package de.cadeda.mcp.adb

import de.cadeda.mcp.model.uihierarchy.LayoutInspectorError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Interface for inspecting Android device logs with filtering capabilities.
 */
interface LogInspector {
    suspend fun getFilteredLogs(deviceId: String, packageName: String?, maxLines: Int, priority: String?): List<LogEntry>
    suspend fun clearLogs(deviceId: String): Boolean
}

/**
 * Data class representing a single log entry.
 */
@kotlinx.serialization.Serializable
data class LogEntry(
    val timestamp: String,
    val processId: String,
    val threadId: String,
    val priority: String,
    val tag: String,
    val message: String,
    val packageName: String?
)

/**
 * Default implementation of LogInspector that uses ADB logcat commands.
 */
class DefaultLogInspector(
    private val shellCommandExecutor: ShellCommandExecutor,
    private val adbPathResolver: AdbPathResolver
) : LogInspector {
    
    companion object {
        private val PRIORITY_MAP = mapOf(
            "V" to "VERBOSE",
            "D" to "DEBUG", 
            "I" to "INFO",
            "W" to "WARN",
            "E" to "ERROR",
            "F" to "FATAL"
        )
    }
    
    override suspend fun getFilteredLogs(
        deviceId: String, 
        packageName: String?, 
        maxLines: Int, 
        priority: String?
    ): List<LogEntry> {
        return try {
            val logOutput = executeLogcatCommand(deviceId, packageName, maxLines, priority)
            parseLogOutput(logOutput, packageName)
        } catch (e: Exception) {
            throw LayoutInspectorError.ShellCommandFailed("Failed to get logs: ${e.message}", deviceId)
        }
    }
    
    override suspend fun clearLogs(deviceId: String): Boolean {
        return try {
            shellCommandExecutor.executeShellCommand(deviceId, "logcat -c")
            true // Command succeeded if no exception was thrown
        } catch (_: Exception) {
            false
        }
    }
    
    private suspend fun executeLogcatCommand(
        deviceId: String, 
        packageName: String?, 
        maxLines: Int, 
        priority: String?
    ): String = withContext(Dispatchers.IO) {
        val adbPath = adbPathResolver.findAdbPath()
        val deviceArg = if (deviceId.isNotEmpty()) arrayOf("-s", deviceId) else emptyArray()
        
        // Build logcat command with filters
        val logcatArgs = mutableListOf<String>().apply {
            add("logcat")
            add("-v")
            add("threadtime") // Include timestamp, PID, TID, priority, tag, message
            add("-t") 
            add(maxLines.toString()) // Tail last N lines
            
            // Add priority filter if specified
            priority?.let { p ->
                if (PRIORITY_MAP.containsKey(p.uppercase()) || PRIORITY_MAP.containsValue(p.uppercase())) {
                    add("*:${p.uppercase()}")
                }
            }
            
            // Add package filter if specified
            packageName?.let { pkg ->
                add("--pid=\$(pidof $pkg)")
            }
        }
        
        val fullCommand = arrayOf(adbPath) + deviceArg + logcatArgs.toTypedArray()
        
        try {
            val process = ProcessBuilder(*fullCommand)
                .redirectErrorStream(false)
                .start()
            
            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            if (exitCode != 0 && stderr.isNotEmpty()) {
                // Try alternative approach without pidof if it failed
                if (packageName != null && stderr.contains("pidof")) {
                    return@withContext executeLogcatWithTagFilter(deviceId, packageName, maxLines, priority)
                }
                throw Exception("logcat failed with exit code $exitCode: $stderr")
            }
            
            return@withContext stdout
            
        } catch (e: Exception) {
            throw LayoutInspectorError.ShellCommandFailed("Failed to execute logcat: ${e.message}", deviceId)
        }
    }
    
    private suspend fun executeLogcatWithTagFilter(
        deviceId: String,
        packageName: String,
        maxLines: Int,
        priority: String?
    ): String = withContext(Dispatchers.IO) {
        val adbPath = adbPathResolver.findAdbPath()
        val deviceArg = if (deviceId.isNotEmpty()) arrayOf("-s", deviceId) else emptyArray()
        
        // Fallback: use tag-based filtering (less precise but more compatible)
        val logcatArgs = mutableListOf<String>().apply {
            add("logcat")
            add("-v")
            add("threadtime")
            add("-t")
            add(maxLines.toString())
            
            // Add priority filter if specified
            priority?.let { p ->
                if (PRIORITY_MAP.containsKey(p.uppercase()) || PRIORITY_MAP.containsValue(p.uppercase())) {
                    add("*:${p.uppercase()}")
                }
            }
        }
        
        val fullCommand = arrayOf(adbPath) + deviceArg + logcatArgs.toTypedArray()
        
        val process = ProcessBuilder(*fullCommand)
            .redirectErrorStream(false)
            .start()
        
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        
        if (exitCode != 0) {
            throw Exception("logcat fallback failed with exit code $exitCode: $stderr")
        }
        
        // Filter by package name in post-processing
        return@withContext filterLogsByPackage(stdout, packageName)
    }
    
    private fun filterLogsByPackage(logOutput: String, packageName: String): String {
        return logOutput.lines()
            .filter { line ->
                // Simple heuristic: check if line contains package name
                line.contains(packageName, ignoreCase = true)
            }
            .joinToString("\n")
    }
    
    private fun parseLogOutput(logOutput: String, filterPackageName: String?): List<LogEntry> {
        val entries = mutableListOf<LogEntry>()
        val lines = logOutput.lines().filter { it.isNotBlank() }
        
        for (line in lines) {
            try {
                parseLogLine(line, filterPackageName)?.let { entry ->
                    entries.add(entry)
                }
            } catch (_: Exception) {
                // Skip malformed lines
                continue
            }
        }
        
        return entries
    }
    
    private fun parseLogLine(line: String, filterPackageName: String?): LogEntry? {
        // Parse threadtime format: MM-dd HH:mm:ss.SSS PID TID Priority Tag: Message
        val threadTimeRegex = Regex(
            """(\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d{3})\s+(\d+)\s+(\d+)\s+([VDIWEF])\s+([^:]+):\s*(.*)""" 
        )
        
        val match = threadTimeRegex.find(line.trim()) ?: return null
        
        val timestamp = match.groupValues[1]
        val pid = match.groupValues[2]
        val tid = match.groupValues[3]
        val priority = PRIORITY_MAP[match.groupValues[4]] ?: match.groupValues[4]
        val tag = match.groupValues[5].trim()
        val message = match.groupValues[6].trim()
        
        return LogEntry(
            timestamp = timestamp,
            processId = pid,
            threadId = tid,
            priority = priority,
            tag = tag,
            message = message,
            packageName = filterPackageName
        )
    }
}