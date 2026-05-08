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
import io.lackstudio.omnihub.compose.ui.gallery.PhotoStackScreen
import io.lackstudio.omnihub.compose.ui.gallery.StackedPhoto
import io.lackstudio.omnihub.compose.ui.navigation.models.PhotoNavData
import io.lackstudio.omnihub.compose.utils.logging.rememberLogger

@Composable
fun XrPhotoStackLayout(navData: PhotoNavData) {
    val logger = rememberLogger("XrPhotoStackLayout")
    val layoutId = remember { java.util.UUID.randomUUID().toString().take(4) }
    val photoStack = remember { mutableStateListOf<StackedPhoto>() }
    var currentPhotoIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(navData.photoId) {
        // Logging: Print Layout instance ID, incoming photoId, and current stack size
        logger.d{"[Debug XR] LayoutInstance:$layoutId | IncomingId:${navData.photoId} | CurrentStackSize:${photoStack.size}"}

        val newPhoto = StackedPhoto(navData.photoId, navData.thumbUrl, navData.ratio)
        if (!photoStack.any { it.id == navData.photoId }) {
            photoStack.add(newPhoto)
        }
        currentPhotoIndex = photoStack.indexOfFirst { it.id == navData.photoId }
    }

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
