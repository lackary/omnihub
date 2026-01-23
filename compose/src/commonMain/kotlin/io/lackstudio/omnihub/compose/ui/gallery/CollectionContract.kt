package io.lackstudio.omnihub.compose.ui.gallery

import io.lackstudio.omnifeed.ui.state.AppUiState

data class Collection(
    val id: String,
    val title: String,
    val username: String,
    val name: String,
    val avatarUrl: String?,
    val description: String?,
    val totalPhotos: Int
)

// Define UI State
data class CollectionDetailUiState(
    // Section 1: Detailed information of the Collection itself (Title, Description, User...)
    val infoState: AppUiState<Collection> = AppUiState.Idle,

    // Section 2: List of photos in this Collection
    val photosState: AppUiState<List<GalleryDisplayable>> = AppUiState.Idle,

    // List pagination control
    val isPhotosLoadingMore: Boolean = false,
    val isPhotosEndOfList: Boolean = false,
    val photosPage: Int = 1
)

// Define UI Intents
sealed interface CollectionDetailIntent {
    // Load data when entering the page
    data class LoadData(val collectionId: String) : CollectionDetailIntent

    // Load more when list is scrolled to the bottom
    data object LoadMorePhotos : CollectionDetailIntent

    // Pull to refresh
    data object Refresh : CollectionDetailIntent
}
