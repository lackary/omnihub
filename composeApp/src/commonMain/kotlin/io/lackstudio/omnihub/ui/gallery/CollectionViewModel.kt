package io.lackstudio.omnihub.ui.gallery

import io.lackstudio.omnifeed.ui.state.AppUiState
import io.lackstudio.omnifeed.ui.viewmodel.BaseViewModel
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetCollectionPhotosParams
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetCollectionPhotosUseCase
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetCollectionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CollectionViewModel(
    private val getCollectionUseCase: GetCollectionUseCase,
    private val getCollectionPhotosUseCase: GetCollectionPhotosUseCase
) : BaseViewModel() {

    private val _state = MutableStateFlow(CollectionDetailUiState())
    val state = _state.asStateFlow()

    private var currentCollectionId: String? = null

    fun handleIntent(intent: CollectionDetailIntent) {
        when (intent) {
            is CollectionDetailIntent.LoadData -> {
                // Only execute when ID is different or upon first load
                if (currentCollectionId != intent.collectionId) {
                    currentCollectionId = intent.collectionId
                    // Trigger both in parallel
                    loadCollectionInfo(intent.collectionId)
                    loadCollectionPhotos(intent.collectionId, isRefresh = true)
                }
            }
            is CollectionDetailIntent.Refresh -> {
                currentCollectionId?.let { id ->
                    loadCollectionInfo(id)
                    loadCollectionPhotos(id, isRefresh = true)
                }
            }
            is CollectionDetailIntent.LoadMorePhotos -> {
                currentCollectionId?.let { id ->
                    loadCollectionPhotos(id, isRefresh = false)
                }
            }
        }
    }

    // --- Part 1: Collection Info ---
    private fun loadCollectionInfo(id: String) {
        handleUseCaseCall(
            useCase = { getCollectionUseCase(id) },
            onLoading = {
                // Set Info state to Loading
                _state.update { it.copy(infoState = AppUiState.Loading) }
            },
            onSuccess = { domainCollection ->
                // Convert Domain Model to UI Model (Collection) here
                val uiCollection = Collection(
                    id = domainCollection.id,
                    title = domainCollection.title,
                    username = domainCollection.user.username,
                    name = domainCollection.user.name,
                    avatarUrl = domainCollection.user.profileImage.medium,
                    description = domainCollection.description,
                    totalPhotos = domainCollection.totalPhotos
                )
                // Set Info state to Success
                _state.update { it.copy(infoState = AppUiState.Success(uiCollection)) }
            },
            onError = { msg ->
                _state.update { it.copy(infoState = AppUiState.Error(msg)) }
            }
        )
    }

    // --- Part 2: Collection Photos (Pagination) ---
    private fun loadCollectionPhotos(id: String, isRefresh: Boolean) {
        val currentState = _state.value

        // Guard: If LoadMore and currently loading or reached end of list, do not execute
        if (!isRefresh && (currentState.isPhotosLoadingMore || currentState.isPhotosEndOfList)) {
            return
        }

        val page = if (isRefresh) 1 else currentState.photosPage + 1
        val params = GetCollectionPhotosParams(id = id, page = page, perPage = 10)
        handleUseCaseCall(
            useCase = { getCollectionPhotosUseCase(params) },
            onLoading = {
                if (isRefresh) {
                    // Pull-to-refresh or first load: Show Loading for the whole list
                    _state.update { it.copy(photosState = AppUiState.Loading) }
                } else {
                    // Load more: Keep list displayed, only show bottom Loading
                    _state.update { it.copy(isPhotosLoadingMore = true) }
                }
            },
            onSuccess = { domainPhotos ->
                // Convert Domain Model to UI Model (CollectionPhoto)
                val newPhotos = domainPhotos.map { photo ->
                    CollectionPhoto(
                        id = photo.id,
                        url = photo.urls.small, // Lists usually use small or regular
                        title = photo.description,
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
                    // Logic: If Refresh, use new data directly; if LoadMore, append new data to old data
                    val combinedPhotos = if (isRefresh) {
                        newPhotos
                    } else {
                        val currentList = (oldState.photosState as? AppUiState.Success)?.data ?: emptyList()
                        currentList + newPhotos
                    }

                    oldState.copy(
                        photosState = AppUiState.Success(combinedPhotos),
                        photosPage = page,
                        isPhotosLoadingMore = false,
                        // If returned data is empty, the end of the list has been reached
                        isPhotosEndOfList = newPhotos.isEmpty()
                    )
                }
            },
            onError = { msg ->
                if (isRefresh) {
                    _state.update { it.copy(photosState = AppUiState.Error(msg)) }
                } else {
                    // LoadMore failed, just turn off Loading state, keep the original list
                    // (Advanced: use SideEffect to show Snackbar error)
                    _state.update { it.copy(isPhotosLoadingMore = false) }
                }
            }
        )
    }
}
