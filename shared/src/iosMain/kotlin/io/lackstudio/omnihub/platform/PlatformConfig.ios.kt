package io.lackstudio.omnihub.platform

import io.lackstudio.omnihub.auth.AuthManager
import org.koin.dsl.module
import io.lackstudio.omnihub.shared.BuildKonfig

actual val isPullToRefreshSupported: Boolean get() = true
actual val authModule = module {
    single<AuthManager> { IosAuthManager() }
}
actual val appName: String = BuildKonfig.APP_NAME
actual val firebaseWebBase64: String = ""
