package de.cadeda.mcp.adb

import de.cadeda.mcp.model.uihierarchy.Bounds
import de.cadeda.mcp.model.uihierarchy.LayoutInfo
import de.cadeda.mcp.model.uihierarchy.LayoutInspectorError
import de.cadeda.mcp.model.uihierarchy.UIElement
import de.cadeda.mcp.model.uihierarchy.UIHierarchy
import de.cadeda.mcp.model.uihierarchy.ViewHierarchy
import de.cadeda.mcp.model.uihierarchy.WindowInfo
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.StringReader
import java.time.Instant
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

class UIHierarchyParser {
    companion object {
        fun parseUIHierarchy(
            xmlContent: String,
            deviceId: String,
            layoutInfo: LayoutInfo? = null,
            windowInfo: WindowInfo? = null,
            activityPid: Int? = null
        ): UIHierarchy {
            return executeWithErrorHandling(deviceId, "parse UI hierarchy") {
                val root = parseXMLWithDOM(xmlContent)
                
                UIHierarchy(
                    device = deviceId,
                    timestamp = Instant.now().toString(),
                    root = root,
                    layoutInfo = layoutInfo,
                    windowInfo = windowInfo,
                    activityPid = activityPid
                )
            }
        }
        
        fun parseViewHierarchy(xmlContent: String, deviceId: String): ViewHierarchy {
            return executeWithErrorHandling(deviceId, "parse view hierarchy") {
                val document = parseXMLDocument(xmlContent)
                val hierarchyElement = document.documentElement
                val rotation = extractRotationAttribute(hierarchyElement)
                val rootElement = extractRootNode(hierarchyElement)
                val root = convertElementToUIElement(rootElement)
                
                ViewHierarchy(
                    device = deviceId,
                    timestamp = Instant.now().toString(),
                    rotation = rotation,
                    root = root
                )
            }
        }
        
        private fun <T> executeWithErrorHandling(deviceId: String, operation: String, block: () -> T): T {
            return try {
                block()
            } catch (e: Exception) {
                throw LayoutInspectorError.ParseError("Failed to $operation: ${e.message}", deviceId)
            }
        }
        
        private fun extractRotationAttribute(hierarchyElement: Element): Int {
            return hierarchyElement.getAttribute("rotation").toIntOrNull() ?: 0
        }
        
        private fun extractRootNode(hierarchyElement: Element): Element {
            val nodeList = hierarchyElement.getElementsByTagName("node")
            return if (nodeList.length > 0) {
                nodeList.item(0) as Element
            } else {
                throw de.cadeda.mcp.model.uihierarchy.LayoutInspectorError.ParseError("No root node found in hierarchy", null)
            }
        }
        
        private fun parseXMLWithDOM(xmlContent: String): UIElement {
            try {
                val document = parseXMLDocument(xmlContent)
                val hierarchyElement = document.documentElement
                val nodeList = hierarchyElement.getElementsByTagName("node")
                
                if (nodeList.length == 0) {
                    throw de.cadeda.mcp.model.uihierarchy.LayoutInspectorError.ParseError("No root node found in hierarchy", null)
                }
                
                val rootElement = nodeList.item(0) as Element
                return convertElementToUIElement(rootElement)
            } catch (e: de.cadeda.mcp.model.uihierarchy.LayoutInspectorError) {
                throw e
            } catch (e: Exception) {
                throw de.cadeda.mcp.model.uihierarchy.LayoutInspectorError.ParseError("DOM XML parsing failed: ${e.message}", null)
            }
        }
        
        private fun parseXMLDocument(xmlContent: String): Document {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val inputSource = InputSource(StringReader(xmlContent))
            return builder.parse(inputSource)
        }
        
        private fun convertElementToUIElement(element: Element): UIElement {
            // Parse bounds
            val boundsStr = element.getAttribute("bounds")
            val boundsMatch = Regex("\\[(\\d+),(\\d+)]\\[(\\d+),(\\d+)]").find(boundsStr)
            val bounds = if (boundsMatch != null) {
                Bounds(
                    left = boundsMatch.groupValues[1].toInt(),
                    top = boundsMatch.groupValues[2].toInt(),
                    right = boundsMatch.groupValues[3].toInt(),
                    bottom = boundsMatch.groupValues[4].toInt()
                )
            } else {
                Bounds(0, 0, 0, 0)
            }
            
            // Convert child node elements to UIElement recursively
            val children = mutableListOf<UIElement>()
            val childNodes = element.childNodes
            for (i in 0 until childNodes.length) {
                val node = childNodes.item(i)
                if (node is Element && node.tagName == "node") {
                    children.add(convertElementToUIElement(node))
                }
            }
            
            return UIElement(
                className = element.getAttribute("class"),
                packageName = element.getAttribute("package"),
                text = element.getAttribute("text").takeIf { it.isNotEmpty() },
                resourceId = element.getAttribute("resource-id").takeIf { it.isNotEmpty() },
                contentDesc = element.getAttribute("content-desc").takeIf { it.isNotEmpty() },
                checkable = element.getAttribute("checkable") == "true",
                checked = element.getAttribute("checked") == "true",
                clickable = element.getAttribute("clickable") == "true",
                enabled = element.getAttribute("enabled") == "true",
                focusable = element.getAttribute("focusable") == "true",
                focused = element.getAttribute("focused") == "true",
                scrollable = element.getAttribute("scrollable") == "true",
                longClickable = element.getAttribute("long-clickable") == "true",
                password = element.getAttribute("password") == "true",
                selected = element.getAttribute("selected") == "true",
                visible = element.getAttribute("visible-to-user") == "true",
                bounds = bounds,
                children = children,
                // Enhanced Layout Inspector properties
                index = element.getAttribute("index").toIntOrNull(),
                instance = element.getAttribute("instance").toIntOrNull(),
                displayed = element.getAttribute("displayed").takeIf { it.isNotEmpty() }?.let { it == "true" },
                navBar = element.getAttribute("nav-bar").takeIf { it.isNotEmpty() }?.let { it == "true" },
                statusBar = element.getAttribute("status-bar").takeIf { it.isNotEmpty() }?.let { it == "true" },
                viewTag = element.getAttribute("view-tag").takeIf { it.isNotEmpty() },
                viewIdName = element.getAttribute("view-id-name").takeIf { it.isNotEmpty() },
                layoutParams = element.getAttribute("layout-params").takeIf { it.isNotEmpty() }
            )
        }
        
        fun findElementsById(root: UIElement, resourceId: String): List<UIElement> {
            return findElements(root) { element ->
                element.resourceId == resourceId
            }
        }
        
        fun findElementsByText(root: UIElement, text: String, exact: Boolean = false): List<UIElement> {
            return findElements(root) { element ->
                element.text?.let { elementText ->
                    if (exact) {
                        elementText == text
                    } else {
                        elementText.lowercase().contains(text.lowercase())
                    }
                } ?: false
            }
        }
        
        fun findElementsByClass(root: UIElement, className: String): List<UIElement> {
            return findElements(root) { element ->
                element.className.contains(className)
            }
        }
        
        /**
         * Generic element finder that traverses the UI hierarchy and applies a predicate
         * @param root The root element to start searching from
         * @param predicate A function that returns true if the element matches the search criteria
         * @return List of matching UIElements
         */
        private fun findElements(root: UIElement, predicate: (UIElement) -> Boolean): List<UIElement> {
            val results = mutableListOf<UIElement>()
            
            fun traverse(element: UIElement) {
                if (predicate(element)) {
                    results.add(element)
                }
                element.children.forEach { traverse(it) }
            }
            
            traverse(root)
            return results
        }
    }
}