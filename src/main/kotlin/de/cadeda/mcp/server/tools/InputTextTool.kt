package de.cadeda.mcp.server.tools

import de.cadeda.mcp.model.*
import de.cadeda.mcp.model.AndroidMcpConstants.Tools

/**
 * Handles text input operations on Android devices.
 */
object InputTextTool : Tool(
    name = Tools.INPUT_TEXT,
    description = "Input text on the Android device (types text into focused field)",
    inputSchema = InputSchema(
        type = "object",
        properties = mapOf(
            "deviceId" to PropertyDefinition(
                type = "string",
                description = "Android device ID (optional, uses first available device if not specified)"
            ),
            "text" to PropertyDefinition(
                type = "string",
                description = "Text to input on the device"
            )
        ),
        required = listOf("text"),
        additionalProperties = false
    )
)
