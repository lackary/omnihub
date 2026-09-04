package io.lackstudio.omnihub.platform

import io.lackstudio.omnifeed.auth.utils.AuthManager
import io.lackstudio.omnifeed.auth.utils.IosAuthManager
import io.lackstudio.omnihub.shared.BuildKonfig
import io.lackstudio.omnihub.utils.Environment
import org.koin.dsl.module

actual val isPullToRefreshSupported: Boolean get() = true
actual val authModule = module {
    single<AuthManager> {
        IosAuthManager().apply {
            setClientId(BuildKonfig.GOOGLE_SERVER_CLIENT_ID)
            setRedirectUrl(Environment.AUTH_LOCAL_REDIRECT_URL)
        }
    }
}
actual val appName: String = BuildKonfig.APP_NAME
actual val firebaseWebBase64: String = BuildKonfig.FIREBASE_WEB_BASE64
