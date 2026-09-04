package io.lackstudio.omnihub.platform

import androidx.compose.runtime.Composable

/**
 * Provides the platform-specific context.
 * On Android, this returns the current Context.
 * On other platforms, it returns null.
 */
@Composable
expect fun rememberPlatformContext(): Any?
