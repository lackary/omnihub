package io.lackstudio.omnihub.ui.gallery

import io.lackstudio.omnifeed.core.domain.usecase.UseCaseResult
import io.lackstudio.omnifeed.ui.state.AppUiState
import io.lackstudio.omnifeed.ui.viewmodel.BaseViewModel
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetCollectionsParams
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetCollectionsUseCase
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetPhotosParams
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetPhotosUseCase
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetTopicsParams
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetTopicsUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

class GalleryViewModel(
    private val getPhotosUseCase: GetPhotosUseCase,
    private val getCollectionsUseCase: GetCollectionsUseCase,
    private val getTopicsUseCase: GetTopicsUseCase
) : BaseViewModel() {

    private val _state = MutableStateFlow(GalleryUiState())
    val state = _state.asStateFlow()

    // Create a Channel for sending one-time events
    private val _sideEffect = Channel<GallerySideEffect>(Channel.BUFFERED)
    // Expose as Flow for UI observation
    val sideEffect = _sideEffect.receiveAsFlow()

    // Page number trackers (defaults to page 1)
    private var photosPage = 1
    private var collectionsPage = 1
    private var topicsPage = 1

    // Flag to prevent duplicate loading (avoid triggering API multiple times on scroll)
    private var isLoadingMore = false

    init {
        fetchPhotos(1)
    }

    fun handleIntent(intent: GalleryIntent) {
        when (intent) {
            is GalleryIntent.SelectTab -> {
                _state.update { it.copy(currentTab = intent.tab) }
                when (intent.tab) {
                    // Check if Idle (means not loaded yet)
                    GalleryTab.Photos -> if (_state.value.photosState is AppUiState.Idle) fetchPhotos(1)
                    GalleryTab.Collections -> if (_state.value.collectionsState is AppUiState.Idle) fetchCollections(1)
                    GalleryTab.Topics -> if (_state.value.topicsState is AppUiState.Idle) fetchTopics(1)
                }
            }
            is GalleryIntent.Refresh -> {
                // Enable Refresh indicator
                _state.update { it.copy(isRefreshing = true) }
                // Execute refresh (force reload)
                refreshCurrentTab()
            }
            is GalleryIntent.LoadMore -> {
                loadNextPage()
            }
        }
    }

    private fun refreshCurrentTab() {
        when (_state.value.currentTab) {
            GalleryTab.Photos -> fetchPhotos(1)
            GalleryTab.Collections -> fetchCollections(1)
            GalleryTab.Topics -> fetchTopics(1)
        }
    }

    private fun loadNextPage() {
        // If loading is in progress, ignore this request (debounce)
        if (isLoadingMore) return

        // If refreshing, do not trigger load more to avoid data inconsistency
        if (_state.value.isRefreshing) return

        // Check if end of list (EndOfList) is reached, if so, do not load
        val isEnd = when (_state.value.currentTab) {
            GalleryTab.Photos -> _state.value.photosEndOfList
            GalleryTab.Collections -> _state.value.collectionsEndOfList
            GalleryTab.Topics -> _state.value.topicsEndOfList
        }
        if (isEnd) return

        // Determine what to load based on current Tab
        when (_state.value.currentTab) {
            GalleryTab.Photos -> fetchPhotos(photosPage + 1)
            GalleryTab.Collections -> fetchCollections(collectionsPage + 1)
            GalleryTab.Topics -> fetchTopics(topicsPage + 1)
        }
    }

    /**
     * Generic Helper function: Handle "Smart Loading" and "State Update" uniformly
     * @param targetPage Target page number
     * @param currentSubState Current state of the field (used to check if old data exists)
     * @param useCase API call logic
     * @param mapper Data transformation logic (Domain -> UI)
     * @param stateReducer State update logic (specify which field of state to update)
     * @param onSuccessUpdatePage Update current page number
     */
    private fun <D, U> fetchCategory(
        targetPage: Int,
        currentSubState: AppUiState<List<U>>,
        useCase: suspend () -> UseCaseResult<D>,
        mapper: (D) -> List<U>,
        stateReducer: (GalleryUiState, AppUiState<List<U>>, Boolean) -> GalleryUiState,
        onSuccessUpdatePage: () -> Unit
    ) {
        val isLoadMore = targetPage > 1

        if (isLoadMore) isLoadingMore = true
        val hasOldData = currentSubState is AppUiState.Success

        handleUseCaseCall(
            onLoading = {
                // Show full-screen Loading only when "Page 1" and "No old data"
                // If LoadMore or Refresh, this will not execute (keep screen)
                if (targetPage == 1 && !hasOldData) {
                    _state.update { currentState ->
                        stateReducer(currentState, AppUiState.Loading, false)
                    }
                }
            },
            useCase = useCase,
            onSuccess = { domainData ->
                val newItems = mapper(domainData)

                // 🔥 Logic: If the fetched items are empty, it means the end of the list
                val isEndOfList = newItems.isEmpty()

                // Data merging logic
                // If LoadMore and has old data -> Old + New
                // Otherwise (Page 1) -> Use new directly
                val finalItems = if (isLoadMore && hasOldData) {
                    currentSubState.data + newItems
                } else {
                    newItems
                }

                _state.update { currentState ->
                    // Update specific field + uniformly disable isRefreshing
                    stateReducer(currentState, AppUiState.Success(finalItems), isEndOfList)
                        .copy(isRefreshing = false)
                }

                // Update page number only when "new data exists" to avoid getting stuck at the last page
                if (!isEndOfList) {
                    onSuccessUpdatePage()
                }
                if (isLoadMore) isLoadingMore = false
            },
            onError = { errorMessage ->
                if (isLoadMore) isLoadingMore = false

                if (hasOldData) {
                    // Has old data: Silent failure, just disable refresh
                    _state.update { it.copy(isRefreshing = false) }
                    // Send Snackbar event (can call send directly since this is in a suspend function)
                    _sideEffect.trySend(GallerySideEffect.ShowSnackbar(errorMessage))
                } else {
                    // No old data: Show full-screen error
                    _state.update { currentState ->
                        // On error, keep original endOfList state (simplified to false here to allow retry)
                        stateReducer(currentState, AppUiState.Error(errorMessage), false)
                            .copy(isRefreshing = false)
                    }
                }
            }
        )
    }

    private fun fetchPhotos(page: Int) {
        val params = GetPhotosParams(page = page, perPage = 10)

        fetchCategory(
            targetPage = page,
            currentSubState = _state.value.photosState,
            useCase = { getPhotosUseCase(params) },
            mapper = { list -> list.map { GalleryPhoto(it.id, it.urls.small, it.description ?: "") } },
            stateReducer = { state, newState, isEnd ->
                state.copy(photosState = newState, photosEndOfList = isEnd)
            },
            onSuccessUpdatePage = { photosPage = page }
        )
    }

    private fun fetchCollections(page: Int) {
        val params = GetCollectionsParams(page = page, perPage = 10)

        fetchCategory(
            targetPage = page,
            currentSubState = _state.value.collectionsState,
            useCase = { getCollectionsUseCase(params) },
            mapper = { list -> list.map { GalleryCollection(it.id, it.coverPhoto?.urls?.small, it.title, it.totalPhotos) } },
            stateReducer = { state, newState, isEnd ->
                state.copy(collectionsState = newState, collectionsEndOfList = isEnd)
            },
            onSuccessUpdatePage = { collectionsPage = page }
        )
    }

    private fun fetchTopics(page: Int) {
        val params = GetTopicsParams(page = page, perPage = 10)
        fetchCategory(
            targetPage = page,
            currentSubState = _state.value.topicsState,
            useCase = { getTopicsUseCase(params) },
            mapper = { list -> list.map { GalleryTopic(it.id,
                it.coverPhoto.urls.small, it.title, it.description) } },
            stateReducer = { state, newState, isEnd ->
                state.copy(topicsState = newState, topicsEndOfList = isEnd)
                           },
            onSuccessUpdatePage = { topicsPage = page }
        )
    }
}
