package de.cadeda.mcp.server.tools

import de.cadeda.mcp.model.InputSchema
import de.cadeda.mcp.model.PropertyDefinition
import de.cadeda.mcp.model.Tool
import de.cadeda.mcp.model.AndroidMcpConstants.Tools

/**
 * Handles drag coordinate operations on Android devices.
 */
object DragCoordinateTool : Tool(
    name = Tools.DRAG_COORDINATE,
    description = "Drag from one coordinate to another on the Android device screen with configurable duration",
    inputSchema = InputSchema(
        type = "object",
        properties = mapOf(
            "deviceId" to PropertyDefinition(
                type = "string",
                description = "Android device ID (optional, uses first available device if not specified)"
            ),
            "startX" to PropertyDefinition(
                type = "number",
                description = "Starting X coordinate for drag gesture"
            ),
            "startY" to PropertyDefinition(
                type = "number",
                description = "Starting Y coordinate for drag gesture"
            ),
            "endX" to PropertyDefinition(
                type = "number",
                description = "Ending X coordinate for drag gesture"
            ),
            "endY" to PropertyDefinition(
                type = "number",
                description = "Ending Y coordinate for drag gesture"
            ),
            "duration" to PropertyDefinition(
                type = "number",
                description = "Duration of drag gesture in milliseconds (default: 300ms)"
            )
        ),
        required = listOf("startX", "startY", "endX", "endY"),
        additionalProperties = false
    )
)
