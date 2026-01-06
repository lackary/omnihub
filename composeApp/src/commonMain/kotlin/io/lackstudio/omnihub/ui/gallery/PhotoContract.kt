package io.lackstudio.omnihub.ui.gallery

import androidx.compose.runtime.Immutable
import io.lackstudio.omnifeed.ui.state.AppUiState

// detail info model (include EXIF and Location)
@Immutable
data class Photo(
    val id: String,
    val fullUrl: String,
    val username: String,
    val userAvatar: String?,
    val description: String?,
    val views: Long = 0,
    val downloads: Long = 0,
    val likes: Int = 0,
    val createdAt: String? = null,
    val exif: PhotoExif?,
    val location: PhotoLocation?
)

@Immutable
data class PhotoExif(
    val make: String? = null,
    val model: String? = null,
    val aperture: String? = null,
    val exposureTime: String? = null,
    val iso: Int? = null,
    val focalLength: String? = null
)

@Immutable
data class PhotoLocation(
    val city: String? = null,
    val country: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    fun displayString(): String? {
        if (city != null && country != null) return "$city, $country"
        return city ?: country
    }
}

// 2. UI State
@Immutable
data class PhotoDetailUiState(
    val detailState: AppUiState<Photo> = AppUiState.Idle
)

// 3. User Intents
sealed interface PhotoDetailIntent {
    data class LoadDetail(val id: String) : PhotoDetailIntent
    data object Retry : PhotoDetailIntent
}
