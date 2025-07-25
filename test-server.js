#!/usr/bin/env node

const { spawn } = require('child_process');
const readline = require('readline');

class MCPTester {
  constructor() {
    this.server = null;
    this.requestId = 1;
    this.showRawOutput = true;
    this.showPrettyOutput = true;
  }

  start() {
    console.log('🚀 Starting Android Layout Inspector MCP Server...\n');
    
    // Start the server process
    this.server = spawn('node', ['dist/index.js'], {
      stdio: ['pipe', 'pipe', 'pipe']
    });

    this.server.stderr.on('data', (data) => {
      console.log('Server:', data.toString().trim());
    });

    this.server.stdout.on('data', (data) => {
      try {
        const response = JSON.parse(data.toString());
        
        if (this.showRawOutput) {
          console.log('\n📨 Raw Response:');
          console.log(JSON.stringify(response, null, 2));
        }
        
        if (this.showPrettyOutput) {
          // Pretty print the response
          this.prettyPrintResponse(response);
        }
      } catch (e) {
        console.log('📨 Raw Response:', data.toString());
      }
    });

    this.server.on('close', (code) => {
      console.log(`\n❌ Server process exited with code ${code}`);
      process.exit(code);
    });

    // Initialize MCP handshake
    this.sendInitialize();
    
    // Start interactive mode after a short delay
    setTimeout(() => {
      this.startInteractiveMode();
    }, 1000);
  }

  sendInitialize() {
    console.log('🤝 Sending initialization...');
    const initRequest = {
      jsonrpc: '2.0',
      id: this.requestId++,
      method: 'initialize',
      params: {
        protocolVersion: '2024-11-05',
        capabilities: {
          tools: {}
        },
        clientInfo: {
          name: 'test-client',
          version: '1.0.0'
        }
      }
    };
    
    this.sendRequest(initRequest);
  }

  sendRequest(request) {
    const requestStr = JSON.stringify(request) + '\n';
    console.log('📤 Sending:', JSON.stringify(request, null, 2));
    this.server.stdin.write(requestStr);
  }

  startInteractiveMode() {
    console.log('\n🎮 Interactive Mode Started!');
    console.log('Available commands:');
    console.log('  1 - List tools');
    console.log('  2 - Get device list');
    console.log('  3 - Get view attributes (debug mode)');
    console.log('  4 - Get current activity');
    console.log('  5 - Find elements (by text "Settings")');
    console.log('  6 - Take screenshot');
    console.log('  7 - Get view hierarchy');
    console.log('  r - Toggle raw output (current: ' + (this.showRawOutput ? 'ON' : 'OFF') + ')');
    console.log('  p - Toggle pretty output (current: ' + (this.showPrettyOutput ? 'ON' : 'OFF') + ')');
    console.log('  q - Quit');
    console.log('');

    const rl = readline.createInterface({
      input: process.stdin,
      output: process.stdout
    });

    const promptUser = () => {
      rl.question('Enter command (1-7, r, p, or q): ', (answer) => {
        this.handleCommand(answer.trim(), rl, promptUser);
      });
    };

    promptUser();
  }

  handleCommand(command, rl, promptFunction) {
    switch (command) {
      case '1':
        this.listTools();
        break;
      case '2':
        this.getDeviceList();
        break;
      case '3':
        this.getViewAttributes();
        break;
      case '4':
        this.getCurrentActivity();
        break;
      case '5':
        this.findElements();
        break;
      case '6':
        this.takeScreenshot();
        break;
      case '7':
        this.getViewHierarchy();
        break;
      case 'r':
        this.showRawOutput = !this.showRawOutput;
        console.log(`🔄 Raw output is now ${this.showRawOutput ? 'ON' : 'OFF'}`);
        break;
      case 'p':
        this.showPrettyOutput = !this.showPrettyOutput;
        console.log(`🎨 Pretty output is now ${this.showPrettyOutput ? 'ON' : 'OFF'}`);
        break;
      case 'q':
        console.log('👋 Goodbye!');
        rl.close();
        this.server.kill();
        return;
      default:
        console.log('❌ Invalid command. Please enter 1-7, r, p, or q. Try again.');
    }
    
    setTimeout(promptFunction, 500);
  }

