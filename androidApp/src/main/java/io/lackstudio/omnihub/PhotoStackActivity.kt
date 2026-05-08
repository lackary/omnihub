package io.lackstudio.omnihub

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.lackstudio.omnihub.compose.platform.PhotoStackEntry
import io.lackstudio.omnihub.compose.ui.navigation.models.PhotoNavData
import kotlinx.coroutines.flow.MutableStateFlow

class PhotoStackActivity : ComponentActivity() {

    private val navDataFlow = MutableStateFlow<PhotoNavData?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle initial Intent
        parseIntent(intent)

        setContent {

            // 🚀 Observe the state flow; when a new Intent arrives, Compose will automatically recompose
            val navData by navDataFlow.collectAsState()

            navData?.let { data ->
                // 直接傳入整個物件，保持 Entry 層級的簡潔
                PhotoStackEntry(navData = data)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseIntent(intent)
    }

    private fun parseIntent(intent: Intent) {
        val navData = PhotoNavData.fromIntent(intent)
        println("PhotoStackActivity photoId: ${navData?.photoId} ")
        if (navData != null) {
            navDataFlow.value = navData
        }
    }
}
