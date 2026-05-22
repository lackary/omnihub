package io.lackstudio.omnihub.auth

import io.lackstudio.omnihub.utils.logging.AppLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object DeepLinkBuffer {
    private val logger = AppLog.withTag("DeepLinkBuffer")
    // Buffered URL, waiting to be consumed by the UI layer
    private val _deepLinkUrl = MutableStateFlow<String?>(null)
    val deepLinkUrl = _deepLinkUrl.asStateFlow()

    // Called by Android/iOS platforms when a deep link is received
    fun setDeepLink(url: String) {
        logger.d { " Received deep link: $url" }
        _deepLinkUrl.update { url }
    }

    // Called by the ViewModel to check for and consume the auth code
    fun consumeDeepLink() {
        logger.d { "Consumed deep link" }
        _deepLinkUrl.update { null }
    }
}
