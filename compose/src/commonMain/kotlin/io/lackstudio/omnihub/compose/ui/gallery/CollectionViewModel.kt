package io.lackstudio.omnihub.compose.ui.gallery

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

    private var internalLoading = false

    fun handleIntent(intent: CollectionDetailIntent) {
        when (intent) {
            is CollectionDetailIntent.LoadData -> {
                logger.d { "handleIntent: LoadData id=${intent.collectionId}" }
                // Only execute when ID is different or upon first load
                if (currentCollectionId != intent.collectionId) {
                    currentCollectionId = intent.collectionId
                    // Trigger both in parallel
                    loadCollectionInfo(intent.collectionId)
                    loadCollectionPhotos(intent.collectionId, isRefresh = true)
                }
            }
            is CollectionDetailIntent.Refresh -> {
                logger.d { "handleIntent: Refresh" }
                _state.update { it.copy(isRefreshing = true) }
                currentCollectionId?.let { id ->
                    loadCollectionInfo(id)
                    loadCollectionPhotos(id, isRefresh = true)
                }
            }
            is CollectionDetailIntent.LoadMorePhotos -> {
                logger.d { "handleIntent: LoadMorePhotos" }
                val currentState = _state.value

                if (internalLoading) return
                if (currentState.isRefreshing) return
                if (currentState.isPhotosEndOfList) return
                if (currentState.photosAppendError != null) return

                currentCollectionId?.let { id ->
                    loadCollectionPhotos(id, isRefresh = false)
                }
            }
        }
    }

    // --- Part 1: Collection Info ---
    private fun loadCollectionInfo(id: String) {
        handleUseCaseCall(
            name = "Collection",
            useCase = { getCollectionUseCase(id) },
            onLoading = {
                // Set Info state to Loading
                _state.update { currentState ->
                    // Anti-flickering: Keep current state instead of reverting to Loading if old data exists
                    if (currentState.infoState is AppUiState.Success) currentState
                    else currentState.copy(infoState = AppUiState.Loading)
                }
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
                logger.e { "Error loading collection info: $msg" }
                _state.update { it.copy(infoState = AppUiState.Error(msg)) }
            }
        )
    }

    // --- Part 2: Collection Photos (Pagination) ---
    private fun loadCollectionPhotos(id: String, isRefresh: Boolean) {
        val currentState = _state.value
        val page = if (isRefresh) 1 else currentState.photosPage + 1

        if (page > 1) internalLoading = true

        val params = GetCollectionPhotosParams(id = id, page = page, perPage = 10)
        handleUseCaseCall(
            name = "Collection Photos",
            useCase = { getCollectionPhotosUseCase(params) },
            onLoading = {
                _state.update { state ->
                    val oldList = (state.photosState as? AppUiState.Success)?.data
                    // If the list has old data, do not show full-screen Loading
                    if (!oldList.isNullOrEmpty()) {
                        state
                    } else {
                        state.copy(photosState = AppUiState.Loading)
                    }
                }
            },
            onSuccess = { domainPhotos ->
                // Convert Domain Model to UI Model (CollectionPhoto)
                val newPhotos = domainPhotos.map { photo ->
                    GalleryPhoto(
                        id = photo.id,
                        url = photo.urls.small, // Lists usually use small or regular
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
                    // Logic: If Refresh, use new data directly; if LoadMore, append new data to old data
                    val oldList =
                        if (isRefresh) emptyList()
                        else (currentState.photosState as? AppUiState.Success)?.data ?: emptyList()

                    val combinedPhotos = (oldList + newPhotos).distinctBy { it.displayId }

                    currentState.copy(
                        photosState = AppUiState.Success(combinedPhotos),
                        photosPage = page,
                        isRefreshing = false,
                        photosAppendError = null,
                        // If returned data is empty, the end of the list has been reached
                        isPhotosEndOfList = newPhotos.isEmpty()
                    )
                }
                if (page > 1) internalLoading = false
            },
            onError = { msg ->
                logger.e { "Error loading collection photos: $msg" }
                _state.update { currentState ->
                    val oldList = (currentState.photosState as? AppUiState.Success)?.data

                    // Prevent clearing: As long as there is old data, only set the error to appendError
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
            }
        )
    }
}
