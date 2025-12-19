package io.lackstudio.omnihub

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.lackstudio.omnihub.di.initKoin
import io.lackstudio.omnihub.ui.App

fun main() = application {

    initKoin()

    Window(
        onCloseRequest = ::exitApplication,
        title = "OmniHub",
    ) {
        App()
    }
}
