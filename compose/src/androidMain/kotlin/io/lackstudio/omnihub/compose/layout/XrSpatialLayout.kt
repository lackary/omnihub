package io.lackstudio.omnihub.compose.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.MovePolicy
import androidx.xr.compose.subspace.ResizePolicy
import androidx.xr.compose.subspace.SpatialCurvedRow
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.width
import io.lackstudio.omnihub.compose.ui.App
import io.lackstudio.omnihub.compose.ui.gallery.PhotoStackScreen
import io.lackstudio.omnihub.compose.ui.gallery.StackedPhoto
import io.lackstudio.omnihub.compose.ui.gallery.UserDetailScreen
import io.lackstudio.omnihub.compose.ui.navigation.XrNavEvent
import io.lackstudio.omnihub.compose.utils.LocalXrNavigation

@Composable
fun XrSpatialLayout() {
    val photoStack = remember { mutableStateListOf<StackedPhoto>() }
    var currentPhotoIndex by remember { mutableIntStateOf(0) }
    var selectedUsername by remember { mutableStateOf<String?>(null) }

    CompositionLocalProvider(
        LocalXrNavigation provides { event ->
            when(event) {
                is XrNavEvent.NavigateToPhoto -> {
                    // Force close the left User panel when opening the right photo panel!
                    selectedUsername = null

                    val newPhoto = StackedPhoto(event.id, event.thumbUrl, event.ratio)
                    if (!photoStack.any { it.id == event.id }) photoStack.add(newPhoto)
                    currentPhotoIndex = photoStack.indexOfFirst { it.id == event.id }
                }
                is XrNavEvent.NavigateToUser -> {

                    selectedUsername = event.username
                }
            }
        }
    ) {
        Subspace {
            SpatialCurvedRow(
                curveRadius = 825.dp
            ) {
                val hasUser = selectedUsername != null
                val hasStack = photoStack.isNotEmpty() && !hasUser

                val panelWidth = 848.dp
                val panelHeight = 800.dp

//                val mainPanelWidth = 560.dp
//                val userPanelWidth = 1000.dp
//                val stackPanelWidth = 1000.dp

//                val gap = 8.dp // Scale down the panel gap proportionally

                // --- Left: User Detail Screen ---
                if (hasUser) {
                    SpatialPanel(
                        modifier = SubspaceModifier
                            .width(panelWidth)
                            .height(panelHeight),
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

                // --- Center: Main Application ---
                SpatialPanel(
                    modifier = SubspaceModifier
                        .width(panelWidth)
                        .height(panelHeight),
                    dragPolicy = MovePolicy(),
                    resizePolicy = ResizePolicy()
                ) {
                    App()
                }

                // --- Right: Photo Stack Panel ---
                if (hasStack) {
                    SpatialPanel(
                        modifier = SubspaceModifier
                            .width(panelWidth)
                            .height(panelHeight),
                        dragPolicy = MovePolicy(),
                        resizePolicy = ResizePolicy()
                    ) {
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
    }
}
