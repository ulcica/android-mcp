package de.cadeda.mcp.integration

import de.cadeda.mcp.model.InitializeParams
import de.cadeda.mcp.model.McpRequest
import de.cadeda.mcp.server.McpServer
import kotlinx.serialization.json.*
import kotlinx.serialization.encodeToString
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class StreamsIntegrationTest {

    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun testCustomInputOutputStream() {
        println("🚀 Starting Custom Streams Integration Test")

        // Test that we can create proper request data using McpRequest data class
        val initParams = InitializeParams(
            protocolVersion = "2024-11-05"
        )
        
        val initRequest = McpRequest(
            method = "initialize",
            params = json.encodeToJsonElement(initParams),
            id = 1
        )
        
        val initRequestJson = json.encodeToString(initRequest)
        println("📤 Generated request JSON: $initRequestJson")
        
        // Verify the JSON is valid and contains expected fields
        val parsedRequest = json.parseToJsonElement(initRequestJson).jsonObject
        assertEquals("2.0", parsedRequest["jsonrpc"]?.jsonPrimitive?.content)
        assertEquals("initialize", parsedRequest["method"]?.jsonPrimitive?.content)
        assertEquals(1, parsedRequest["id"]?.jsonPrimitive?.int)
        
        val params = parsedRequest["params"]?.jsonObject
        assertNotNull(params)
        assertEquals("2024-11-05", params["protocolVersion"]?.jsonPrimitive?.content)
        
        // Test that we can instantiate McpServer with custom streams
        val inputStream = ByteArrayInputStream(initRequestJson.toByteArray())
        val outputStream = ByteArrayOutputStream()
        val errorStream = ByteArrayOutputStream()
        
        // Create server with custom streams - this tests the constructor
        val server = McpServer(inputStream, outputStream, errorStream)
        assertNotNull(server, "Server should be created with custom streams")
        
        // Test that we can also create server with default streams
        val defaultServer = McpServer()
        assertNotNull(defaultServer, "Server should be created with default streams")
    }

}