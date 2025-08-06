package de.cadeda.mcp.model.uihierarchy

import kotlinx.serialization.Serializable

@Serializable
data class AndroidDevice(
    val id: String,
    val model: String? = null,
    val state: DeviceState
)
