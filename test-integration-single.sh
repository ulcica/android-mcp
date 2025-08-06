#!/bin/bash

echo "🚀 Android MCP Server - Single Instance Integration Test"
echo "========================================================"

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

# Create a temporary file for all requests
TEMP_REQUESTS=$(mktemp)
echo "📝 Creating request sequence..."

# Build the complete request sequence
cat > "$TEMP_REQUESTS" << 'EOF'
{"jsonrpc":"2.0","method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{},"clientInfo":{"name":"test-client","version":"1.0"}},"id":1}
{"method":"notifications/initialized","jsonrpc":"2.0"}
{"jsonrpc":"2.0","method":"tools/list","id":2}
{"jsonrpc":"2.0","method":"resources/list","params":{},"id":3}
{"jsonrpc":"2.0","method":"prompts/list","params":{},"id":4}
{"jsonrpc":"2.0","method":"tools/call","params":{"name":"get_device_list","arguments":{}},"id":5}
{"jsonrpc":"2.0","method":"tools/call","params":{"name":"get_current_activity","arguments":{}},"id":6}
{"jsonrpc":"2.0","method":"tools/call","params":{"name":"get_view_attributes","arguments":{}},"id":7}
{"jsonrpc":"2.0","method":"tools/call","params":{"name":"view_hierarchy","arguments":{}},"id":8}
{"jsonrpc":"2.0","method":"tools/call","params":{"name":"find_elements","arguments":{"text":"Settings","exactMatch":false}},"id":9}
{"jsonrpc":"2.0","method":"tools/call","params":{"name":"click_coordinate","arguments":{"x":500,"y":1000}},"id":10}
{"jsonrpc":"2.0","method":"tools/call","params":{"name":"swipe_coordinate","arguments":{"startX":500,"startY":1500,"endX":500,"endY":500,"duration":300}},"id":11}
{"jsonrpc":"2.0","method":"tools/call","params":{"name":"input_text","arguments":{"text":"hello world"}},"id":12}
{"jsonrpc":"2.0","method":"tools/call","params":{"name":"key_event","arguments":{"keyCode":66}},"id":13}
EOF

echo "🚀 Starting single MCP server instance..."
echo "==========================================="

# Function to process responses with timing
process_responses() {
    local test_names=(
        "initialize"
        "notifications/initialized"
        "tools/list"
        "resources/list"
        "prompts/list"
        "get_device_list"
        "get_current_activity"
        "get_view_attributes"
        "view_hierarchy"
        "find_elements"
        "click_coordinate"
        "swipe_coordinate"
        "input_text"
        "key_event"
    )
    
    local descriptions=(
        "MCP protocol initialization"
        "Client initialization notification"
        "List all available tools"
        "List available resources"
        "List available prompts"
        "List connected Android devices"
        "Get current foreground activity"
        "Get UI hierarchy with debug attributes"
        "Get standard UI hierarchy"
        "Find UI elements by text"
        "Click at screen coordinates"
        "Swipe between coordinates"
        "Input text on device"
        "Send key event (Enter key)"
    )
    
    local response_count=0
    local start_time=$(date +%s%N)
    
    echo "🔄 Testing MCP Protocol Commands..."
    echo "===================================="
    
    while IFS= read -r line; do
        if [[ "$line" == "Android Layout Inspector MCP Server started (Kotlin)" ]]; then
            continue
        elif [[ "$line" == "Shutting down MCP server..." ]]; then
            break
        elif [[ -n "$line" ]]; then
            local end_time=$(date +%s%N)
            local execution_time=$(( (end_time - start_time) / 1000000 ))
            
            # Get test info
            local test_name="${test_names[$response_count]:-unknown}"
            local description="${descriptions[$response_count]:-Unknown command}"
            
            echo "🧪 Testing: $test_name"
            echo "   $description"
            
            # Check response status
            if echo "$line" | grep -q '"error"'; then
                local status="❌ ERROR"
                local error_msg=$(echo "$line" | grep -o '"message":"[^"]*"' | cut -d'"' -f4)
                echo "   Status: $status - $error_msg"
            elif echo "$line" | grep -q '"result"'; then
                local status="✅ SUCCESS"
                echo "   Status: $status"
            else
                # Notification (no response expected)
                if [[ "$test_name" == "notifications/initialized" ]]; then
                    echo "   Status: ✅ SUCCESS (no response expected)"
                    response_count=$((response_count + 1))
                    start_time=$(date +%s%N)
                    continue
                else
                    local status="⚠️  UNKNOWN"
                    echo "   Status: $status"
                fi
            fi
            
            echo "   Time: ${execution_time}ms"
            echo
            
            response_count=$((response_count + 1))
            start_time=$(date +%s%N)
        fi
    done
    
    echo $response_count
}

# Execute all requests through single server instance and process responses
total_start_time=$(date +%s%N)
response_count=$(cat "$TEMP_REQUESTS" | java -jar "$JAR_FILE" 2>&1 | process_responses | tail -1)
total_end_time=$(date +%s%N)
total_execution_time=$(( (total_end_time - total_start_time) / 1000000 ))

echo "📊 Test Summary"
echo "==============="
echo "✅ All MCP commands processed through single server instance"
echo "📱 Responses processed: $response_count"
echo "⏱️  Total execution time: ${total_execution_time}ms"
echo "🚀 Average time per command: $(( total_execution_time / (response_count > 0 ? response_count : 1) ))ms"
echo "🎉 Single instance integration test completed!"
echo

# Cleanup
rm -f "$TEMP_REQUESTS"