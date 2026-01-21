package io.lackstudio.omnihub.compose

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import io.lackstudio.omnihub.compose.auth.DeepLinkBuffer
import io.lackstudio.omnihub.compose.di.initKoin
import io.lackstudio.omnihub.compose.ui.App
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class, ExperimentalWasmJsInterop::class)
fun main() {

    initKoin()

    val currentUrl = window.location.href
    if (currentUrl.contains("code=")) {
        // 接住回傳的 Code
        DeepLinkBuffer.setDeepLink(currentUrl)

        // (選用) 清理網址列，避免 F5 重整又觸發
        val cleanUrl = window.location.origin + window.location.pathname
        window.history.replaceState(null, "", cleanUrl)
    }
    println("web main")
    ComposeViewport {
        App()
    }
}
