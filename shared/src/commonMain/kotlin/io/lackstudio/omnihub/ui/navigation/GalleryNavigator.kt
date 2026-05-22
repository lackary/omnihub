package io.lackstudio.omnihub.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.lackstudio.omnihub.ui.gallery.GalleryDisplayable
import io.lackstudio.omnihub.utils.LocalXrNavigation

class GalleryNavigator(
    private val xrNav: ((XrNavEvent) -> Unit)?,
    private val onNavigateToFeature: (Feature) -> Unit
) {
    // Unified logic for handling photo clicks (automatically calculates ratio)
    fun navigateToPhoto(item: GalleryDisplayable) {
        val ratio = if (item.displayHeight > 0) {
            item.displayWidth / item.displayHeight.toFloat()
        } else 1f
        val url = item.displayImageUrl ?: ""

        if (xrNav != null) {
            xrNav(XrNavEvent.NavigateToPhoto(item.displayId, url, ratio))
        } else {
            onNavigateToFeature(Feature.Photo(item.displayId, url))
        }
    }

    // Unified logic for handling user clicks
    fun navigateToUser(username: String) {
        if (xrNav != null) {
            xrNav(XrNavEvent.NavigateToUser(username))
        } else {
            onNavigateToFeature(Feature.User(username))
        }
    }
}

// Create a convenient Composable function to easily obtain the Navigator in every screen
@Composable
fun rememberGalleryNavigator(onNavigateToFeature: (Feature) -> Unit): GalleryNavigator {
    val xrNav = LocalXrNavigation.current
    return remember(xrNav, onNavigateToFeature) {
        GalleryNavigator(xrNav, onNavigateToFeature)
    }
}
