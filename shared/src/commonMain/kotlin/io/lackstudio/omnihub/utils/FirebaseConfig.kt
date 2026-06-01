package io.lackstudio.omnihub.utils

import io.lackstudio.omnihub.shared.BuildKonfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private val json = Json { ignoreUnknownKeys = true }

@Serializable
data class GoogleServiceWeb(
    @SerialName("api_key")
    val apiKey: String,
    @SerialName("auth_domain")
    val authDomain: String,
    @SerialName("project_id")
    val projectId: String,
    @SerialName("storage_bucket")
    val storageBucket: String,
    @SerialName("messaging_sender_id")
    val messagingSenderId: String,
    @SerialName("app_id")
    val appId: String,
    @SerialName("measurement_id")
    val measurementId: String
)

@OptIn(ExperimentalEncodingApi::class)
fun getFirebaseWebConfig(): GoogleServiceWeb? {
    val base64Config = BuildKonfig.FIREBASE_WEB_BASE64

    if (base64Config.isEmpty()) return null

    return try {
        val decodedBytes = Base64.decode(base64Config)
        val jsonString = decodedBytes.decodeToString()
        json.decodeFromString<GoogleServiceWeb>(jsonString)
    } catch (e: Exception) {
        null
    }
}
