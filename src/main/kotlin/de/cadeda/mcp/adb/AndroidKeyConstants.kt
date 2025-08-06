package de.cadeda.mcp.adb

import de.cadeda.mcp.model.AndroidMcpConstants.KeyCodes

/**
 * Common Android key codes for convenience
 * Reference: https://developer.android.com/reference/android/view/KeyEvent
 *
 * Uses centralized key codes from AndroidMcpConstants.KeyCodes where available.
 */
object AndroidKeyConstants {
    const val KEYCODE_BACK = KeyCodes.BACK
    const val KEYCODE_HOME = KeyCodes.HOME
    const val KEYCODE_MENU = KeyCodes.MENU
    const val KEYCODE_ENTER = KeyCodes.ENTER
    const val KEYCODE_DEL = 67
    const val KEYCODE_SPACE = 62
    const val KEYCODE_TAB = 61
    const val KEYCODE_ESCAPE = 111
    const val KEYCODE_POWER = KeyCodes.POWER
    const val KEYCODE_VOLUME_UP = KeyCodes.VOLUME_UP
    const val KEYCODE_VOLUME_DOWN = KeyCodes.VOLUME_DOWN
    const val KEYCODE_CAMERA = KeyCodes.CAMERA
    const val KEYCODE_SEARCH = KeyCodes.SEARCH

    // Directional keys
    const val KEYCODE_DPAD_UP = 19
    const val KEYCODE_DPAD_DOWN = 20
    const val KEYCODE_DPAD_LEFT = 21
    const val KEYCODE_DPAD_RIGHT = 22
    const val KEYCODE_DPAD_CENTER = 23

    // Function keys
    const val KEYCODE_F1 = 131
    const val KEYCODE_F2 = 132
    const val KEYCODE_F3 = 133
    const val KEYCODE_F4 = 134

    // Map for key descriptions to reduce cyclomatic complexity
    private val keyDescriptions = mapOf(
        KEYCODE_BACK to "Back",
        KEYCODE_HOME to "Home",
        KEYCODE_MENU to "Menu",
        KEYCODE_ENTER to "Enter",
        KEYCODE_DEL to "Delete",
        KEYCODE_SPACE to "Space",
        KEYCODE_TAB to "Tab",
        KEYCODE_ESCAPE to "Escape",
        KEYCODE_POWER to "Power",
        KEYCODE_VOLUME_UP to "Volume Up",
        KEYCODE_VOLUME_DOWN to "Volume Down",
        KEYCODE_CAMERA to "Camera",
        KEYCODE_SEARCH to "Search",
        KEYCODE_DPAD_UP to "D-pad Up",
        KEYCODE_DPAD_DOWN to "D-pad Down",
        KEYCODE_DPAD_LEFT to "D-pad Left",
        KEYCODE_DPAD_RIGHT to "D-pad Right",
        KEYCODE_DPAD_CENTER to "D-pad Center"
    )

    /**
     * Get human-readable description for key code
     * Reduced complexity by using map lookup instead of large when statement
     */
    fun getKeyDescription(keyCode: Int): String {
        return keyDescriptions[keyCode]
            ?: if (keyCode in KEYCODE_F1..KEYCODE_F4) {
                "F${keyCode - KEYCODE_F1 + 1}"
            } else {
                "Key $keyCode"
            }
    }

    /**
     * Check if the key code is a navigation key
     */
    fun isNavigationKey(keyCode: Int): Boolean {
        return keyCode in KEYCODE_DPAD_UP..KEYCODE_DPAD_CENTER ||
            keyCode == KEYCODE_BACK ||
            keyCode == KEYCODE_HOME ||
            keyCode == KEYCODE_MENU
    }
}
