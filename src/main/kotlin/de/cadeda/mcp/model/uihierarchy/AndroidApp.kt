package de.cadeda.mcp.model.uihierarchy

import kotlinx.serialization.Serializable

/**
 * Represents an installed Android application.
 */
@Serializable
data class AndroidApp(
    val packageName: String,
    val appName: String? = null,
    val versionName: String? = null,
    val versionCode: Int? = null,
    val isSystemApp: Boolean = false,
    val isEnabled: Boolean = true,
    val isDebuggable: Boolean? = null, // null when details not requested
    val appFlags: List<String>? = null // Application flags when details requested
)

/**
 * Result wrapper for app listing operations.
 */
@Serializable
data class AppListResult(
    val device: String,
    val apps: List<AndroidApp>,
    val count: Int,
    val includeSystemApps: Boolean = false,
    val timestamp: String = java.time.Instant.now().toString()
)