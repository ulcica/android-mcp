package de.cadeda.mcp.adb

import de.cadeda.mcp.model.uihierarchy.CommandResult
import de.cadeda.mcp.model.uihierarchy.LayoutInspectorError
import de.cadeda.mcp.model.AndroidMcpConstants.AdbPaths
import de.cadeda.mcp.model.AndroidMcpConstants.ErrorMessages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Responsible for discovering and caching the ADB executable path.
 * Extracted from AdbManager to follow Single Responsibility Principle.
 */
class AdbPathResolver {
    @Volatile
    private var cachedAdbPath: String? = null

    companion object {
        // Using centralized ADB paths from AndroidMcpConstants
    }

    suspend fun findAdbPath(): String {
        cachedAdbPath?.let { return it }

        // Try default ADB command first
        tryDefaultAdbCommand()?.let { return it }
        
        // Try fallback paths
        tryFallbackPaths()?.let { return it }

        throw LayoutInspectorError.AdbNotFound(ErrorMessages.ADB_NOT_FOUND)
    }

    private suspend fun tryDefaultAdbCommand(): String? {
        return try {
            val result = executeVersionCommand(AdbPaths.DEFAULT_COMMAND)
            if (result.isSuccess) {
                cachedAdbPath = AdbPaths.DEFAULT_COMMAND
                AdbPaths.DEFAULT_COMMAND
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun tryFallbackPaths(): String? {
        for (path in AdbPaths.FALLBACK_PATHS) {
            if (File(path).exists()) {
                val validPath = tryAdbPath(path)
                if (validPath != null) return validPath
            }
        }
        return null
    }

    private suspend fun tryAdbPath(path: String): String? {
        return try {
            val result = executeVersionCommand(path)
            if (result.isSuccess) {
                cachedAdbPath = path
                path
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun executeVersionCommand(adbPath: String): Result<CommandResult> = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder(adbPath, "version")
                .redirectErrorStream(false)
                .start()

            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                Result.success(CommandResult(stdout, stderr))
            } else {
                Result.failure(de.cadeda.mcp.model.uihierarchy.LayoutInspectorError.AdbNotFound("ADB command failed with exit code $exitCode: $stderr"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Clears the cached ADB path. Useful for testing or when ADB location changes.
     */
    fun clearCache() {
        cachedAdbPath = null
    }
}