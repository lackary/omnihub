package io.lackstudio.omnihub.compose.layout

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
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.fillMaxSize
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.scene
import io.lackstudio.omnihub.compose.ui.gallery.UserDetailScreen

@Composable
fun XrUserDetailLayout(username: String) {
    val session = LocalSession.current
    val isSpatialUiEnabled = LocalSpatialCapabilities.current.isAppEnvironmentEnabled

    // Move the UserDetail activity to the left (start side) and ensure Full Space
    LaunchedEffect(session, isSpatialUiEnabled) {
        if (session != null) {
            if (!isSpatialUiEnabled) {
                session.scene.requestFullSpaceMode()
            }
//            session.scene.mainPanelEntity.setPose(Pose(Vector3(-0.85f, 0f, 0f)))
        }
    }
    Subspace {
        SpatialPanel(SubspaceModifier.fillMaxSize()) {
            SharedTransitionLayout {
                AnimatedVisibility(visible = true) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        key(username) {
                            UserDetailScreen(
                                username = username,
                                onBack = { /* In multi-activity mode, this might close the activity */ },
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
