package de.cadeda.mcp.model.uihierarchy

import kotlinx.serialization.Serializable

@Serializable
data class ContentItem(
    val type: String,
    val text: String? = null,
    val data: String? = null,
    val mimeType: String? = null
)