  listTools() {
    const request = {
      jsonrpc: '2.0',
      id: this.requestId++,
      method: 'tools/list',
      params: {}
    };
    this.sendRequest(request);
  }

  getDeviceList() {
    const request = {
      jsonrpc: '2.0',
      id: this.requestId++,
      method: 'tools/call',
      params: {
        name: 'get_device_list',
        arguments: {}
      }
    };
    this.sendRequest(request);
  }



  getViewAttributes() {
    console.log('🔧 Getting view attributes (enabling debug mode temporarily)...');
    const request = {
      jsonrpc: '2.0',
      id: this.requestId++,
      method: 'tools/call',
      params: {
        name: 'get_view_attributes',
        arguments: {}
      }
    };
    this.sendRequest(request);
  }

  getCurrentActivity() {
    const request = {
      jsonrpc: '2.0',
      id: this.requestId++,
      method: 'tools/call',
      params: {
        name: 'get_current_activity',
        arguments: {}
      }
    };
    this.sendRequest(request);
  }

  findElements() {
    const request = {
      jsonrpc: '2.0',
      id: this.requestId++,
      method: 'tools/call',
      params: {
        name: 'find_elements',
        arguments: {
          text: 'Settings'
        }
      }
    };
    this.sendRequest(request);
  }

  takeScreenshot() {
    console.log('📸 Taking screenshot (this may take a moment)...');
    const request = {
      jsonrpc: '2.0',
      id: this.requestId++,
      method: 'tools/call',
      params: {
        name: 'take_screenshot',
        arguments: {}
      }
    };
    this.sendRequest(request);
  }

  getViewHierarchy() {
    console.log('🌳 Getting view hierarchy...');
    const request = {
      jsonrpc: '2.0',
      id: this.requestId++,
      method: 'tools/call',
      params: {
        name: 'view_hierarchy',
        arguments: {}
      }
    };
    this.sendRequest(request);
  }

  prettyPrintResponse(response) {
    console.log('\\n\ud83c\udf89 Pretty Output:');
    console.log('='.repeat(50));
    
    if (response.error) {
      console.log('\u274c Error:', response.error.message);
      if (response.error.data) {
        console.log('Details:', response.error.data);
      }
      return;
    }

    if (!response.result || !response.result.content) {
      console.log('\ud83d\udcac No content in response');
      return;
    }

    try {
      const contents = response.result.content;
      
      // Handle mixed content (e.g., image + text)
      const imageContent = contents.find(c => c.type === 'image');
      const textContent = contents.find(c => c.type === 'text');
      
      if (imageContent) {
        this.formatScreenshotResponse(imageContent, textContent);
      } else if (textContent) {
        const data = JSON.parse(textContent.text);
        this.formatOutputByType(data);
      } else {
        console.log('\ud83d\udcac Unknown content type');
      }
    } catch (e) {
      console.log('\ud83d\udcac Response content:', response.result.content[0]?.text || 'No text content');
    }
  }

  formatScreenshotResponse(imageContent, textContent) {
    console.log('\ud83d\udcf7 Screenshot Response:');
    console.log(`\ud83d\uddbc\ufe0f Format: ${imageContent.mimeType}`);
    console.log(`\ud83d\udcbe Size: ${Math.round(imageContent.data.length / 1024)} KB (base64)`);
    
    if (textContent) {
      console.log(`\ud83d\udcc4 ${textContent.text}`);
    }
    
    console.log('\ud83c\uddee\ud83c\uddf2 Image available in native MCP format');
    console.log('  (Image should display directly in MCP-compatible clients)');
  }

