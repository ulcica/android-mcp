import { UIElement, UIHierarchy, ViewHierarchy, LayoutInspectorError } from '../types';

export class UIHierarchyParser {
  static parseUIHierarchy(
    xmlContent: string, 
    deviceId: string, 
    layoutInfo?: { screenSize: { width: number; height: number }; density: number; rotation: number },
    windowInfo?: { focused: boolean; visible: boolean; hasInputFocus: boolean },
    activityPid?: number
  ): UIHierarchy {
    try {
      // Basic XML parsing without external dependencies
      const parser = new UIHierarchyParser();
      const root = parser.parseXML(xmlContent);
      
      return {
        device: deviceId,
        timestamp: new Date().toISOString(),
        root,
        layoutInfo,
        windowInfo,
        activityPid
      };
    } catch (error) {
      throw new LayoutInspectorError(`Failed to parse UI hierarchy: ${error}`, 'PARSE_ERROR', deviceId);
    }
  }


  static parseViewHierarchy(xmlContent: string, deviceId: string): ViewHierarchy {
    try {
      // Parse rotation from hierarchy element
      const rotationMatch = xmlContent.match(/<hierarchy[^>]*rotation="(\d+)"/);
      const rotation = rotationMatch ? parseInt(rotationMatch[1]) : 0;
      
      const parser = new UIHierarchyParser();
      const root = parser.parseXML(xmlContent);
      
      return {
        device: deviceId,
        timestamp: new Date().toISOString(),
        rotation,
        root
      };
    } catch (error) {
      throw new LayoutInspectorError(`Failed to parse view hierarchy: ${error}`, 'PARSE_ERROR', deviceId);
    }
  }

  private parseXML(xmlContent: string): UIElement {
    // Remove XML declaration and whitespace
    const cleanXml = xmlContent.replace(/<\?xml[^>]*\?>/, '').trim();
    
    // Find the root hierarchy element
    const hierarchyMatch = cleanXml.match(/<hierarchy[^>]*>(.*)<\/hierarchy>/s);
    if (!hierarchyMatch) {
      throw new Error('No hierarchy element found in XML');
    }

    const hierarchyContent = hierarchyMatch[1].trim();
    
    // Find the complete root node including all its nested content
    const rootNodeStart = hierarchyContent.indexOf('<node');
    if (rootNodeStart === -1) {
      throw new Error('No root node found in hierarchy');
    }
    
    // Extract the complete root node by counting open/close tags
    const rootNodeContent = this.extractCompleteNode(hierarchyContent, rootNodeStart);
    
    return this.parseNode(rootNodeContent);
  }

  private extractCompleteNode(content: string, startIndex: number): string {
    let depth = 0;
    let i = startIndex;
    let inTag = false;
    let tagName = '';
    let isClosingTag = false;
    let isSelfClosing = false;
    
    while (i < content.length) {
      const char = content[i];
      
      if (char === '<') {
        inTag = true;
        isClosingTag = content[i + 1] === '/';
        isSelfClosing = false;
        tagName = '';
      } else if (char === '>' && inTag) {
        inTag = false;
        isSelfClosing = content[i - 1] === '/';
        
        if (tagName === 'node') {
          if (isClosingTag) {
            depth--;
            if (depth === 0) {
              return content.substring(startIndex, i + 1);
            }
          } else if (!isSelfClosing) {
            depth++;
          }
        }
      } else if (inTag && char.match(/[a-zA-Z]/)) {
        tagName += char;
      }
      
      i++;
    }
    
    // If we reach here, the node wasn't properly closed
    return content.substring(startIndex);
  }

