package de.cadeda.mcp.server.tools

import de.cadeda.mcp.model.InputSchema
import de.cadeda.mcp.model.PropertyDefinition
import de.cadeda.mcp.model.Tool

object GetLogsTool : Tool(
    name = "get_logs",
    description = "Get Android device logs with optional filtering by app package name and priority level",
    inputSchema = InputSchema(
        type = "object",
        properties = mapOf(
            "deviceId" to PropertyDefinition(
                type = "string",
                description = "Android device ID (optional, uses first available device if not specified)"
            ),
            "packageName" to PropertyDefinition(
                type = "string",
                description = "Package name to filter logs for specific app (optional)"
            ),
            "maxLines" to PropertyDefinition(
                type = "integer",
                description = "Maximum number of log lines to return (default: 100, max: 1000)",
                default = "100"
            ),
            "priority" to PropertyDefinition(
                type = "string",
                description = "Minimum log priority level: V (VERBOSE), D (DEBUG), I (INFO), W (WARN), E (ERROR), F (FATAL)"
            )
        ),
        additionalProperties = false
    )
)
