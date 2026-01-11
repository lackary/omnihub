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
    val height: Int,
    val userProfileImage: String? = null,
    val username: String? = null,
    val name: String? = null
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
    val photosState: AppUiState<List<UserPhoto>> = AppUiState.Idle,
    val collectionsState: AppUiState<List<GalleryCollection>> = AppUiState.Idle,
    val likesState: AppUiState<List<UserPhoto>> = AppUiState.Idle
)

// UI Intents
sealed interface UserDetailIntent {
    data class LoadData(val username: String) : UserDetailIntent
    data class SelectTab(val tab: UserTab) : UserDetailIntent
    data object LoadMore : UserDetailIntent
    data object Refresh : UserDetailIntent
}
