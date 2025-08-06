package de.cadeda.mcp.model.uihierarchy

sealed class LayoutInspectorError(
    message: String,
    val code: ErrorCode,
    val deviceId: String? = null
) : Exception(message) {

    enum class ErrorCode {
        ADB_NOT_FOUND,
        DEVICE_NOT_FOUND,
        UI_DUMP_FAILED,
        PARSE_ERROR,
        CONNECTION_CLOSED,
        COMMAND_TIMEOUT,
        SHELL_COMMAND_FAILED,
        MCP_PROTOCOL_ERROR,
        UNKNOWN_ERROR
    }

    class AdbNotFound(message: String) : LayoutInspectorError(message, ErrorCode.ADB_NOT_FOUND)
    class DeviceNotFound(message: String, deviceId: String?) :
        LayoutInspectorError(message, ErrorCode.DEVICE_NOT_FOUND, deviceId)

    class UiDumpFailed(message: String, deviceId: String?) :
        LayoutInspectorError(message, ErrorCode.UI_DUMP_FAILED, deviceId)

    class ParseError(message: String, deviceId: String?) :
        LayoutInspectorError(message, ErrorCode.PARSE_ERROR, deviceId)

    class ConnectionClosed(message: String, deviceId: String? = null) :
        LayoutInspectorError(message, ErrorCode.CONNECTION_CLOSED, deviceId)

    class CommandTimeout(message: String, deviceId: String? = null) :
        LayoutInspectorError(message, ErrorCode.COMMAND_TIMEOUT, deviceId)

    class ShellCommandFailed(message: String, deviceId: String? = null) :
        LayoutInspectorError(message, ErrorCode.SHELL_COMMAND_FAILED, deviceId)

    class McpProtocolError(message: String, deviceId: String? = null) :
        LayoutInspectorError(message, ErrorCode.MCP_PROTOCOL_ERROR, deviceId)

    class UnknownError(message: String, deviceId: String? = null) :
        LayoutInspectorError(message, ErrorCode.UNKNOWN_ERROR, deviceId)
}
