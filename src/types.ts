export interface AndroidDevice {
  id: string;
  model?: string;
  state: 'device' | 'offline' | 'unauthorized';
}

export interface UIElement {
  class: string;
  package: string;
  text?: string;
  'resource-id'?: string;
  'content-desc'?: string;
  checkable: boolean;
  checked: boolean;
  clickable: boolean;
  enabled: boolean;
  focusable: boolean;
  focused: boolean;
  scrollable: boolean;
  'long-clickable': boolean;
  password: boolean;
  selected: boolean;
  visible: boolean;
  bounds: {
    left: number;
    top: number;
    right: number;
    bottom: number;
  };
  children: UIElement[];
  // Enhanced Layout Inspector properties
  index?: number;
  instance?: number;
  displayed?: boolean;
  'nav-bar'?: boolean;
  'status-bar'?: boolean;
  // View attributes (when debug_view_attributes is enabled)
  'view-tag'?: string;
  'view-id-name'?: string;
  'layout-params'?: string;
}

export interface UIHierarchy {
  device: string;
  timestamp: string;
  packageName?: string;
  activityName?: string;
  root: UIElement;
  layoutInfo?: {
    screenSize: { width: number; height: number };
    density: number;
    rotation: number;
  };
  windowInfo?: {
    focused: boolean;
    visible: boolean;
    hasInputFocus: boolean;
  };
  activityPid?: number;
}


export interface ViewHierarchy {
  device: string;
  timestamp: string;
  rotation: number;
  root: UIElement;
}

export class LayoutInspectorError extends Error {
  code: 'ADB_NOT_FOUND' | 'DEVICE_NOT_FOUND' | 'UI_DUMP_FAILED' | 'PARSE_ERROR' | 'UNKNOWN_ERROR';
  deviceId?: string;

  constructor(message: string, code: 'ADB_NOT_FOUND' | 'DEVICE_NOT_FOUND' | 'UI_DUMP_FAILED' | 'PARSE_ERROR' | 'UNKNOWN_ERROR' = 'UNKNOWN_ERROR', deviceId?: string) {
    super(message);
    this.name = 'LayoutInspectorError';
    this.code = code;
    this.deviceId = deviceId;
  }
}