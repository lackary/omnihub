package io.lackstudio.omnihub.auth

data class GoogleAuthTokens(
    val idToken: String,
    val accessToken: String? = null
)

interface AuthManager {
    fun getRedirectUrl(): String
    fun startLogin(authUrl: String)
    suspend fun signInWithGoogle(context: Any? = null): GoogleAuthTokens?
}
