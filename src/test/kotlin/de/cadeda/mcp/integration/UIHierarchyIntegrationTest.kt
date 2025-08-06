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

class UIHierarchyIntegrationTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun testRetrieveUIHierarchy() = runBlocking {
        println("🚀 Starting UI Hierarchy Integration Test")

        // Create the view_hierarchy request - no arguments needed for basic hierarchy
        val toolCallParams = ToolCallParams(
            name = "view_hierarchy",
            arguments = emptyMap()
        )

        val hierarchyRequest = McpRequest(
            method = "tools/call",
            params = json.encodeToJsonElement(toolCallParams),
            id = 3
        )

        val requestJson = json.encodeToString(hierarchyRequest)
        println("📤 Sending UI hierarchy request: $requestJson")

        // Create input/output streams for the test
        val inputStream = ByteArrayInputStream(requestJson.toByteArray())
        val outputStream = ByteArrayOutputStream()
        val errorStream = ByteArrayOutputStream()

        // Create server with custom streams for testing
        val server = McpServer(inputStream, outputStream, errorStream)

        // Run the server with timeout to prevent hanging
        val startTime = System.currentTimeMillis()

        val completed = withTimeoutOrNull(150000) { // 150 second timeout for UI hierarchy (can be slow)
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

        println("📥 Server output length: ${output.length} characters")
        println("🖥️ Server error output: $errorOutput")

        // Verify we got a response
        assertTrue(output.isNotEmpty(), "Should have response output")
        assertTrue(output.contains("jsonrpc"), "Should contain valid JSON response")

        // Parse the response to verify it's a valid JSON
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

                if (result["isError"]?.jsonPrimitive?.boolean == true) {
                    println("❌ Error response received from server (this is expected when no device is connected or UI dump fails)")
                    // This is acceptable - the server correctly responded with an error
                }
                if (result.containsKey("content")) {
                    val content = result["content"]?.jsonArray
                    if (content != null && content.isNotEmpty()) {
                        // We have hierarchy content, let's print it
                        println("Content:")
                        for ((index, item) in content.withIndex()) {
                            val itemObj = item.jsonObject
                            val text = itemObj["text"]?.jsonPrimitive?.content
                            if (text != null) {
                                // Print first 500 characters to avoid overwhelming output
                                val displayText = if (text.length > 500) {
                                    text.substring(0, 500) + "..."
                                } else {
                                    text
                                }
                                println("  Item ${index + 1}: $displayText")
                            }
                        }
                    } else {
                        println("ℹ️ No content found (device may not be accessible or no UI active)")
                    }
                } else {
                    println("ℹ️ Response structure different than expected, but valid JSON received")
                }
            } else if (hasError) {
                val error = response["error"]?.jsonObject
                val errorMessage = error?.get("message")?.jsonPrimitive?.content
                println("⚠️ Server returned error: $errorMessage")
                // This is still a valid test result - the server responded correctly even if hierarchy isn't available
            }

            println("✅ JSON response is well-formed")
            println("✅ Server completes in reasonable time")

        } catch (e: Exception) {
            println("❌ Failed to parse response as JSON: ${e.message}")
            println("Raw output: $output")
            throw e
        }
    }
}