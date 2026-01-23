package io.lackstudio.omnihub.compose.platform

import io.lackstudio.omnihub.compose.auth.AuthManager
import io.lackstudio.omnihub.compose.auth.WebAuthManager
import org.koin.dsl.module

actual val isPullToRefreshSupported: Boolean get() = false
actual val authModule = module {
    single<AuthManager> { WebAuthManager() }
}
