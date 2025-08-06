package de.cadeda.mcp.adb

import kotlin.test.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UIHierarchyParserXMLTest {
    
    @Test
    fun testParseWindowDumpXML() {
        // Read the window_dump.xml file
        val xmlFile = File("src/test/window_dump.xml")
        val xmlContent = xmlFile.readText()
        
        // Test parsing
        val result = UIHierarchyParser.parseViewHierarchy(xmlContent, "test-device")
        
        // Verify basic structure
        assertNotNull(result)
        assertEquals("test-device", result.device)
        assertEquals(0, result.rotation)
        assertNotNull(result.root)
        
        // Verify root element properties
        val root = result.root
        assertEquals("android.widget.FrameLayout", root.className)
        assertEquals("com.google.android.youtube", root.packageName)
        assertTrue(root.children.isNotEmpty())
        
        // Verify bounds parsing
        assertEquals(0, root.bounds.left)
        assertEquals(0, root.bounds.top)
        assertEquals(1344, root.bounds.right)
        assertEquals(2992, root.bounds.bottom)
        
        // Test finding elements by resource ID
        val logoElements = UIHierarchyParser.findElementsById(root, "com.google.android.youtube:id/youtube_logo")
        assertTrue(logoElements.isNotEmpty())
        assertEquals("android.widget.ImageView", logoElements[0].className)
        assertEquals("YouTube", logoElements[0].contentDesc)
        
        // Test finding elements by text
        val homeElements = UIHierarchyParser.findElementsByText(root, "Home")
        assertTrue(homeElements.isNotEmpty())
        assertEquals("Home", homeElements[0].text)
        
        // Test finding elements by class
        val buttonElements = UIHierarchyParser.findElementsByClass(root, "Button")
        assertTrue(buttonElements.isNotEmpty())
        
        println("✅ All XML parsing tests passed!")
        println("Root element children: ${root.children.size}")
        println("Found ${logoElements.size} logo elements")
        println("Found ${homeElements.size} home text elements") 
        println("Found ${buttonElements.size} button elements")
    }
    
    @Test
    fun testParseUIHierarchy() {
        val xmlFile = File("src/test/window_dump.xml")
        val xmlContent = xmlFile.readText()
        
        // Test parseUIHierarchy method
        val result = UIHierarchyParser.parseUIHierarchy(xmlContent, "test-device")
        
        assertNotNull(result)
        assertEquals("test-device", result.device)
        assertNotNull(result.root)
        assertNotNull(result.timestamp)
        
        println("✅ parseUIHierarchy test passed!")
    }
}