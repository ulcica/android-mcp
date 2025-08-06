package de.cadeda.mcp.model.uihierarchy

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CurrentActivity(
    @SerialName("package") val packageName: String,
    val activity: String,
    val pid: Int? = null,
    val windowInfo: WindowInfo? = null
)
