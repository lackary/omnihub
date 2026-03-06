package io.lackstudio.omnihub.compose.ui.extensions

import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import io.lackstudio.omnihub.compose.ui.navigation.Feature

/**
 * Smart Navigation: Automatically check if we should "pop" to the previous page.
 * Used to prevent infinite loops like User -> Photo -> User -> Photo.
 */
fun NavHostController.navigateToFeatureSmart(feature: Feature) {
    val previousEntry = this.previousBackStackEntry

    val shouldPop = try {
        when (feature) {
            // 1. Check User loops
            is Feature.User -> {
                val prev = previousEntry?.toRoute<Feature.User>()
                prev?.username == feature.username
            }
            // 2. Check Collection loops
            is Feature.Collection -> {
                val prev = previousEntry?.toRoute<Feature.Collection>()
                prev?.id == feature.id
            }
            // 3. Check Photo loops
            is Feature.Photo -> {
                val prev = previousEntry?.toRoute<Feature.Photo>()
                prev?.id == feature.id
            }
            // 4. Check Topic loops
            is Feature.Topic -> {
                val prev = previousEntry?.toRoute<Feature.Topic>()
                prev?.idOrSlug == feature.idOrSlug
            }
            else -> false
        }
    } catch (e: Exception) {
        // If the previous page's route type doesn't match the target (e.g., coming from Home),
        // toRoute might throw an exception.
        false
    }

    if (shouldPop) {
        // If the destination is the same as the previous page, pop back to create a "return to page" feel.
        this.popBackStack()
    } else {
        // Otherwise, navigate to the new page normally.
        this.navigate(feature)
    }
}
