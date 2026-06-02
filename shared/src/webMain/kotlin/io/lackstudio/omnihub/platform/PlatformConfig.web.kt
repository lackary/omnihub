package io.lackstudio.omnihub.platform

import io.lackstudio.omnihub.auth.AuthManager
import io.lackstudio.omnihub.auth.WebAuthManager
import org.koin.dsl.module
import io.lackstudio.omnihub.shared.BuildKonfig

actual val isPullToRefreshSupported: Boolean get() = false
actual val authModule = module {
    single<AuthManager> { WebAuthManager() }
}
actual val appName: String = BuildKonfig.APP_NAME
actual val firebaseWebBase64: String = BuildKonfig.FIREBASE_WEB_BASE64
