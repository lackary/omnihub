package io.lackstudio.omnihub.compose.ui.gallery

import io.lackstudio.omnifeed.ui.state.AppUiState

// User profile information (for Header)
data class UserProfile(
    val id: String,
    val username: String,
    val name: String,
    val avatarUrl: String?,
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

enum class UserTab(val title: String) {
    Photos("Photos"),
    Collections("Collections"),
    Likes("Likes");

    companion object {
        fun getByIndex(index: Int): UserTab = entries.getOrElse(index) { Photos }
    }
}

// UI State
data class UserDetailUiState(
    val currentTab: UserTab = UserTab.Photos,

    val loadingMoreStatus: Map<UserTab, Boolean> = emptyMap(),
    val endOfListStatus: Map<UserTab, Boolean> = emptyMap(),
    val pages: Map<UserTab, Int> = emptyMap(),

    val infoState: AppUiState<UserProfile> = AppUiState.Idle,
    val photosState: AppUiState<List<GalleryDisplayable>> = AppUiState.Idle,
    val collectionsState: AppUiState<List<GalleryDisplayable>> = AppUiState.Idle,
    val likesState: AppUiState<List<GalleryDisplayable>> = AppUiState.Idle
)

// UI Intents
sealed interface UserDetailIntent {
    data class LoadData(val username: String) : UserDetailIntent
    data class SelectTab(val tab: UserTab) : UserDetailIntent
    data object LoadMore : UserDetailIntent
    data object Refresh : UserDetailIntent
}
