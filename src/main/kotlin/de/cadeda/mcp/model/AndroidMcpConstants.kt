package de.cadeda.mcp.model

/**
 * Central location for all constants used throughout the Android MCP server.
 * Eliminates magic numbers and provides a single source of truth for configuration values.
 */
object AndroidMcpConstants {
    
    // Timing Constants
    object Timing {
        const val DEVICE_CACHE_TTL_MS = 5_000L // 5 seconds
        const val CONNECTION_TIMEOUT_MS = 30_000L // 30 seconds
        const val DEBUG_VIEW_ATTRIBUTES_DELAY_MS = 200L // milliseconds
        const val COMMAND_TIMEOUT_MS = 10_000L // 10 seconds
        const val ADB_SESSION_INIT_TIMEOUT_MS = 5_000L // 5 seconds
        const val TARGET_DEVICE_CACHE_TTL_MS = 10_000L // 10 seconds
    }
    
    // Input Constants
    object Input {
        const val DEFAULT_SWIPE_DURATION_MS = 300 // milliseconds
        const val DEFAULT_LONG_PRESS_DURATION_MS = 1000 // milliseconds (1 second)
        const val SHELL_COMMAND_POLL_INTERVAL_MS = 10L // milliseconds
    }
    
    // Android Key Codes
    object KeyCodes {
        const val ENTER = 66
        const val BACK = 4
        const val HOME = 3
        const val MENU = 82
        const val SEARCH = 84
        const val VOLUME_UP = 24
        const val VOLUME_DOWN = 25
        const val POWER = 26
        const val APP_SWITCH = 187
        const val CAMERA = 27
        const val CALL = 5
        const val ENDCALL = 6
    }
    
    // MCP Protocol Constants
    object Protocol {
        const val PROTOCOL_VERSION = "2024-11-05"
        const val SERVER_NAME = "android-mcp"
        const val INTERNAL_ERROR_CODE = -32603
    }
    
    // Tool Names
    object Tools {
        const val GET_DEVICE_LIST = "get_device_list"
        const val GET_APP_LIST = "get_app_list"
        const val GET_VIEW_ATTRIBUTES = "get_view_attributes"
        const val GET_CURRENT_ACTIVITY = "get_current_activity"
        const val FIND_ELEMENTS = "find_elements"
        const val VIEW_HIERARCHY = "view_hierarchy"
        const val CLICK_COORDINATE = "click_coordinate"
        const val LONG_PRESS_COORDINATE = "long_press_coordinate"
        const val SWIPE_COORDINATE = "swipe_coordinate"
        const val INPUT_TEXT = "input_text"
        const val KEY_EVENT = "key_event"
        const val START_INTENT = "start_intent"
        const val GET_LOGS = "get_logs"
    }
    
    // ADB Paths (platform-specific)
    object AdbPaths {
        val FALLBACK_PATHS = listOf(
            "${System.getProperty("user.home")}/Library/Android/sdk/platform-tools/adb", // macOS
            "${System.getProperty("user.home")}/Android/sdk/platform-tools/adb.exe", // Windows
            "${System.getProperty("user.home")}/Android/sdk/platform-tools/adb", // Linux
            "/usr/local/bin/adb", // Homebrew on macOS
            "/opt/android-sdk/platform-tools/adb" // Linux common location
        )
        const val DEFAULT_COMMAND = "adb"
    }
    
    // Shell Commands
    object ShellCommands {
        const val ENABLE_DEBUG_ATTRIBUTES = "settings put global debug_view_attributes 1"
        const val DISABLE_DEBUG_ATTRIBUTES = "settings delete global debug_view_attributes"
        const val UI_DUMP = "uiautomator dump /dev/tty"
        const val WINDOW_DUMP = "dumpsys window | grep -E \"mCurrentFocus|mFocusedApp\" | head -5"
        const val ACTIVITY_DUMP = "dumpsys activity activities | grep -A2 -B1 \"topResumedActivity\\|packageName=\\|processName=\" | head -10"
        const val LIST_PACKAGES = "pm list packages -3"
    }
    
    // Regex Patterns
    object Patterns {
        const val CURRENT_FOCUS = "mCurrentFocus=Window\\{[^}]*\\s+u\\d+\\s+([^/\\s]+)/([^}\\s]+)"
        const val FOCUSED_APP = "mFocusedApp=.*\\s+([^/\\s]+)/\\.?([^}\\s]+)"
        const val PID_EXTRACTION = "pid=(\\d+)"
        const val ACTIVITY_RECORD = "ActivityRecord\\{[^}]*\\s+u\\d+\\s+[^/]+/[^}]+\\s+t(\\d+)\\}"
        const val PROCESS_NAME_PID = "processName=([^\\s]+).*pid=(\\d+)"
        const val MODEL_EXTRACTION = "model:(\\S+)"
        const val XML_WITH_HEADER = "<\\?xml.*?</hierarchy>"
        const val HIERARCHY_ONLY = "<hierarchy.*?</hierarchy>"
    }
    
    // Error Messages
    object ErrorMessages {
        const val ADB_NOT_FOUND = "ADB not found in PATH or common locations. Please install Android SDK platform-tools."
        const val NO_DEVICES_FOUND = "No available Android devices found"
        const val UI_DUMP_FAILED = "Failed to get UI hierarchy data"
        const val COMMAND_TIMEOUT = "Command execution timeout"
        const val CONNECTION_CLOSED = "ADB shell connection closed"
        const val SESSION_CREATION_FAILED = "Failed to create ADB shell session"
    }
}