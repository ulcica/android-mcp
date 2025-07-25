# Android Layout Inspector MCP Server

A Model Context Protocol (MCP) server that provides Android Layout Inspector functionality through ADB commands. This server allows LLMs and other MCP clients to inspect Android UI hierarchies, find elements, take screenshots, and analyze app layouts.

## Features

- **Device Management**: List and connect to Android devices via ADB with automatic fallback paths
- **Enhanced UI Hierarchy Inspection**: Extract complete UI hierarchy using Layout Inspector features
- **Debug View Attributes**: Access detailed view attributes and layout parameters
- **Element Search**: Find UI elements by resource ID, text content, or class name
- **Screenshot Capture**: Take device screenshots in base64 format

## Prerequisites

- Node.js (version 14 or higher)
- Android Debug Bridge (ADB) - automatically detects from:
  - System PATH
  - `~/Library/Android/sdk/platform-tools/adb` (macOS)
  - `~/Android/Sdk/platform-tools/adb` (Windows/Linux)
  - `/usr/local/bin/adb` (Homebrew)
  - `/opt/android-sdk/platform-tools/adb` (Linux)
- Android device connected via USB with USB debugging enabled
- TypeScript (for development)

## Installation

1. Clone or download this repository
2. Install dependencies:
   ```bash
   npm install
   ```
3. Build the project:
   ```bash
   npm run build
   ```

## Usage

### Running the Server

```bash
npm start
```

The server communicates via stdio and implements the Model Context Protocol.

### Available Tools

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

#### 5. `take_screenshot`
Captures a screenshot of the device screen.

**Input**:
- `deviceId` (optional): Android device ID

**Output**: Native MCP image content (PNG format) with metadata text

#### 6. `view_hierarchy`
Gets the UI view hierarchy from uiautomator dump with rotation information.

**Input**:
- `deviceId` (optional): Android device ID

**Output**: Structured view hierarchy with device info, timestamp, rotation, and UI element tree

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

The server includes comprehensive error handling with specific error codes:

- `ADB_NOT_FOUND`: ADB command not found in PATH
- `DEVICE_NOT_FOUND`: Specified Android device not found
- `UI_DUMP_FAILED`: Failed to dump UI hierarchy
- `PARSE_ERROR`: Failed to parse XML hierarchy
- `UNKNOWN_ERROR`: Generic error

## Development

### Project Structure

```
src/
├── index.ts           # Main MCP server implementation
├── types.ts           # TypeScript type definitions
├── utils/
│   ├── adb.ts         # ADB command wrapper utilities
│   └── xmlParser.ts   # UI hierarchy XML parser
└── tools/             # (Reserved for additional tools)
```

### Building

```bash
npm run build
```

### Development Mode

```bash
npm run dev
```

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
      "command": "node",
      "args": ["/path/to/android-mcp/dist/index.js"]
    }
  }
}
```

### Claude CLI Setup

**Manual Setup:**

```bash
claude mcp add android-mcp node /path/to/android-mcp/dist/index.js
```

**Usage Examples:**
```bash
claude "List my connected Android devices"
claude "Show me the UI hierarchy of my Android screen"
claude "Get the view hierarchy with rotation info from my Android device"
claude "Take a screenshot of my Android device"
claude "Find elements with text Settings on my Android device"
```

## Use Cases

- **UI Testing**: Automated testing of Android applications
- **Accessibility Analysis**: Analyze app accessibility properties
- **Layout Debugging**: Debug layout issues and element positioning
- **App Analysis**: Reverse engineer app UI structures
- **Quality Assurance**: Validate UI consistency across screens

## Troubleshooting

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

## License

MIT License.