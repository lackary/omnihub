package io.lackstudio.omnihub.utils

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
