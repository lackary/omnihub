package io.lackstudio.omnihub.ui.gallery

import io.lackstudio.omnifeed.ui.state.AppUiState
import io.lackstudio.omnifeed.ui.viewmodel.BaseViewModel
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetTopicPhotosParams
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetTopicPhotosUseCase
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetTopicUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TopicViewModel(
    private val getTopicUseCase: GetTopicUseCase,
    private val getTopicPhotosUseCase: GetTopicPhotosUseCase
) : BaseViewModel() {

    private val _state = MutableStateFlow(TopicDetailUiState())
    val state = _state.asStateFlow()

    private var currentTopicId: String? = null

    fun handleIntent(intent: TopicDetailIntent) {
        when (intent) {
            is TopicDetailIntent.LoadData -> {
                if (currentTopicId != intent.topicId) {
                    currentTopicId = intent.topicId
                    loadTopicInfo(intent.topicId)
                    loadTopicPhotos(intent.topicId, isRefresh = true)
                }
            }
            is TopicDetailIntent.Refresh -> {
                currentTopicId?.let { id ->
                    loadTopicInfo(id)
                    loadTopicPhotos(id, isRefresh = true)
                }
            }
            is TopicDetailIntent.LoadMorePhotos -> {
                currentTopicId?.let { id ->
                    loadTopicPhotos(id, isRefresh = false)
                }
            }
        }
    }

    private fun loadTopicInfo(id: String) {
        handleUseCaseCall(
            useCase = { getTopicUseCase(id) },
            onLoading = { _state.update { it.copy(infoState = AppUiState.Loading) } },
            onSuccess = { domainTopic ->
                // Convert Domain Model -> UI Model
                val uiTopic = Topic(
                    id = domainTopic.id,
                    title = domainTopic.title,
                    description = domainTopic.description,
                    // Process contributors: take the top 5 and convert the format
                    contributors = domainTopic.topContributors?.take(5)?.map { user ->
                        TopicContributor(
                            username = user.username,
                            name = user.name,
                            avatarUrl = user.profileImage.medium
                        )
                    } ?: emptyList()
                )
                _state.update { it.copy(infoState = AppUiState.Success(uiTopic)) }
            },
            onError = { msg -> _state.update { it.copy(infoState = AppUiState.Error(msg)) } }
        )
    }

    private fun loadTopicPhotos(id: String, isRefresh: Boolean) {
        val currentState = _state.value
        if (!isRefresh && (currentState.isPhotosLoadingMore || currentState.isPhotosEndOfList)) return

        val page = if (isRefresh) 1 else currentState.photosPage + 1
        val topicPhotoParams = GetTopicPhotosParams(id, page = page, perPage = 10)

        handleUseCaseCall(
            useCase = { getTopicPhotosUseCase(topicPhotoParams) },
            onLoading = {
                if (isRefresh) _state.update { it.copy(photosState = AppUiState.Loading) }
                else _state.update { it.copy(isPhotosLoadingMore = true) }
            },
            onSuccess = { domainPhotos ->
                val newPhotos = domainPhotos.map { photo ->
                    TopicPhoto(
                        id = photo.id,
                        url = photo.urls.small,
                        title = photo.description ?: photo.altDescription,
                        userProfileImage = photo.user.profileImage.medium,
                        username = photo.user.username,
                        name = photo.user.name,
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
