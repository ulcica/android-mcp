package de.cadeda.mcp.model.uihierarchy

import kotlinx.serialization.Serializable

@Serializable
data class ScreenSize(
    val width: Int,
    val height: Int
)
