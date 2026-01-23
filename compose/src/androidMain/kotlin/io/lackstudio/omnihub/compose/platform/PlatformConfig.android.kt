package io.lackstudio.omnihub.compose.platform

import io.lackstudio.omnihub.compose.auth.AndroidAuthManager
import io.lackstudio.omnihub.compose.auth.AuthManager
import org.koin.dsl.module

actual val isPullToRefreshSupported: Boolean get() = true
actual val authModule = module {
    single<AuthManager> { AndroidAuthManager(get()) }
}
