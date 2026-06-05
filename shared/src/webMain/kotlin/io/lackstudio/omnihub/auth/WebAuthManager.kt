package io.lackstudio.omnihub.auth

import co.touchlab.kermit.Logger
import io.lackstudio.omnihub.shared.BuildKonfig
import kotlinx.browser.window
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsString

class WebAuthManager : AuthManager {

    private val logger = Logger.withTag("WebAuthManager")

    override fun getRedirectUrl(): String {
        // Get the current URL as the redirect_uri (e.g., http://localhost:8081)
        val origin = window.location.protocol + "//" + window.location.host + window.location.pathname
        // Remove the trailing '/' to avoid issues with OAuth providers
        return origin.removeSuffix("/")
    }

    override fun startLogin(authUrl: String) {
        logger.d { "Web Redirecting to: $authUrl" }
        window.location.href = authUrl
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    override suspend fun signInWithGoogle(context: Any?): GoogleAuthTokens? = suspendCancellableCoroutine { continuation ->
        try {
            initAndPromptGoogleSignIn(
                clientId = BuildKonfig.GOOGLE_SERVER_CLIENT_ID
            ) { credential ->
                if (credential != null) {
                    logger.d { "Google Sign-In: Received credential" }
                    continuation.resume(GoogleAuthTokens(idToken = credential.toString()))
                } else {
                    logger.w { "Google Sign-In: Credential is null" }
                    continuation.resume(null)
                }
            }
        } catch (e: Exception) {
            logger.e(throwable = e) { "Google Sign-In: Error during initialization" }
            continuation.resume(null)
        }
    }
}

/**
 * References the function defined in google-auth-bridge.js
 */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(clientId, callback) => window.initAndPromptGoogleSignIn(clientId, callback)")
private external fun initAndPromptGoogleSignIn(clientId: String, callback: (JsString?) -> Unit)
