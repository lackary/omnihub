package io.lackstudio.omnihub.platform

import io.lackstudio.omnifeed.auth.AuthManager
import io.lackstudio.omnifeed.auth.WebAuthManager
import org.koin.dsl.module
import io.lackstudio.omnihub.shared.BuildKonfig

actual val isPullToRefreshSupported: Boolean get() = false
actual val authModule = module {
    single<AuthManager> {
        WebAuthManager().apply {
            setClientId(BuildKonfig.GOOGLE_SERVER_CLIENT_ID)
        }
    }
}
actual val appName: String = BuildKonfig.APP_NAME
actual val firebaseWebBase64: String = BuildKonfig.FIREBASE_WEB_BASE64
