package de.cadeda.mcp.server

import de.cadeda.mcp.adb.AdbManager
import de.cadeda.mcp.adb.UIHierarchyParser
import de.cadeda.mcp.model.*
import de.cadeda.mcp.model.AndroidMcpConstants.ErrorMessages
import de.cadeda.mcp.model.AndroidMcpConstants.Input
import de.cadeda.mcp.model.AndroidMcpConstants.Protocol
import de.cadeda.mcp.model.AndroidMcpConstants.Timing
import de.cadeda.mcp.model.AndroidMcpConstants.Tools
import de.cadeda.mcp.model.uihierarchy.*
import de.cadeda.mcp.server.tools.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.*
import java.time.Instant

class McpServer(
    private val inputStream: InputStream = System.`in`,
    private val outputStream: OutputStream = System.out,
    private val errorStream: OutputStream = System.err,
) {
    companion object {
        // Keep our version, respond to client with what they sent
        private const val PROTOCOL_VERSION = Protocol.PROTOCOL_VERSION
        private const val SERVER_NAME = Protocol.SERVER_NAME
    }

    private val adbManager = AdbManager.getInstance()
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    // Cached target device to avoid repeated lookups
    private var cachedTargetDeviceId: String? = null
    private var targetDeviceLastChecked: Long = 0

    // Helper function to create content responses
    private inline fun <reified T> createContentResponse(data: T, id: JsonElement?): String {
        val contentResult = ContentResult(
            content = listOf(ContentItem(type = "text", text = json.encodeToString(data))),
            isError = false
        )
        val response = McpSuccessResponse(
            id = id?.jsonPrimitive?.intOrNull,
            result = contentResult
        )
        return json.encodeToString(response)
    }

    suspend fun start() {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val writer = PrintStream(outputStream)
        val errorWriter = PrintStream(errorStream)

        errorWriter.println("Android Layout Inspector MCP Server started (Kotlin)")

        while (true) {
            try {
                val line = reader.readLine() ?: break
                if (line.trim().isEmpty()) continue

                val response = processRequest(line)
                // Only send response if one was generated (not for notifications)
                if (response != null) {
                    writer.println(response)
                    writer.flush()
                }
            } catch (e: Exception) {
                errorWriter.println("Error processing request: ${e.message}")
                val errorResponse = createErrorResponse("Error processing request: ${e.message}")
                writer.println(errorResponse)
                writer.flush()
            }
        }

        // EOF reached - server completed normally
        errorWriter.println("Shutting down MCP server...")
    }

    private suspend fun processRequest(requestLine: String): String? {
        try {
            val requestJson = json.parseToJsonElement(requestLine).jsonObject
            val method = requestJson["method"]?.jsonPrimitive?.content ?: ""
            val params = requestJson["params"]?.jsonObject
            val id = requestJson["id"]

            // Handle notifications (no response required)
            if (id == null) {
                return handleNotification(method, params)
            }

            return when (method) {
                "initialize" -> handleInitialize(params, id)
                "tools/list" -> handleListTools(id)
                "tools/call" -> handleToolCall(params, id)
                "resources/list" -> handleEmptyResourcesList(id)
                "prompts/list" -> handleEmptyPromptsList(id)
                else -> createErrorResponse("Unknown method: $method", id)
            }
        } catch (e: Exception) {
            return createErrorResponse("Failed to parse request: ${e.message}")
        }
    }

    private fun handleNotification(method: String, params: JsonObject?): String? {
        return when (method) {
            "notifications/initialized" -> {
                // Client has finished initialization - no response required
                null
            }

            "notifications/cancelled" -> {
                // Request cancelled - no response required
                null
            }

            else -> {
                // Unknown notification - ignore it (per JSON-RPC spec)
                null
            }
        }
    }

    private fun handleInitialize(params: JsonObject?, id: JsonElement?): String {
        // Use the client's protocol version for compatibility
        val clientProtocolVersion = params?.get("protocolVersion")?.jsonPrimitive?.content ?: PROTOCOL_VERSION

        val initializeResult = InitializeResult(
            protocolVersion = clientProtocolVersion,
            capabilities = Capabilities(tools = emptyMap()),
            serverInfo = ServerInfo(
                name = SERVER_NAME,
                version = AppVersion.VERSION
            )
        )

        val response = McpSuccessResponse(
            id = id?.jsonPrimitive?.intOrNull,
            result = initializeResult
        )

        return json.encodeToString(response)
    }

    private fun handleListTools(id: JsonElement?): String {
        val tools = createToolDefinitions()

        val toolListResult = ToolListResult(tools = tools)
        val response = McpSuccessResponse(
            id = id?.jsonPrimitive?.intOrNull,
            result = toolListResult
        )

        return json.encodeToString(response)
    }

    private fun createToolDefinitions(): List<Tool> {
        return listOf(
            DeviceListTool,
            AppListTool,
            DeviceOptionalTool(
                Tools.GET_VIEW_ATTRIBUTES,
                "Get UI hierarchy with enhanced view attributes (enables debug mode temporarily)"
            ),
            DeviceOptionalTool(
                Tools.GET_CURRENT_ACTIVITY,
                "Get information about the current foreground activity"
            ),
            FindElementsTool,
            DeviceOptionalTool(
                Tools.VIEW_HIERARCHY,
                "Get the UI view hierarchy from uiautomator dump"
            ),
            ClickCoordinateTool,
            LongPressCoordinateTool,
            DragCoordinateTool,
            SwipeCoordinateTool,
            InputTextTool,
            KeyEventTool,
            StartIntentTool,
            GetLogsTool
        )
    }

    private suspend fun handleToolCall(params: JsonObject?, id: JsonElement?): String {
        return try {
            val name = params?.get("name")?.jsonPrimitive?.content
                ?: throw LayoutInspectorError.McpProtocolError("Missing tool name")
            val arguments = params["arguments"]?.jsonObject ?: JsonObject(emptyMap())

            when (name) {
                Tools.GET_DEVICE_LIST -> handleGetDeviceList(id)
                Tools.GET_APP_LIST -> handleGetAppList(arguments, id)
                Tools.GET_VIEW_ATTRIBUTES -> handleGetViewAttributes(arguments, id)
                Tools.GET_CURRENT_ACTIVITY -> handleGetCurrentActivity(arguments, id)
                Tools.FIND_ELEMENTS -> handleFindElements(arguments, id)
                Tools.VIEW_HIERARCHY -> handleViewHierarchy(arguments, id)
                Tools.CLICK_COORDINATE -> handleClickCoordinate(arguments, id)
                Tools.LONG_PRESS_COORDINATE -> handleLongPressCoordinate(arguments, id)
                Tools.DRAG_COORDINATE -> handleDragCoordinate(arguments, id)
                Tools.SWIPE_COORDINATE -> handleSwipeCoordinate(arguments, id)
                Tools.INPUT_TEXT -> handleInputText(arguments, id)
                Tools.KEY_EVENT -> handleKeyEvent(arguments, id)
                Tools.START_INTENT -> handleStartIntent(arguments, id)
                Tools.GET_LOGS -> handleGetLogs(arguments, id)
                else -> createErrorResponse("Unknown tool: $name", id)
            }
        } catch (e: Exception) {
            createErrorResponse("Error: ${e.message}", id)
        }
    }

    private suspend fun getTargetDevice(deviceId: String?): String {
        if (!deviceId.isNullOrEmpty()) {
            cachedTargetDeviceId = deviceId // Update cache with explicit device
            return deviceId
        }

        val now = System.currentTimeMillis()

        // Return cached device if still valid
        cachedTargetDeviceId?.let { cached ->
            if (now - targetDeviceLastChecked < Timing.TARGET_DEVICE_CACHE_TTL_MS) {
                return cached
            }
        }

        val devices = adbManager.getDevices()
        val availableDevice = devices.find { it.state == DeviceState.DEVICE }
            ?: throw LayoutInspectorError.DeviceNotFound(ErrorMessages.NO_DEVICES_FOUND, null)

        // Cache the result
        cachedTargetDeviceId = availableDevice.id
        targetDeviceLastChecked = now

        return availableDevice.id
    }

    private suspend fun handleGetDeviceList(id: JsonElement?): String {
        val devices = adbManager.getDevices()
        return createContentResponse(devices, id)
    }

    private suspend fun handleGetAppList(arguments: JsonObject, id: JsonElement?): String {
        val deviceId = arguments["deviceId"]?.jsonPrimitive?.content
        val includeSystemApps = arguments["includeSystemApps"]?.jsonPrimitive?.boolean ?: false
        val targetDevice = getTargetDevice(deviceId)

        val apps = adbManager.getAppList(targetDevice, includeSystemApps)
        val result = AppListResult(
            device = targetDevice,
            apps = apps,
            count = apps.size,
            includeSystemApps = includeSystemApps
        )

        return createContentResponse(result, id)
    }

    private suspend fun handleGetViewAttributes(arguments: JsonObject, id: JsonElement?): String {
        val deviceId = arguments["deviceId"]?.jsonPrimitive?.content
        val targetDevice = getTargetDevice(deviceId)

        val xmlContent = adbManager.getViewAttributes(targetDevice)
        val hierarchy = UIHierarchyParser.parseUIHierarchy(xmlContent, targetDevice)

        // Get current activity info
        val activityInfo = adbManager.getCurrentActivity(targetDevice)
        val enrichedHierarchy = hierarchy.copy(
            packageName = activityInfo?.packageName,
            activityName = activityInfo?.activity,
            activityPid = activityInfo?.pid,
            windowInfo = activityInfo?.windowInfo
        )

        return createContentResponse(enrichedHierarchy, id)
    }

    private suspend fun handleGetCurrentActivity(arguments: JsonObject, id: JsonElement?): String {
        val deviceId = arguments["deviceId"]?.jsonPrimitive?.content
        val targetDevice = getTargetDevice(deviceId)

        val activityInfo = adbManager.getCurrentActivity(targetDevice)
        val result: CurrentActivityResult = if (activityInfo != null) {
            CurrentActivityResult.Success(activityInfo)
        } else {
            CurrentActivityResult.NotFound("Could not determine current activity", targetDevice)
        }

        return createContentResponse(result, id)
    }

    @Serializable
    sealed class CurrentActivityResult {
        @Serializable
        data class Success(
            val activity: CurrentActivity
        ) : CurrentActivityResult()

        @Serializable
        data class NotFound(
            val message: String,
            val device: String,
            val timestamp: String = Instant.now().toString()
        ) : CurrentActivityResult()
    }

    private suspend fun handleFindElements(arguments: JsonObject, id: JsonElement?): String {
        val deviceId = arguments["deviceId"]?.jsonPrimitive?.content
        val resourceId = arguments["resourceId"]?.jsonPrimitive?.content
        val text = arguments["text"]?.jsonPrimitive?.content
        val className = arguments["className"]?.jsonPrimitive?.content
        val exactMatch = arguments["exactMatch"]?.jsonPrimitive?.boolean ?: false
        val targetDevice = getTargetDevice(deviceId)

        if (resourceId.isNullOrEmpty() && text.isNullOrEmpty() && className.isNullOrEmpty()) {
            return createErrorResponse("Must specify at least one search criteria: resourceId, text, or className", id)
        }

        val xmlContent = adbManager.getViewHierarchy(targetDevice)
        val hierarchy = UIHierarchyParser.parseUIHierarchy(xmlContent, targetDevice)

        val results = when {
            !resourceId.isNullOrEmpty() -> UIHierarchyParser.findElementsById(hierarchy.root, resourceId)
            !text.isNullOrEmpty() -> UIHierarchyParser.findElementsByText(hierarchy.root, text, exactMatch)
            !className.isNullOrEmpty() -> UIHierarchyParser.findElementsByClass(hierarchy.root, className)
            else -> emptyList()
        }

        val searchResult = FindElementsResult(
            device = targetDevice,
            searchCriteria = SearchCriteria(
                resourceId = resourceId,
                text = text,
                className = className,
                exactMatch = exactMatch
            ),
            results = results,
            count = results.size
        )

        return createContentResponse(searchResult, id)
    }

    private suspend fun handleViewHierarchy(arguments: JsonObject, id: JsonElement?): String {
        val deviceId = arguments["deviceId"]?.jsonPrimitive?.content
        val targetDevice = getTargetDevice(deviceId)

        val xmlContent = adbManager.getViewHierarchy(targetDevice)
        val viewHierarchy = UIHierarchyParser.parseViewHierarchy(xmlContent, targetDevice)

        return createContentResponse(viewHierarchy, id)
    }

    private suspend fun handleClickCoordinate(arguments: JsonObject, id: JsonElement?): String {
        val deviceId = arguments["deviceId"]?.jsonPrimitive?.content
        val x =
            arguments["x"]?.jsonPrimitive?.int ?: throw LayoutInspectorError.McpProtocolError("Missing x coordinate")
        val y =
            arguments["y"]?.jsonPrimitive?.int ?: throw LayoutInspectorError.McpProtocolError("Missing y coordinate")
        val targetDevice = getTargetDevice(deviceId)

        adbManager.clickCoordinate(x, y, targetDevice)

        val result = ClickCoordinateResult(
            device = targetDevice,
            action = "click",
            coordinates = Coordinates(x, y),
            timestamp = Instant.now().toString(),
            success = true
        )

        return createContentResponse(result, id)
    }

    private suspend fun handleLongPressCoordinate(arguments: JsonObject, id: JsonElement?): String {
        val deviceId = arguments["deviceId"]?.jsonPrimitive?.content
        val x =
            arguments["x"]?.jsonPrimitive?.int ?: throw LayoutInspectorError.McpProtocolError("Missing x coordinate")
        val y =
            arguments["y"]?.jsonPrimitive?.int ?: throw LayoutInspectorError.McpProtocolError("Missing y coordinate")
        val duration = arguments["duration"]?.jsonPrimitive?.int ?: 1000
        val targetDevice = getTargetDevice(deviceId)

        adbManager.longPressCoordinate(x, y, duration, targetDevice)

        val result = ClickCoordinateResult(
            device = targetDevice,
            action = "long_press",
            coordinates = Coordinates(x, y),
            timestamp = Instant.now().toString(),
            success = true
        )

        return createContentResponse(result, id)
    }

    private suspend fun handleDragCoordinate(arguments: JsonObject, id: JsonElement?): String {
        val deviceId = arguments["deviceId"]?.jsonPrimitive?.content
        val startX = arguments["startX"]?.jsonPrimitive?.int ?: throw LayoutInspectorError.McpProtocolError("Missing startX coordinate")
        val startY = arguments["startY"]?.jsonPrimitive?.int ?: throw LayoutInspectorError.McpProtocolError("Missing startY coordinate")
        val endX = arguments["endX"]?.jsonPrimitive?.int ?: throw LayoutInspectorError.McpProtocolError("Missing endX coordinate")
        val endY = arguments["endY"]?.jsonPrimitive?.int ?: throw LayoutInspectorError.McpProtocolError("Missing endY coordinate")
        val duration = arguments["duration"]?.jsonPrimitive?.int ?: Input.DEFAULT_SWIPE_DURATION_MS

        val targetDevice = getTargetDevice(deviceId)
        adbManager.dragCoordinate(startX, startY, endX, endY, duration, targetDevice)

        val result = SwipeCoordinateResult(
            device = targetDevice,
            action = "drag",
            startCoordinates = Coordinates(startX, startY),
            endCoordinates = Coordinates(endX, endY),
            duration = duration,
            timestamp = Instant.now().toString(),
            success = true
        )
        return createContentResponse(result, id)
    }

    private suspend fun handleSwipeCoordinate(arguments: JsonObject, id: JsonElement?): String {
        val deviceId = arguments["deviceId"]?.jsonPrimitive?.content
        val startX = arguments["startX"]?.jsonPrimitive?.int ?: throw Exception("Missing startX coordinate")
        val startY = arguments["startY"]?.jsonPrimitive?.int ?: throw Exception("Missing startY coordinate")
        val endX = arguments["endX"]?.jsonPrimitive?.int ?: throw Exception("Missing endX coordinate")
        val endY = arguments["endY"]?.jsonPrimitive?.int ?: throw Exception("Missing endY coordinate")
        val duration = arguments["duration"]?.jsonPrimitive?.int ?: Input.DEFAULT_SWIPE_DURATION_MS
        val targetDevice = getTargetDevice(deviceId)

        adbManager.swipeCoordinate(startX, startY, endX, endY, duration, targetDevice)

        val result = SwipeCoordinateResult(
            device = targetDevice,
            action = "swipe",
            startCoordinates = Coordinates(startX, startY),
            endCoordinates = Coordinates(endX, endY),
            duration = duration,
            timestamp = Instant.now().toString(),
            success = true
        )

        return createContentResponse(result, id)
    }

    private suspend fun handleInputText(arguments: JsonObject, id: JsonElement?): String {
        val deviceId = arguments["deviceId"]?.jsonPrimitive?.content
        val text = arguments["text"]?.jsonPrimitive?.content ?: throw Exception("Missing text input")
        val targetDevice = getTargetDevice(deviceId)

        adbManager.inputText(text, targetDevice)

        val result = InputTextResult(
            device = targetDevice,
            action = "input_text",
            text = text,
            timestamp = Instant.now().toString(),
            success = true
        )

        return createContentResponse(result, id)
    }

    private suspend fun handleKeyEvent(arguments: JsonObject, id: JsonElement?): String {
        val deviceId = arguments["deviceId"]?.jsonPrimitive?.content
        val keyCode = arguments["keyCode"]?.jsonPrimitive?.int ?: throw Exception("Missing keyCode")
        val targetDevice = getTargetDevice(deviceId)

        adbManager.sendKeyEvent(keyCode, targetDevice)

        val result = KeyEventResult(
            device = targetDevice,
            action = "key_event",
            keyCode = keyCode,
            timestamp = Instant.now().toString(),
            success = true
        )

        return createContentResponse(result, id)
    }

    private suspend fun handleStartIntent(arguments: JsonObject, id: JsonElement?): String {
        val deviceId = arguments["deviceId"]?.jsonPrimitive?.content
        val action = arguments["action"]?.jsonPrimitive?.content
        val category = arguments["category"]?.jsonPrimitive?.content
        val dataUri = arguments["dataUri"]?.jsonPrimitive?.content
        val packageName = arguments["packageName"]?.jsonPrimitive?.content
        val className = arguments["className"]?.jsonPrimitive?.content
        val extras = arguments["extras"]?.jsonObject?.mapValues { it.value.jsonPrimitive.content } ?: emptyMap()
        val targetDevice = getTargetDevice(deviceId)

        val result = adbManager.startIntent(action, category, dataUri, packageName, className, extras, targetDevice)
        
        return if (result.isSuccess) {
            createContentResponse(result.getOrNull(), id)
        } else {
            createErrorResponse("Failed to start intent: ${result.exceptionOrNull()?.message}", id)
        }
    }

    private suspend fun handleGetLogs(arguments: JsonObject, id: JsonElement?): String {
        val deviceId = arguments["deviceId"]?.jsonPrimitive?.content
        val packageName = arguments["packageName"]?.jsonPrimitive?.content
        val maxLines = arguments["maxLines"]?.jsonPrimitive?.int ?: 100
        val priority = arguments["priority"]?.jsonPrimitive?.content
        val targetDevice = getTargetDevice(deviceId)

        // Validate maxLines parameter
        val validMaxLines = maxLines.coerceIn(1, 1000)

        val logEntries = adbManager.getLogs(packageName, validMaxLines, priority, targetDevice)
        
        val result = LogResult(
            device = targetDevice,
            entries = logEntries,
            count = logEntries.size,
            filters = LogFilters(
                packageName = packageName,
                maxLines = validMaxLines,
                priority = priority
            )
        )

        return createContentResponse(result, id)
    }

    private fun createErrorResponse(message: String, id: JsonElement? = null): String {
        val mcpError = McpError(
            code = Protocol.INTERNAL_ERROR_CODE,
            message = message
        )
        val response = McpErrorResponse(
            id = id?.jsonPrimitive?.intOrNull,
            error = mcpError
        )
        return json.encodeToString(response)
    }

    private fun handleEmptyResourcesList(id: JsonElement?): String {
        // This server doesn't provide resources - return empty list
        @Serializable
        data class ResourcesListResult(val resources: List<String> = emptyList())

        val emptyResult = ResourcesListResult()
        val response = McpSuccessResponse(
            id = id?.jsonPrimitive?.intOrNull,
            result = emptyResult
        )
        return json.encodeToString(response)
    }

    private fun handleEmptyPromptsList(id: JsonElement?): String {
        // This server doesn't provide prompts - return empty list
        @Serializable
        data class PromptsListResult(val prompts: List<String> = emptyList())

        val emptyResult = PromptsListResult()
        val response = McpSuccessResponse(
            id = id?.jsonPrimitive?.intOrNull,
            result = emptyResult
        )
        return json.encodeToString(response)
    }
}