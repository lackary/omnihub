package io.lackstudio.omnihub.platform

import io.lackstudio.omnifeed.auth.utils.AuthManager
import io.lackstudio.omnifeed.auth.utils.GoogleAuthTokens
import io.lackstudio.omnihub.shared.BuildKonfig
import org.koin.dsl.module

private class WasmAuthManager : AuthManager {
    override fun setRedirectUrl(url: String) {}
    override fun setClientId(id: String) {}
    override fun setSuccessHtml(html: String) {}
    override fun getRedirectUrl(): String = ""
    override fun startLogin(authUrl: String) {}
    override suspend fun signInWithGoogle(context: Any?): GoogleAuthTokens? = null
    override suspend fun signInWithOAuthPopup(authUrl: String): String? = null
    override suspend fun signOut() {}
}

actual val isPullToRefreshSupported: Boolean get() = false
actual val authModule = module {
    single<AuthManager> {
        WasmAuthManager()
    }
}
actual val appName: String = BuildKonfig.APP_NAME
actual val firebaseWebBase64: String = BuildKonfig.FIREBASE_WEB_BASE64
