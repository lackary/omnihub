package io.lackstudio.omnihub.compose.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.fillMaxSize
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.scene
import io.lackstudio.omnihub.compose.ui.gallery.PhotoStackScreen
import io.lackstudio.omnihub.compose.ui.gallery.StackedPhoto

@Composable
fun XrPhotoStackLayout(photoId: String) {
    val session = LocalSession.current
    val isSpatialUiEnabled = LocalSpatialCapabilities.current.isAppEnvironmentEnabled
    var isReady by remember { mutableStateOf(false) }

    // Move the PhotoStack activity to the right (end side) and apply inward rotation
    LaunchedEffect(session, isSpatialUiEnabled) {
        if (session != null) {
            if (!isSpatialUiEnabled) {
                session.scene.requestFullSpaceMode()
            }
            // Surround Pose: Right offset (0.9m) and rotated -20 degrees to face user
            val surroundPose = Pose(
                Vector3(0.9f, 0f, 0.1f),
                Quaternion.fromEulerAngles(0f, -20f, 0f)
            )
            session.scene.mainPanelEntity.setPose(surroundPose)
            isReady = true
        }
    }

    val photoStack = remember { 
        mutableStateListOf<StackedPhoto>().apply {
            add(StackedPhoto(id = photoId, thumbUrl = "", ratio = 1f))
        }
    }
    var currentPhotoIndex by remember { mutableIntStateOf(0) }

    if (isReady) {
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
}
