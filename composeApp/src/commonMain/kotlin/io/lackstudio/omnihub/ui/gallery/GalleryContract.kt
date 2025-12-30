package io.lackstudio.omnihub.ui.gallery

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Topic
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import io.lackstudio.omnifeed.ui.state.AppUiState

// Define display interface
// All objects that want to be displayed with GalleryCard must implement this interface
interface GalleryDisplayable {
    val displayId: String
    val displayImageUrl: String?
    val displayTitle: String
    val displayUserAvatar: String? // Allow null (because Collections or Topic might not have a User)
    val displayUsername: String?  // Allow null
    val displayCount: Int
    val displayLikes: Int
    val displayBlurHash: String?
    val displayWidth: Int?
    val displayHeight: Int?
}

data class GalleryPhoto(
    val id: String,
    val url: String,
    val title: String,
    val userProfileImage: String?,
    val username: String,
    val likes: Int,
    val blurhash: String,
    val width: Int,
    val height: Int
) : GalleryDisplayable {
    // Implement interface field mapping
    override val displayId: String get() = id
    override val displayImageUrl: String get() = url
    override val displayTitle: String get() = title
    override val displayCount: Int get() = 0
    override val displayUserAvatar: String? get() = userProfileImage
    override val displayUsername: String get() = username
    override val displayLikes: Int get() = likes
    override val displayBlurHash: String get() = blurhash
    override val displayWidth: Int get() = width
    override val displayHeight: Int get() = height
}

data class GalleryCollection(
    val id: String,
    val coverUrl: String?,
    val title: String,
    val totalPhotos: Int,
    val userProfileImage: String? = null,
    val username: String? = null,
    val blurhash: String? = null,
    val width: Int? = 0,
    val height: Int? = 0
) : GalleryDisplayable {
    override val displayId: String get() = id
    override val displayImageUrl: String? get() = coverUrl
    override val displayTitle: String get() = title
    override val displayCount: Int get() = totalPhotos
    override val displayUserAvatar: String? get() = userProfileImage
    override val displayUsername: String? get() = username
    override val displayLikes: Int get() = 0
    override val displayBlurHash: String? get() = blurhash
    override val displayWidth: Int? get() = width
    override val displayHeight: Int? get() = height
}

data class GalleryTopic(
    val id: String,
    val coverUrl: String?,
    val title: String,
    val description: String,
    val totalPhotos: Int,
    val blurhash: String? = null,
    val width: Int? = 0,
    val height: Int? = 0
) : GalleryDisplayable {
    override val displayId: String get() = id
    override val displayImageUrl: String? get() = coverUrl
    override val displayTitle: String get() = title
    override val displayCount: Int get() = totalPhotos
    override val displayUserAvatar: String? get() = null
    override val displayUsername: String? get() = null
    override val displayLikes: Int get() = 0
    override val displayBlurHash: String? get() = blurhash
    override val displayWidth: Int? get() = width
    override val displayHeight: Int? get() = height
}

// Define Enum with properties
enum class GalleryTab(
    val title: String,
    val icon: ImageVector
) {
    Photos("Photos", Icons.Filled.PhotoLibrary),
    Collections("Collections", Icons.Filled.PhotoAlbum),
    Topics("Topics", Icons.Filled.Topic);

    // Helper: Find Enum by index (for Pager)
    companion object {
        fun getByIndex(index: Int): GalleryTab = entries.getOrElse(index) { Photos }
    }
}

// Define State (UI state)
@Immutable
data class GalleryUiState(
    // Global state
    val currentTab: GalleryTab = GalleryTab.Photos,

    // Use Map to track the refreshing status of each Tab
    val refreshingStatus: Map<GalleryTab, Boolean> = emptyMap(),

    val photosState: AppUiState<List<GalleryPhoto>> = AppUiState.Idle,
    val photosEndOfList: Boolean = false,

    val collectionsState: AppUiState<List<GalleryCollection>> = AppUiState.Idle,
    val collectionsEndOfList: Boolean = false,

    val topicsState: AppUiState<List<GalleryTopic>> = AppUiState.Idle,
    val topicsEndOfList: Boolean = false
)

// Define Intents (User intents)
sealed interface GalleryIntent {
    data class SelectTab(val tab: GalleryTab) : GalleryIntent
    data object Refresh : GalleryIntent
    data object LoadMore : GalleryIntent
}

// Define One-time Events (Side Effect)
sealed interface GallerySideEffect {
    data class ShowSnackbar(val message: String) : GallerySideEffect
}
