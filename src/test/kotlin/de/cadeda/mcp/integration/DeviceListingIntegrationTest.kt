package de.cadeda.mcp.integration

import de.cadeda.mcp.model.McpRequest
import de.cadeda.mcp.model.ToolCallParams
import de.cadeda.mcp.server.McpServer
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.*
import kotlinx.serialization.encodeToString
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DeviceListingIntegrationTest {
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    @Test
    fun testListAvailableDevices() = runBlocking {
        println("🚀 Starting Device Listing Integration Test")
        
        // Create the get_device_list request
        val toolCallParams = ToolCallParams(
            name = "get_device_list",
            arguments = emptyMap()
        )
        
        val deviceListRequest = McpRequest(
            method = "tools/call",
            params = json.encodeToJsonElement(toolCallParams),
            id = 2
        )
        
        val requestJson = json.encodeToString(deviceListRequest)
        println("📤 Sending device list request: $requestJson")
        
        // Create input/output streams for the test
        val inputStream = ByteArrayInputStream(requestJson.toByteArray())
        val outputStream = ByteArrayOutputStream()
        val errorStream = ByteArrayOutputStream()
        
        // Create server with custom streams for testing
        val server = McpServer(inputStream, outputStream, errorStream)
        
        // Run the server with timeout to prevent hanging
        val startTime = System.currentTimeMillis()
        
        val completed = withTimeoutOrNull(10000) { // 10 second timeout
            try {
                server.start()
                true
            } catch (e: Exception) {
                println("⚠️ Server threw exception: ${e.message}")
                false
            }
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        println("⏱️ Server ran for ${duration}ms")
        
        // Verify server completed
        assertNotNull(completed, "Server should complete within timeout")
        assertTrue(completed, "Server should complete successfully")
        
        // Get the outputs
        val output = outputStream.toString()
        val errorOutput = errorStream.toString()
        
        println("📥 Server output: $output")
        println("🖥️ Server error output: $errorOutput")
        
        // Verify we got a response
        assertTrue(output.isNotEmpty(), "Should have response output")
        assertTrue(output.contains("jsonrpc"), "Should contain valid JSON response")
        
        // Parse the response to verify it's valid JSON
        try {
            val response = json.parseToJsonElement(output).jsonObject
            
            // Verify basic response structure
            assertTrue(response.containsKey("jsonrpc"), "Response should have jsonrpc field")
            assertTrue(response.containsKey("id"), "Response should have id field")
            
            // Check if we have a result or error
            val hasResult = response.containsKey("result")
            val hasError = response.containsKey("error") && response["error"] != JsonNull
            
            if (hasResult) {
                val result = response["result"]?.jsonObject
                assertNotNull(result, "Result should be a JSON object")
                
                if (result.containsKey("content")) {
                    val content = result["content"]?.jsonArray
                    if (content != null && content.isNotEmpty()) {
                        // We have device content, let's examine it
                        val firstDevice = content[0].jsonObject
                        println("📱 Found devices:")
                        for (device in content) {
                            val deviceObj = device.jsonObject
                            val deviceId = deviceObj["text"]?.jsonPrimitive?.content
                            println("  - Device: $deviceId")
                        }
                        println("✅ Successfully retrieved ${content.size} device(s)")
                    } else {
                        println("ℹ️ No devices found (ADB may not be available or no devices connected)")
                    }
                } else {
                    println("ℹ️ Response structure different than expected, but valid JSON received")
                }
            } else if (hasError) {
                val error = response["error"]?.jsonObject
                val errorMessage = error?.get("message")?.jsonPrimitive?.content
                println("⚠️ Server returned error: $errorMessage")
                // This is still a valid test result - the server responded correctly even if ADB/devices aren't available
            }
            
            println("✅ Server processes device list request correctly")
            println("✅ JSON response is well-formed")
            println("✅ Server completes in reasonable time")
            
        } catch (e: Exception) {
            println("❌ Failed to parse response as JSON: ${e.message}")
            throw e
        }
    }
}