package io.lackstudio.omnihub

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.lackstudio.omnihub.compose.platform.UserDetailEntry
import io.lackstudio.omnihub.compose.ui.navigation.XrNavigationController
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * UserDetailActivity displays detailed information about a user.
 * 
 * DESIGN DECISIONS:
 * 1. GLOBAL STATE OBSERVATION: Observes [XrNavigationController.currentUser] for real-time updates.
 *    This allows the activity to update its content without being restarted when a user navigates 
 *    to a different profile while the panel is already open.
 * 2. LIFECYCLE SYNC: Uses [XrNavigationController.markPanelClosed] in [onDestroy] to keep the 
 *    navigation controller's panel map in sync with the actual activity lifecycle.
 */
class UserDetailActivity : ComponentActivity() {

    private val activityId = System.identityHashCode(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("[Debug Lifecycle] UserDetailActivity@$activityId onCreate")

        setContent {
            // Read state from the global singleton instead of local Flow/Intent
            val username by XrNavigationController.currentUser.collectAsState()
            
            username?.let {
                UserDetailEntry(it)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        println("[Debug Lifecycle] UserDetailActivity@$activityId onNewIntent")
        setIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        println("[Debug Lifecycle] UserDetailActivity@$activityId onDestroy")
        if (!isChangingConfigurations) {
            XrNavigationController.markPanelClosed("UserDetailPanel")
        }
    }
}
