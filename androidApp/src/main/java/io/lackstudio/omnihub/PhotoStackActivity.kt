package io.lackstudio.omnihub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import io.lackstudio.omnihub.compose.platform.PhotoStackEntry

class PhotoStackActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val photoId = intent.getStringExtra("PHOTO_ID")
        setContent {
            photoId?.let {
                PhotoStackEntry(it)
            }
        }
    }
}
