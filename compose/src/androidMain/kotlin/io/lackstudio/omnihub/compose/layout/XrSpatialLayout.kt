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
import androidx.xr.compose.subspace.SpatialBox
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.offset
import androidx.xr.compose.subspace.layout.width
import io.lackstudio.omnihub.compose.ui.App
import io.lackstudio.omnihub.compose.ui.gallery.PhotoStackScreen
import io.lackstudio.omnihub.compose.ui.gallery.StackedPhoto
import io.lackstudio.omnihub.compose.ui.gallery.UserDetailScreen
import io.lackstudio.omnihub.compose.ui.navigation.XrNavEvent
import io.lackstudio.omnihub.compose.utils.LocalXrNavigation

enum class PanelType { STACK, USER }

@Composable
fun XrSpatialLayout() {
    val photoStack = remember { mutableStateListOf<StackedPhoto>() }
    var currentPhotoIndex by remember { mutableIntStateOf(0) }
    var selectedUsername by remember { mutableStateOf<String?>(null) }

    val panelOrder = remember { mutableStateListOf<PanelType>() }

    CompositionLocalProvider(
        LocalXrNavigation provides { event ->
            when(event) {
                is XrNavEvent.NavigateToPhoto -> {
                    val newPhoto = StackedPhoto(event.id, event.thumbUrl, event.ratio)
                    if (!photoStack.any { it.id == event.id }) {
                        photoStack.add(newPhoto)
                    }
                    currentPhotoIndex = photoStack.indexOfFirst { it.id == event.id }

                    panelOrder.remove(PanelType.STACK)
                    panelOrder.add(PanelType.STACK)
                }
                is XrNavEvent.NavigateToUser -> {
                    selectedUsername = event.username

                    panelOrder.remove(PanelType.USER)
                    panelOrder.add(PanelType.USER)
                }
            }
        }
    ) {
        Subspace {
            SpatialBox {
                val hasStack = photoStack.isNotEmpty()
                val hasUser = selectedUsername != null

                val mainPanelWidth = 1280.dp
                val mainPanelHeight = 800.dp
                val userPanelWidth = 1280.dp
                val stackPanelWidth = if (hasStack) 1000.dp else 0.dp

                // Spatial layout calculations
                val relativeMainX = 0.dp

                val relativeUserX = if (hasUser) {
                    relativeMainX - (mainPanelWidth / 2) - 120.dp - (userPanelWidth / 2)
                } else 0.dp

                val relativeStackX = if (hasStack) {
                    relativeMainX + (mainPanelWidth / 2) + 48.dp + (stackPanelWidth / 2)
                } else 0.dp

                // Determine global field of view focus
                val focusedPanel = panelOrder.lastOrNull()
                val targetShiftX = when (focusedPanel) {
                    PanelType.STACK -> -relativeStackX
                    PanelType.USER -> -relativeUserX
                    null -> -relativeMainX
                }

                // Global translation animation
                val globalShiftX by animateDpAsState(targetValue = targetShiftX, label = "globalShiftX")

                // --- Left: User Detail Screen ---
                if (hasUser) {
                    SpatialPanel(
                        modifier = SubspaceModifier
                            .width(userPanelWidth)
                            .height(mainPanelHeight)
                            // Keep only X-axis translation
                            .offset(x = relativeUserX + globalShiftX),
                        dragPolicy = MovePolicy(),
                        resizePolicy = ResizePolicy()
                    ) {
                        SharedTransitionLayout {
                            AnimatedVisibility(visible = true) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    key(selectedUsername) {
                                        UserDetailScreen(
                                            username = selectedUsername!!,
                                            onBack = {
                                                selectedUsername = null
                                                panelOrder.remove(PanelType.USER)
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

                // --- Center: Main App ---
                SpatialPanel(
                    modifier = SubspaceModifier
                        .width(mainPanelWidth)
                        .height(mainPanelHeight)
                        // Keep only X-axis translation
                        .offset(x = relativeMainX + globalShiftX),
                    dragPolicy = MovePolicy(),
                    resizePolicy = ResizePolicy()
                ) {
                    App()
                }

                // --- Right: Photo Stack Panel ---
                if (hasStack) {
                    SpatialPanel(
                        modifier = SubspaceModifier
                            .width(stackPanelWidth)
                            .height(mainPanelHeight)
                            // Keep only X-axis translation
                            .offset(x = relativeStackX + globalShiftX),
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
                                            if (photoStack.isEmpty()) {
                                                panelOrder.remove(PanelType.STACK)
                                            } else if (currentPhotoIndex >= photoStack.size) {
                                                currentPhotoIndex = photoStack.size - 1
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
