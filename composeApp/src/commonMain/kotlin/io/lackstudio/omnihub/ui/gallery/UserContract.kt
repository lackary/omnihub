package io.lackstudio.omnihub.ui.gallery

import io.lackstudio.omnifeed.ui.state.AppUiState

// User profile information (for Header)
data class UserProfile(
    val id: String,
    val username: String,
    val name: String,
    val avatarUrl: String,
    val bio: String?,
    val location: String?,
    // Statistics
    val totalPhotos: Long,
    val totalCollections: Long,
    val totalLikes: Long,
    val portfolioUrl: String? = null,
    val instagramUsername: String? = null,
    val twitterUsername: String? = null
)

// User photo list (for List)
data class UserPhoto(
    val id: String,
    val url: String,
    val title: String?,
    val likes: Int,
    val blurhash: String?,
    val width: Int,
    val height: Int
)

// UI State
data class UserDetailUiState(
    val infoState: AppUiState<UserProfile> = AppUiState.Idle,
    val photosState: AppUiState<List<UserPhoto>> = AppUiState.Idle,
    val isPhotosLoadingMore: Boolean = false,
    val isPhotosEndOfList: Boolean = false,
    val photosPage: Int = 1
)

// UI Intents
sealed interface UserDetailIntent {
    data class LoadData(val username: String) : UserDetailIntent
    data object LoadMorePhotos : UserDetailIntent
    data object Refresh : UserDetailIntent
}
