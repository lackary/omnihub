package io.lackstudio.omnihub.compose.platform

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.scenecore.scene
import io.lackstudio.omnihub.compose.layout.XrPhotoStackLayout
import io.lackstudio.omnihub.compose.layout.XrSpatialLayout
import io.lackstudio.omnihub.compose.ui.App
import io.lackstudio.omnihub.compose.ui.gallery.UserDetailScreen
import io.lackstudio.omnihub.compose.ui.navigation.models.PhotoNavData

@Composable
actual fun AppEntry() {
    val session = LocalSession.current
    val isSpatialUiEnabled = LocalSpatialCapabilities.current.isAppEnvironmentEnabled
    LaunchedEffect(session, isSpatialUiEnabled) {
        if (session != null && !isSpatialUiEnabled) {
            session.scene.requestFullSpaceMode()
        }
    }
    if (isSpatialUiEnabled) {
        Log.d("AppEntry","xr")
        // === XR Mode ===
        XrSpatialLayout()
    } else {
        Log.d("AppEntry","mobile")
        // === Mobile Mode ===
        // Call App directly, LocalXrNavigation defaults to null, so it will use standard navigation
        App()
    }

}

@Composable
fun PhotoStackEntry(navData: PhotoNavData) {
    XrPhotoStackLayout(navData)
}

@Composable
fun UserDetailEntry(username: String) {
//    XrUserDetailLayout(username)
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
