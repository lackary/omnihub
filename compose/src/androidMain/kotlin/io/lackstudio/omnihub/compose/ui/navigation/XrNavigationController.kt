package io.lackstudio.omnihub.compose.ui.navigation

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.ActivityPanelEntity
import io.lackstudio.omnihub.compose.ui.navigation.models.PhotoNavData
import io.lackstudio.omnihub.compose.ui.navigation.models.PhotoNavData.Companion.putPhotoNavData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object XrNavigationController {

    private val _openedPanels = MutableStateFlow<Set<String>>(emptySet())
    val openedPanels = _openedPanels.asStateFlow()

    private val panelsMap = mutableMapOf<String, ActivityPanelEntity>()

    private val _navRequests = MutableSharedFlow<XrNavEvent>(extraBufferCapacity = 10)
    val navRequests = _navRequests.asSharedFlow()

    private val _photoStack = MutableStateFlow<List<PhotoNavData>>(emptyList())
    val photoStack = _photoStack.asStateFlow()

    private val _currentPhotoId = MutableStateFlow<String?>(null)
    val currentPhotoId = _currentPhotoId.asStateFlow()

    fun navigate(context: Context, session: Session?, density: Density, event: XrNavEvent) {
        println("[Debug XR] navigate called with event: $event")
        
        if (event is XrNavEvent.NavigateToPhoto) {
            updatePhotoStack(PhotoNavData(event.id, event.thumbUrl, event.ratio))
        }

        if (session == null) {
            println("[Debug XR] Session is null, aborting navigate")
            return
        }

        val sideWidthPx = with(density) { 1000.dp.roundToPx() }
        val sideHeightPx = with(density) { 800.dp.roundToPx() }
        val panelSize = IntSize2d(sideWidthPx, sideHeightPx)
        val offsetX = (1000.dp.value / 1000f / 2f) + (sideWidthPx / 1000f / 2f)

        val (intent, panelName, launchPose) = when (event) {
            is XrNavEvent.NavigateToPhoto -> {
                val i = Intent().setClassName(context.packageName, "${context.packageName}.PhotoStackActivity").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putPhotoNavData(PhotoNavData(event.id, event.thumbUrl, event.ratio))
                }
                Triple(i, "PhotoStackPanel", Pose(Vector3(offsetX, 0f, 0.15f), Quaternion.fromEulerAngles(0f, -25f, 0f)))
            }
            is XrNavEvent.NavigateToUser -> {
                val i = Intent().setClassName(context.packageName, "${context.packageName}.UserDetailActivity").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("USERNAME", event.username)
                }
                Triple(i, "UserDetailPanel", Pose(Vector3(-offsetX, 0f, 0.15f), Quaternion.fromEulerAngles(0f, 25f, 0f)))
            }
        }

        val existingPanel = panelsMap[panelName]
        if (existingPanel != null) {
            try {
                println("[Debug XR] Using existing panel for $panelName")
                existingPanel.startActivity(intent)
                return
            } catch (e: Exception) {
                println("[Debug XR] Failed to use existing panel $panelName: ${e.message}")
                panelsMap.remove(panelName)
                _openedPanels.update { it - panelName }
            }
        }

        try {
            println("[Debug XR] Creating new ActivityPanelEntity for $panelName")
            val panel = ActivityPanelEntity.create(session, panelSize, panelName, launchPose)
            panel.startActivity(intent)
            panelsMap[panelName] = panel
            _openedPanels.update { it + panelName }
        } catch (e: Exception) {
            println("[Debug XR] Failed to create panel, falling back to startActivity: ${e.message}")
            e.printStackTrace()
            context.startActivity(intent)
        }
    }

    fun markPanelClosed(panelName: String) {
        println("[Debug XR] markPanelClosed: $panelName")
        panelsMap.remove(panelName)
        _openedPanels.update { it - panelName }
    }

    fun proxyNavigate(event: XrNavEvent) {
        println("[Debug XR] proxyNavigate emitting event: $event")
        _navRequests.tryEmit(event)
    }

    private fun updatePhotoStack(navData: PhotoNavData) {
        _currentPhotoId.value = navData.photoId
        _photoStack.update { currentList ->
            if (currentList.any { it.photoId == navData.photoId }) {
                currentList
            } else {
                currentList + navData
            }
        }
    }

    fun removePhotoFromStack(photoId: String) {
        _photoStack.update { list -> list.filter { it.photoId != photoId } }
        if (_currentPhotoId.value == photoId) {
            _currentPhotoId.value = _photoStack.value.lastOrNull()?.photoId
        }
    }
}
