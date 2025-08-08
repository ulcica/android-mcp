package de.cadeda.mcp.adb

import de.cadeda.mcp.model.AndroidMcpConstants.Input
import de.cadeda.mcp.model.AndroidResult
import de.cadeda.mcp.model.SwipeParams
import de.cadeda.mcp.model.uihierarchy.LayoutInspectorError

/**
 * Interface for controlling input operations on Android devices.
 */
interface DeviceInputController {
    suspend fun clickCoordinate(x: Int, y: Int, deviceId: String)
    suspend fun longPressCoordinate(x: Int, y: Int, duration: Int, deviceId: String)
    suspend fun dragCoordinate(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        duration: Int,
        deviceId: String
    )
    suspend fun swipeCoordinate(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        duration: Int,
        deviceId: String
    )
    suspend fun inputText(text: String, deviceId: String)
    suspend fun sendKeyEvent(keyCode: Int, deviceId: String)
    suspend fun startIntent(
        action: String? = null,
        category: String? = null,
        dataUri: String? = null,
        packageName: String? = null,
        className: String? = null,
        extras: Map<String, String> = emptyMap(),
        deviceId: String
    ): Result<AndroidResult>

    /**
     * Overload with reduced parameter count using SwipeParams
     */
    suspend fun swipeCoordinate(params: SwipeParams, deviceId: String) {
        swipeCoordinate(params.startX, params.startY, params.endX, params.endY, params.duration, deviceId)
    }
}

/**
 * Default implementation of DeviceInputController that uses ADB shell commands.
 * Handles all input operations including taps, swipes, text input, and key events.
 */
