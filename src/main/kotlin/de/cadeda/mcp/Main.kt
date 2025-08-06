package de.cadeda.mcp

import de.cadeda.mcp.model.AppVersion
import de.cadeda.mcp.server.McpServer
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    // Handle version and help arguments
    when {
        args.contains("--version") || args.contains("-v") -> {
            println("Android MCP Server ${AppVersion.VERSION}")
            exitProcess(0)
        }
        args.contains("--help") || args.contains("-h") -> {
            println("""Android MCP Server ${AppVersion.VERSION}
                |
Usage: java -jar android-mcp.jar [OPTIONS]
                |
Options:
                |  --version, -v    Show version information
                |  --help, -h       Show this help message
                |
                |This is a Model Context Protocol (MCP) server for Android UI inspection.
                |It communicates via stdio and should be configured in your MCP client.
                |""".trimMargin())
            exitProcess(0)
        }
    }
    
    val server = McpServer()
    
    runBlocking {
        try {
            server.start()
            // Server completed normally (EOF reached)
        } catch (e: Exception) {
            System.err.println("Server error: ${e.message}")
            e.printStackTrace()
            exitProcess(1)
        }
    }
    
    // Server completed normally, exit cleanly
    exitProcess(0)
}