package io.lackstudio.omnihub.compose.ui.gallery

import io.lackstudio.omnifeed.core.domain.usecase.UseCaseResult
import io.lackstudio.omnifeed.ui.state.AppUiState
import io.lackstudio.omnifeed.ui.viewmodel.BaseViewModel
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetUserCollectionsParams
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetUserCollectionsUseCase
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetUserLikedPhotosParams
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetUserLikedPhotosUseCase
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetUserPhotosParams
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetUserPhotosUseCase
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetUserPublicProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class UserViewModel(
    private val getUserPublicProfileUseCase: GetUserPublicProfileUseCase,
    private val getUserPhotosUseCase: GetUserPhotosUseCase,
    private val getUserCollectionsUseCase: GetUserCollectionsUseCase,
    private val getUserLikedPhotosUseCase: GetUserLikedPhotosUseCase
) : BaseViewModel() {

    private val _state = MutableStateFlow(UserDetailUiState())
    val state = _state.asStateFlow()

    private var currentUsername: String? = null

    // Internal loading lock (prevents duplicate triggers), Key is Tab
    private val internalLoadingMap = mutableMapOf<UserTab, Boolean>().withDefault { false }

    fun handleIntent(intent: UserDetailIntent) {
        when (intent) {
            is UserDetailIntent.LoadData -> {
                logger.d { "handleIntent: LoadData username=${intent.username}" }
                if (currentUsername != intent.username) {
                    currentUsername = intent.username
                    loadUserInfo(intent.username)
                    // Initial load of Photos (Page 1)
                    fetchUserPhotos(intent.username, 1)
                }
            }
            is UserDetailIntent.SelectTab -> {
                logger.d { "handleIntent: SelectTab tab=${intent.tab}" }
                _state.update { it.copy(currentTab = intent.tab) }

                // Automatically load when switching tabs if in Idle state
                val shouldLoad = when (intent.tab) {
                    UserTab.Photos -> _state.value.photosState is AppUiState.Idle
                    UserTab.Collections -> _state.value.collectionsState is AppUiState.Idle
                    UserTab.Likes -> _state.value.likesState is AppUiState.Idle
                }

                if (shouldLoad && currentUsername != null) {
                    loadContent(intent.tab, currentUsername!!, page = 1)
                }
            }
            is UserDetailIntent.LoadMore -> {
                logger.d { "handleIntent: LoadMore" }
                // Unified handling of LoadMore
                currentUsername?.let { loadNextPage(it) }
            }
            is UserDetailIntent.Refresh -> {
                logger.d { "handleIntent: Refresh" }
                currentUsername?.let { username ->
                    // Reload user information
                    loadUserInfo(username)
                    // Refresh the current Tab (reset to Page 1)
                    loadContent(_state.value.currentTab, username, page = 1)
                }
            }
        }
    }

    private fun loadUserInfo(username: String) {
        handleUseCaseCall(
            name = "User",
            useCase = { getUserPublicProfileUseCase(username) },
            onLoading = { _state.update { it.copy(infoState = AppUiState.Loading) } },
            onSuccess = { domainUser ->
                val uiProfile = UserProfile(
                    id = domainUser.id,
                    username = domainUser.username,
                    name = domainUser.name,
                    avatarUrl = domainUser.profileImage.large,
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
            onError = { msg ->
                logger.e { "Error loading user info: $msg" }
                _state.update { it.copy(infoState = AppUiState.Error(msg)) }
            }
        )
    }

    // Determine if next page loading is needed
    private fun loadNextPage(username: String) {
        val currentTab = _state.value.currentTab

        // Check lock (is it currently loading)
        if (internalLoadingMap.getValue(currentTab)) return

        // Check if end of list reached
        val isEndOfList = _state.value.endOfListStatus[currentTab] ?: false
        if (isEndOfList) return

        // Calculate next page
        val currentPage = _state.value.pages[currentTab] ?: 1
        val nextPage = currentPage + 1

        // Execute loading
        loadContent(currentTab, username, nextPage)
    }

    // Unified entry point
    private fun loadContent(tab: UserTab, username: String, page: Int) {
        when (tab) {
            UserTab.Photos -> fetchUserPhotos(username, page)
            UserTab.Collections -> fetchUserCollections(username, page)
            UserTab.Likes -> fetchUserLikes(username, page)
        }
    }

    /**
     * ★ Generic function: aligned with GalleryViewModel ★
     * isRefresh is not required; UI state is automatically determined via targetPage and currentSubState.
     */
    private fun <T, R> fetchUserCategory(
        tab: UserTab,
        targetPage: Int,
        currentSubState: AppUiState<List<R>>,
        getOldList: (UserDetailUiState) -> List<R>?,
        useCase: suspend () -> UseCaseResult<List<T>>,
        mapper: (List<T>) -> List<R>,
        stateReducer: (UserDetailUiState, AppUiState<List<R>>) -> UserDetailUiState
    ) {
        // Set internal lock
        internalLoadingMap[tab] = true

        handleUseCaseCall(
            name = "User $tab",
            useCase = useCase,
            onLoading = {
                // Logic: Show full-page loading only if it's page 1 and there is no data
                // If it's page 1 but has data (representing Pull-to-Refresh), do not show full-page loading
                // If it's Load More (Page > 1), show footer loading indicator
                val hasOldData = targetPage > 1 || (currentSubState is AppUiState.Success && currentSubState.data.isNotEmpty())

                if (!hasOldData) {
                    _state.update { stateReducer(it, AppUiState.Loading) }
                } else if (targetPage > 1) {
                    updateLoadingMap(tab, true)
                }
            },
            onSuccess = { resultData ->
                val newItems = mapper(resultData)
                val isEndOfList = newItems.isEmpty()

                _state.update { currentState ->
                    // Get old data (if Page 1, clear it, representing Refresh)
                    val oldList = if (targetPage > 1) {
                        getOldList(currentState).orEmpty()
                    } else {
                        emptyList()
                    }

                    // Merge data
                    val combinedList = oldList + newItems

                    // Update specific data fields
                    val intermediateState = stateReducer(currentState, AppUiState.Success(combinedList))

                    // Update paging and status Map
                    intermediateState.copy(
                        pages = intermediateState.pages + (tab to targetPage),
                        endOfListStatus = intermediateState.endOfListStatus + (tab to isEndOfList),
                        loadingMoreStatus = intermediateState.loadingMoreStatus + (tab to false)
                    )
                }
                // Unlock
                internalLoadingMap[tab] = false
            },
            onError = { msg ->
                logger.e { "Error fetching user category ($tab): $msg" }
                // If page 1 fails, show full-page error
                // If Load More fails, only hide footer loading indicator
                if (targetPage == 1) {
                    _state.update { stateReducer(it, AppUiState.Error(msg)) }
                } else {
                    updateLoadingMap(tab, false)
                }
                internalLoadingMap[tab] = false
            }
        )
    }

    private fun fetchUserPhotos(username: String, page: Int) {
        val params = GetUserPhotosParams(username = username, page = page, perPage = 10)

        fetchUserCategory(
            tab = UserTab.Photos,
            targetPage = page,
            currentSubState = _state.value.photosState,
            // Lambda for getting old data
            getOldList = { state -> (state.photosState as? AppUiState.Success)?.data },
            // UseCase
            useCase = { getUserPhotosUseCase(params) },
            // Mapper
            mapper = { list ->
                list.map { photo ->
                    GalleryPhoto(
                        id = photo.id,
                        url = photo.urls.small,
                        title = photo.description ?: "",
                        likes = photo.likes,
                        userProfileImage = null,
                        username = "",
                        name = "",
                        blurhash = photo.blurHash,
                        width = photo.width,
                        height = photo.height
                    )
                }
            },
            // Reducer
            stateReducer = { state, newState -> state.copy(photosState = newState) }
        )
    }

    private fun fetchUserCollections(username: String, page: Int) {
        val params = GetUserCollectionsParams(username, page = page, perPage = 10)

        fetchUserCategory(
            tab = UserTab.Collections,
            targetPage = page,
            currentSubState = _state.value.collectionsState,
            // Get old data
            getOldList = { state -> (state.collectionsState as? AppUiState.Success)?.data },
            // UseCase
            useCase = { getUserCollectionsUseCase(params) },
            // Mapper
            mapper = { list ->
                list.map { collection ->
                    val isSameUser = collection.user.username == currentUsername
                    GalleryCollection(
                        id = collection.id,
                        coverUrl = collection.coverPhoto?.urls?.small,
                        title = collection.title,
                        totalPhotos = collection.totalPhotos,
                        userProfileImage = if (isSameUser) null else collection.user.profileImage.small,
                        username = if (isSameUser) "" else collection.user.username,
                        name = if (isSameUser) "" else collection.user.name,
                        blurhash = collection.coverPhoto?.blurHash,
                        width = collection.coverPhoto?.width ?: 0,
                        height = collection.coverPhoto?.height ?: 0,
                        previewPhotos = collection.previewPhotos?.map {
                            GalleryPreview(it.urls.small, it.blurHash)
                        } ?: emptyList()
                    )
                }
            },
            // Reducer
            stateReducer = { state, newState -> state.copy(collectionsState = newState) }
        )
    }

    private fun fetchUserLikes(username: String, page: Int) {
        val params = GetUserLikedPhotosParams(username = username, page = page, perPage = 10)

        fetchUserCategory(
            tab = UserTab.Likes,
            targetPage = page,
            currentSubState = _state.value.likesState,
            // Get old data
            getOldList = { state -> (state.likesState as? AppUiState.Success)?.data },
            useCase = { getUserLikedPhotosUseCase(params) },

            mapper = { list ->
                list.map { photo ->
                    val isSameUser = photo.user.username == currentUsername
                    GalleryPhoto(
                        id = photo.id,
                        url = photo.urls.small,
                        title = photo.description?: "",
                        likes = photo.likes,
                        userProfileImage = if (isSameUser) null else photo.user.profileImage.small,
                        username = if (isSameUser) "" else photo.user.username,
                        name = if (isSameUser) "" else photo.user.name,
                        blurhash = photo.blurHash,
                        width = photo.width,
                        height = photo.height,
                    )
                }
            },
            // Reducer: Update likesState
            stateReducer = { state, newState -> state.copy(likesState = newState) }
        )
    }

    // Helper function: Update Loading Map (for Load More footer loading)
    private fun updateLoadingMap(tab: UserTab, isLoading: Boolean) {
        _state.update {
            it.copy(loadingMoreStatus = it.loadingMoreStatus + (tab to isLoading))
        }
    }
}
