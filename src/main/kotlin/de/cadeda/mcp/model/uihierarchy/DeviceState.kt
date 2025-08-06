package de.cadeda.mcp.model.uihierarchy

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class DeviceState {
    @SerialName("device") DEVICE,
    @SerialName("offline") OFFLINE,
    @SerialName("unauthorized") UNAUTHORIZED
}
