package de.cadeda.mcp.model.uihierarchy

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UIElement(
    @SerialName("class") val className: String,
    @SerialName("package") val packageName: String,
    val text: String? = null,
    @SerialName("resource-id") val resourceId: String? = null,
    @SerialName("content-desc") val contentDesc: String? = null,
    val checkable: Boolean,
    val checked: Boolean,
    val clickable: Boolean,
    val enabled: Boolean,
    val focusable: Boolean,
    val focused: Boolean,
    val scrollable: Boolean,
    @SerialName("long-clickable") val longClickable: Boolean,
    val password: Boolean,
    val selected: Boolean,
    val visible: Boolean,
    val bounds: Bounds,
    val children: List<UIElement> = emptyList(),
    // Enhanced Layout Inspector properties
    val index: Int? = null,
    val instance: Int? = null,
    val displayed: Boolean? = null,
    @SerialName("nav-bar") val navBar: Boolean? = null,
    @SerialName("status-bar") val statusBar: Boolean? = null,
    // View attributes (when debug_view_attributes is enabled)
    @SerialName("view-tag") val viewTag: String? = null,
    @SerialName("view-id-name") val viewIdName: String? = null,
    @SerialName("layout-params") val layoutParams: String? = null
)
