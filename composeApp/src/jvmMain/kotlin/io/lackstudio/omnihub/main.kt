package io.lackstudio.omnihub

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.lackstudio.omnihub.ui.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "OmniHub",
    ) {
        App()
    }
}
