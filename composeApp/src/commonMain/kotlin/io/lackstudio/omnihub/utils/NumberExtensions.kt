package io.lackstudio.omnihub.utils

import kotlin.math.round

/**
 * Formats the number into a compact display string (e.g. 1.2k, 1.5m)
 * Suitable for UI count display
 */
fun Long.toCompactDisplayString(): String {
    return when {
        this < 1000 -> this.toString()
        this < 1_000_000 -> {
            val value = this / 1000.0
            "${value.roundToOneDecimal()} k"
        }
        else -> {
            val value = this / 1_000_000.0
            "${value.roundToOneDecimal()} m"
        }
    }
}

fun Int.toCompactDisplayString(): String = this.toLong().toCompactDisplayString()

// Private helper function, only used within this file
private fun Double.roundToOneDecimal(): Double {
    return round(this * 10) / 10.0
}
