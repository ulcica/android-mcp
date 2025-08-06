# Android Layout Inspector MCP Server

A Model Context Protocol (MCP) server that provides Android Layout Inspector functionality through ADB commands. This server allows LLMs and other MCP clients to inspect Android UI hierarchies, find elements, and analyze app layouts.

## Features

- **Device Management**: List and manage connected Android devices via ADB with automatic fallback paths
- **Enhanced UI Hierarchy Inspection**: Extract complete UI hierarchy using Layout Inspector features with enhanced attributes
- **Debug View Attributes**: Access detailed view attributes and layout parameters
- **Element Search**: Find UI elements by resource ID, text content, or class name with exact matching options
- **User Interaction**: Click, swipe, and input text on Android devices with coordinate precision
- **Activity Information**: Get current foreground activity details with window state information
- **Performance Optimized**: Uses coroutines and caching for improved response times

## Requirements

- **Java 17+** (required for Kotlin)
- **Android SDK Platform Tools** (for ADB) - automatically detects from:
  - System PATH
  - `~/Library/Android/sdk/platform-tools/adb` (macOS)
  - `~/Android/Sdk/platform-tools/adb` (Windows/Linux)
  - `/usr/local/bin/adb` (Homebrew)
  - `/opt/android-sdk/platform-tools/adb` (Linux)
- **Connected Android device** (physical device or emulator)
- **Developer options and USB debugging enabled** on the Android device

## Installation & Setup

### 1. Build the Application

```bash
# Build the Kotlin JAR file
./gradlew build
```

### 2. Configure Claude Desktop

Add this configuration to your Claude Desktop config file:

**macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
**Windows**: `%APPDATA%/Claude/claude_desktop_config.json`
**Linux**: `~/.config/claude/claude_desktop_config.json`

```json
{
  "mcpServers": {
    "android-mcp": {
      "command": "java",
      "args": ["-jar", "/path/to/android-mcp/build/libs/android-mcp.jar"]
    }
  }
}
```

## Usage

### Development Commands

```bash
# Build and run the Kotlin server
./gradlew run

# Build only
./gradlew build

# Run tests
./gradlew test

# Run the JAR directly
java -jar build/libs/android-mcp.jar

# Check version
java -jar build/libs/android-mcp.jar --version

# Show help
java -jar build/libs/android-mcp.jar --help
```

### Available Tools

The Kotlin MCP server provides 9 comprehensive tools:

#### 1. `get_device_list`
Lists all connected Android devices.

**Input**: None
**Output**: Array of connected devices with their IDs, states, and models

#### 2. `get_current_activity`
Gets enhanced information about the current foreground activity.

**Input**:
- `deviceId` (optional): Android device ID

**Output**: Current package name, activity name, PID, and window state information

#### 3. `get_view_attributes`
Gets UI hierarchy with enhanced view attributes by temporarily enabling debug mode.

**Input**:
- `deviceId` (optional): Android device ID

**Output**: UI hierarchy with detailed view attributes, layout parameters, and debug information

#### 4. `find_elements`
Searches for UI elements using various criteria.

**Input**:
- `deviceId` (optional): Android device ID
- `resourceId` (optional): Resource ID to search for
- `text` (optional): Text content to search for
- `className` (optional): Class name to search for  
- `exactMatch` (optional): Whether to use exact text matching (default: false)

**Output**: Array of matching UI elements

#### 5. `view_hierarchy`
Gets the UI view hierarchy from uiautomator dump with rotation information.

**Input**:
- `deviceId` (optional): Android device ID

**Output**: Structured view hierarchy with device info, timestamp, rotation, and UI element tree

#### 6. `click_coordinate`
Clicks at specific (x,y) coordinates on the Android device screen.

**Input**:
- `x` (required): X coordinate to click
- `y` (required): Y coordinate to click
- `deviceId` (optional): Android device ID

**Output**: Success confirmation with device info, coordinates, and timestamp

#### 7. `swipe_coordinate`
Swipe from start (x,y) coordinates to end (x,y) coordinates on the Android device screen.

**Input**:
- `deviceId` (optional): Android device ID
- `startX` (required): Starting X coordinate
- `startY` (required): Starting Y coordinate
- `endX` (required): Ending X coordinate
- `endY` (required): Ending Y coordinate
- `duration` (optional): Swipe duration in milliseconds (default: 300)

**Output**: Success confirmation with device info, coordinates, duration, and timestamp

#### 8. `input_text`
Input text on the Android device (types text into focused field).

**Input**:
- `deviceId` (optional): Android device ID
- `text` (required): Text to input on the device

**Output**: Success confirmation with device info, text, and timestamp

#### 9. `key_event`
Send key event to Android device (e.g., Enter=66, Back=4, Home=3).

**Input**:
- `deviceId` (optional): Android device ID
- `keyCode` (required): Android key code (e.g., 66 for Enter, 4 for Back)

