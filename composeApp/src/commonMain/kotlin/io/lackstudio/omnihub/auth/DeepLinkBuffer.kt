package io.lackstudio.omnihub.auth

object DeepLinkBuffer {
    // Buffered URL, waiting to be consumed by the UI layer
    private var pendingUrl: String? = null

    // Called by Android/iOS platforms when a deep link is received
    fun setDeepLink(url: String) {
        println("🔗 DeepLink received: $url")
        pendingUrl = url
    }

    // Called by the ViewModel to check for and consume the auth code
    fun consumeCode(): String? {
        val url = pendingUrl ?: return null

        // Simple parsing example: omnihub://auth/callback?code=12345
        if (url.contains("code=")) {
            pendingUrl = null // Clear after reading to prevent multiple triggers
            return url.substringAfter("code=").substringBefore("&")
        }
        return null
    }
}
