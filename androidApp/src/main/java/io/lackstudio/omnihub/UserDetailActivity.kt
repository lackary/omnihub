package io.lackstudio.omnihub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.lackstudio.omnihub.compose.layout.XrUserDetailLayout
import io.lackstudio.omnihub.compose.platform.UserDetailEntry

class UserDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val username = intent.getStringExtra("USERNAME")
        setContent {
            username?.let {
                UserDetailEntry(username)
            }
        }
    }
}
