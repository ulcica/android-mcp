package de.cadeda.mcp.adb

import de.cadeda.mcp.model.AndroidMcpConstants
import de.cadeda.mcp.model.uihierarchy.CommandResult
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.ConcurrentHashMap

/**
 * Interface for executing shell commands on Android devices.
 */
interface ShellCommandExecutor {
    suspend fun executeShellCommand(deviceId: String, command: String): CommandResult
    fun cleanup()
}

/**
 * Implementation that maintains persistent ADB shell sessions for performance.
 * Handles connection pooling, session lifecycle, and fallback to direct execution.
 */
class PersistentShellCommandExecutor(
    private val adbPathResolver: AdbPathResolver
) : ShellCommandExecutor {
    
    companion object {
        private val CONNECTION_TIMEOUT = AndroidMcpConstants.Timing.CONNECTION_TIMEOUT_MS
        private val COMMAND_TIMEOUT = AndroidMcpConstants.Timing.COMMAND_TIMEOUT_MS
        private val POLL_INTERVAL = AndroidMcpConstants.Input.SHELL_COMMAND_POLL_INTERVAL_MS
        private val SESSION_INIT_TIMEOUT = AndroidMcpConstants.Timing.ADB_SESSION_INIT_TIMEOUT_MS
    }
    
    private val adbShellSessions = ConcurrentHashMap<String, AdbShellSession>()
    
    private data class AdbShellSession(
        val deviceId: String,
        val process: Process,
        val writer: BufferedWriter,
        val reader: BufferedReader,
        var lastUsed: Long,
        var isAlive: Boolean = true,
        val mutex: Mutex = Mutex()
    )
    
    override suspend fun executeShellCommand(deviceId: String, command: String): CommandResult {
        return try {
            executeViaPersistentConnection(deviceId, command)
        } catch (e: Exception) {
            // Log the exception for debugging
            System.err.println("Persistent connection failed for command '$command': ${e.message}")
            // Fallback to direct execution
            executeDirectCommand(deviceId, command)
        }
    }
    
    private suspend fun executeViaPersistentConnection(deviceId: String, command: String): CommandResult = withContext(Dispatchers.IO) {
        val session = getOrCreateSession(deviceId)
        
        session.mutex.withLock {
            if (!session.isAlive || !session.process.isAlive) {
                // Session died, remove it and create new one
                adbShellSessions.remove(deviceId)
                return@withContext executeViaPersistentConnection(deviceId, command)
            }
            
            try {
                executeCommandInSession(session, command)
            } catch (e: Exception) {
                session.isAlive = false
                adbShellSessions.remove(deviceId)
                throw e
            }
        }
    }
    
    private suspend fun executeCommandInSession(session: AdbShellSession, command: String): CommandResult {
        // Use unique markers to identify command boundaries
        val startMarker = "CMD_START_${System.nanoTime()}"
        val endMarker = "CMD_END_${System.nanoTime()}"
        
        // Send command with markers
        session.writer.write("echo '$startMarker'\n")
        session.writer.write("$command\n")
        session.writer.write("echo '$endMarker'\n")
        session.writer.flush()
        
        // Read response between markers
        val output = StringBuilder()
        val startTime = System.currentTimeMillis()
        var foundStart = false
        var foundEnd = false
        
        while (!foundEnd && System.currentTimeMillis() - startTime < COMMAND_TIMEOUT) {
            if (session.reader.ready()) {
                val line = session.reader.readLine()
                if (line == null) {
                    // Connection closed
                    session.isAlive = false
                    throw de.cadeda.mcp.model.uihierarchy.LayoutInspectorError.ConnectionClosed(
                        AndroidMcpConstants.ErrorMessages.CONNECTION_CLOSED,
                        session.deviceId
                    )
                }
                
                when {
                    line.contains(startMarker) -> {
                        foundStart = true
                    }
                    line.contains(endMarker) -> {
                        foundEnd = true
                    }
                    foundStart && !foundEnd -> {
                        output.appendLine(line)
                    }
                }
            } else {
                delay(POLL_INTERVAL)
            }
        }
        
        if (!foundEnd) {
            throw de.cadeda.mcp.model.uihierarchy.LayoutInspectorError.CommandTimeout(
                "${AndroidMcpConstants.ErrorMessages.COMMAND_TIMEOUT}: $command",
                session.deviceId
            )
        }
        
        session.lastUsed = System.currentTimeMillis()
        return CommandResult(output.toString().trim(), "")
    }
    
    private suspend fun getOrCreateSession(deviceId: String): AdbShellSession = withContext(Dispatchers.IO) {
        cleanupDeadSessions()
        
        // Try to reuse existing session
        getExistingSession(deviceId)?.let { return@withContext it }
        
        // Create new session if none exists or existing one is dead
        createNewSession(deviceId)
    }
    
    /**
     * Attempts to get and validate an existing session for the device.
     * Returns null if no valid session exists.
     */
    private fun getExistingSession(deviceId: String): AdbShellSession? {
        val session = adbShellSessions[deviceId] ?: return null
        
        return if (session.isAlive && session.process.isAlive) {
            session.lastUsed = System.currentTimeMillis()
            System.err.println("Reusing existing ADB shell session for device: $deviceId")
            session
        } else {
            // Remove dead session
            adbShellSessions.remove(deviceId)
            null
        }
    }
    
    /**
     * Creates a new ADB shell session for the specified device.
     */
    private suspend fun createNewSession(deviceId: String): AdbShellSession {
        System.err.println("Creating new ADB shell session for device: $deviceId")
        
        val process = startAdbShellProcess(deviceId)
        val writer = BufferedWriter(OutputStreamWriter(process.outputStream))
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        
        waitForSessionReady(writer, reader)
        
        val session = AdbShellSession(
            deviceId = deviceId,
            process = process,
            writer = writer,
            reader = reader,
            lastUsed = System.currentTimeMillis()
        )
        
        adbShellSessions[deviceId] = session
        System.err.println("ADB shell session created successfully for device: $deviceId")
        return session
    }
    
    /**
     * Starts the ADB shell process for the given device.
     */
    private suspend fun startAdbShellProcess(deviceId: String): Process {
        val adbPath = adbPathResolver.findAdbPath()
        val deviceArg = if (deviceId.isNotEmpty()) arrayOf("-s", deviceId) else emptyArray()
        val fullCommand = arrayOf(adbPath) + deviceArg + arrayOf("shell")
        
        return ProcessBuilder(*fullCommand)
            .redirectErrorStream(false)
            .start()
    }
    
    /**
     * Waits for the ADB shell session to be ready by sending an echo command.
     */
    private suspend fun waitForSessionReady(writer: BufferedWriter, reader: BufferedReader) {
        writer.write("echo 'ADB_READY'\n")
        writer.flush()
        
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < SESSION_INIT_TIMEOUT) {
            if (reader.ready()) {
                val line = reader.readLine()
                if (line?.contains("ADB_READY") == true) {
                    return
                }
            }
            delay(POLL_INTERVAL)
        }
    }
    
    private suspend fun executeDirectCommand(deviceId: String, command: String): CommandResult {
        val adbPath = adbPathResolver.findAdbPath()
        val deviceArg = if (deviceId.isNotEmpty()) arrayOf("-s", deviceId) else emptyArray()
        
        val fullCommand = arrayOf(adbPath) + deviceArg + arrayOf("shell", command)
        val result = executeCommand(*fullCommand)
        
        return result.getOrElse {
            CommandResult("", it.message ?: "Command failed")
        }
    }
    
    private suspend fun executeCommand(vararg command: String): Result<CommandResult> = withContext(Dispatchers.IO) {
        try {
            val process = ProcessBuilder(*command)
                .redirectErrorStream(false)
                .start()
            
            val stdout = process.inputStream.bufferedReader().readText()
            val stderr = process.errorStream.bufferedReader().readText()
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                Result.success(CommandResult(stdout, stderr))
            } else {
                Result.failure(de.cadeda.mcp.model.uihierarchy.LayoutInspectorError.ShellCommandFailed("Command failed with exit code $exitCode: $stderr"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun cleanupDeadSessions() {
        val now = System.currentTimeMillis()
        val sessionsToRemove = mutableListOf<String>()
        
        for ((deviceId, session) in adbShellSessions) {
            if (!session.isAlive || !session.process.isAlive || 
                (now - session.lastUsed > CONNECTION_TIMEOUT)) {
                sessionsToRemove.add(deviceId)
                try {
                    session.process.destroyForcibly()
                } catch (e: Exception) {
                    // Ignore cleanup errors
                }
            }
        }
        
        for (deviceId in sessionsToRemove) {
            adbShellSessions.remove(deviceId)
        }
    }
    
    override fun cleanup() {
        // Cleanup ADB shell sessions
        for ((_, session) in adbShellSessions) {
            try {
                session.writer.close()
                session.reader.close()
                session.process.destroyForcibly()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
        adbShellSessions.clear()
    }
}