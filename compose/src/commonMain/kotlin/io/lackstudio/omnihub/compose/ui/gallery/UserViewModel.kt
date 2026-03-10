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
                _state.update { currentState ->
                    logger.d { "handleIntent: Refresh" }
                    currentState.copy(
                        refreshingStatus = currentState.refreshingStatus +
                                (currentState.currentTab to true)
                    )
                }
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
        val currentState = _state.value
        val currentTab = currentState.currentTab

        // Check lock (is it currently loading)
        if (internalLoadingMap[currentTab] == true) return

        val isRefreshing = currentState.refreshingStatus[currentTab] ?: false
        if (isRefreshing) return

        // Check if end of list reached
        val isEndOfList = currentState.endOfListStatus[currentTab] ?: false
        if (isEndOfList) return

        // Calculate next page
        val currentPage = currentState.pages[currentTab] ?: 1
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
//        currentSubState: AppUiState<List<R>>,
        getOldList: (UserDetailUiState) -> List<R>?,
        useCase: suspend () -> UseCaseResult<List<T>>,
        mapper: (List<T>) -> List<R>,
        distinctBy: (R) -> Any,
        stateReducer: (UserDetailUiState, AppUiState<List<R>>) -> UserDetailUiState
    ) {
        // Set internal lock
        if (targetPage > 1 && internalLoadingMap.getValue(tab)) return
        if (targetPage > 1) internalLoadingMap[tab] = true

        handleUseCaseCall(
            name = "User $tab",
            useCase = useCase,
            onLoading = {
                _state.update { currentState ->
                    val oldList = getOldList(currentState)
                    val hasOldData = !oldList.isNullOrEmpty()

                    if (!hasOldData) {
                        stateReducer(currentState, AppUiState.Loading)
                    } else {
                        currentState
                    }
                }
            },
            onSuccess = { resultData ->
                val newItems = mapper(resultData)
                val isEndOfList = newItems.isEmpty()

                _state.update { currentState ->
                    val oldList = if (targetPage > 1) {
                        getOldList(currentState).orEmpty()
                    } else {
                        emptyList()
                    }

                    val combinedList = (oldList + newItems).distinctBy(distinctBy)

                    val newPagesMap = currentState.pages + (tab to targetPage)
                    val newEndOfListMap = currentState.endOfListStatus + (tab to isEndOfList)
                    val newErrorMap = currentState.appendError - tab
                    val newRefreshMap = currentState.refreshingStatus + (tab to false)

                    stateReducer(currentState, AppUiState.Success(combinedList))
                        .copy(
                            pages = newPagesMap,
                            endOfListStatus = newEndOfListMap,
                            appendError = newErrorMap,
                            refreshingStatus = newRefreshMap
                        )
                }
                // Unlock
                internalLoadingMap[tab] = false
            },
            onError = { errorMessage ->
                logger.e { "Error fetching user category ($tab): $errorMessage" }
                // If page 1 fails, show full-page error
                // If Load More fails, only hide footer loading indicator
                _state.update { currentState ->
                    val oldList = getOldList(currentState)
                    val newRefreshMap = currentState.refreshingStatus + (tab to false)

                    if (!oldList.isNullOrEmpty()) {
                        val newErrorMap = currentState.appendError + (tab to errorMessage)
                        stateReducer(currentState, AppUiState.Success(oldList))
                            .copy(
                                refreshingStatus = newRefreshMap,
                                appendError = newErrorMap
                            )
                    } else {
                        // Only show a full-screen error when there is absolutely no data (e.g., no internet on the first app launch).
                        val newErrorMap = currentState.appendError - tab
                        stateReducer(currentState, AppUiState.Error(errorMessage))
                            .copy(
                                refreshingStatus = newRefreshMap,
                                appendError = newErrorMap
                            )
                    }
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
            distinctBy = {it.displayId},
            // Reducer
            stateReducer = { state, newState -> state.copy(photosState = newState) }
        )
    }

    private fun fetchUserCollections(username: String, page: Int) {
        val params = GetUserCollectionsParams(username, page = page, perPage = 10)

        fetchUserCategory(
            tab = UserTab.Collections,
            targetPage = page,
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
            distinctBy = {it.displayId},
            // Reducer
            stateReducer = { state, newState -> state.copy(collectionsState = newState) }
        )
    }

    private fun fetchUserLikes(username: String, page: Int) {
        val params = GetUserLikedPhotosParams(username = username, page = page, perPage = 10)

        fetchUserCategory(
            tab = UserTab.Likes,
            targetPage = page,
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
            distinctBy = {it.displayId},
            // Reducer: Update likesState
            stateReducer = { state, newState -> state.copy(likesState = newState) }
        )
    }
}
