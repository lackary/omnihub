package io.lackstudio.omnihub.ui.gallery

import androidx.compose.runtime.Immutable

// Define simple Data Models (to be replaced by real Models later)
data class GalleryPhoto(val id: String, val url: String, val title: String)
data class GalleryCollection(val id: String, val coverUrl: String, val title: String, val totalPhotos: Int)
data class GalleryTopic(val id: String, val coverUrl: String, val title: String, val description: String)

// Define UI State (Nested structure)
@Immutable
data class GalleryUiState(
    // Global state
    val currentTabIndex: Int = 0,

    // Sub-state: Photo list
    val photosState: PhotosState = PhotosState(),

    // Sub-state: Collection list
    val collectionsState: CollectionsState = CollectionsState(),

    // Sub-state: Topic list
    val topicsState: TopicsState = TopicsState()
)

@Immutable
data class PhotosState(
    val isLoading: Boolean = false,
    val items: List<GalleryPhoto> = emptyList(),
    val error: String? = null
)

@Immutable
data class CollectionsState(
    val isLoading: Boolean = false,
    val items: List<GalleryCollection> = emptyList(),
    val error: String? = null
)

@Immutable
data class TopicsState(
    val isLoading: Boolean = false,
    val items: List<GalleryTopic> = emptyList(),
    val error: String? = null
)

// Define Intents (User intents)
sealed interface GalleryIntent {
    data class SelectTab(val index: Int) : GalleryIntent
    data object Refresh : GalleryIntent
    // data object OnBackClick : GalleryIntent (if navigation logic is handled in VM)
}
