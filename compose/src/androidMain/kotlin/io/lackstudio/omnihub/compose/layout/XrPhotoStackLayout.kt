package io.lackstudio.omnihub.compose.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.fillMaxSize
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.scene
import io.lackstudio.omnihub.compose.ui.gallery.PhotoStackScreen
import io.lackstudio.omnihub.compose.ui.gallery.StackedPhoto

@Composable
fun XrPhotoStackLayout(photoId: String) {
    val session = LocalSession.current
    val isSpatialUiEnabled = LocalSpatialCapabilities.current.isAppEnvironmentEnabled

    // Move the PhotoStack activity to the right (end side) and ensure Full Space
    LaunchedEffect(session, isSpatialUiEnabled) {
        if (session != null) {
            if (!isSpatialUiEnabled) {
                session.scene.requestFullSpaceMode()
            }
//            session.scene.mainPanelEntity.setPose(Pose(Vector3(0.85f, 0f, 0f)))
        }
    }
    // In Multi-Activity mode, we initialize the stack with the photo that triggered the activity.
    // Future navigations to photos within this activity could add to this stack.
    val photoStack = remember { 
        mutableStateListOf<StackedPhoto>().apply {
            // Note: We don't have the full StackedPhoto info (thumbUrl, ratio) here yet 
            // if we only pass photoId. 
            // In a real app, we'd fetch it from a repository.
            // For now, we'll placeholder it or assume the screen can handle partial info/loading.
            add(StackedPhoto(id = photoId, thumbUrl = "", ratio = 1f))
        }
    }
    var currentPhotoIndex by remember { mutableIntStateOf(0) }

    Subspace {
        SpatialPanel(SubspaceModifier.fillMaxSize()) {
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
                            // If stack is empty, we might want to close the activity
                        },
                        onNavigateToFeature = { },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@AnimatedVisibility
                    )
                }
            }
        }
    }
}
