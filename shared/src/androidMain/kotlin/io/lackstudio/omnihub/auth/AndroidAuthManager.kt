package io.lackstudio.omnihub.auth

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.core.net.toUri
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import io.lackstudio.omnihub.shared.BuildKonfig
import io.lackstudio.omnihub.utils.logging.AppLog
import io.lackstudio.omnihub.utils.Environment

import java.util.UUID

class AndroidAuthManager(private val context: Context) : AuthManager {

    private val logger = AppLog.withTag("AndroidAuthManager")
    private val credentialManager = CredentialManager.create(context)

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

    override suspend fun signInWithGoogle(context: Any?): GoogleAuthTokens? {
        val activityContext = (context as? Context)?.findActivity() ?: return null
        
        val nonce = UUID.randomUUID().toString()

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildKonfig.GOOGLE_SERVER_CLIENT_ID)
            .setNonce(nonce)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            logger.i { "Launching Credential Manager..." }
            val result = credentialManager.getCredential(
                context = activityContext,
                request = request
            )
            val credential = result.credential
            
            // Use static method of GoogleIdTokenCredential to parse from CustomCredential
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                logger.i { "Successfully parsed GoogleIdTokenCredential" }
                GoogleAuthTokens(idToken = googleIdTokenCredential.idToken)
            } catch (e: Exception) {
                logger.e(throwable = e) { "Failed to parse GoogleIdTokenCredential: ${e.message}" }
                // Extra log for type to assist in debugging
                if (credential is androidx.credentials.CustomCredential) {
                    logger.d { "CustomCredential type: ${credential.type}" }
                }
                null
            }
        } catch (e: Exception) {
            logger.e(throwable = e) { "Error during getCredential: ${e.message}" }
            e.printStackTrace()
            null
        }
    }

    private fun Context.findActivity(): Activity? {
        var context = this
        while (context is ContextWrapper) {
            if (context is Activity) return context
            context = context.baseContext
        }
        return null
    }
}
