package de.cadeda.mcp.model

import de.cadeda.mcp.model.uihierarchy.LayoutInspectorError
import kotlinx.serialization.Serializable

/**
 * Simple result data class for MCP responses.
 */
@Serializable
data class AndroidResult(
    val success: Boolean,
    val message: String,
    val data: Map<String, String> = emptyMap()
)

/**
 * A Result type for consistent error handling across the Android MCP server.
 * Provides type-safe error handling and eliminates the need for nullable returns.
 * 
 * Note: This is primarily for internal use. Serialization support is limited due to
 * LayoutInspectorError complexity.
 */
sealed class AndroidInternalResult<out T> {
    data class Success<T>(val data: T) : AndroidInternalResult<T>()
    data class Failure<T>(val error: LayoutInspectorError) : AndroidInternalResult<T>()
    
    companion object {
        fun <T> success(data: T): AndroidInternalResult<T> = Success(data)
        fun <T> failure(error: LayoutInspectorError): AndroidInternalResult<T> = Failure(error)
        fun <T> failure(message: String, deviceId: String? = null): AndroidInternalResult<T> = 
            Failure(LayoutInspectorError.UnknownError(message, deviceId))
    }
    
    /**
     * Returns true if this result represents a successful operation.
     */
    val isSuccess: Boolean get() = this is Success
    
    /**
     * Returns true if this result represents a failed operation.
     */
    val isFailure: Boolean get() = this is Failure
    
    /**
     * Returns the data if successful, or null if failed.
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Failure -> null
    }
    
    /**
     * Returns the data if successful, or throws the contained error if failed.
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Failure -> throw error
    }
    
    /**
     * Returns the data if successful, or the default value if failed.
     */
    fun getOrDefault(defaultValue: @UnsafeVariance T): @UnsafeVariance T = when (this) {
        is Success -> data
        is Failure -> defaultValue
    }
    
    /**
     * Returns the data if successful, or the result of calling the function if failed.
     */
    inline fun getOrElse(onFailure: (error: LayoutInspectorError) -> @UnsafeVariance T): @UnsafeVariance T = when (this) {
        is Success -> data
        is Failure -> onFailure(error)
    }
    
    /**
     * Transforms the data if successful, or returns the same failure.
     */
    inline fun <R> map(transform: (T) -> R): AndroidInternalResult<R> = when (this) {
        is Success -> success(transform(data))
        is Failure -> Failure(error)
    }
    
    /**
     * Flat-maps the data if successful, or returns the same failure.
     */
    inline fun <R> flatMap(transform: (T) -> AndroidInternalResult<R>): AndroidInternalResult<R> = when (this) {
        is Success -> transform(data)
        is Failure -> Failure(error)
    }
    
    /**
     * Performs the given action if successful.
     */
    inline fun onSuccess(action: (T) -> Unit): AndroidInternalResult<T> {
        if (this is Success) action(data)
        return this
    }
    
    /**
     * Performs the given action if failed.
     */
    inline fun onFailure(action: (LayoutInspectorError) -> Unit): AndroidInternalResult<T> {
        if (this is Failure) action(error)
        return this
    }
}

/**
 * Extension function to catch exceptions and convert them to AndroidInternalResult.
 */
inline fun <T> androidResultOf(block: () -> T): AndroidInternalResult<T> {
    return try {
        AndroidInternalResult.success(block())
    } catch (e: LayoutInspectorError) {
        AndroidInternalResult.failure(e)
    } catch (e: Exception) {
        AndroidInternalResult.failure(LayoutInspectorError.UnknownError(e.message ?: "Unknown error"))
    }
}

/**
 * Extension function for suspend functions to catch exceptions and convert them to AndroidInternalResult.
 */
suspend inline fun <T> suspendAndroidResultOf(crossinline block: suspend () -> T): AndroidInternalResult<T> {
    return try {
        AndroidInternalResult.success(block())
    } catch (e: LayoutInspectorError) {
        AndroidInternalResult.failure(e)
    } catch (e: Exception) {
        AndroidInternalResult.failure(LayoutInspectorError.UnknownError(e.message ?: "Unknown error"))
    }
}