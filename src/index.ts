#!/usr/bin/env node

import {Server} from '@modelcontextprotocol/sdk/server/index.js';
import {StdioServerTransport} from '@modelcontextprotocol/sdk/server/stdio.js';
import {z} from 'zod';
import {ADBManager} from './utils/adb';
import {UIHierarchyParser} from './utils/xmlParser';
import {UIElement} from './types';
import {ListToolsRequestSchema, CallToolRequestSchema} from '@modelcontextprotocol/sdk/types.js';

const adbManager = ADBManager.getInstance();

// Helper function to get target device
async function getTargetDevice(deviceId?: string): Promise<string> {
    if (deviceId) {
        return deviceId;
    }
    
    const devices = await adbManager.getDevices();
    const availableDevice = devices.find(d => d.state === 'device');
    if (!availableDevice) {
        throw new Error('No available Android devices found');
    }
    return availableDevice.id;
}

// Zod schemas for tool inputs
const DeviceIdSchema = z.object({
    deviceId: z.string().optional().describe('Android device ID (optional, uses first available device if not specified)')
});


const FindElementsSchema = z.object({
    deviceId: z.string().optional().describe('Android device ID'),
    resourceId: z.string().optional().describe('Resource ID to search for'),
    text: z.string().optional().describe('Text content to search for'),
    className: z.string().optional().describe('Class name to search for'),
    exactMatch: z.boolean().optional().default(false).describe('Whether to use exact text matching')
});

const ClickCoordinateSchema = z.object({
    deviceId: z.string().optional().describe('Android device ID (optional, uses first available device if not specified)'),
    x: z.number().describe('X coordinate to click'),
    y: z.number().describe('Y coordinate to click')
});

const server = new Server({
    name: 'android-layout-inspector',
    version: '1.0.0'
}, {
    capabilities: {
        tools: {}
    }
});

// Tool: Get list of connected Android devices
server.setRequestHandler(ListToolsRequestSchema, async () => ({
    tools: [
        {
            name: 'get_device_list',
            description: 'Get list of connected Android devices',
            inputSchema: {
                type: 'object',
                properties: {},
                additionalProperties: false
            }
        },
        {
            name: 'get_view_attributes',
            description: 'Get UI hierarchy with enhanced view attributes (enables debug mode temporarily)',
            inputSchema: {
                type: 'object',
                properties: {
                    deviceId: {
                        type: 'string',
                        description: 'Android device ID (optional, uses first available device if not specified)'
                    }
                },
                additionalProperties: false
            }
        },
        {
            name: 'get_current_activity',
            description: 'Get information about the current foreground activity',
            inputSchema: {
                type: 'object',
                properties: {
                    deviceId: {
                        type: 'string',
                        description: 'Android device ID (optional, uses first available device if not specified)'
                    }
                },
                additionalProperties: false
            }
        },
        {
            name: 'find_elements',
            description: 'Find UI elements by various criteria (resource ID, text, class name)',
            inputSchema: {
                type: 'object',
                properties: {
                    deviceId: {
                        type: 'string',
                        description: 'Android device ID'
                    },
                    resourceId: {
                        type: 'string',
                        description: 'Resource ID to search for'
                    },
                    text: {
                        type: 'string',
                        description: 'Text content to search for'
                    },
                    className: {
                        type: 'string',
                        description: 'Class name to search for'
                    },
                    exactMatch: {
                        type: 'boolean',
                        description: 'Whether to use exact text matching',
                        default: false
                    }
                },
                additionalProperties: false
            }
        },
        {
            name: 'take_screenshot',
            description: 'Take a screenshot of the Android device screen',
            inputSchema: {
                type: 'object',
                properties: {
                    deviceId: {
                        type: 'string',
                        description: 'Android device ID (optional, uses first available device if not specified)'
                    }
                },
                additionalProperties: false
            }
        },
        {
            name: 'view_hierarchy',
            description: 'Get the UI view hierarchy from uiautomator dump',
            inputSchema: {
                type: 'object',
                properties: {
                    deviceId: {
                        type: 'string',
                        description: 'Android device ID (optional, uses first available device if not specified)'
                    }
                },
                additionalProperties: false
            }
        },
        {
            name: 'click_coordinate',
            description: 'Click at specific (x,y) coordinates on the Android device screen',
            inputSchema: {
                type: 'object',
                properties: {
                    deviceId: {
                        type: 'string',
                        description: 'Android device ID (optional, uses first available device if not specified)'
                    },
                    x: {
                        type: 'number',
                        description: 'X coordinate to click'
                    },
                    y: {
                        type: 'number',
                        description: 'Y coordinate to click'
                    }
                },
                required: ['x', 'y'],
                additionalProperties: false
            }
        }
    ]
}));

