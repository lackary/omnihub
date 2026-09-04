package io.lackstudio.omnihub.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberPlatformContext(): Any? = LocalContext.current
