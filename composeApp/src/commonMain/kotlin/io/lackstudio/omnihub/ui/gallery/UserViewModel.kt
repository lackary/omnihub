package io.lackstudio.omnihub.ui.gallery

import io.lackstudio.omnifeed.ui.state.AppUiState
import io.lackstudio.omnifeed.ui.viewmodel.BaseViewModel
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetUserPhotosParams
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetUserPhotosUseCase
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetUserPublicProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class UserViewModel(
    private val getUserPublicProfileUseCase: GetUserPublicProfileUseCase,
    private val getUserPhotosUseCase: GetUserPhotosUseCase
) : BaseViewModel() {

    private val _state = MutableStateFlow(UserDetailUiState())
    val state = _state.asStateFlow()

    private var currentUsername: String? = null

    fun handleIntent(intent: UserDetailIntent) {
        when (intent) {
            is UserDetailIntent.LoadData -> {
                if (currentUsername != intent.username) {
                    currentUsername = intent.username
                    loadUserInfo(intent.username)
                    loadUserPhotos(intent.username, isRefresh = true)
                }
            }
            is UserDetailIntent.Refresh -> {
                currentUsername?.let { username ->
                    loadUserInfo(username)
                    loadUserPhotos(username, isRefresh = true)
                }
            }
            is UserDetailIntent.LoadMorePhotos -> {
                currentUsername?.let { username ->
                    loadUserPhotos(username, isRefresh = false)
                }
            }
        }
    }

    private fun loadUserInfo(username: String) {
        handleUseCaseCall(
            useCase = { getUserPublicProfileUseCase(username) },
            onLoading = { _state.update { it.copy(infoState = AppUiState.Loading) } },
            onSuccess = { domainUser ->
                val uiProfile = UserProfile(
                    id = domainUser.id,
                    username = domainUser.username,
                    name = domainUser.name,
                    avatarUrl = domainUser.profileImage.large, // Use large image for better clarity
                    bio = domainUser.bio,
                    location = domainUser.location,
                    totalPhotos = domainUser.totalPhotos,
                    totalCollections = domainUser.totalCollections,
                    totalLikes = domainUser.totalLikes,
                    portfolioUrl = domainUser.portfolioUrl,
                    instagramUsername = domainUser.instagramUsername,
                    twitterUsername = domainUser.twitterUsername
                )
                _state.update { it.copy(infoState = AppUiState.Success(uiProfile)) }
            },
            onError = { msg -> _state.update { it.copy(infoState = AppUiState.Error(msg)) } }
        )
    }

    private fun loadUserPhotos(username: String, isRefresh: Boolean) {
        val currentState = _state.value
        if (!isRefresh && (currentState.isPhotosLoadingMore || currentState.isPhotosEndOfList)) return

        val page = if (isRefresh) 1 else currentState.photosPage + 1
        val userPhotosParams = GetUserPhotosParams(username, page = page, perPage = 10)

        handleUseCaseCall(
            useCase = { getUserPhotosUseCase(userPhotosParams) }, // Note: Ensure UseCase signature supports these parameters
            onLoading = {
                if (isRefresh) _state.update { it.copy(photosState = AppUiState.Loading) }
                else _state.update { it.copy(isPhotosLoadingMore = true) }
            },
            onSuccess = { domainPhotos ->
                val newPhotos = domainPhotos.map { photo ->
                    UserPhoto(
                        id = photo.id,
                        url = photo.urls.small,
                        title = photo.description ?: photo.altDescription,
                        likes = photo.likes,
                        blurhash = photo.blurHash,
                        width = photo.width,
                        height = photo.height
                    )
                }

                _state.update { oldState ->
                    val combinedList = if (isRefresh) newPhotos else {
                        (oldState.photosState as? AppUiState.Success)?.data.orEmpty() + newPhotos
                    }
                    oldState.copy(
                        photosState = AppUiState.Success(combinedList),
                        photosPage = page,
                        isPhotosLoadingMore = false,
                        isPhotosEndOfList = newPhotos.isEmpty()
                    )
                }
            },
            onError = { msg ->
                if (isRefresh) _state.update { it.copy(photosState = AppUiState.Error(msg)) }
                else _state.update { it.copy(isPhotosLoadingMore = false) }
            }
        )
    }
}
