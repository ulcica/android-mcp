package de.cadeda.mcp.server.tools

import de.cadeda.mcp.model.*
import de.cadeda.mcp.model.AndroidMcpConstants.Tools

/**
 * Handles app list operations on Android devices.
 */
object AppListTool : Tool(
    name = Tools.GET_APP_LIST,
    description = "Get list of installed applications on the Android device with optional detailed information",
    inputSchema = InputSchema(
        type = "object",
        properties = mapOf(
            "deviceId" to PropertyDefinition(
                type = "string",
                description = "Android device ID (optional, uses first available device if not specified)"
            ),
            "includeSystemApps" to PropertyDefinition(
                type = "boolean",
                description = "Include system applications in the list (default: false, shows only user-installed apps)"
            ),
            "includeDetails" to PropertyDefinition(
                type = "boolean",
                description = "Include detailed app information such as debuggable status and flags (default: false, slower when enabled)"
            )
        ),
        required = emptyList(),
        additionalProperties = false
    )
)