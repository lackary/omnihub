package io.lackstudio.omnihub

import androidx.compose.ui.window.ComposeUIViewController
import io.lackstudio.omnihub.di.initKoin
import io.lackstudio.omnihub.ui.App

fun MainViewController() = ComposeUIViewController(
    configure = {
        initKoin()
    }
) {
    App()
}
