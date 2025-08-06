package de.cadeda.mcp.model

import de.cadeda.mcp.model.uihierarchy.ContentItem
import de.cadeda.mcp.model.uihierarchy.UIElement
import kotlinx.serialization.Serializable


@Serializable
data class McpSuccessResponse<T>(
    val jsonrpc: String = "2.0",
    val id: Int?,
    val result: T
)

@Serializable
data class McpErrorResponse(
    val jsonrpc: String = "2.0",
    val id: Int?,
    val error: McpError
)

@Serializable
data class McpError(
    val code: Int,
    val message: String,
    val data: String? = null
)

@Serializable
data class InitializeResult(
    val protocolVersion: String,
    val capabilities: Capabilities,
    val serverInfo: ServerInfo
)

@Serializable
data class Capabilities(
    val tools: Map<String, Boolean> = emptyMap()
)

@Serializable
data class ServerInfo(
    val name: String,
    val version: String
)

@Serializable
data class ToolListResult(
    val tools: List<Tool>
)

@Serializable
open class Tool(
    val name: String,
    val description: String,
    val inputSchema: InputSchema
)

@Serializable
data class InputSchema(
    val type: String,
    val properties: Map<String, PropertyDefinition> = emptyMap(),
    val required: List<String> = emptyList(),
    val additionalProperties: Boolean = false
)

@Serializable
data class PropertyDefinition(
    val type: String,
    val description: String,
    val default: String? = null
)

@Serializable
data class ContentResult(
    val content: List<ContentItem>,
    val isError: Boolean = false
)

/**
 * Parameters for the initialize method
 */
@Serializable
data class InitializeParams(
    val protocolVersion: String,
    val capabilities: Map<String, String> = emptyMap()
)

/**
 * Parameters for tool call method
 */
@Serializable
data class ToolCallParams(
    val name: String,
    val arguments: Map<String, String> = emptyMap()
)

@Serializable
data class ClickCoordinateResult(
    val device: String,
    val action: String,
    val coordinates: Coordinates,
    val timestamp: String,
    val success: Boolean
)

@Serializable
data class Coordinates(
    val x: Int,
    val y: Int
)

@Serializable
data class FindElementsResult(
    val device: String,
    val searchCriteria: SearchCriteria,
    val results: List<UIElement>,
    val count: Int
)

@Serializable
data class SearchCriteria(
    val resourceId: String?,
    val text: String?,
    val className: String?,
    val exactMatch: Boolean
)

@Serializable
data class InputTextResult(
    val device: String,
    val action: String,
    val text: String,
    val timestamp: String,
    val success: Boolean
)

@Serializable
data class KeyEventResult(
    val device: String,
    val action: String,
    val keyCode: Int,
    val timestamp: String,
    val success: Boolean
)

@Serializable
data class SwipeCoordinateResult(
    val device: String,
    val action: String,
    val startCoordinates: Coordinates,
    val endCoordinates: Coordinates,
    val duration: Int?,
    val timestamp: String,
    val success: Boolean
)
