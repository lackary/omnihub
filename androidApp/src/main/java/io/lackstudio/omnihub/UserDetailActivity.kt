package io.lackstudio.omnihub

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.lackstudio.omnihub.compose.platform.UserDetailEntry
import kotlinx.coroutines.flow.MutableStateFlow

class UserDetailActivity : ComponentActivity() {

    private val usernameFlow = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        parseIntent(intent)

        setContent {
            val username by usernameFlow.collectAsState()
            username?.let {
                UserDetailEntry(it)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        parseIntent(intent)
    }

    private fun parseIntent(intent: Intent) {
        val username = intent.getStringExtra("USERNAME")
        if (username != null) {
            usernameFlow.value = username
        }
    }
}
