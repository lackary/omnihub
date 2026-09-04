package io.lackstudio.omnihub

import androidx.compose.ui.window.ComposeUIViewController
import io.lackstudio.omnihub.di.initKoin
import io.lackstudio.omnihub.ui.App
import io.lackstudio.omnifeed.auth.platform.initializeFirebase
import io.lackstudio.omnihub.platform.firebaseWebBase64

fun MainViewController() = ComposeUIViewController(
    configure = {
        initializeFirebase(firebaseWebBase64)
        initKoin()
    }
) {
    App()
}
