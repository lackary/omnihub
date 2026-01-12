package io.lackstudio.omnihub.utils

object UnsplashLinks {
    // Replace with your registered Unsplash Application Name
    private const val APP_NAME = "OmniHub"
    private const val UTM_PARAMS = "?utm_source=$APP_NAME&utm_medium=referral"

    // Generates a user profile link with UTM parameters
    fun userProfile(username: String): String {
        return "https://unsplash.com/@$username$UTM_PARAMS"
    }

    // Generates the Unsplash home page link with UTM parameters
    fun home(): String {
        return "https://unsplash.com/$UTM_PARAMS"
    }

    // (Optional) Generates a photo page link with UTM parameters
    fun photo(photoId: String): String {
        return "https://unsplash.com/photos/$photoId$UTM_PARAMS"
    }
}
