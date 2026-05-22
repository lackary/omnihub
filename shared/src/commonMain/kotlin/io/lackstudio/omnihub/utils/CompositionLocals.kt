package io.lackstudio.omnihub.utils

import androidx.compose.runtime.staticCompositionLocalOf
import io.lackstudio.omnihub.ui.navigation.XrNavEvent
import io.lackstudio.omnihub.utils.logging.AppLog

// Define a global LocalLogger
// Default value: Directly use AppLog (since AppLog itself is Preview Safe)
val LocalLogger = staticCompositionLocalOf {
    // If not provided, default to a Logger with the tag "Preview" or "Unknown"
    AppLog.withTag("Preview")
}

// Define an "XR Navigation Interceptor"
// Its type is: a function that receives (photoId, url, ratio). If null, it means it's not currently in XR mode
val LocalXrNavigation = staticCompositionLocalOf<((XrNavEvent) -> Unit)?> {
    null
}
