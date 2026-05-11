package io.lackstudio.omnihub

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import io.lackstudio.omnihub.compose.platform.PhotoStackEntry
import io.lackstudio.omnihub.compose.ui.navigation.XrNavigationController

/**
 * PhotoStackActivity handles the display of multiple photos in a stacked view.
 * 
 * DESIGN DECISIONS:
 * 1. STATELESS ACTIVITY: This activity does not manage its own state. It observes the global 
 *    singleton [XrNavigationController.photoStackState]. This ensures the photo stack is preserved
 *    even if the activity is recreated due to XR environment changes (e.g., panel layout shifts).
 * 2. REACTIVE UPDATES: Instead of relying on Intent extras for every update, it reacts to 
 *    global state changes. This avoids activity instance stacking and flickering.
 * 3. SMART CAST FIX: We use `.collectAsState().value` to obtain a stable local reference for 
 *    the compiler to perform smart casts on nullable state fields.
 */
class PhotoStackActivity : ComponentActivity() {

    private val activityId = System.identityHashCode(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("[Debug Lifecycle] PhotoStackActivity@$activityId onCreate with flags: ${Integer.toHexString(intent.flags)}")
        println("[Debug Lifecycle] PhotoStackActivity@$activityId isTaskRoot: $isTaskRoot, taskId: $taskId")

        // No need to parseIntent here anymore, because XrNavigationController.navigate handles data updates

        setContent {
            // Use direct value to avoid delegation and smart cast issues
            val state = XrNavigationController.photoStackState.collectAsState().value

            state.currentPhotoId?.let { photoId ->
                if (state.photos.isNotEmpty()) {
                    PhotoStackEntry(
                        photoStack = state.photos,
                        currentPhotoId = photoId,
                        onClosePhoto = { closedId ->
                            XrNavigationController.removePhotoFromStack(closedId)
                            if (XrNavigationController.photoStackState.value.photos.isEmpty()) {
                                finish()
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        println("[Debug Lifecycle] PhotoStackActivity@$activityId onNewIntent")
        setIntent(intent)
        // Although the global singleton is already updated, we keep this for potential extensibility (e.g., deep links)
    }

    override fun onDestroy() {
        super.onDestroy()
        println("[Debug Lifecycle] PhotoStackActivity@$activityId onDestroy")
        if (!isChangingConfigurations) {
            XrNavigationController.markPanelClosed("PhotoStackPanel")
        }
    }
}
