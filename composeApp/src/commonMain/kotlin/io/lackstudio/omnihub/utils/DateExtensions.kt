package io.lackstudio.omnihub.utils

import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Simple date formatting
 * Assumes the input is an ISO string (e.g. "2023-10-05T14:48:00.000Z")
 * Directly takes the first 10 characters (YYYY-MM-DD)
 */
fun String.toSimpleDateStr(): String {
    // Add check to avoid crash due to empty string or insufficient length
    if (this.length < 10) return this
    return this.take(10)
}

/**
 * [Real Implementation] Display relative time
 * Uses kotlinx-datetime to calculate the time difference
 */
fun String.toRelativeTime(): String {
    try {
        // Parse ISO 8601 string (e.g. "2023-10-05T14:48:00Z")
        val instant = Instant.parse(this)

        // Get current time
        val now = Clock.System.now()

        // Calculate duration
        val duration = now - instant

        val seconds = duration.inWholeSeconds
        val minutes = duration.inWholeMinutes
        val hours = duration.inWholeHours
        val days = duration.inWholeDays

        // Return corresponding string based on duration
        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            days < 7 -> "${days}d ago"
            days < 30 -> "${days / 7}w ago"
            days < 365 -> "${days / 30}mo ago"
            else -> "${days / 365}y ago"
        }
    } catch (e: Exception) {
        // Fallback to simple string processing if parsing fails (e.g., empty string or format error)
        return this.toSimpleDateStr()
    }
}