// Tool implementations
server.setRequestHandler(CallToolRequestSchema, async (request) => {
    const {name, arguments: args} = request.params;

    try {
        switch (name) {
            case 'get_device_list': {
                const devices = await adbManager.getDevices();
                return {
                    content: [
                        {
                            type: 'text',
                            text: JSON.stringify(devices, null, 2)
                        }
                    ]
                };
            }


            case 'get_view_attributes': {
                const input = DeviceIdSchema.parse(args);
                const targetDevice = await getTargetDevice(input.deviceId);

                const xmlContent = await adbManager.getViewAttributes(targetDevice);
                const hierarchy = UIHierarchyParser.parseUIHierarchy(xmlContent, targetDevice);

                // Get current activity info
                const activityInfo = await adbManager.getCurrentActivity(targetDevice);
                if (activityInfo) {
                    hierarchy.packageName = activityInfo.package;
                    hierarchy.activityName = activityInfo.activity;
                    hierarchy.activityPid = activityInfo.pid;
                    hierarchy.windowInfo = activityInfo.windowInfo;
                }

                return {
                    content: [
                        {
                            type: 'text',
                            text: JSON.stringify(hierarchy, null, 2)
                        }
                    ]
                };
            }

            case 'get_current_activity': {
                const input = DeviceIdSchema.parse(args);
                const targetDevice = await getTargetDevice(input.deviceId);

                const activityInfo = await adbManager.getCurrentActivity(targetDevice);

                return {
                    content: [
                        {
                            type: 'text',
                            text: JSON.stringify(activityInfo || {message: 'Could not determine current activity'}, null, 2)
                        }
                    ]
                };
            }

            case 'find_elements': {
                const input = FindElementsSchema.parse(args);
                const targetDevice = await getTargetDevice(input.deviceId);

                const xmlContent = await adbManager.dumpUIHierarchy(targetDevice);
                const hierarchy = UIHierarchyParser.parseUIHierarchy(xmlContent, targetDevice);

                if (!input.resourceId && !input.text && !input.className) {
                    return {
                        content: [
                            {
                                type: 'text',
                                text: 'Error: Must specify at least one search criteria: resourceId, text, or className'
                            }
                        ],
                        isError: true
                    };
                }

                let results: UIElement[] = [];

                if (input.resourceId) {
                    results = UIHierarchyParser.findElementsById(hierarchy.root, input.resourceId);
                } else if (input.text) {
                    results = UIHierarchyParser.findElementsByText(hierarchy.root, input.text, input.exactMatch);
                } else if (input.className) {
                    results = UIHierarchyParser.findElementsByClass(hierarchy.root, input.className);
                }

                return {
                    content: [
                        {
                            type: 'text',
                            text: JSON.stringify({
                                device: targetDevice,
                                searchCriteria: input,
                                results,
                                count: results.length
                            }, null, 2)
                        }
                    ]
                };
            }

            case 'take_screenshot': {
                const input = DeviceIdSchema.parse(args);
                const targetDevice = await getTargetDevice(input.deviceId);

                const screenshot = await adbManager.takeScreenshot(targetDevice);
                const base64Screenshot = screenshot.toString('base64');

                return {
                    content: [
                        {
                            type: 'image',
                            data: base64Screenshot,
                            mimeType: 'image/png'
                        },
                        {
                            type: 'text',
                            text: `Screenshot captured from device: ${targetDevice} at ${new Date().toISOString()}`
                        }
                    ]
                };
            }

            case 'view_hierarchy': {
                const input = DeviceIdSchema.parse(args);
                const targetDevice = await getTargetDevice(input.deviceId);

                const xmlContent = await adbManager.getViewHierarchy(targetDevice);
                const viewHierarchy = UIHierarchyParser.parseViewHierarchy(xmlContent, targetDevice);

                return {
                    content: [
                        {
                            type: 'text',
                            text: JSON.stringify(viewHierarchy, null, 2)
                        }
                    ]
                };
            }

            case 'click_coordinate': {
                const input = ClickCoordinateSchema.parse(args);
                const targetDevice = await getTargetDevice(input.deviceId);

                await adbManager.clickCoordinate(input.x, input.y, targetDevice);

                return {
                    content: [
                        {
                            type: 'text',
                            text: JSON.stringify({
                                device: targetDevice,
                                action: 'click',
                                coordinates: { x: input.x, y: input.y },
                                timestamp: new Date().toISOString(),
                                success: true
                            }, null, 2)
                        }
                    ]
                };
            }

            default:
                return {
                    content: [
                        {
                            type: 'text',
                            text: `Error: Unknown tool: ${name}`
                        }
                    ],
                    isError: true
                };
        }
    } catch (error) {
        return {
            content: [
                {
                    type: 'text',
                    text: `Error: ${error instanceof Error ? error.message : String(error)}`
                }
            ],
            isError: true
        };
    }
});

// Start the server
async function main() {
    const transport = new StdioServerTransport();
    await server.connect(transport);
    console.error('Android Layout Inspector MCP Server started');
}

if (require.main === module) {
    main().catch((error) => {
        console.error('Server error:', error);
        process.exit(1);
    });
}