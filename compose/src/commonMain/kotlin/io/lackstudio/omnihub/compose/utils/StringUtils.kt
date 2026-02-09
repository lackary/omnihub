package io.lackstudio.omnihub.compose.utils

import io.lackstudio.omnifeed.unsplash.utils.Environment
import io.lackstudio.omnihub.compose.utils.logging.AppLog

object UnsplashLinks {
    private val logger = AppLog.withTag("UnsplashLinks")
    // Replace with your registered Unsplash Application Name
    private const val APP_NAME = "OmniHub"
    private const val UTM_PARAMS = "?utm_source=$APP_NAME&utm_medium=referral"

    // Generates the Unsplash home page link with UTM parameters
    fun home(): String {
        val link = "https://${Environment.HOST_NAME}/$UTM_PARAMS"
        logger.d { "Home Link: $link" }
        return link
    }

    // Generates a photo page link with UTM parameters
    fun photo(id: String): String {
        val link = "https://${Environment.HOST_NAME}${Environment.API_PHOTOS}/$id$UTM_PARAMS"
        logger.d { "Photo Link: $link" }
        return link
    }

    // Generates a collection page link with UTM parameters
    fun collection(id: String): String {
        val link = "https://${Environment.HOST_NAME}${Environment.API_COLLECTIONS}/$id$UTM_PARAMS"
        logger.d { "Collection Link: $link" }
        return link
    }

    // Generates a topics page link with UTM parameters
    fun topic(slugOrId: String): String {
        val link = "https://${Environment.HOST_NAME}/t/$slugOrId$UTM_PARAMS"
        logger.d { "Topic Link: $link" }
        return link
    }

    // Generates a user profile link with UTM parameters
    fun userProfile(username: String): String {
        val link = "https://${Environment.HOST_NAME}/@$username$UTM_PARAMS"
        logger.d { "User Profile Link: $link" }
        return link
    }
}
