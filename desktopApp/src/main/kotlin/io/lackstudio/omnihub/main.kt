package io.lackstudio.omnihub

import android.app.Application

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.google.firebase.FirebasePlatform
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.FirebaseOptions
import dev.gitlive.firebase.initialize
import io.lackstudio.omnihub.di.initKoin
import io.lackstudio.omnihub.platform.appName
import io.lackstudio.omnihub.ui.App
import io.lackstudio.omnihub.utils.getFirebaseWebConfig

fun main() {
    System.setProperty("PID", ProcessHandle.current().pid().toString())

    val firebaseConfig = getFirebaseWebConfig() ?: return

    // Desktop (JVM) specific: Initialize the underlying FirebasePlatform
    FirebasePlatform.initializeFirebasePlatform(
        object : FirebasePlatform() {
            val storage = mutableMapOf<String, String>()
            override fun store(key: String, value: String) = storage.set(key, value)
            override fun retrieve(key: String) = storage[key]
            override fun clear(key: String) { storage.remove(key) }
            override fun log(msg: String) = println(msg)
        }
    )

    Firebase.initialize(
        context = Application(),
        options = FirebaseOptions(
            apiKey = firebaseConfig.apiKey,
            authDomain = firebaseConfig.authDomain,
            projectId = firebaseConfig.projectId,
            storageBucket = firebaseConfig.storageBucket,
            applicationId = firebaseConfig.appId
        )
    )

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
