package io.lackstudio.omnihub.platform

import io.lackstudio.omnihub.auth.AuthManager
import io.lackstudio.omnihub.auth.WebAuthManager
import org.koin.dsl.module

actual val isPullToRefreshSupported: Boolean get() = false
actual val authModule = module {
    single<AuthManager> { WebAuthManager() }
}
