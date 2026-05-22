package io.lackstudio.omnihub.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.lackstudio.omnihub.ui.gallery.PhotoStackScreen
import io.lackstudio.omnihub.ui.gallery.StackedPhoto
import io.lackstudio.omnihub.ui.navigation.XrNavigationController
import io.lackstudio.omnihub.ui.navigation.models.PhotoNavData
import io.lackstudio.omnihub.utils.LocalXrNavigation
import io.lackstudio.omnihub.utils.logging.rememberLogger

@Composable
fun XrPhotoStackLayout(
    navDataList: List<PhotoNavData>,
    currentPhotoId: String,
    onClosePhoto: (String) -> Unit = {}
) {
    val logger = rememberLogger("XrPhotoStackLayout")
    val layoutId = remember { java.util.UUID.randomUUID().toString().take(4) }
    
    // We convert PhotoNavData to StackedPhoto for the UI layer
    val photoStack = remember(navDataList) {
        navDataList.map { StackedPhoto(it.photoId, it.thumbUrl, it.ratio) }
    }
    
    var currentPhotoIndex by remember(navDataList, currentPhotoId) {
        mutableIntStateOf(navDataList.indexOfFirst { it.photoId == currentPhotoId }.coerceAtLeast(0))
    }

    LaunchedEffect(layoutId, currentPhotoId) {
        logger.d{ "[XR] LayoutInstance:$layoutId |" +
                "CurrentId:$currentPhotoId | " +
                "StackSize:${photoStack.size}"
        }
    }

    CompositionLocalProvider(
        LocalXrNavigation provides { event ->
            logger.d{ "[XR] Event received: $event" }
            // 🚀 Since PhotoStackActivity doesn't have a Session, we forward the request to MainActivity for handling
            XrNavigationController.proxyNavigate(event)
        }
    ) {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                PhotoStackScreen(
                    photos = photoStack,
                    currentIndex = currentPhotoIndex,
                    onIndexChanged = { newIndex -> currentPhotoIndex = newIndex },
                    onClosePhoto = onClosePhoto,
                    onNavigateToFeature = { },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedVisibility
                )
            }
        }
    }
}
