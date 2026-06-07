package io.lackstudio.omnihub.ui.navigation

import kotlinx.serialization.Serializable

// Main top-level Destination
@Serializable
sealed interface Screen {
    @Serializable
    data object MainTabs : Screen

    @Serializable
    data object Home : Screen

    @Serializable
    data object Settings : Screen

    @Serializable
    data object Login : Screen

    @Serializable
    data object Register : Screen

    @Serializable
    data object Account : Screen
}

// Destination for each feature module (where to navigate after clicking the list)
@Serializable
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

    @Serializable
    data class Collection(val id: String, val title: String) : Feature

    @Serializable
    data class Topic(val idOrSlug: String, val title: String) : Feature

    @Serializable
    data class User(val username: String) : Feature
}

@Serializable
sealed interface XrNavEvent {
    @Serializable
    data class NavigateToPhoto(val id: String, val thumbUrl: String, val ratio: Float) : XrNavEvent
    @Serializable
    data class NavigateToUser(val username: String) : XrNavEvent
}
