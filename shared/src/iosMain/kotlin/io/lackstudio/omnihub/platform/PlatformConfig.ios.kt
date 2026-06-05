package io.lackstudio.omnihub.platform

import io.lackstudio.omnihub.auth.AuthManager
import io.lackstudio.omnihub.shared.BuildKonfig
import org.koin.dsl.module

actual val isPullToRefreshSupported: Boolean get() = true
actual val authModule = module {
    single<AuthManager> { IosAuthManager() }
}
actual val appName: String = BuildKonfig.APP_NAME
actual val firebaseWebBase64: String = ""
