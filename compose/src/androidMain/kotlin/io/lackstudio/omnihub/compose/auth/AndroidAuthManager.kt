package io.lackstudio.omnihub.compose.auth

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import io.lackstudio.omnihub.compose.utils.Environment

class AndroidAuthManager(private val context: Context) : AuthManager {

    override fun getRedirectUrl(): String {
        return Environment.AUTH_LOCAL_REDIRECT_URL
    }

    override fun startLogin(authUrl: String) {
        // Android/iOS must use Deep Link Scheme
        // Create a standard ACTION_VIEW Intent
        val intent = Intent(Intent.ACTION_VIEW, authUrl.toUri())
        // Since it's started from a non-Activity context (Application Context), this flag must be added
        @SuppressLint("WrongConstant")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        
        // Open the device's default browser
        context.startActivity(intent)
    }
}
