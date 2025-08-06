package de.cadeda.mcp.adb

import de.cadeda.mcp.model.uihierarchy.CurrentActivity
import de.cadeda.mcp.model.uihierarchy.LayoutInspectorError
import de.cadeda.mcp.model.uihierarchy.WindowInfo
import de.cadeda.mcp.model.AndroidMcpConstants.Patterns
import de.cadeda.mcp.model.AndroidMcpConstants.ShellCommands
import de.cadeda.mcp.model.AndroidMcpConstants.Timing
import kotlinx.coroutines.*

/**
 * Interface for inspecting UI hierarchies and activities on Android devices.
 */
interface UIInspector {
    suspend fun getViewAttributes(deviceId: String): String
    suspend fun getViewHierarchy(deviceId: String): String
    suspend fun getCurrentActivity(deviceId: String): CurrentActivity?
}

/**
 * Default implementation of UIInspector that uses ADB shell commands.
 * Handles debug view attributes, UI dumps, and activity inspection.
 */
class DefaultUIInspector(
    private val shellCommandExecutor: ShellCommandExecutor,
    private val adbPathResolver: AdbPathResolver
) : UIInspector {
    
    companion object {
        // Using centralized timing constants
    }
    
    override suspend fun getViewAttributes(deviceId: String): String {
        try {
            // Enable debug view attributes temporarily
            shellCommandExecutor.executeShellCommand(deviceId, ShellCommands.ENABLE_DEBUG_ATTRIBUTES)
            
            // Reduced wait time for better performance
            delay(Timing.DEBUG_VIEW_ATTRIBUTES_DELAY_MS)
            
            // Get UI dump with enhanced attributes
            val xmlContent = executeUiAutomatorDump(deviceId)
            
            // Cleanup debug setting (non-blocking)
            cleanupDebugViewAttributes(deviceId)
            
            return xmlContent
            
        } catch (e: Exception) {
            throw LayoutInspectorError.UiDumpFailed("Failed to get view attributes: ${e.message}", deviceId)
        }
    }
    
    override suspend fun getViewHierarchy(deviceId: String): String {
        try {
            return executeUiAutomatorDump(deviceId)
        } catch (e: Exception) {
            throw LayoutInspectorError.UiDumpFailed("Failed to get view hierarchy: ${e.message}", deviceId)
        }
    }
    
    override suspend fun getCurrentActivity(deviceId: String): CurrentActivity? {
        return try {
            val (windowOutput, activityOutput) = getDumpsysData(deviceId)
            parseCurrentActivity(windowOutput, activityOutput)
        } catch (e: Exception) {
            // Non-critical error, return null instead of throwing
            null
        }
    }
    
    private suspend fun executeUiAutomatorDump(deviceId: String): String = withContext(Dispatchers.IO) {
        // Use exec-out approach which bypasses persistent connection issues for large XML output
        val adbPath = adbPathResolver.findAdbPath()
        val deviceArg = if (deviceId.isNotEmpty()) arrayOf("-s", deviceId) else emptyArray()
        
        val fullCommand = arrayOf(adbPath) + deviceArg + arrayOf("exec-out", ShellCommands.UI_DUMP)
        
        try {
            val process = ProcessBuilder(*fullCommand)
                .redirectErrorStream(false)
                .start()
            
            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            if (exitCode != 0) {
                throw Exception("uiautomator dump failed with exit code $exitCode: $stderr")
            }
            
            val xmlContent = extractXmlContent(stdout)
            
            if (xmlContent.isEmpty()) {
                throw LayoutInspectorError.UiDumpFailed("No UI hierarchy data received. Output: $stdout", deviceId)
            }
            
            return@withContext xmlContent
            
        } catch (e: Exception) {
            throw LayoutInspectorError.UiDumpFailed("Failed to dump UI hierarchy: ${e.message}", deviceId)
        }
    }
    
    private fun extractXmlContent(output: String): String {
        return XmlExtractionUtils.extractXmlContent(output)
    }
    
    private suspend fun getDumpsysData(deviceId: String): Pair<String, String> {
        return coroutineScope {
            val windowDeferred = async { 
                shellCommandExecutor.executeShellCommand(
                    deviceId, 
                    "dumpsys window | grep -E \"mCurrentFocus|mFocusedApp\" | head -5"
                )
            }
            val activityDeferred = async { 
                // Use faster command that targets resumed activities directly
                shellCommandExecutor.executeShellCommand(
                    deviceId, 
                    "dumpsys activity activities | grep -A2 -B1 \"topResumedActivity\\|packageName=\\|processName=\" | head -10"
                )
            }
            
            val windowResult = windowDeferred.await()
            val activityResult = activityDeferred.await()
            
            Pair(windowResult.stdout, activityResult.stdout)
        }
    }
    
    private fun parseCurrentActivity(windowOutput: String, activityOutput: String): CurrentActivity? {
        // Try to parse mCurrentFocus first (more reliable)
        parseCurrentFocus(windowOutput, activityOutput)?.let { return it }
        
        // Fallback to mFocusedApp
        return parseFocusedApp(windowOutput)
    }
    
    private fun parseCurrentFocus(windowOutput: String, activityOutput: String): CurrentActivity? {
        val currentFocusRegex = Regex(Patterns.CURRENT_FOCUS)
        val currentFocusMatch = currentFocusRegex.find(windowOutput) ?: return null
        
        val packageName = currentFocusMatch.groupValues[1]
        val activityName = currentFocusMatch.groupValues[2]
        val pid = extractPid(activityOutput)
        val windowInfo = createWindowInfo(windowOutput)
        
        return CurrentActivity(
            packageName = packageName,
            activity = activityName,
            pid = pid,
            windowInfo = windowInfo
        )
    }
    
    private fun parseFocusedApp(windowOutput: String): CurrentActivity? {
        val focusedAppRegex = Regex(Patterns.FOCUSED_APP)
        val focusedAppMatch = focusedAppRegex.find(windowOutput) ?: return null
        
        val packageName = focusedAppMatch.groupValues[1]
        val activityName = focusedAppMatch.groupValues[2]
        val normalizedActivityName = if (activityName.startsWith(".")) activityName else ".$activityName"
        
        return CurrentActivity(
            packageName = packageName,
            activity = normalizedActivityName,
            windowInfo = WindowInfo(
                focused = true,
                visible = true,
                hasInputFocus = true
            )
        )
    }
    
    private fun extractPid(activityOutput: String): Int? {
        // Try multiple patterns to extract PID
        val patterns = listOf(
            Regex(Patterns.PID_EXTRACTION),                    // Original format: pid=12345
            Regex(Patterns.ACTIVITY_RECORD), // ActivityRecord format
            Regex(Patterns.PROCESS_NAME_PID), // processName with pid
        )
        
        for (pattern in patterns) {
            val match = pattern.find(activityOutput)
            if (match != null) {
                // For the ActivityRecord pattern, we need to look for the PID in a different way
                if (pattern.pattern.contains("ActivityRecord")) {
                    // Try to find PID in nearby text
                    val pidPattern = Regex("pid=(\\d+)")
                    val pidMatch = pidPattern.find(activityOutput)
                    return pidMatch?.groupValues?.get(1)?.toIntOrNull()
                } else {
                    return match.groupValues.getOrNull(if (pattern.pattern.contains("processName") && match.groupValues.size >= 3) 2 else 1)?.toIntOrNull()
                }
            }
        }
        return null
    }
    
    private fun createWindowInfo(windowOutput: String): WindowInfo {
        return WindowInfo(
            focused = windowOutput.contains("mCurrentFocus"),
            visible = !windowOutput.contains("NOT_VISIBLE"),
            hasInputFocus = windowOutput.contains("mFocusedApp")
        )
    }
    
    private fun cleanupDebugViewAttributes(deviceId: String) {
        GlobalScope.launch {
            try {
                shellCommandExecutor.executeShellCommand(deviceId, ShellCommands.DISABLE_DEBUG_ATTRIBUTES)
            } catch (e: Exception) {
                // Ignore cleanup errors - not critical
            }
        }
    }
}