**Output**: Success confirmation with device info, key code, and timestamp

## Architecture

### Kotlin-Specific Improvements

- **Coroutines**: All ADB operations use Kotlin coroutines for non-blocking execution
- **Type Safety**: Leverages Kotlin's null safety and type system
- **Data Classes**: Clean, immutable data structures with built-in serialization
- **Sealed Classes**: Type-safe error handling with sealed error classes
- **Concurrency**: Better thread safety with concurrent data structures
- **Clean Architecture**: SOLID principles with separated concerns and focused classes
- **Parameter Optimization**: SwipeParams data class reduces parameter count from 7+ to 2
- **Specific Exception Handling**: Enhanced error types for better debugging and handling

### Key Components

**Main (`Main.kt`)**
- Entry point that starts the MCP server using coroutines

**MCP Server (`McpServer.kt`)**
- Handles JSON-RPC communication over stdio
- Implements all MCP tool endpoints
- Manages device caching and connection pooling

**ADB Manager (`AdbManager.kt`)**
- Refactored facade coordinating specialized components following SOLID principles
- Delegates to focused classes: DeviceManager, UIInspector, DeviceInputController
- Maintains backward compatibility while providing cleaner architecture
- Provides both legacy parameter methods and new SwipeParams-based methods

**Specialized ADB Components:**
- **AdbPathResolver**: Auto-discovery of ADB path with caching and fallback strategies
- **DeviceManager**: Device listing and management operations with connection pooling
- **UIInspector**: UI hierarchy inspection with debug mode and XML extraction
- **DeviceInputController**: All input operations (tap, swipe, text, key events)
- **ShellCommandExecutor**: Persistent ADB shell connections with command batching

**UI Hierarchy Parser (`UIHierarchyParser.kt`)**
- Custom XML parser for Android UI hierarchies
- Element search utilities with functional programming patterns
- Support for enhanced Layout Inspector attributes
- **XmlExtractionUtils**: Dedicated utility for XML content extraction with multiple fallback strategies

**Type System (`model/` package)**
- Comprehensive Kotlin data classes with serialization support
- Enhanced sealed class hierarchy for type-safe error handling
- SwipeParams data class for parameter optimization
- AndroidMcpConstants for centralized configuration
- Enum classes for device states and comprehensive error codes

### Performance Optimizations

The Kotlin version includes several performance improvements:

1. **Coroutine-based Execution**: Non-blocking ADB operations
2. **Advanced Caching**: Device list and ADB path caching with TTL
3. **Connection Pooling**: Persistent device connections with cleanup
4. **Parallel Processing**: Concurrent execution of independent ADB commands
5. **Reduced Latency**: Optimized debug mode timing (200ms vs 500ms)

## UI Element Properties

Each UI element in the hierarchy contains the following properties:

- `class`: Android class name (e.g., "android.widget.TextView")
- `package`: App package name
- `text`: Visible text content (if any)
- `resource-id`: Android resource identifier
- `content-desc`: Content description for accessibility
- `bounds`: Element bounds `{left, top, right, bottom}`
- Boolean properties: `checkable`, `checked`, `clickable`, `enabled`, `focusable`, `focused`, `scrollable`, `long-clickable`, `password`, `selected`, `visible`
- `children`: Array of child UI elements

## Error Handling

The Kotlin version uses sealed classes for type-safe error handling with enhanced specific exception types:

```kotlin
sealed class LayoutInspectorError {
    class AdbNotFound(message: String)
    class DeviceNotFound(message: String, deviceId: String?)
    class UiDumpFailed(message: String, deviceId: String?)
    class ParseError(message: String, deviceId: String?)
    class ConnectionClosed(message: String, deviceId: String?)
    class CommandTimeout(message: String, deviceId: String?)
    class ShellCommandFailed(message: String, deviceId: String?)
    class McpProtocolError(message: String, deviceId: String?)
    class UnknownError(message: String, deviceId: String?)
}
```

The server includes comprehensive error handling with specific error codes:

- `ADB_NOT_FOUND`: ADB command not found in PATH
- `DEVICE_NOT_FOUND`: Specified Android device not found
- `UI_DUMP_FAILED`: Failed to dump UI hierarchy
- `PARSE_ERROR`: Failed to parse XML hierarchy
- `CONNECTION_CLOSED`: ADB connection closed unexpectedly
- `COMMAND_TIMEOUT`: Command execution timeout
- `SHELL_COMMAND_FAILED`: Shell command execution failed
- `MCP_PROTOCOL_ERROR`: MCP protocol violations
- `UNKNOWN_ERROR`: Generic error

## Development

### Project Structure

