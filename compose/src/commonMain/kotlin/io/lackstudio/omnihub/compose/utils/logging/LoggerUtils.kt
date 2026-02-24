package io.lackstudio.omnihub.compose.utils.logging

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import co.touchlab.kermit.Logger
import io.lackstudio.omnihub.compose.utils.LocalLogger


@Composable
fun rememberLogger(tag: String): Logger {
    // Get the Logger provided by the parent (usually the Root Logger)
    val parentLogger = LocalLogger.current

    // Create a Logger with a new tag using remember
    // Recreate only when tag or parentLogger changes
    return remember(parentLogger, tag) {
        parentLogger.withTag(tag)
    }
}
