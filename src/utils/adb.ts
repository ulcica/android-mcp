import { exec } from 'child_process';
import { promisify } from 'util';
import { existsSync } from 'fs';
import { join } from 'path';
import { homedir } from 'os';
import { AndroidDevice, LayoutInspectorError } from '../types';

const execAsync = promisify(exec);

export class ADBManager {
  private static instance: ADBManager;
  private adbPath: string | null = null;
  
  static getInstance(): ADBManager {
    if (!ADBManager.instance) {
      ADBManager.instance = new ADBManager();
    }
    return ADBManager.instance;
  }

  private async findADBPath(): Promise<string> {
    if (this.adbPath) {
      return this.adbPath;
    }

    // Try 'adb' in PATH first
    try {
      await execAsync('adb version');
      this.adbPath = 'adb';
      return this.adbPath;
    } catch (error) {
      // ADB not in PATH, try fallback locations
    }

    // Common ADB locations to try
    const fallbackPaths = [
      join(homedir(), 'Library', 'Android', 'sdk', 'platform-tools', 'adb'), // macOS
      join(homedir(), 'Android', 'Sdk', 'platform-tools', 'adb.exe'), // Windows
      join(homedir(), 'Android', 'Sdk', 'platform-tools', 'adb'), // Linux
      '/usr/local/bin/adb', // Homebrew on macOS
      '/opt/android-sdk/platform-tools/adb' // Linux common location
    ];

    for (const path of fallbackPaths) {
      if (existsSync(path)) {
        try {
          await execAsync(`"${path}" version`);
          this.adbPath = path;
          console.error(`Found ADB at: ${path}`);
          return this.adbPath;
        } catch (error) {
          // This path doesn't work, try next
        }
      }
    }

    throw new LayoutInspectorError('ADB not found in PATH or common locations. Please install Android SDK platform-tools.', 'ADB_NOT_FOUND');
  }


  async getDevices(): Promise<AndroidDevice[]> {
    const adbPath = await this.findADBPath();

    try {
      const { stdout } = await execAsync(`"${adbPath}" devices -l`);
      const lines = stdout.trim().split('\n').slice(1); // Skip first line "List of devices attached"
      
      const devices: AndroidDevice[] = [];
      for (const line of lines) {
        if (line.trim()) {
          const parts = line.trim().split(/\s+/);
          const id = parts[0];
          const state = parts[1] as AndroidDevice['state'];
          
          // Extract model if available
          const modelMatch = line.match(/model:(\S+)/);
          const model = modelMatch ? modelMatch[1] : undefined;
          
          devices.push({ id, state, model });
        }
      }
      
      return devices;
    } catch (error) {
      throw new LayoutInspectorError(`Failed to get devices: ${error}`, 'UNKNOWN_ERROR');
    }
  }

  async dumpUIHierarchy(deviceId?: string, options?: { compressed?: boolean; verbose?: boolean }): Promise<string> {
    const adbPath = await this.findADBPath();
    const deviceArg = deviceId ? `-s ${deviceId}` : '';
    
    try {
      // Build uiautomator dump command with options
      let dumpCmd = 'uiautomator dump';
      if (options?.compressed) {
        dumpCmd += ' --compressed';
      }
      if (options?.verbose) {
        dumpCmd += ' --verbose';
      }
      dumpCmd += ' /dev/tty';
      
      const { stdout } = await execAsync(`"${adbPath}" ${deviceArg} exec-out ${dumpCmd}`);
      return stdout;
    } catch (error) {
      throw new LayoutInspectorError(`Failed to dump UI hierarchy: ${error}`, 'UI_DUMP_FAILED', deviceId);
    }
  }


  async getViewAttributes(deviceId?: string): Promise<string> {
    const adbPath = await this.findADBPath();
    const deviceArg = deviceId ? `-s ${deviceId}` : '';
    
    try {
      // Enable debug view attributes for more detailed view information
      await execAsync(`"${adbPath}" ${deviceArg} shell settings put global debug_view_attributes 1`);
      
      // Wait a moment for the setting to take effect
      await new Promise(resolve => setTimeout(resolve, 500));
      
      // Dump with enhanced attributes
      const { stdout } = await execAsync(`"${adbPath}" ${deviceArg} exec-out uiautomator dump /dev/tty`);
      
      // Optionally disable debug view attributes afterwards
      try {
        await execAsync(`"${adbPath}" ${deviceArg} shell settings delete global debug_view_attributes`);
      } catch (e) {
        // Non-critical error
      }
      
      return stdout;
    } catch (error) {
      throw new LayoutInspectorError(`Failed to get view attributes: ${error}`, 'UI_DUMP_FAILED', deviceId);
    }
  }

