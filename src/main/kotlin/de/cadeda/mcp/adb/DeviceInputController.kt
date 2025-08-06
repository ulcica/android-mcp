package de.cadeda.mcp.adb

import de.cadeda.mcp.model.AndroidMcpConstants.Input
import de.cadeda.mcp.model.SwipeParams
import de.cadeda.mcp.model.uihierarchy.LayoutInspectorError

/**
 * Interface for controlling input operations on Android devices.
 */
interface DeviceInputController {
    suspend fun clickCoordinate(x: Int, y: Int, deviceId: String)
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
