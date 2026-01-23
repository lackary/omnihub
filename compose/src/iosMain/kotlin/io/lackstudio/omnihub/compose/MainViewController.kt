package io.lackstudio.omnihub.compose

import androidx.compose.ui.window.ComposeUIViewController
import io.lackstudio.omnihub.compose.di.initKoin
import io.lackstudio.omnihub.compose.ui.App

fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin()
    }
) {
    App()
}