```
src/main/kotlin/de/cadeda/mcp/
├── Main.kt                    # Application entry point
├── server/
│   ├── McpServer.kt          # MCP protocol implementation
│   └── tools/                # Individual tool implementations
│       ├── ClickCoordinateTool.kt
│       ├── SwipeCoordinateTool.kt
│       ├── InputTextTool.kt
│       ├── KeyEventTool.kt
│       └── FindElementsTool.kt
├── adb/                      # ADB interaction layer
│   ├── AdbManager.kt         # Facade coordinating specialized components
│   ├── AdbPathResolver.kt    # ADB path discovery with caching
│   ├── DeviceManager.kt      # Device listing and management
│   ├── UIInspector.kt        # UI hierarchy inspection
│   ├── DeviceInputController.kt # Input operations (tap, swipe, text, keys)
│   ├── ShellCommandExecutor.kt  # Persistent ADB shell connections
│   ├── UIHierarchyParser.kt  # XML parsing and element search
│   ├── XmlExtractionUtils.kt # XML content extraction utilities
│   └── AndroidKeyConstants.kt # Android key code constants
└── model/
    ├── SwipeParams.kt        # Parameter optimization for swipe operations
    ├── AndroidMcpConstants.kt # Centralized constants
    ├── AndroidResult.kt      # Result type for consistent error handling
    └── uihierarchy/          # UI hierarchy types and error classes
        └── LayoutInspectorError.kt # Specific exception types
```

### Building from Source

```bash
# Clean and build
./gradlew clean build

# Run tests
./gradlew test

# Create distribution JAR
./gradlew jar
```

### Version Management

The project version is centrally managed in `build.gradle.kts`. The build system automatically:
- Generates `AppVersion.kt` with the current version
- Creates a version-less JAR file (`android-mcp.jar`) for deployment stability
- Embeds version information in the JAR manifest for debugging
- Updates version references throughout the codebase

To update the version, modify only the `version` property in `build.gradle.kts`.

**JAR Naming**: The JAR uses a version-less name (`android-mcp.jar`) following best practices for standalone applications. This ensures configuration files and deployment scripts remain stable across version updates. The actual version is available via the JAR manifest and runtime version reporting.

## MCP Integration

This server implements the Model Context Protocol and can be used with any MCP-compatible client:

1. **Claude Desktop**: Add to your MCP configuration
2. **Claude CLI**: Add to your CLI configuration
3. **Custom Applications**: Connect via stdio transport
4. **Development Tools**: Use for Android app testing and analysis

### Claude Desktop Configuration

```json
{
  "mcpServers": {
    "android-mcp": {
      "command": "java",
      "args": ["-jar", "/path/to/android-mcp/build/libs/android-mcp.jar"]
    }
  }
}
```

### Claude CLI Setup

**Manual Setup:**

```bash
claude mcp add android-mcp java -jar /path/to/android-mcp/build/libs/android-mcp.jar
```

**Usage Examples:**
```bash
claude "List my connected Android devices"
claude "Show me the UI hierarchy of my Android screen"
claude "Get the view hierarchy with rotation info from my Android device"
claude "Find elements with text Settings on my Android device"
claude "Click at coordinates 200,300 on my Android device"
claude "Swipe from 100,500 to 100,200 on my Android device"
claude "Input text 'hello world' on my Android device"
claude "Send Enter key (code 66) to my Android device"
```

## Use Cases

- **UI Testing**: Automated testing of Android applications
- **Accessibility Analysis**: Analyze app accessibility properties
- **Layout Debugging**: Debug layout issues and element positioning
- **App Analysis**: Reverse engineer app UI structures
- **Quality Assurance**: Validate UI consistency across screens
- **Automated Interaction**: Programmatically interact with Android apps through touch events, swipes, and text input
- **Performance Testing**: Measure UI response times and interaction latencies
- **User Journey Automation**: Automate complex user workflows and scenarios

## Troubleshooting

### Common Issues

1. **Java Version**: Ensure Java 17+ is installed
2. **ADB Path**: Make sure Android SDK platform-tools are in PATH
3. **Device Connection**: Verify device is connected and authorized
4. **JAR Location**: Update paths in Claude config if JAR location changes

### ADB Issues
- The server automatically searches for ADB in common locations
- If ADB is not found, install Android SDK platform-tools
- Verify USB debugging is enabled on your Android device
- Check device authorization status with `adb devices` (or use full path if needed)

### Permission Issues
- Ensure your Android device is authorized for debugging
- Check that the connected device appears in `adb devices`

### UI Dump Failures
- Some apps may block UI inspection for security reasons
- Ensure the target app is in the foreground
- Try refreshing the device connection

### Debug Mode

Add logging to see detailed ADB command execution:

```kotlin
// In AdbManager.kt, uncomment debug statements
System.err.println("Executing: ${command.joinToString(" ")}")
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make changes in the Kotlin source files
4. Test with `./gradlew test`
5. Build with `./gradlew build`
6. Submit a pull request

## License

MIT License.