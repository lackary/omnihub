package io.lackstudio.omnihub

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import io.lackstudio.omnihub.auth.DeepLinkBuffer
import io.lackstudio.omnihub.di.initKoin
import io.lackstudio.omnihub.platform.appName
import io.lackstudio.omnihub.ui.App
import io.lackstudio.omnihub.platform.initializeFirebase
import kotlinx.browser.document
import kotlinx.browser.window
import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalComposeUiApi::class, ExperimentalWasmJsInterop::class)
fun main() {
    initializeFirebase()

    initKoin()

    document.title = appName

    val currentUrl = window.location.href
    if (currentUrl.contains("code=")) {
        // Catch the returned code
        DeepLinkBuffer.setDeepLink(currentUrl)

        // (Optional) Clear the URL to prevent re-triggering on page refresh
        val cleanUrl = window.location.origin + window.location.pathname
        window.history.replaceState(null, "", cleanUrl)
    }

    ComposeViewport {
        App()
    }
}
