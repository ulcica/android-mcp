package de.cadeda.mcp.server.tools

import de.cadeda.mcp.model.InputSchema
import de.cadeda.mcp.model.PropertyDefinition
import de.cadeda.mcp.model.Tool
import de.cadeda.mcp.model.AndroidMcpConstants.Tools


/**
 * Handles finding UI elements by various criteria on Android devices.
 */
object FindElementsTool : Tool(
    name = Tools.FIND_ELEMENTS,
    description = "Find UI elements by various criteria (resource ID, text, class name)",
    inputSchema = InputSchema(
        type = "object",
        properties = mapOf(
            "deviceId" to PropertyDefinition(
                type = "string",
                description = "Android device ID"
            ),
            "resourceId" to PropertyDefinition(
                type = "string",
                description = "Resource ID to search for"
            ),
            "text" to PropertyDefinition(
                type = "string",
                description = "Text content to search for"
            ),
            "className" to PropertyDefinition(
                type = "string",
                description = "Class name to search for"
            ),
            "exactMatch" to PropertyDefinition(
                type = "boolean",
                description = "Whether to use exact text matching",
                default = "false"
            )
        ),
        additionalProperties = false
    )
)
