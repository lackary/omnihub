package io.lackstudio.omnihub

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.lackstudio.omnihub.auth.DeepLinkBuffer
import io.lackstudio.omnihub.platform.AppEntry
import io.lackstudio.omnihub.ui.App

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        setContent {
            AppEntry()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?)  {
        val data = intent?.data
        if (data != null && data.scheme == "omnihub") {
            // Convert Android URI to string and pass it to the KMP common layer
            DeepLinkBuffer.setDeepLink(data.toString())
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
