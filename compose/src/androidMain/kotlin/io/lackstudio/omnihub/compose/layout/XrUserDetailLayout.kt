package io.lackstudio.omnihub.compose.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import io.lackstudio.omnihub.compose.ui.gallery.UserDetailScreen

@Composable
fun XrUserDetailLayout(username: String) {
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
