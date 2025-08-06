package de.cadeda.mcp.server.tools

import de.cadeda.mcp.model.InputSchema
import de.cadeda.mcp.model.PropertyDefinition
import de.cadeda.mcp.model.Tool
import de.cadeda.mcp.model.AndroidMcpConstants.Tools
import de.cadeda.mcp.model.AndroidMcpConstants.KeyCodes

/**
 * Handles key event operations on Android devices.
 */
object KeyEventTool : Tool(
    name = Tools.KEY_EVENT,
    description = "Send key event to Android device (e.g., Enter=${KeyCodes.ENTER}, Back=${KeyCodes.BACK}, Home=${KeyCodes.HOME})",
    inputSchema = InputSchema(
        type = "object",
        properties = mapOf(
            "deviceId" to PropertyDefinition(
                type = "string",
                description = "Android device ID (optional, uses first available device if not specified)"
            ),
            "keyCode" to PropertyDefinition(
                type = "number",
                description = "Android key code (e.g., ${KeyCodes.ENTER} for Enter, ${KeyCodes.BACK} for Back)"
            )
        ),
        required = listOf("keyCode"),
        additionalProperties = false
    )
)
