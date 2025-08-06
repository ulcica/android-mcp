package de.cadeda.mcp.server.tools

import de.cadeda.mcp.model.InputSchema
import de.cadeda.mcp.model.Tool

object DeviceListTool : Tool(
    name = "get_device_list",
    description = "Get list of connected Android devices",
    inputSchema = InputSchema(
        type = "object",
        properties = emptyMap(),
        additionalProperties = false
    )
)
