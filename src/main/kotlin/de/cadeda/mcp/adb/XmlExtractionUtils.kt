package de.cadeda.mcp.adb

import de.cadeda.mcp.model.AndroidMcpConstants.Patterns

/**
 * Utility class for extracting XML content from ADB command output.
 * Consolidates XML parsing logic to reduce complexity in UIInspector.
 */
object XmlExtractionUtils {

    /**
     * Extracts XML content from ADB command output using multiple strategies.
     */
    fun extractXmlContent(output: String): String {
        // Try different extraction methods in order of preference
        return findCompleteXml(output)
            ?: findHierarchyWithoutHeader(output)
            ?: findXmlInLines(output)
            ?: ""
    }

    private fun findCompleteXml(output: String): String? {
        val xmlMatch = Regex("<\\?xml.*?</hierarchy>", RegexOption.DOT_MATCHES_ALL).find(output)
        return xmlMatch?.value
    }

    private fun findHierarchyWithoutHeader(output: String): String? {
        val hierarchyMatch = Regex(Patterns.HIERARCHY_ONLY, RegexOption.DOT_MATCHES_ALL).find(output)
        return hierarchyMatch?.let { 
            "<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>${it.value}" 
        }
    }

    private fun findXmlInLines(output: String): String? {
        val lines = output.split('\n')
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("<?xml") || trimmed.startsWith("<hierarchy")) {
                return extractXmlFromLine(trimmed)
            }
        }
        return null
    }

    private fun extractXmlFromLine(line: String): String? {
        val singleLineXml = Regex(Patterns.XML_WITH_HEADER, RegexOption.DOT_MATCHES_ALL).find(line)
        return singleLineXml?.let { match ->
            val xmlContent = match.value
            if (xmlContent.startsWith("<?xml")) {
                xmlContent
            } else {
                "<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>$xmlContent"
            }
        }
    }
}