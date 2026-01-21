package io.lackstudio.omnihub.compose.auth

interface AuthManager {
    fun getRedirectUrl(): String
    fun startLogin(authUrl: String)
}
