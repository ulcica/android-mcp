package de.cadeda.mcp.adb

import de.cadeda.mcp.model.AndroidResult
import de.cadeda.mcp.model.uihierarchy.AndroidApp
import de.cadeda.mcp.model.uihierarchy.AndroidDevice
import de.cadeda.mcp.model.uihierarchy.CurrentActivity

/**
 * Facade coordinating specialized components.
 */
class AdbManager private constructor(
    private val deviceManager: DeviceManager,
    private val uiInspector: UIInspector,
    private val inputController: DeviceInputController,
    private val logInspector: LogInspector
) {
    companion object {
        @Volatile
        private var INSTANCE: AdbManager? = null
        
        fun getInstance(): AdbManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: createDefaultInstance().also { INSTANCE = it }
            }
        }
        
        /**
         * Creates the default AdbManager instance with all dependencies wired up.
         */
        private fun createDefaultInstance(): AdbManager {
            val adbPathResolver = AdbPathResolver()
            val deviceManager = DefaultDeviceManager(adbPathResolver)
            val shellCommandExecutor = PersistentShellCommandExecutor(adbPathResolver)
            val uiInspector = DefaultUIInspector(shellCommandExecutor, adbPathResolver)
            val inputController = DefaultDeviceInputController(shellCommandExecutor, adbPathResolver)
            val logInspector = DefaultLogInspector(shellCommandExecutor, adbPathResolver)
            
            return AdbManager(deviceManager, uiInspector, inputController, logInspector)
        }
        
        /**
         * Factory method for dependency injection in tests.
         */
        fun createInstance(
            deviceManager: DeviceManager,
            uiInspector: UIInspector,
            inputController: DeviceInputController,
            logInspector: LogInspector
        ): AdbManager {
            return AdbManager(deviceManager, uiInspector, inputController, logInspector)
        }
    }
    
    // Device Management - delegates to DeviceManager
    suspend fun getDevices(): List<AndroidDevice> {
        return deviceManager.getDevices()
    }
    
    suspend fun getAppList(deviceId: String? = null, includeSystemApps: Boolean = false): List<AndroidApp> {
        val targetDevice = deviceId ?: getFirstAvailableDeviceId()
        return deviceManager.getAppList(targetDevice, includeSystemApps)
    }
    
    // UI Inspection - delegates to UIInspector
    suspend fun getViewAttributes(deviceId: String? = null): String {
        val targetDevice = deviceId ?: getFirstAvailableDeviceId()
        return uiInspector.getViewAttributes(targetDevice)
    }
    
    suspend fun getViewHierarchy(deviceId: String? = null): String {
        val targetDevice = deviceId ?: getFirstAvailableDeviceId()
        return uiInspector.getViewHierarchy(targetDevice)
    }
    
    suspend fun getCurrentActivity(deviceId: String? = null): CurrentActivity? {
        val targetDevice = deviceId ?: getFirstAvailableDeviceId()
        return uiInspector.getCurrentActivity(targetDevice)
    }
    
    // Input Control - delegates to DeviceInputController
    suspend fun clickCoordinate(x: Int, y: Int, deviceId: String? = null) {
        val targetDevice = deviceId ?: getFirstAvailableDeviceId()
        inputController.clickCoordinate(x, y, targetDevice)
    }
    
    suspend fun longPressCoordinate(x: Int, y: Int, duration: Int = 1000, deviceId: String? = null) {
        val targetDevice = deviceId ?: getFirstAvailableDeviceId()
        inputController.longPressCoordinate(x, y, duration, targetDevice)
    }
    
    suspend fun dragCoordinate(
        startX: Int, startY: Int, endX: Int, endY: Int,
        duration: Int = 300, deviceId: String? = null
    ) {
        val targetDevice = deviceId ?: getFirstAvailableDeviceId()
        inputController.dragCoordinate(startX, startY, endX, endY, duration, targetDevice)
    }
    
    suspend fun swipeCoordinate(
        startX: Int, startY: Int, endX: Int, endY: Int, 
        duration: Int = 300, deviceId: String? = null
    ) {
        val targetDevice = deviceId ?: getFirstAvailableDeviceId()
        inputController.swipeCoordinate(startX, startY, endX, endY, duration, targetDevice)
    }
    
    /**
     * Overload with reduced parameter count using SwipeParams
     */
    suspend fun swipeCoordinate(params: de.cadeda.mcp.model.SwipeParams, deviceId: String? = null) {
        val targetDevice = deviceId ?: getFirstAvailableDeviceId()
        inputController.swipeCoordinate(params, targetDevice)
    }
    
    suspend fun inputText(text: String, deviceId: String? = null) {
        val targetDevice = deviceId ?: getFirstAvailableDeviceId()
        inputController.inputText(text, targetDevice)
    }
    
    suspend fun sendKeyEvent(keyCode: Int, deviceId: String? = null) {
        val targetDevice = deviceId ?: getFirstAvailableDeviceId()
        inputController.sendKeyEvent(keyCode, targetDevice)
    }
    
    suspend fun startIntent(
        action: String? = null,
        category: String? = null,
        dataUri: String? = null,
        packageName: String? = null,
        className: String? = null,
        extras: Map<String, String> = emptyMap(),
        deviceId: String? = null
    ): Result<AndroidResult> {
        val targetDevice = deviceId ?: getFirstAvailableDeviceId()
        return inputController.startIntent(action, category, dataUri, packageName, className, extras, targetDevice)
    }
    
    // Log Inspection - delegates to LogInspector
    suspend fun getLogs(
        packageName: String? = null,
        maxLines: Int = 100,
        priority: String? = null,
        deviceId: String? = null
    ): List<LogEntry> {
        val targetDevice = deviceId ?: getFirstAvailableDeviceId()
        return logInspector.getFilteredLogs(targetDevice, packageName, maxLines, priority)
    }
    
    suspend fun clearLogs(deviceId: String? = null): Boolean {
        val targetDevice = deviceId ?: getFirstAvailableDeviceId()
        return logInspector.clearLogs(targetDevice)
    }
    
    // Helper method to get first available device ID
    private suspend fun getFirstAvailableDeviceId(): String {
        return deviceManager.getFirstAvailableDevice().id
    }
    
    /**
     * Cleanup resources. Should be called when shutting down.
     */
    fun cleanup() {
        if (deviceManager is DefaultDeviceManager) {
            deviceManager.cleanup()
        }
        
        // Cleanup shell command executor if it's the persistent type
        if (inputController is DefaultDeviceInputController) {
            inputController.cleanup()
        }
        
        // Clear singleton instance
        synchronized(AdbManager::class.java) {
            INSTANCE = null
        }
    }
}