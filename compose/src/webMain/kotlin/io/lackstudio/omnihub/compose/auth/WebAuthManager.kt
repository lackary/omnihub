package io.lackstudio.omnihub.compose.auth

import kotlinx.browser.window

class WebAuthManager : AuthManager {

    override fun getRedirectUrl(): String {
        // Get the current URL as the redirect_uri (e.g., http://localhost:8081)
        val origin = window.location.protocol + "//" + window.location.host + window.location.pathname
        // Remove the trailing '/' to avoid issues with OAuth providers
        return origin.removeSuffix("/")
    }

    override fun startLogin(authUrl: String) {
        println("🌐 Web Redirecting to: $authUrl")
        window.location.href = authUrl
    }
}
