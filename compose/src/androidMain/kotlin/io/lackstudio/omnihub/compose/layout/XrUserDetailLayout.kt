package io.lackstudio.omnihub.compose.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import io.lackstudio.omnihub.compose.ui.gallery.UserDetailScreen
import io.lackstudio.omnihub.compose.ui.navigation.XrNavigationController
import io.lackstudio.omnihub.compose.utils.LocalXrNavigation
import io.lackstudio.omnihub.compose.utils.logging.rememberLogger

@Composable
fun XrUserDetailLayout(username: String) {
    val logger = rememberLogger("XrUserDetailLayout")
    CompositionLocalProvider(
        LocalXrNavigation provides { event ->
            logger.d{ "[XR] Event received: $event" }
            // Forward events to XrNavigationController (just like PhotoStack does)
            XrNavigationController.proxyNavigate(event)
        }
    ) {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                Box(modifier = Modifier.fillMaxSize()) {
                    key(username) {
                        UserDetailScreen(
                            username = username,
                            onBack = { /* activity will be closed */ },
                            onNavigateToFeature = { },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedVisibility
                        )
                    }
                }
            }
        }
    }
}
