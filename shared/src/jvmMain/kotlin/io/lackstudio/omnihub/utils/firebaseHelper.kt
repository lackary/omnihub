package io.lackstudio.omnihub.utils

import io.lackstudio.omnihub.shared.BuildKonfig
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

private val json = Json { ignoreUnknownKeys = true }

fun getFirebaseWebConfig(): GoogleServiceWeb? {
    val base64Config = BuildKonfig.FIREBASE_WEB_BASE64

    if (base64Config.isEmpty()) return null

    val decodedBytes = Base64.decode(base64Config)
    val jsonString = decodedBytes.decodeToString()

    val firebaseWebConfig = json.decodeFromString<GoogleServiceWeb>(jsonString)

    return firebaseWebConfig
}
