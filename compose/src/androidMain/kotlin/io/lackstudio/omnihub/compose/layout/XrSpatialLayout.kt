package io.lackstudio.omnihub.compose.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.MovePolicy
import androidx.xr.compose.subspace.ResizePolicy
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.width
import io.lackstudio.omnihub.compose.ui.App
import io.lackstudio.omnihub.compose.ui.gallery.PhotoDetailScreen
import io.lackstudio.omnihub.compose.ui.gallery.UserDetailScreen
import io.lackstudio.omnihub.compose.ui.navigation.XrNavEvent
import io.lackstudio.omnihub.compose.utils.LocalXrNavigation

@Composable
fun XrSpatialLayout() {
    // Keep track of the photo ID and info to be displayed on the right
    var selectedPhotoId by remember { mutableStateOf<String?>(null) }
    var selectedThumbUrl by remember { mutableStateOf<String?>(null) }
    var selectedPhotoRatio by remember { mutableStateOf(1f) }

    var selectedUsername by remember { mutableStateOf<String?>(null) }

    // Inject interceptor
    // Any Composable inside this Provider (including nested GalleryScreen)
    // will receive this lambda when calling LocalXrNavigation.current
    CompositionLocalProvider(
        LocalXrNavigation provides { event ->
            when(event) {
                is XrNavEvent.NavigateToPhoto -> {
                    selectedPhotoId = event.id
                    selectedThumbUrl = event.thumbUrl
                    selectedPhotoRatio = event.ratio
                }
                is XrNavEvent.NavigateToUser -> {
                    selectedUsername = event.username
                }
            }
            // Update state here when GalleryScreen triggers navigation
        }
    ) {
        Subspace {
            SpatialBox {
                val mainPanelWidth = 1280.dp
                val mainPanelHeight = 800.dp
                val gap = 48.dp // Increase the gap slightly for a more comfortable look

                val hasPhoto = selectedPhotoId != null
                val hasUser = selectedUsername != null

                val photoPanelWidth = if (hasPhoto) {
                    (mainPanelHeight.value * selectedPhotoRatio).dp.coerceIn(400.dp, 1200.dp)
                } else 0.dp

                val userPanelWidth = 1280.dp

                // --- 1. Calculate relative coordinates (Assume main screen at 0, others queue to the right) ---
                val relativeMainX = 0.dp
                val relativePhotoX = if (hasPhoto) {
                    relativeMainX + (mainPanelWidth / 2) + gap + (photoPanelWidth / 2)
                } else 0.dp
                val relativeUserX = if (hasUser) {
                    relativePhotoX + (photoPanelWidth / 2) + gap + (userPanelWidth / 2)
                } else 0.dp

                // --- 2. Determine visual focus (which one to move to x = 0) ---
                val targetShiftX = when {
                    hasUser -> -relativeUserX    // When User is open, move everything left to align with User
                    hasPhoto -> -relativePhotoX  // When Photo is open, move everything left to align with Photo
                    else -> -relativeMainX       // When none are open, align with main screen
                }

                // Global smooth animation: push the entire spatial track
                val globalShiftX by animateDpAsState(targetValue = targetShiftX, label = "globalShiftX")

                // Dynamic Z-axis animation: make the focused panel pop out slightly by 50.dp
                val mainZ by animateDpAsState(targetValue = if (!hasPhoto && !hasUser) 50.dp else 0.dp, label = "mainZ")
                val photoZ by animateDpAsState(targetValue = if (hasPhoto && !hasUser) 50.dp else 0.dp, label = "photoZ")
                val userZ by animateDpAsState(targetValue = if (hasUser) 50.dp else 0.dp, label = "userZ")

                // --- Left side: Main application ---
                SpatialPanel(
                    modifier = SubspaceModifier
                        .width(mainPanelWidth)
                        .height(mainPanelHeight)
                        .offset(x = relativeMainX + globalShiftX, z = mainZ), // Smoothly move left based on the calculation results
                    dragPolicy = MovePolicy(),
                    resizePolicy = ResizePolicy()
                ) {
                    App()
                }

                // --- Central focus: Detail page ---
                if (hasPhoto) {
                    SpatialPanel(
                        modifier = SubspaceModifier
                            .width(photoPanelWidth)
                            .height(mainPanelHeight)
                            .offset(x = relativePhotoX + globalShiftX, z = photoZ),
                        dragPolicy = MovePolicy(),
                        resizePolicy = ResizePolicy()
                    ) {
                        SharedTransitionLayout {
                            AnimatedVisibility(visible = true) {
                                Box(modifier = Modifier.fillMaxSize()) {

                                    key(selectedPhotoId) {
                                        PhotoDetailScreen(
                                            id = selectedPhotoId!!,
                                            thumbUrl = selectedThumbUrl ?: "",
                                            onBack = { selectedPhotoId = null },
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

                // --- Right side: User detail page ---
                if (hasUser) {
                    SpatialPanel(
                        modifier = SubspaceModifier
                            .width(userPanelWidth)
                            .height(mainPanelHeight)
                            .offset(x = relativeUserX + globalShiftX, z = userZ),
                        dragPolicy = MovePolicy(),
                        resizePolicy = ResizePolicy()
                    ) {
                        SharedTransitionLayout {
                            AnimatedVisibility(visible = true) {
                                Box(modifier = Modifier.fillMaxSize()) {

                                    key(selectedUsername) {
                                        UserDetailScreen(
                                            username = selectedUsername!!,
                                            onBack = { selectedUsername = null },
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
    }
}

@Preview(showBackground = true)
@Composable
fun XrSpatialLayoutPreview() {
    XrSpatialLayout()
}
