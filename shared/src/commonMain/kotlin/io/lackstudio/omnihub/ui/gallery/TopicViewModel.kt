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
    private var internalLoading = false

    fun handleIntent(intent: TopicDetailIntent) {
        when (intent) {
            is TopicDetailIntent.LoadData -> {
                logger.d { "handleIntent: LoadData id=${intent.topicId}" }
                if (currentTopicId != intent.topicId) {
                    currentTopicId = intent.topicId
                    loadTopicInfo(intent.topicId)
                    loadTopicPhotos(intent.topicId, isRefresh = true)
                }
            }
            is TopicDetailIntent.Refresh -> {
                logger.d { "handleIntent: Refresh" }
                _state.update { it.copy(isRefreshing = true) }
                currentTopicId?.let { id ->
                    loadTopicInfo(id)
                    loadTopicPhotos(id, isRefresh = true)
                }
            }
            is TopicDetailIntent.LoadMorePhotos -> {
                logger.d { "handleIntent: LoadMorePhotos" }
                val currentState = _state.value

                if (internalLoading) return
                if (currentState.isRefreshing) return
                if (currentState.isPhotosEndOfList) return
                if (currentState.photosAppendError != null) return
                currentTopicId?.let { id ->
                    loadTopicPhotos(id, isRefresh = false)
                }
            }
        }
    }

    private fun loadTopicInfo(id: String) {
        handleUseCaseCall(
            name = "Topic",
            useCase = { getTopicUseCase(id) },
            onLoading = {
                _state.update { currentState ->
                    if (currentState.infoState is AppUiState.Success) currentState
                    else currentState.copy(infoState = AppUiState.Loading)
                }
            },
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
            onError = { msg ->
                logger.e { "Error loading topic info: $msg" }
                _state.update { it.copy(infoState = AppUiState.Error(msg)) }
            }
        )
    }

    private fun loadTopicPhotos(id: String, isRefresh: Boolean) {
        val currentState = _state.value
        val page = if (isRefresh) 1 else currentState.photosPage + 1

        if (page > 1) internalLoading = true

        val topicPhotoParams = GetTopicPhotosParams(id, page = page, perPage = 10)

        handleUseCaseCall(
            name = "Topic Photos",
            useCase = { getTopicPhotosUseCase(topicPhotoParams) },
            onLoading = {
                _state.update { state ->
                    val oldList = (state.photosState as? AppUiState.Success)?.data
                    if (!oldList.isNullOrEmpty()) {
                        state
                    } else {
                        state.copy(photosState = AppUiState.Loading)
                    }
                }
            },
            onSuccess = { domainPhotos ->
                val newPhotos = domainPhotos.map { photo ->
                    GalleryPhoto(
                        id = photo.id,
                        url = photo.urls.small,
                        title = photo.description?: "",
                        userProfileImage = photo.user.profileImage.medium,
                        username = photo.user.username,
                        name = photo.user.name,
                        likes = photo.likes,
                        blurhash = photo.blurHash,
                        width = photo.width,
                        height = photo.height
                    )
                }

                _state.update { currentState ->
                    val oldList =
                        if (isRefresh) emptyList()
                        else (currentState.photosState as? AppUiState.Success)?.data ?: emptyList()

                    val combinedList = (oldList + newPhotos).distinctBy { it.displayId }

                    currentState.copy(
                        photosState = AppUiState.Success(combinedList),
                        photosPage = page,
                        isRefreshing = false,
                        photosAppendError = null,
                        isPhotosEndOfList = newPhotos.isEmpty()
                    )
                }

                if (page > 1) internalLoading = false
            },
            onError = { msg ->
                logger.e { "Error loading topic photos: $msg" }
                _state.update { currentState ->
                    val oldList = (currentState.photosState as? AppUiState.Success)?.data
                    if (!oldList.isNullOrEmpty()) {
                        currentState.copy(
                            photosAppendError = msg,
                            isRefreshing = false
                        )
                    } else {
                        currentState.copy(
                            photosState = AppUiState.Error(msg),
                            photosAppendError = null,
                            isRefreshing = false
                        )
                    }
                }

                if (page > 1) internalLoading = false
            }
        )
    }
}
