package io.lackstudio.omnihub.ui.navigation

import kotlinx.serialization.Serializable

// Main top-level Destination
sealed interface Screen {
    @Serializable
    data object Home : Screen

    @Serializable
    data object Account : Screen

}

// Destination for each feature module (where to navigate after clicking the list)
sealed interface Feature {
    @Serializable
    data object Gallery : Feature

    @Serializable
    data object News : Feature

    @Serializable
    data object Stocks : Feature

    // We need the url for sharedTransition
    @Serializable
    data class Photo(val id: String, val url: String) : Feature
}
