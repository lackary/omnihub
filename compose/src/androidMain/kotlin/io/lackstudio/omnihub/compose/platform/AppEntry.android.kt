package io.lackstudio.omnihub.compose.platform

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.LocalSpatialCapabilities
import androidx.xr.scenecore.scene
import io.lackstudio.omnihub.compose.layout.XrSpatialLayout
import io.lackstudio.omnihub.compose.ui.App

@Composable
actual fun AppEntry() {
    val session = LocalSession.current
    val isSpatialUiEnabled = LocalSpatialCapabilities.current.isAppEnvironmentEnabled
    LaunchedEffect(session, isSpatialUiEnabled) {
        if (session != null && !isSpatialUiEnabled) {
            session.scene.requestFullSpaceMode()
        }
    }
    if (isSpatialUiEnabled) {
        Log.d("AppEntry","xr")
        // === XR Mode ===
        XrSpatialLayout()
    } else {
        Log.d("AppEntry","mobile")
        // === Mobile Mode ===
        // Call App directly, LocalXrNavigation defaults to null, so it will use standard navigation
        App()
    }

}
