package io.lackstudio.omnihub.ui.navigation

import PhotoStackState
import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.ActivityPanelEntity
import io.lackstudio.omnihub.ui.navigation.models.PhotoNavData
import io.lackstudio.omnihub.utils.logging.AppLog
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
    private val logger = AppLog.withTag("XrNavigationController")

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

    fun navigate(
        context: Context,
        session: Session?,
        density: Density,
        event: XrNavEvent,
        panelWith: Dp,
        panelHeight: Dp
    ) {
        logger.d{ "[XR] navigate called with event: $event" }

        // Update Global State First
        when (event) {
            is XrNavEvent.NavigateToPhoto -> updatePhotoStack(PhotoNavData(event.id, event.thumbUrl, event.ratio))
            is XrNavEvent.NavigateToUser -> _currentUser.value = event.username
        }

        // If session changed (e.g., MainActivity recreated), clear invalid panel references
        if (session != currentSession) {
            logger.d { "[XR] Session changed, clearing panelsMap" }
            panelsMap.clear()
            _openedPanels.value = emptySet()
            currentSession = session
        }

        if (session == null) {
            logger.d { "[XR] Session is null, aborting navigate" }
            return
        }

        // Set size to match the main panel's scale
        val sideWidthMeters = 1.5f
        val sideHeightMeters = 1.6f
        val mainWidthMeters = 1.0f
        val gapMeters = 0.8f


        val sideWidthPx = with(density) { panelWith.roundToPx() }
        val sideHeightPx = with(density) { panelHeight.roundToPx() }
        val panelSize = IntSize2d(sideWidthPx, sideHeightPx)

        // Calculate center point offset: (half of main panel width) + gap + (half of side panel width)
        val offsetX = (mainWidthMeters / 2f) + gapMeters + (sideWidthMeters / 2f)

        logger.d { "sideWidthPx: $sideWidthPx, sideHeightPx: $sideHeightPx  panelSize: $panelSize, offsetX: $offsetX" }

        val panelName = when (event) {
            is XrNavEvent.NavigateToPhoto -> "PhotoStackPanel"
            is XrNavEvent.NavigateToUser -> "UserDetailPanel"
        }

        val existingPanel = panelsMap[panelName]
        if (existingPanel != null) {
            // IMPORTANT: If panel exists, we've already updated the Global State above.
            // The running Activity will automatically recompose. 
            // Calling startActivity here causes flickering and multiple instances.
            logger.d { "[XR] Using existing panel for $panelName, state updated." }
            return
        }

        val (intent, launchPose) = when (event) {
            is XrNavEvent.NavigateToPhoto -> {
                val i = Intent().setClassName(context.packageName, "${context.packageName}.PhotoStackActivity").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                i to Pose(Vector3(offsetX, 0f, 0.2f), Quaternion.fromEulerAngles(0f, -25f, 0f))
            }
            is XrNavEvent.NavigateToUser -> {
                val i = Intent().setClassName(context.packageName, "${context.packageName}.UserDetailActivity").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                i to Pose(Vector3(-offsetX, 0f, 0.2f), Quaternion.fromEulerAngles(0f, 25f, 0f))
            }
        }

        try {
            logger.d { "[XR] Creating new ActivityPanelEntity for $panelName" }
            val panel = ActivityPanelEntity.create(session, panelSize, panelName, launchPose)
            logger.d { "panel before: ${panel.size}"}
            // KEY: Explicitly set physical entity dimensions (meters).
            // This overrides the Alpha 11 default behavior where pixels map directly to meters, ensuring the panel size matches expectations.
            // Alpha 11 default is w 1.25 x h 1.0
            panel.size = FloatSize2d(sideWidthMeters, sideHeightMeters)

            logger.d { "panel after: ${panel.size}"}

            panel.startActivity(intent)
            panelsMap[panelName] = panel
            _openedPanels.update { it + panelName }
        } catch (e: Exception) {
            logger.e(e) { "[XR] Failed to create panel: ${e.message}" }
        }
    }

    fun markPanelClosed(panelName: String) {
        logger.d { "[XR] markPanelClosed: $panelName" }
        panelsMap.remove(panelName)
        _openedPanels.update { it - panelName }
    }

    fun proxyNavigate(event: XrNavEvent) {
        logger.d { "[XR] proxyNavigate emitting event: $event" }
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
