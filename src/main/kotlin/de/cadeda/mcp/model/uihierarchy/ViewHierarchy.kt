package de.cadeda.mcp.model.uihierarchy

import kotlinx.serialization.Serializable

@Serializable
data class ViewHierarchy(
    val device: String,
    val timestamp: String,
    val rotation: Int,
    val root: UIElement
)
