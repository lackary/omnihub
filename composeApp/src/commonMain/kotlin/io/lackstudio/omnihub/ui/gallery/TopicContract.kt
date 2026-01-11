package io.lackstudio.omnihub.ui.gallery

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

// Photos within a Topic
data class TopicPhoto(
    val id: String,
    val url: String,
    val title: String?,
    val userProfileImage: String?,
    val username: String,
    val name: String,
    val likes: Int,
    val blurhash: String?,
    val width: Int,
    val height: Int
)

// UI State
data class TopicDetailUiState(
    val infoState: AppUiState<Topic> = AppUiState.Idle,
    val photosState: AppUiState<List<GalleryPhoto>> = AppUiState.Idle,
    val isPhotosLoadingMore: Boolean = false,
    val isPhotosEndOfList: Boolean = false,
    val photosPage: Int = 1
)

// UI Intents
sealed interface TopicDetailIntent {
    data class LoadData(val topicId: String) : TopicDetailIntent
    data object LoadMorePhotos : TopicDetailIntent
    data object Refresh : TopicDetailIntent
}
