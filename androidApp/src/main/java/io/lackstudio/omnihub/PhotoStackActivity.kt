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

        // 處理初始 Intent
        parseIntent(intent)

        setContent {

            // 🚀 觀察狀態流，當有新 Intent 進來時，Compose 會自動重組
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
    }

    private fun parseIntent(intent: Intent) {
        val navData = PhotoNavData.fromIntent(intent)
        if (navData != null) {
            navDataFlow.value = navData
        }
    }
}
