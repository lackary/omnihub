package io.lackstudio.omnihub.platform

import io.lackstudio.omnihub.auth.AuthManager
import org.koin.dsl.module

actual val isPullToRefreshSupported: Boolean get() = true
actual val authModule = module {
    single<AuthManager> { IosAuthManager() }
}
