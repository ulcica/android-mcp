package de.cadeda.mcp.server

import de.cadeda.mcp.model.InitializeParams
import de.cadeda.mcp.model.McpRequest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.*
import kotlinx.serialization.encodeToString
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class McpServerEofTest {
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    @Test
    fun testServerExitsOnEof() = runBlocking {
        println("🚀 Starting EOF behavior test")
        
        // Create a request that will be processed
        val initParams = InitializeParams(protocolVersion = "2024-11-05")
        val initRequest = McpRequest(
            method = "initialize",
            params = json.encodeToJsonElement(initParams),
            id = 1
        )
        val requestJson = json.encodeToString(initRequest)
        
        // Create an input stream that will reach EOF after one request
        val inputStream = ByteArrayInputStream(requestJson.toByteArray())
        val outputStream = ByteArrayOutputStream()
        val errorStream = ByteArrayOutputStream()
        
        // Create server with limited input for testing
        val server = McpServer(inputStream, outputStream, errorStream)
        
        // Measure how long the server runs - it should exit when EOF is reached
        val startTime = System.currentTimeMillis()
        
        // Server should exit when input stream reaches EOF
        val completed = withTimeoutOrNull(5000) {
            try {
                server.start()
                true // Server completed normally
            } catch (e: Exception) {
                println("⚠️ Server threw exception: ${e.message}")
                false
            }
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        println("⏱️ Server ran for ${duration}ms")
        
        // Verify server completed within reasonable time (not hanging indefinitely)
        assertNotNull(completed, "Server should complete within timeout")
        assertTrue(completed, "Server should complete successfully")
        assertTrue(duration < 4000, "Server should exit quickly after EOF, took ${duration}ms")
        
        // Verify we got output from processing the request
        val output = outputStream.toString()
        val errorOutput = errorStream.toString()
        
        println("📥 Server output: $output")
        println("🖥️ Server error output: $errorOutput")
        
        // Should have startup message
        assertTrue(
            errorOutput.contains("Android Layout Inspector MCP Server started"),
            "Should have startup message"
        )
        
        // Should have processed the initialize request
        assertTrue(output.isNotEmpty(), "Should have response output")
        assertTrue(output.contains("jsonrpc"), "Should contain valid JSON response")
        
        println("✅ Server exits properly on EOF")
        println("✅ Server processes requests before EOF")
        println("✅ Server completes in reasonable time")
        println("🎉 EOF behavior test completed successfully!")
    }
}