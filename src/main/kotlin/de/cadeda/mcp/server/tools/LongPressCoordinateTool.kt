package de.cadeda.mcp.server.tools

import de.cadeda.mcp.model.*
import de.cadeda.mcp.model.AndroidMcpConstants.Tools

/**
 * Handles long press coordinate operations on Android devices.
 */
object LongPressCoordinateTool : Tool(
    name = Tools.LONG_PRESS_COORDINATE,
    description = "Long press at specific (x,y) coordinates on the Android device screen",
    inputSchema = InputSchema(
        type = "object",
        properties = mapOf(
            "deviceId" to PropertyDefinition(
                type = "string",
                description = "Android device ID (optional, uses first available device if not specified)"
            ),
            "x" to PropertyDefinition(
                type = "number",
                description = "X coordinate to long press"
            ),
            "y" to PropertyDefinition(
                type = "number",
                description = "Y coordinate to long press"
            ),
            "duration" to PropertyDefinition(
                type = "number",
                description = "Duration of long press in milliseconds (optional, default: 1000ms)"
            )
        ),
        required = listOf("x", "y"),
        additionalProperties = false
    )
)