  formatOutputByType(data) {
    // Detect the type of data and format accordingly
    if (Array.isArray(data)) {
      this.formatDeviceList(data);
    } else if (data.device && data.root && data.rotation !== undefined) {
      this.formatViewHierarchy(data);
    } else if (data.device && data.root) {
      this.formatUIHierarchy(data);
    } else if (data.device && data.searchCriteria) {
      this.formatSearchResults(data);
    } else if (data.device && data.screenshot) {
      this.formatScreenshot(data);
    } else if (data.package && data.activity) {
      this.formatActivityInfo(data);
    } else if (data.tools) {
      this.formatToolsList(data.tools);
    } else {
      console.log('\ud83d\udccb Data:', JSON.stringify(data, null, 2));
    }
  }

  formatDeviceList(devices) {
    console.log('\ud83d\udcf1 Connected Android Devices:');
    if (devices.length === 0) {
      console.log('  No devices found');
      return;
    }
    
    devices.forEach((device, index) => {
      console.log(`  ${index + 1}. Device ID: ${device.id}`);
      console.log(`     State: ${device.state === 'device' ? '\u2713 Ready' : '\u26a0\ufe0f ' + device.state}`);
      if (device.model) {
        console.log(`     Model: ${device.model}`);
      }
      console.log('');
    });
  }

  formatUIHierarchy(hierarchy) {
    console.log(`\ud83c\udf33 UI Hierarchy for ${hierarchy.device}`);
    console.log(`\ud83d\udd70\ufe0f Timestamp: ${new Date(hierarchy.timestamp).toLocaleString()}`);
    
    if (hierarchy.packageName) {
      console.log(`\ud83d\udce6 Package: ${hierarchy.packageName}`);
    }
    if (hierarchy.activityName) {
      console.log(`\ud83c\udfa8 Activity: ${hierarchy.activityName}`);
    }
    if (hierarchy.activityPid) {
      console.log(`\ud83c\udd94 PID: ${hierarchy.activityPid}`);
    }
    
    if (hierarchy.layoutInfo) {
      console.log(`\ud83d\udcf1 Screen: ${hierarchy.layoutInfo.screenSize.width}x${hierarchy.layoutInfo.screenSize.height}`);
      console.log(`\ud83d\udd0d Density: ${hierarchy.layoutInfo.density} DPI`);
      console.log(`\ud83d\udd04 Rotation: ${hierarchy.layoutInfo.rotation}\u00b0`);
    }
    
    if (hierarchy.windowInfo) {
      console.log(`\ud83d\udcbc Window: ${hierarchy.windowInfo.focused ? 'Focused' : 'Not Focused'} | ${hierarchy.windowInfo.visible ? 'Visible' : 'Hidden'}`);
    }
    
    console.log('\\n\ud83c\udf33 Element Tree:');
    this.printElementTree(hierarchy.root, 0);
  }

  printElementTree(element, depth, maxDepth = 3) {
    if (depth > maxDepth) return;
    
    const indent = '  '.repeat(depth);
    const prefix = depth === 0 ? '\ud83c\udf33' : (depth === 1 ? '\ud83c\udf32' : '\ud83c\udf31');
    
    let elementInfo = `${prefix} ${element.class.split('.').pop()}`;
    
    if (element.text) {
      elementInfo += ` "${element.text}"`;
    }
    if (element['resource-id']) {
      elementInfo += ` #${element['resource-id'].split('/').pop()}`;
    }
    
    const states = [];
    if (element.clickable) states.push('clickable');
    if (element.enabled) states.push('enabled');
    if (element.visible) states.push('visible');
    if (element.focused) states.push('focused');
    
    if (states.length > 0) {
      elementInfo += ` [${states.join(', ')}]`;
    }
    
    elementInfo += ` (${element.bounds.left},${element.bounds.top})-(${element.bounds.right},${element.bounds.bottom})`;
    
    console.log(indent + elementInfo);
    
    // Show first few children
    const childrenToShow = Math.min(element.children.length, 5);
    for (let i = 0; i < childrenToShow; i++) {
      this.printElementTree(element.children[i], depth + 1, maxDepth);
    }
    
    if (element.children.length > childrenToShow) {
      console.log(indent + `  ... and ${element.children.length - childrenToShow} more children`);
    }
  }

