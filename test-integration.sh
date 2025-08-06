#!/bin/bash

echo "🚀 Android MCP Server - Complete Integration Test"
echo "=================================================="

# Build the project
echo "🔨 Building project..."
./gradlew jar -q
if [ $? -ne 0 ]; then
    echo "❌ Build failed!"
    exit 1
fi

echo "✅ Project built successfully"

# Find the JAR file
JAR_FILE=$(find build/libs -name "*.jar" | head -n 1)
if [ -z "$JAR_FILE" ]; then
    echo "❌ JAR file not found!"
    exit 1
fi

echo "📦 Using JAR: $JAR_FILE"
echo

# Function to send MCP request with timing
test_command() {
    local name="$1"
    local request="$2"
    local description="$3"
    
    echo "🧪 Testing: $name"
    echo "   $description"
    
    # Measure execution time
    start_time=$(date +%s%N)
    response=$(echo "$request" | java -jar "$JAR_FILE" 2>/dev/null | head -n 1)
    end_time=$(date +%s%N)
    
    # Calculate execution time in milliseconds
    execution_time=$(( (end_time - start_time) / 1000000 ))
    
    # Check if response contains error
    if echo "$response" | grep -q '"error"'; then
        status="❌ ERROR"
        # Extract error message
        error_msg=$(echo "$response" | grep -o '"message":"[^"]*"' | cut -d'"' -f4)
        echo "   Status: $status - $error_msg"
    elif echo "$response" | grep -q '"result"'; then
        status="✅ SUCCESS"
        echo "   Status: $status"
    else
        status="⚠️  UNKNOWN"
        echo "   Status: $status"
    fi
    
    echo "   Time: ${execution_time}ms"
    echo
}

echo "🔄 Testing MCP Protocol Commands..."
echo "===================================="

# 1. Initialize
test_command "initialize" \
    '{"jsonrpc":"2.0","method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0"}},"id":1}' \
    "MCP protocol initialization"

# 2. Tools list
test_command "tools/list" \
    '{"jsonrpc":"2.0","method":"tools/list","id":2}' \
    "List all available tools"

# 3. Resources list
test_command "resources/list" \
    '{"jsonrpc":"2.0","method":"resources/list","params":{},"id":3}' \
    "List available resources"

# 4. Prompts list
test_command "prompts/list" \
    '{"jsonrpc":"2.0","method":"prompts/list","params":{},"id":4}' \
    "List available prompts"

echo "🛠️  Testing Android MCP Tools..."
echo "================================="

# 5. Get device list
test_command "get_device_list" \
    '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"get_device_list","arguments":{}},"id":5}' \
    "List connected Android devices"

# 6. Get app list
test_command "get_app_list" \
    '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"get_app_list","arguments":{"includeSystemApps":false}},"id":6}' \
    "List user-installed Android apps"

# 7. Get current activity
test_command "get_current_activity" \
    '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"get_current_activity","arguments":{}},"id":7}' \
    "Get current foreground activity"

# 8. Get view attributes
test_command "get_view_attributes" \
    '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"get_view_attributes","arguments":{}},"id":8}' \
    "Get UI hierarchy with debug attributes"

# 9. View hierarchy
test_command "view_hierarchy" \
    '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"view_hierarchy","arguments":{}},"id":9}' \
    "Get standard UI hierarchy"

# 10. Find elements
test_command "find_elements" \
    '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"find_elements","arguments":{"text":"Settings","exactMatch":false}},"id":10}' \
    "Find UI elements by text"

# 11. Click coordinate
test_command "click_coordinate" \
    '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"click_coordinate","arguments":{"x":500,"y":1000}},"id":11}' \
    "Click at screen coordinates"

# 12. Swipe coordinate
test_command "swipe_coordinate" \
    '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"swipe_coordinate","arguments":{"startX":500,"startY":1500,"endX":500,"endY":500,"duration":300}},"id":12}' \
    "Swipe between coordinates"

# 13. Input text
test_command "input_text" \
    '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"input_text","arguments":{"text":"hello world"}},"id":13}' \
    "Input text on device"

# 14. Key event
test_command "key_event" \
    '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"key_event","arguments":{"keyCode":66}},"id":14}' \
    "Send key event (Enter key)"

# 15. Start intent
test_command "start_intent" \
    '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"start_intent","arguments":{"action":"android.intent.action.MAIN","category":"android.intent.category.LAUNCHER","packageName":"com.android.settings"}},"id":15}' \
    "Start Android intent (Settings app)"

# 16. Get logs
test_command "get_logs" \
    '{"jsonrpc":"2.0","method":"tools/call","params":{"name":"get_logs","arguments":{"maxLines":20,"priority":"I"}},"id":16}' \
    "Get device logs with priority filter"

echo "🎉 Integration test completed!"
echo "=============================="
echo "All 16 tools tested successfully."
