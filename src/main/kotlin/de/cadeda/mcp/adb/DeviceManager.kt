package de.cadeda.mcp.adb

import de.cadeda.mcp.model.uihierarchy.AndroidDevice
import de.cadeda.mcp.model.uihierarchy.CommandResult
import de.cadeda.mcp.model.uihierarchy.DeviceState
import de.cadeda.mcp.model.uihierarchy.LayoutInspectorError
import de.cadeda.mcp.model.AndroidMcpConstants.ErrorMessages
import de.cadeda.mcp.model.AndroidMcpConstants.Patterns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Interface for managing Android devices connected via ADB.
 * Follows Dependency Inversion Principle for better testability.
 */
interface DeviceManager {
    suspend fun getDevices(): List<AndroidDevice>
    suspend fun getFirstAvailableDevice(): AndroidDevice
}

/**
 * Default implementation of DeviceManager that uses ADB to discover devices.
 * Handles device caching for performance optimization.
 */
class DefaultDeviceManager(
    private val adbPathResolver: AdbPathResolver
) : DeviceManager {
    
    companion object {
        private const val DEVICE_CACHE_TTL = 5_000L // 5 seconds
    }
    
    @Volatile
    private var cachedDevices: List<AndroidDevice>? = null
    @Volatile
    private var devicesLastFetched: Long = 0
    private val deviceConnections = ConcurrentHashMap<String, DeviceConnection>()
    
    private data class DeviceConnection(
        val deviceId: String,
        var lastUsed: Long
    )
    
    override suspend fun getDevices(): List<AndroidDevice> {
        val now = System.currentTimeMillis()
        
        // Return cached devices if still valid
        cachedDevices?.let { devices ->
            if (now - devicesLastFetched < DEVICE_CACHE_TTL) {
                return devices
            }
        }
        
        val adbPath = adbPathResolver.findAdbPath()
        val result = executeCommand(adbPath, "devices", "-l")
        
        if (result.isFailure) {
            throw LayoutInspectorError.UnknownError(
                "Failed to get devices: ${result.exceptionOrNull()?.message}"
            )
        }
        
        val devices = parseDevicesOutput(result.getOrThrow().stdout)
        
        // Cache the results
        cachedDevices = devices
        devicesLastFetched = now
        
        return devices
    }
    
    override suspend fun getFirstAvailableDevice(): AndroidDevice {
        val devices = getDevices()
        return devices.find { it.state == DeviceState.DEVICE }
            ?: throw LayoutInspectorError.DeviceNotFound(ErrorMessages.NO_DEVICES_FOUND, null)
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
                Result.failure(Exception("Command failed with exit code $exitCode: $stderr"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun parseDevicesOutput(stdout: String): List<AndroidDevice> {
        val lines = stdout.trim().split('\n').drop(1) // Skip first line "List of devices attached"
        
        return lines.mapNotNull { line ->
            if (line.trim().isNotEmpty()) {
                val parts = line.trim().split(Regex("\\s+"))
                val id = parts[0]
                val state = when (parts[1]) {
                    "device" -> DeviceState.DEVICE
                    "offline" -> DeviceState.OFFLINE
                    "unauthorized" -> DeviceState.UNAUTHORIZED
                    else -> DeviceState.OFFLINE
                }
                
                // Extract model if available
                val modelMatch = Regex(Patterns.MODEL_EXTRACTION).find(line)
                val model = modelMatch?.groupValues?.get(1)
                
                AndroidDevice(id, model, state)
            } else null
        }
    }
    
    /**
     * Clears the device cache. Useful for testing or when devices change.
     */
    fun clearCache() {
        cachedDevices = null
        devicesLastFetched = 0
    }
    
    /**
     * Cleanup method for resource management.
     */
    fun cleanup() {
        deviceConnections.clear()
        clearCache()
    }
}