  private parseNode(nodeStr: string): UIElement {
    // Extract attributes from the node
    const attributes = this.parseAttributes(nodeStr);
    
    // Parse bounds
    const boundsStr = attributes.bounds || '[0,0][0,0]';
    const boundsMatch = boundsStr.match(/\[(\d+),(\d+)]\[(\d+),(\d+)]/);
    const bounds = boundsMatch ? {
      left: parseInt(boundsMatch[1]),
      top: parseInt(boundsMatch[2]),
      right: parseInt(boundsMatch[3]),
      bottom: parseInt(boundsMatch[4])
    } : { left: 0, top: 0, right: 0, bottom: 0 };

    // Parse boolean attributes and enhanced Layout Inspector properties
    const element: UIElement = {
      class: attributes.class || '',
      package: attributes.package || '',
      text: attributes.text || undefined,
      'resource-id': attributes['resource-id'] || undefined,
      'content-desc': attributes['content-desc'] || undefined,
      checkable: attributes.checkable === 'true',
      checked: attributes.checked === 'true',
      clickable: attributes.clickable === 'true',
      enabled: attributes.enabled === 'true',
      focusable: attributes.focusable === 'true',
      focused: attributes.focused === 'true',
      scrollable: attributes.scrollable === 'true',
      'long-clickable': attributes['long-clickable'] === 'true',
      password: attributes.password === 'true',
      selected: attributes.selected === 'true',
      visible: attributes['visible-to-user'] === 'true',
      bounds,
      children: [],
      // Enhanced Layout Inspector properties
      index: attributes.index ? parseInt(attributes.index) : undefined,
      instance: attributes.instance ? parseInt(attributes.instance) : undefined,
      displayed: attributes.displayed === 'true',
      'nav-bar': attributes['nav-bar'] === 'true',
      'status-bar': attributes['status-bar'] === 'true',
      'view-tag': attributes['view-tag'] || undefined,
      'view-id-name': attributes['view-id-name'] || undefined,
      'layout-params': attributes['layout-params'] || undefined
    };

    // Parse child nodes by finding direct children
    const directChildren = this.extractDirectChildren(nodeStr);
    
    for (const childNodeStr of directChildren) {
      element.children.push(this.parseNode(childNodeStr));
    }

    return element;
  }

  private parseAttributes(nodeStr: string): Record<string, string> {
    const attributes: Record<string, string> = {};
    
    // Extract the opening tag
    const openTagMatch = nodeStr.match(/<node([^>]*)/);
    if (!openTagMatch) return attributes;

    const attributeStr = openTagMatch[1];
    
    // Parse individual attributes
    const attrRegex = /(\w+(?:-\w+)*)="([^"]*)"/g;
    let match;
    
    while ((match = attrRegex.exec(attributeStr)) !== null) {
      attributes[match[1]] = match[2];
    }

    return attributes;
  }

  private extractDirectChildren(nodeStr: string): string[] {
    const children: string[] = [];
    
    // Check if this is a self-closing node
    if (nodeStr.endsWith('/>')) {
      return children; // Self-closing node has no children
    }
    
    // Find the content between the opening and closing tags
    const openTagMatch = nodeStr.match(/<node[^>]*>/);
    if (!openTagMatch) return children;
    
    const openTagEnd = nodeStr.indexOf('>', openTagMatch.index!) + 1;
    const lastCloseTagStart = nodeStr.lastIndexOf('</node>');
    
    if (lastCloseTagStart === -1 || lastCloseTagStart <= openTagEnd) {
      return children; // No content between tags
    }
    
    const content = nodeStr.substring(openTagEnd, lastCloseTagStart).trim();
    if (!content) return children;
    
    // Use regex to find all direct child nodes
    const childMatches = content.match(/<node[^>]*(?:\/>|>.*?<\/node>)/gs);
    
    if (childMatches) {
      children.push(...childMatches);
    }
    
    return children;
  }

  static findElementsById(root: UIElement, resourceId: string): UIElement[] {
    const results: UIElement[] = [];
    
    function search(element: UIElement) {
      if (element['resource-id'] === resourceId) {
        results.push(element);
      }
      
      for (const child of element.children) {
        search(child);
      }
    }
    
    search(root);
    return results;
  }

  static findElementsByText(root: UIElement, text: string, exact: boolean = false): UIElement[] {
    const results: UIElement[] = [];
    
    function search(element: UIElement) {
      if (element.text) {
        const matches = exact 
          ? element.text === text
          : element.text.toLowerCase().includes(text.toLowerCase());
        
        if (matches) {
          results.push(element);
        }
      }
      
      for (const child of element.children) {
        search(child);
      }
    }
    
    search(root);
    return results;
  }

  static findElementsByClass(root: UIElement, className: string): UIElement[] {
    const results: UIElement[] = [];
    
    function search(element: UIElement) {
      if (element.class.includes(className)) {
        results.push(element);
      }
      
      for (const child of element.children) {
        search(child);
      }
    }
    
    search(root);
    return results;
  }
}