class DefaultDeviceInputController(
    private val shellCommandExecutor: ShellCommandExecutor,
    private val adbPathResolver: AdbPathResolver
) : DeviceInputController {

    /**
     * Cleanup resources. Delegates to shell command executor.
     */
    fun cleanup() {
        shellCommandExecutor.cleanup()
    }

    companion object {
        // Using centralized input constants
    }

    override suspend fun clickCoordinate(x: Int, y: Int, deviceId: String) {
        try {
            shellCommandExecutor.executeShellCommand(deviceId, "input tap $x $y")
        } catch (e: Exception) {
            throw LayoutInspectorError.UnknownError(
                "Failed to click at coordinates ($x, $y): ${e.message}",
                deviceId
            )
        }
    }

    override suspend fun longPressCoordinate(x: Int, y: Int, duration: Int, deviceId: String) {
        val pressDuration = if (duration > 0) duration else Input.DEFAULT_LONG_PRESS_DURATION_MS

        try {
            // Long press is implemented as a swipe with the same start and end coordinates
            shellCommandExecutor.executeShellCommand(
                deviceId,
                "input swipe $x $y $x $y $pressDuration"
            )
        } catch (e: Exception) {
            throw LayoutInspectorError.UnknownError(
                "Failed to long press at coordinates ($x, $y): ${e.message}",
                deviceId
            )
        }
    }

    override suspend fun dragCoordinate(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        duration: Int,
        deviceId: String
    ) {
        val dragDuration = if (duration > 0) duration else Input.DEFAULT_SWIPE_DURATION_MS
        try {
            shellCommandExecutor.executeShellCommand(
                deviceId,
                "input draganddrop $startX $startY $endX $endY $dragDuration"
            )
        } catch (e: Exception) {
            throw LayoutInspectorError.UnknownError(
                "Failed to drag from ($startX, $startY) to ($endX, $endY): ${e.message}",
                deviceId
            )
        }
    }

    override suspend fun swipeCoordinate(
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        duration: Int,
        deviceId: String
    ) {
        val swipeDuration = if (duration > 0) duration else Input.DEFAULT_SWIPE_DURATION_MS

        try {
            shellCommandExecutor.executeShellCommand(
                deviceId,
                "input swipe $startX $startY $endX $endY $swipeDuration"
            )
        } catch (e: Exception) {
            throw LayoutInspectorError.UnknownError(
                "Failed to swipe from ($startX, $startY) to ($endX, $endY): ${e.message}",
                deviceId
            )
        }
    }

    override suspend fun inputText(text: String, deviceId: String) {
        try {
            // Escape special characters and spaces for shell input
            val escapedText = text.replace("'", "\\'").replace(" ", "%s")
            shellCommandExecutor.executeShellCommand(deviceId, "input text '$escapedText'")
        } catch (e: Exception) {
            throw LayoutInspectorError.UnknownError(
                "Failed to input text \"$text\": ${e.message}",
                deviceId
            )
        }
    }

    override suspend fun sendKeyEvent(keyCode: Int, deviceId: String) {
        try {
            shellCommandExecutor.executeShellCommand(deviceId, "input keyevent $keyCode")
        } catch (e: Exception) {
            throw LayoutInspectorError.UnknownError(
                "Failed to send key event $keyCode: ${e.message}",
                deviceId
            )
        }
    }

    override suspend fun startIntent(
        action: String?,
        category: String?,
        dataUri: String?,
        packageName: String?,
        className: String?,
        extras: Map<String, String>,
        deviceId: String
    ): Result<AndroidResult> {
        return try {
            // Auto-discover main launcher activity if packageName is provided without className
            val resolvedClassName = if (packageName != null && className == null &&
                                       action == "android.intent.action.MAIN" &&
                                       category == "android.intent.category.LAUNCHER") {
                discoverMainActivity(packageName, deviceId)
            } else {
                className
            }

            val command = buildIntentCommand(action, category, dataUri, packageName, resolvedClassName, extras)

            shellCommandExecutor.executeShellCommand(deviceId, command)

            Result.success(
                AndroidResult(
                    success = true,
                    message = "Intent started successfully",
                    data = mapOf(
                        "command" to command,
                        "action" to (action ?: ""),
                        "packageName" to (packageName ?: ""),
                        "className" to (resolvedClassName ?: ""),
                        "category" to (category ?: ""),
                        "dataUri" to (dataUri ?: ""),
                        "extras" to extras.toString()
                    )
                )
            )
        } catch (e: Exception) {
            Result.failure(
                LayoutInspectorError.UnknownError(
                    "Failed to start intent: ${e.message}",
                    deviceId
                )
            )
        }
    }

    /**
     * Discovers the main launcher activity for a package by querying the package manager
     */
    private suspend fun discoverMainActivity(packageName: String, deviceId: String): String? {
        return try {
            // Use pm resolve-activity to get the default MAIN/LAUNCHER activity for this package
            val result = shellCommandExecutor.executeShellCommand(
                deviceId,
                "pm resolve-activity -a android.intent.action.MAIN -c android.intent.category.LAUNCHER $packageName"
            )

            // Parse the output to extract the activity class
            // Use the 'name' field as this is the actual exported launcher activity
            // The 'targetActivity' is what it routes to internally but might not be exported
            val nameMatch = Regex("""name=(\S+)""").find(result.stdout)
            val activityName = nameMatch?.groupValues?.get(1)

            // Extract the class part from full activity name
            activityName?.let { fullName ->
                // If activity name starts with package, remove the package prefix
                if (fullName.startsWith(packageName)) {
                    return fullName.substring(packageName.length)
                } else {
                    // For cross-package activities (like Google Maps), return the full name
                    return fullName
                }
            }

            null
        } catch (_: Exception) {
            null // Fallback to null if discovery fails
        }
    }

    private fun buildIntentCommand(
        action: String?,
        category: String?,
        dataUri: String?,
        packageName: String?,
        className: String?,
        extras: Map<String, String>
    ): String {
        val command = StringBuilder("am start")

        // Add action
        action?.let { command.append(" -a \"$it\"") }

        // Add category
        category?.let { command.append(" -c \"$it\"") }

        // Add data URI
        dataUri?.let { command.append(" -d \"$it\"") }

        // Add extras
        extras.forEach { (key, value) ->
            command.append(" -e \"$key\" \"$value\"")
        }

        // Add component (package/class)
        when {
            packageName != null && className != null -> {
                command.append(" \"$packageName/$className\"")
            }
            packageName != null -> {
                command.append(" \"$packageName\"")
            }
        }

        return command.toString()
    }
}

// Key codes are now centralized in AndroidMcpConstants.KeyCodes

/**
 * Extension functions for DeviceInputController to make common operations more convenient.
 */
suspend fun DeviceInputController.pressEnter(deviceId: String) {
    sendKeyEvent(AndroidKeyConstants.KEYCODE_ENTER, deviceId)
}

suspend fun DeviceInputController.pressBack(deviceId: String) {
    sendKeyEvent(AndroidKeyConstants.KEYCODE_BACK, deviceId)
}

suspend fun DeviceInputController.pressHome(deviceId: String) {
    sendKeyEvent(AndroidKeyConstants.KEYCODE_HOME, deviceId)
}
