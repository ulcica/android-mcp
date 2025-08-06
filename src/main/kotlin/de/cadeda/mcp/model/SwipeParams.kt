package de.cadeda.mcp.model

/**
 * Parameters for swipe operations to reduce parameter count.
 * Groups coordinates and duration into a single object following clean code principles.
 */
data class SwipeParams(
    val startX: Int,
    val startY: Int,
    val endX: Int, 
    val endY: Int,
    val duration: Int
)