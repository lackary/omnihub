package io.lackstudio.omnihub

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import io.lackstudio.omnihub.compose.platform.PhotoStackEntry
import io.lackstudio.omnihub.compose.ui.navigation.XrNavigationController

class PhotoStackActivity : ComponentActivity() {

    private val activityId = System.identityHashCode(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("[Debug Lifecycle] PhotoStackActivity@$activityId onCreate")

        // No need to parseIntent here anymore, because XrNavigationController.navigate handles data updates

        setContent {
            // Read state from the global singleton
            val photoStack = XrNavigationController.photoStack.collectAsState().value
            val currentPhotoId = XrNavigationController.currentPhotoId.collectAsState().value

            currentPhotoId?.let { photoId ->
                if (photoStack.isNotEmpty()) {
                    PhotoStackEntry(
                        photoStack = photoStack,
                        currentPhotoId = photoId,
                        onClosePhoto = { closedId ->
                            XrNavigationController.removePhotoFromStack(closedId)
                            if (XrNavigationController.photoStack.value.isEmpty()) {
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
