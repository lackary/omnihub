package io.lackstudio.omnihub.utils

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    val messagingSenderId:String,
    @SerialName("app_id")
    val appId: String,
    @SerialName("measurement_id")
    val measurementId: String
)
