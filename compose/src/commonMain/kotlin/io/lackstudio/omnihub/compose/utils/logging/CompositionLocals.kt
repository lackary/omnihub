package io.lackstudio.omnihub.compose.utils.logging

import androidx.compose.runtime.staticCompositionLocalOf

// Define a global LocalLogger
// Default value: Directly use AppLog (since AppLog itself is Preview Safe)
val LocalLogger = staticCompositionLocalOf {
    // If not provided, default to a Logger with the tag "Preview" or "Unknown"
    AppLog.withTag("Preview")
}
