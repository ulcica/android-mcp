package de.cadeda.mcp.model.uihierarchy

import kotlinx.serialization.Serializable

@Serializable
data class LayoutInfo(
    val screenSize: ScreenSize,
    val density: Int,
    val rotation: Int
)
