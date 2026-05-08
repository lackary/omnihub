package io.lackstudio.omnihub.compose.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import io.lackstudio.omnihub.compose.ui.gallery.UserDetailScreen

@Composable
fun XrUserDetailLayout(username: String) {
    val session = LocalSession.current
    val isSpatialUiEnabled = LocalSpatialCapabilities.current.isAppEnvironmentEnabled
    var isReady by remember { mutableStateOf(false) }

    // Move the UserDetail activity to the left (start side) and apply inward rotation
    LaunchedEffect(session, isSpatialUiEnabled) {
        if (session != null) {
            if (!isSpatialUiEnabled) {
                session.scene.requestFullSpaceMode()
            }
            // Surround Pose: Left offset (-0.9m) and rotated 20 degrees to face user
            val surroundPose = Pose(
                Vector3(-0.9f, 0f, 0.1f), 
                Quaternion.fromEulerAngles(0f, 20f, 0f)
            )
            session.scene.mainPanelEntity.setPose(surroundPose)
            isReady = true
        }
    }

    if (isReady) {
        Subspace {
            SpatialPanel(SubspaceModifier.fillMaxSize()) {
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
    }
}