  async getLayoutBounds(deviceId?: string): Promise<{
    screenSize: { width: number; height: number };
    density: number;
    rotation: number;
  }> {
    const adbPath = await this.findADBPath();
    const deviceArg = deviceId ? `-s ${deviceId}` : '';
    
    try {
      const { stdout } = await execAsync(`"${adbPath}" ${deviceArg} shell wm size && "${adbPath}" ${deviceArg} shell wm density && "${adbPath}" ${deviceArg} shell dumpsys input | grep 'SurfaceOrientation'`);
      
      // Parse screen size
      const sizeMatch = stdout.match(/Physical size: (\d+)x(\d+)/);
      const densityMatch = stdout.match(/Physical density: (\d+)/);
      const rotationMatch = stdout.match(/SurfaceOrientation: (\d+)/);
      
      return {
        screenSize: {
          width: sizeMatch ? parseInt(sizeMatch[1]) : 0,
          height: sizeMatch ? parseInt(sizeMatch[2]) : 0
        },
        density: densityMatch ? parseInt(densityMatch[1]) : 0,
        rotation: rotationMatch ? parseInt(rotationMatch[1]) : 0
      };
    } catch (error) {
      // Return default values if parsing fails
      return {
        screenSize: { width: 0, height: 0 },
        density: 0,
        rotation: 0
      };
    }
  }

  async getCurrentActivity(deviceId?: string): Promise<{
    package: string;
    activity: string;
    pid?: number;
    windowInfo?: {
      focused: boolean;
      visible: boolean;
      hasInputFocus: boolean;
    };
  } | null> {
    const adbPath = await this.findADBPath();
    const deviceArg = deviceId ? `-s ${deviceId}` : '';
    
    try {
      // Get comprehensive activity information
      const [windowOutput, activityOutput] = await Promise.all([
        execAsync(`"${adbPath}" ${deviceArg} shell dumpsys window | grep -E 'mCurrentFocus|mFocusedApp|Window #'`).then(r => r.stdout),
        execAsync(`"${adbPath}" ${deviceArg} shell dumpsys activity top | head -20`).then(r => r.stdout)
      ]);
      
      // Parse window focus information
      const focusMatch = windowOutput.match(/\{[^}]*\s([^/\s]+)\/([^}\s]+)/);
      if (!focusMatch) return null;
      
      const packageName = focusMatch[1];
      const activityName = focusMatch[2];
      
      // Extract PID if available
      const pidMatch = activityOutput.match(/pid=(\d+)/);
      const pid = pidMatch ? parseInt(pidMatch[1]) : undefined;
      
      // Parse window state
      const windowInfo = {
        focused: windowOutput.includes('mCurrentFocus'),
        visible: !windowOutput.includes('NOT_VISIBLE'),
        hasInputFocus: windowOutput.includes('mFocusedApp')
      };
      
      return {
        package: packageName,
        activity: activityName,
        pid,
        windowInfo
      };
    } catch (error) {
      // Non-critical error, return null instead of throwing
      return null;
    }
  }

  async getViewHierarchy(deviceId?: string): Promise<string> {
    const adbPath = await this.findADBPath();
    const deviceArg = deviceId ? `-s ${deviceId}` : '';
    
    try {
      const { stdout } = await execAsync(`"${adbPath}" ${deviceArg} exec-out uiautomator dump /dev/tty`);
      return stdout;
    } catch (error) {
      throw new LayoutInspectorError(`Failed to get view hierarchy: ${error}`, 'UI_DUMP_FAILED', deviceId);
    }
  }

  async takeScreenshot(deviceId?: string): Promise<Buffer> {
    const adbPath = await this.findADBPath();
    const deviceArg = deviceId ? `-s ${deviceId}` : '';
    
    try {
      const { stdout } = await execAsync(`"${adbPath}" ${deviceArg} exec-out screencap -p`, { encoding: 'buffer' });
      return stdout;
    } catch (error) {
      throw new LayoutInspectorError(`Failed to take screenshot: ${error}`, 'UNKNOWN_ERROR', deviceId);
    }
  }

  async clickCoordinate(x: number, y: number, deviceId?: string): Promise<void> {
    const adbPath = await this.findADBPath();
    const deviceArg = deviceId ? `-s ${deviceId}` : '';
    
    try {
      await execAsync(`"${adbPath}" ${deviceArg} shell input tap ${x} ${y}`);
    } catch (error) {
      throw new LayoutInspectorError(`Failed to click at coordinates (${x}, ${y}): ${error}`, 'UNKNOWN_ERROR', deviceId);
    }
  }
}