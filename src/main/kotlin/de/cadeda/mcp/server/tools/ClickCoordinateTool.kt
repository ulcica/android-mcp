package de.cadeda.mcp.server.tools

import de.cadeda.mcp.model.InputSchema
import de.cadeda.mcp.model.PropertyDefinition
import de.cadeda.mcp.model.Tool
import de.cadeda.mcp.model.AndroidMcpConstants.Tools

/**
 * Handles click coordinate operations on Android devices.
 */
object ClickCoordinateTool : Tool(
    name = Tools.CLICK_COORDINATE,
    description = "Click at specific (x,y) coordinates on the Android device screen",
    inputSchema = InputSchema(
        type = "object",
        properties = mapOf(
            "deviceId" to PropertyDefinition(
                type = "string",
                description = "Android device ID (optional, uses first available device if not specified)"
            ),
            "x" to PropertyDefinition(
                type = "number",
                description = "X coordinate to click"
            ),
            "y" to PropertyDefinition(
                type = "number",
                description = "Y coordinate to click"
            )
        ),
        required = listOf("x", "y"),
        additionalProperties = false
    )
)
