package io.lackstudio.omnihub.auth

interface AuthManager {
    fun getRedirectUrl(): String
    fun startLogin(authUrl: String)
}
