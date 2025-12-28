package io.lackstudio.omnihub.ui.gallery

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Topic
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import io.lackstudio.omnifeed.ui.state.AppUiState

// Define simple Data Models
data class GalleryPhoto(val id: String, val url: String, val title: String)
data class GalleryCollection(val id: String, val coverUrl: String?, val title: String, val totalPhotos: Int)
data class GalleryTopic(val id: String, val coverUrl: String?, val title: String, val description: String)

// 🆕 1. 定義帶有屬性的 Enum
enum class GalleryTab(
    val title: String,
    val icon: ImageVector
) {
    Photos("Photos", Icons.Filled.PhotoLibrary),
    Collections("Collections", Icons.Filled.PhotoAlbum),
    Topics("Topics", Icons.Filled.Topic);

    // Helper: 透過 index 找 Enum (給 Pager 用)
    companion object {
        fun getByIndex(index: Int): GalleryTab = entries.getOrElse(index) { Photos }
    }
}

// Define State (UI state)
@Immutable
data class GalleryUiState(
    // Global state
    val currentTab: GalleryTab = GalleryTab.Photos,

    val isRefreshing: Boolean = false,

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
