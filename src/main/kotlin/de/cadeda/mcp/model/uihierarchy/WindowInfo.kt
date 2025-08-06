package de.cadeda.mcp.model.uihierarchy

import kotlinx.serialization.Serializable

@Serializable
data class WindowInfo(
    val focused: Boolean,
    val visible: Boolean,
    val hasInputFocus: Boolean
)
