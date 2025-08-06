package de.cadeda.mcp.model.uihierarchy

import kotlinx.serialization.Serializable

@Serializable
data class UIHierarchy(
    val device: String,
    val timestamp: String,
    val packageName: String? = null,
    val activityName: String? = null,
    val root: UIElement,
    val layoutInfo: LayoutInfo? = null,
    val windowInfo: WindowInfo? = null,
    val activityPid: Int? = null
)

