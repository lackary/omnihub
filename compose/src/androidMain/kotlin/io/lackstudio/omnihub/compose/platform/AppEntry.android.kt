package io.lackstudio.omnihub.compose.platform

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.scenecore.scene
import io.lackstudio.omnihub.compose.layout.XrSpatialLayout
import io.lackstudio.omnihub.compose.ui.App
import io.lackstudio.omnihub.compose.ui.gallery.PhotoStackScreen
import io.lackstudio.omnihub.compose.ui.gallery.StackedPhoto
import io.lackstudio.omnihub.compose.ui.gallery.UserDetailScreen

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
fun PhotoStackEntry(photoId: String) {
//    XrPhotoStackLayout(photoId)
    val photoStack = remember {
        mutableStateListOf<StackedPhoto>().apply {
            add(StackedPhoto(id = photoId, thumbUrl = "", ratio = 1f))
        }
    }
    var currentPhotoIndex by remember { mutableIntStateOf(0) }
    SharedTransitionLayout {
        AnimatedVisibility(visible = true) {
            PhotoStackScreen(
                photos = photoStack,
                currentIndex = currentPhotoIndex,
                onIndexChanged = { newIndex -> currentPhotoIndex = newIndex },
                onClosePhoto = { closedId ->
                    val indexToRemove = photoStack.indexOfFirst { it.id == closedId }
                    if (indexToRemove != -1) {
                        photoStack.removeAt(indexToRemove)
                        if (currentPhotoIndex >= photoStack.size) {
                            currentPhotoIndex = maxOf(0, photoStack.size - 1)
                        }
                    }
                },
                onNavigateToFeature = { },
                sharedTransitionScope = this@SharedTransitionLayout,
                animatedVisibilityScope = this@AnimatedVisibility
            )
        }
    }
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
