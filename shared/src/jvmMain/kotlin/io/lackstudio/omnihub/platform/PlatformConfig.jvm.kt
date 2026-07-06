package io.lackstudio.omnihub.platform

import io.lackstudio.omnifeed.auth.utils.AuthManager
import io.lackstudio.omnifeed.auth.utils.DesktopAuthManager
import io.lackstudio.omnihub.shared.BuildKonfig
import org.koin.dsl.module

actual val isPullToRefreshSupported: Boolean get() = false

actual val authModule = module {
    single<AuthManager> {
        DesktopAuthManager().apply {
            setClientId(BuildKonfig.GOOGLE_SERVER_CLIENT_ID)
            
            // Load success HTML from resources
            val html = try {
                val resourceStream = Thread.currentThread().contextClassLoader.getResourceAsStream("auth_success.html")
                resourceStream?.bufferedReader()?.use { it.readText() } ?: ""
            } catch (e: Exception) { "" }
            
            if (html.isNotEmpty()) {
                setSuccessHtml(html)
            }
        }
    }
}
actual val appName: String = BuildKonfig.APP_NAME
actual val firebaseWebBase64: String = BuildKonfig.FIREBASE_WEB_BASE64
