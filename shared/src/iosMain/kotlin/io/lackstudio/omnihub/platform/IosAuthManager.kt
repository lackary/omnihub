package io.lackstudio.omnihub.platform

import co.touchlab.kermit.Logger
import cocoapods.GoogleSignIn.GIDSignIn
import io.lackstudio.omnihub.auth.AuthManager
import io.lackstudio.omnihub.auth.GoogleAuthTokens
import io.lackstudio.omnihub.utils.Environment
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIWindow
import platform.UIKit.UIViewController
import kotlin.coroutines.resume

class IosAuthManager : AuthManager {

    private val logger = Logger.withTag("IosAuthManager")

    override fun getRedirectUrl(): String {
        return Environment.AUTH_LOCAL_REDIRECT_URL
    }

    override fun startLogin(authUrl: String) {
        // Convert to NSURL
        val nsUrl = NSURL.URLWithString(authUrl)?: return

        // Open system Safari
        if (UIApplication.sharedApplication.canOpenURL(nsUrl)) {
            UIApplication.sharedApplication.openURL(
                nsUrl,
                mapOf<Any?, Any>(), // Pass an empty map
                null // No completion handler needed
            )
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun signInWithGoogle(context: Any?): GoogleAuthTokens? = suspendCancellableCoroutine { continuation ->
        val rootViewController = getRootViewController()
        if (rootViewController == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        GIDSignIn.sharedInstance.signInWithPresentingViewController(rootViewController) { result, error ->
            if (error != null) {
                logger.d { "iOS Google Sign-In Error: ${error.localizedDescription}" }
                continuation.resume(null)
            } else {
                val idToken = result?.user?.idToken?.tokenString
                val accessToken = result?.user?.accessToken?.tokenString
                
                if (idToken != null) {
                    continuation.resume(GoogleAuthTokens(idToken, accessToken))
                } else {
                    continuation.resume(null)
                }
            }
        }
    }

    private fun getRootViewController(): UIViewController? {
        val keyWindow = UIApplication.sharedApplication.windows.asSequence()
            .mapNotNull { it as? UIWindow }
            .firstOrNull { it.isKeyWindow() }
        return keyWindow?.rootViewController ?: UIApplication.sharedApplication.keyWindow?.rootViewController
    }
}
