package io.lackstudio.omnihub.compose.platform

import io.lackstudio.omnihub.compose.auth.AuthManager
import io.lackstudio.omnihub.compose.utils.Environment
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

class IosAuthManager : AuthManager {
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
}
