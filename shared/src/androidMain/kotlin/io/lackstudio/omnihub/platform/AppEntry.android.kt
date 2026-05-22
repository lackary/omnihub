package io.lackstudio.omnihub.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.scenecore.scene
import io.lackstudio.omnihub.layout.XrPhotoStackLayout
import io.lackstudio.omnihub.layout.XrAppLayout
import io.lackstudio.omnihub.layout.XrUserDetailLayout
import io.lackstudio.omnihub.ui.App
import io.lackstudio.omnihub.ui.navigation.models.PhotoNavData
import io.lackstudio.omnihub.utils.logging.rememberLogger

@Composable
actual fun AppEntry() {
    val logger = rememberLogger("AppEntry")
    val session = LocalSession.current
    val isSpatialUiEnabled = LocalSpatialCapabilities.current.isAppEnvironmentEnabled
    LaunchedEffect(session, isSpatialUiEnabled) {
        if (session != null && !isSpatialUiEnabled) {
            session.scene.requestFullSpaceMode()
        }
    }
    if (isSpatialUiEnabled) {
        logger.i{ "XR mode" }
        // === XR Mode ===
        XrAppLayout()
    } else {
        logger.i{ "mobile mode" }
        // === Mobile Mode ===
        // Call App directly, LocalXrNavigation defaults to null, so it will use standard navigation
        App()
    }

}

@Composable
fun PhotoStackEntry(
    photoStack: List<PhotoNavData>,
    currentPhotoId: String,
    onClosePhoto: (String) -> Unit = {}
) {
    XrPhotoStackLayout(photoStack, currentPhotoId, onClosePhoto)
}

@Composable
fun UserDetailEntry(
    username: String,
    onClose: () -> Unit = {}
) {
    XrUserDetailLayout(username, onClose)
}