  formatViewHierarchy(hierarchy) {
    console.log(`🌳 View Hierarchy for ${hierarchy.device}`);
    console.log(`🕰️ Timestamp: ${new Date(hierarchy.timestamp).toLocaleString()}`);
    console.log(`🔄 Rotation: ${hierarchy.rotation}°`);
    
    console.log('\n🌳 Element Tree:');
    this.printElementTree(hierarchy.root, 0);
  }

  formatSearchResults(data) {
    console.log(`\ud83d\udd0d Search Results for ${data.device}`);
    console.log(`\ud83c\udfaf Criteria: ${JSON.stringify(data.searchCriteria)}`);
    console.log(`\ud83d\udcca Found ${data.count} elements`);
    
    if (data.results.length > 0) {
      console.log('\\n\ud83c\udfaf Matching Elements:');
      data.results.forEach((element, index) => {
        console.log(`  ${index + 1}. ${element.class.split('.').pop()}`);
        if (element.text) console.log(`     Text: "${element.text}"`);
        if (element['resource-id']) console.log(`     ID: ${element['resource-id']}`);
        console.log(`     Bounds: (${element.bounds.left},${element.bounds.top})-(${element.bounds.right},${element.bounds.bottom})`);
        console.log(`     States: clickable=${element.clickable}, enabled=${element.enabled}, visible=${element.visible}`);
        console.log('');
      });
    } else {
      console.log('  No matching elements found');
    }
  }

  formatScreenshot(data) {
    console.log(`\ud83d\udcf7 Screenshot for ${data.device}`);
    console.log(`\ud83d\udd70\ufe0f Timestamp: ${new Date(data.timestamp).toLocaleString()}`);
    console.log(`\ud83d\udcbe Size: ${Math.round(data.screenshot.length / 1024)} KB (base64)`);
    console.log('\ud83c\uddee\ud83c\uddf2 Screenshot data available in base64 format');
    console.log('  (Use the base64 data to save or display the image)');
  }

  formatActivityInfo(data) {
    console.log('\ud83c\udfa8 Current Activity Information:');
    console.log(`  Package: ${data.package}`);
    console.log(`  Activity: ${data.activity}`);
    if (data.pid) {
      console.log(`  PID: ${data.pid}`);
    }
    if (data.windowInfo) {
      console.log(`  Window State:`);
      console.log(`    Focused: ${data.windowInfo.focused ? '\u2713' : '\u2717'}`);
      console.log(`    Visible: ${data.windowInfo.visible ? '\u2713' : '\u2717'}`);
      console.log(`    Has Input Focus: ${data.windowInfo.hasInputFocus ? '\u2713' : '\u2717'}`);
    }
  }

  formatToolsList(tools) {
    console.log('\ud83d\udee0\ufe0f Available MCP Tools:');
    tools.forEach((tool, index) => {
      console.log(`  ${index + 1}. ${tool.name}`);
      console.log(`     ${tool.description}`);
      
      if (tool.inputSchema && tool.inputSchema.properties) {
        const props = Object.keys(tool.inputSchema.properties);
        if (props.length > 0) {
          console.log(`     Parameters: ${props.join(', ')}`);
        }
      }
      console.log('');
    });
  }
}

// Handle process termination gracefully
process.on('SIGINT', () => {
  console.log('\n👋 Shutting down...');
  process.exit(0);
});

// Start the tester
const tester = new MCPTester();
tester.start();