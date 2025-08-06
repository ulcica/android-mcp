package de.cadeda.mcp.model

import kotlinx.serialization.Serializable

/**
 * Value class for Android device IDs to eliminate primitive obsession.
 * Provides type safety and validation for device identifiers.
 */
@Serializable
@JvmInline
value class DeviceId(val value: String) {
    init {
        require(value.isNotBlank()) { "Device ID cannot be blank" }
    }
    
    companion object {
        /**
         * Creates a DeviceId from a nullable string, returning null if the string is null or blank.
         */
        fun fromString(value: String?): DeviceId? {
            return if (value.isNullOrBlank()) null else DeviceId(value)
        }
        
        /**
         * Creates a DeviceId from a nullable string, using the provided default if null or blank.
         */
        fun fromStringOrDefault(value: String?, default: DeviceId): DeviceId {
            return fromString(value) ?: default
        }
    }
    
    override fun toString(): String = value
}

/**
 * Sealed class representing device selection strategies.
 * Provides type-safe alternatives to nullable device IDs.
 */
@Serializable
sealed class DeviceSelection {
    @Serializable
    object Auto : DeviceSelection()
    
    @Serializable
    data class Specific(val deviceId: DeviceId) : DeviceSelection()
    
    companion object {
        /**
         * Creates a DeviceSelection from a nullable string.
         */
        fun fromString(deviceId: String?): DeviceSelection {
            return DeviceId.fromString(deviceId)?.let { Specific(it) } ?: Auto
        }
    }
}

/**
 * Extension functions for working with DeviceId in existing code.
 */
fun String?.toDeviceSelection(): DeviceSelection = DeviceSelection.fromString(this)

fun DeviceId?.orAuto(): DeviceSelection = this?.let { DeviceSelection.Specific(it) } ?: DeviceSelection.Auto