package io.lackstudio.omnihub

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.lackstudio.omnihub.di.initKoin
import io.lackstudio.omnihub.ui.App

fun main() = application {

    initKoin()
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
        title = "OmniHub",
    ) {
        App()
    }
}
