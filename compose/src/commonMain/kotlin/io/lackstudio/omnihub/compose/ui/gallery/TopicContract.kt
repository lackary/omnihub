package io.lackstudio.omnihub.compose.ui.gallery

import io.lackstudio.omnifeed.ui.state.AppUiState

// Detailed information of a Topic
data class Topic(
    val id: String,
    val title: String,
    val description: String?,
    // Added: List of contributors (stores only the top five)
    val contributors: List<TopicContributor>
)

// Data structure for a contributor
data class TopicContributor(
    val username: String,
    val name: String,
    val avatarUrl: String?
)

// UI State
data class TopicDetailUiState(
    val infoState: AppUiState<Topic> = AppUiState.Idle,
    val photosState: AppUiState<List<GalleryDisplayable>> = AppUiState.Idle,

    val isRefreshing: Boolean = false,
    val isPhotosEndOfList: Boolean = false,
    val photosAppendError: String? = null,
    val photosPage: Int = 1
)

// UI Intents
sealed interface TopicDetailIntent {
    data class LoadData(val topicId: String) : TopicDetailIntent
    data object LoadMorePhotos : TopicDetailIntent
    data object Refresh : TopicDetailIntent
}
