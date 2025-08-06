package de.cadeda.mcp.server.tools

import de.cadeda.mcp.model.*
import de.cadeda.mcp.model.AndroidMcpConstants.Tools
import de.cadeda.mcp.model.AndroidMcpConstants.Input

/**
 * Handles swipe coordinate operations on Android devices.
 */
object SwipeCoordinateTool : Tool(
    name = Tools.SWIPE_COORDINATE,
    description = "Swipe from start (x,y) coordinates to end (x,y) coordinates on the Android device screen",
    inputSchema = InputSchema(
        type = "object",
        properties = mapOf(
            "deviceId" to PropertyDefinition(
                type = "string",
                description = "Android device ID (optional, uses first available device if not specified)"
            ),
            "startX" to PropertyDefinition(
                type = "number",
                description = "Starting X coordinate"
            ),
            "startY" to PropertyDefinition(
                type = "number",
                description = "Starting Y coordinate"
            ),
            "endX" to PropertyDefinition(
                type = "number",
                description = "Ending X coordinate"
            ),
            "endY" to PropertyDefinition(
                type = "number",
                description = "Ending Y coordinate"
            ),
            "duration" to PropertyDefinition(
                type = "number",
                description = "Swipe duration in milliseconds (default: ${Input.DEFAULT_SWIPE_DURATION_MS})"
            )
        ),
        required = listOf("startX", "startY", "endX", "endY"),
        additionalProperties = false
    )
)
