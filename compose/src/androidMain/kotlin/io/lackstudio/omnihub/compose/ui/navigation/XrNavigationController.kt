package io.lackstudio.omnihub.compose.ui.navigation

import PhotoStackState
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * XrNavigationController is a global singleton managing spatial navigation and state.
 * 
 * CORE ARCHITECTURE: "STATE-DRIVEN NAVIGATION"
 * 
 * 1. PERSISTENT STATE: Maintains the "Single Source of Truth" for PhotoStack and UserDetail.
 *    This state survives any Activity recreation or panel reshuffling.
 * 
 * 2. INTELLIGENT ROUTING: 
 *    - If a panel doesn't exist: It creates a new [ActivityPanelEntity] and starts the Activity.
 *    - If a panel already exists: It ONLY updates the global StateFlow. The existing Activity 
 *      (which is observing the state) will automatically recompose with the new data.
 *    - WHY? This avoids calling `startActivity` repeatedly, which causes visual flickering, 
 *      Task reconstruction, and multiple activity instances stacking within the same panel.
 * 
 * 3. SESSION MANAGEMENT: Automatically clears panel references when the XR Session changes 
 *    (e.g., MainActivity recreation) to prevent stale references to disposed panels.
 */
object XrNavigationController {

    private val _openedPanels = MutableStateFlow<Set<String>>(emptySet())
    val openedPanels = _openedPanels.asStateFlow()

    private val panelsMap = mutableMapOf<String, ActivityPanelEntity>()

    private val _navRequests = MutableSharedFlow<XrNavEvent>(extraBufferCapacity = 10)
    val navRequests = _navRequests.asSharedFlow()

    private val _photoStackState = MutableStateFlow(PhotoStackState())
    val photoStackState = _photoStackState.asStateFlow()

    private val _currentUser = MutableStateFlow<String?>(null)
    val currentUser = _currentUser.asStateFlow()

    private var currentSession: Session? = null

    fun navigate(context: Context, session: Session?, density: Density, event: XrNavEvent) {
        println("[Debug XR] navigate called with event: $event")

        // Update Global State First
        when (event) {
            is XrNavEvent.NavigateToPhoto -> updatePhotoStack(PhotoNavData(event.id, event.thumbUrl, event.ratio))
            is XrNavEvent.NavigateToUser -> _currentUser.value = event.username
        }

        // If session changed (e.g., MainActivity recreated), clear invalid panel references
        if (session != currentSession) {
            println("[Debug XR] Session changed, clearing panelsMap")
            panelsMap.clear()
            _openedPanels.value = emptySet()
            currentSession = session
        }

        if (session == null) {
            println("[Debug XR] Session is null, aborting navigate")
            return
        }

        val sideWidthPx = with(density) { 1000.dp.roundToPx() }
        val sideHeightPx = with(density) { 800.dp.roundToPx() }
        val panelSize = IntSize2d(sideWidthPx, sideHeightPx)
        val offsetX = (1000.dp.value / 1000f / 2f) + (sideWidthPx / 1000f / 2f)

        val panelName = when (event) {
            is XrNavEvent.NavigateToPhoto -> "PhotoStackPanel"
            is XrNavEvent.NavigateToUser -> "UserDetailPanel"
        }

        val existingPanel = panelsMap[panelName]
        if (existingPanel != null) {
            // 🚀 IMPORTANT: If panel exists, we've already updated the Global State above.
            // The running Activity will automatically recompose. 
            // Calling startActivity here causes flickering and multiple instances.
            println("[Debug XR] Using existing panel for $panelName, state updated.")
            return
        }

        val (intent, launchPose) = when (event) {
            is XrNavEvent.NavigateToPhoto -> {
                val i = Intent().setClassName(context.packageName, "${context.packageName}.PhotoStackActivity").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                i to Pose(Vector3(offsetX, 0f, 0.15f), Quaternion.fromEulerAngles(0f, -25f, 0f))
            }
            is XrNavEvent.NavigateToUser -> {
                val i = Intent().setClassName(context.packageName, "${context.packageName}.UserDetailActivity").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                i to Pose(Vector3(-offsetX, 0f, 0.15f), Quaternion.fromEulerAngles(0f, 25f, 0f))
            }
        }

        try {
            println("[Debug XR] Creating new ActivityPanelEntity for $panelName")
            val panel = ActivityPanelEntity.create(session, panelSize, panelName, launchPose)
            panel.startActivity(intent)
            panelsMap[panelName] = panel
            _openedPanels.update { it + panelName }
        } catch (e: Exception) {
            println("[Debug XR] Failed to create panel: ${e.message}")
            e.printStackTrace()
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
        _photoStackState.update { currentState ->
            val updatedList = if (currentState.photos.any { it.photoId == navData.photoId }) {
                currentState.photos
            } else {
                currentState.photos + navData
            }
            currentState.copy(
                photos = updatedList,
                currentPhotoId = navData.photoId
            )
        }
    }

    fun removePhotoFromStack(photoId: String) {
        _photoStackState.update { currentState ->
            val updatedList = currentState.photos.filter { it.photoId != photoId }
            val nextPhotoId = if (currentState.currentPhotoId == photoId) {
                updatedList.lastOrNull()?.photoId
            } else {
                currentState.currentPhotoId
            }
            currentState.copy(
                photos = updatedList,
                currentPhotoId = nextPhotoId
            )
        }
    }
}
