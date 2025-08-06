package de.cadeda.mcp.model.uihierarchy

import kotlinx.serialization.Serializable

@Serializable
data class Bounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    /**
     * Width of the bounded area
     */
    val width: Int get() = right - left
    
    /**
     * Height of the bounded area
     */
    val height: Int get() = bottom - top
    
    /**
     * Center X coordinate
     */
    val centerX: Int get() = left + width / 2
    
    /**
     * Center Y coordinate
     */
    val centerY: Int get() = top + height / 2
    
    /**
     * Area of the bounded region
     */
    val area: Int get() = width * height
    
    /**
     * Check if these bounds contain the given point
     */
    fun contains(x: Int, y: Int): Boolean {
        return x >= left && x <= right && y >= top && y <= bottom
    }
    
    /**
     * Check if these bounds are empty (zero or negative area)
     */
    fun isEmpty(): Boolean = width <= 0 || height <= 0
}
