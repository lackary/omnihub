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
import io.lackstudio.omnihub.compose.utils.LocalXrNavigation

@Composable
fun XrSpatialLayout() {
    // Keep track of the photo ID and info to be displayed on the right
    var selectedPhotoId by remember { mutableStateOf<String?>(null) }
    var selectedThumbUrl by remember { mutableStateOf<String?>(null) }
    var selectedPhotoRatio by remember { mutableStateOf(1f) }

    // Inject interceptor
    // Any Composable inside this Provider (including nested GalleryScreen)
    // will receive this lambda when calling LocalXrNavigation.current
    CompositionLocalProvider(
        LocalXrNavigation provides { photoId, url, ratio ->
            // Update state here when GalleryScreen triggers navigation
            selectedPhotoId = photoId
            selectedThumbUrl = url
            selectedPhotoRatio = ratio
        }
    ) {
        Subspace {
            SpatialBox {
                val mainPanelWidth = 1280.dp
                val mainPanelHeight = 800.dp
                val gap = 48.dp // Increase the gap slightly for a more comfortable look

                // Pre-calculate the width of the detail window
                val detailPanelWidth = (mainPanelHeight.value * selectedPhotoRatio).dp.coerceIn(400.dp, 1200.dp)

                // Precisely calculate the distance the main panel needs to move away
                // If a photo is selected, the main panel moves to the left (half of main panel + half of detail panel + gap)
                val targetMainOffsetX = if (selectedPhotoId != null) {
                    -((mainPanelWidth / 2) + (detailPanelWidth / 2) + gap)
                } else {
                    0.dp
                }

                // Apply smooth animation
                val mainOffsetX by animateDpAsState(
                    targetValue = targetMainOffsetX,
                    label = "mainPanelOffsetAnimation"
                )

                // --- Left side: Main application ---
                SpatialPanel(
                    modifier = SubspaceModifier
                        .width(mainPanelWidth)
                        .height(mainPanelHeight)
                        .offset(x = mainOffsetX), // Smoothly move left based on the calculation results
                    dragPolicy = MovePolicy(),
                    resizePolicy = ResizePolicy()
                ) {
                    App()
                }

                // --- Central focus: Detail page ---
                if (selectedPhotoId != null) {
                    SpatialPanel(
                        modifier = SubspaceModifier
                            .width(detailPanelWidth)
                            .height(mainPanelHeight)
                            // ★ Key: X = 0 makes it perfectly occupy the center of vision, Z = 50 makes it pop out slightly
                            .offset(x = 0.dp, z = 50.dp),
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
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun XrSpatialLayoutPreview() {
    XrSpatialLayout()
}
