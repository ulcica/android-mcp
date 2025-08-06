package de.cadeda.mcp.model.uihierarchy

import kotlinx.serialization.Serializable

@Serializable
data class CommandResult(
    val stdout: String,
    val stderr: String
)
