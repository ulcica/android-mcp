package de.cadeda.mcp.server.tools

import de.cadeda.mcp.model.AndroidMcpConstants.Tools
import de.cadeda.mcp.model.InputSchema
import de.cadeda.mcp.model.PropertyDefinition
import de.cadeda.mcp.model.Tool

object StartIntentTool : Tool(
    name = Tools.START_INTENT,
    description = "Start an Android intent/activity with specified parameters",
    inputSchema = InputSchema(
        type = "object",
        properties = mapOf(
            "action" to PropertyDefinition(
                type = "string",
                description = "Intent action (e.g., 'android.intent.action.MAIN', 'android.settings.SETTINGS')"
            ),
            "category" to PropertyDefinition(
                type = "string",
                description = "Intent category (e.g., 'android.intent.category.LAUNCHER')"
            ),
            "dataUri" to PropertyDefinition(
                type = "string",
                description = "Data URI for the intent (e.g., 'https://example.com', 'tel:+1234567890')"
            ),
            "packageName" to PropertyDefinition(
                type = "string",
                description = "Target package name (e.g., 'com.android.settings')"
            ),
            "className" to PropertyDefinition(
                type = "string",
                description = "Target class name (e.g., 'com.android.settings.Settings')"
            ),
            "extras" to PropertyDefinition(
                type = "object",
                description = "String extras to pass with the intent as key-value pairs"
            ),
            "deviceId" to PropertyDefinition(
                type = "string",
                description = "Target device ID (optional, uses first available device if not specified)"
            )
        ),
        required = emptyList(),
        additionalProperties = false
    )
)
