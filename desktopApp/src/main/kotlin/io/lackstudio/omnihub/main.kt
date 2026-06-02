package io.lackstudio.omnihub

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.lackstudio.omnihub.di.initKoin
import io.lackstudio.omnihub.platform.appName
import io.lackstudio.omnihub.ui.App
import io.lackstudio.omnifeed.auth.platform.initializeFirebase
import io.lackstudio.omnihub.platform.firebaseWebBase64

fun main() {
    System.setProperty("PID", ProcessHandle.current().pid().toString())

    initializeFirebase("io.lackstudio.omnihub.firebase", firebaseWebBase64)

    initKoin()

    application {

        val windowState = rememberWindowState(
            placement = WindowPlacement.Floating,
            position = WindowPosition.PlatformDefault,
            width = 1024.dp,
            height = 768.dp
        )
        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            resizable = true,
            title = appName,
        ) {
            App()
        }
    }
}
