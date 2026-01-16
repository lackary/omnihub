package io.lackstudio.omnihub.ui.gallery

import androidx.lifecycle.viewModelScope
import io.lackstudio.omnifeed.core.domain.usecase.UseCaseResult
import io.lackstudio.omnifeed.core.network.oauth.AccessTokenProvider
import io.lackstudio.omnifeed.ui.state.AppUiState
import io.lackstudio.omnifeed.ui.viewmodel.BaseViewModel
import io.lackstudio.omnifeed.unsplash.domain.usecase.ExchangeOAuthUseCase
import io.lackstudio.omnifeed.unsplash.domain.model.OAuthCode as UnsplashOAuthCode
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetCollectionsParams
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetCollectionsUseCase
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetMeUseCase
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetPhotosParams
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetPhotosUseCase
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetTopicsParams
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetTopicsUseCase
import io.lackstudio.omnihub.auth.DeepLinkBuffer
import io.lackstudio.omnihub.platform.getUnsplashAccessKey
import io.lackstudio.omnihub.platform.getUnsplashSecretKey
import io.lackstudio.omnihub.utils.Environment
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GalleryViewModel(
    private val getPhotosUseCase: GetPhotosUseCase,
    private val getCollectionsUseCase: GetCollectionsUseCase,
    private val getTopicsUseCase: GetTopicsUseCase,
    private val exchangeOAuthUseCase: ExchangeOAuthUseCase,
    private val accessTokenProvider: AccessTokenProvider,
    private val meUseCase: GetMeUseCase,
) : BaseViewModel() {

    private val _state = MutableStateFlow(GalleryUiState())
    val state = _state.asStateFlow()

    // Create a Channel for sending one-time events
    private val _sideEffect = Channel<GallerySideEffect>(Channel.BUFFERED)
    // Expose as Flow for UI observation
    val sideEffect = _sideEffect.receiveAsFlow()

    private var photosPage = 1
    private var collectionsPage = 1
    private var topicsPage = 1

    // Flag to prevent duplicate loading (avoid triggering API multiple times on scroll)
    // Use Map to track loading status of each Tab
    private val loadingStatus = mutableMapOf<GalleryTab, Boolean>().withDefault { false }

    init {
        fetchPhotos(1)

        viewModelScope.launch {
            accessTokenProvider.authToken.collect { token ->
                if (token.value.isNotEmpty()) {
                    // If token exists -> fetch user profile
                    fetchMeProfile()
                } else {
                    // If no token (e.g., just logged out) -> clear user profile
                    _state.update { it.copy(meProfile = null) }
                }
            }
        }

        viewModelScope.launch {
            DeepLinkBuffer.deepLinkUrl.collect { url ->
                if (url != null && url.contains("code=")) {
                    val code = url.substringAfter("code=").substringBefore("&")

                    println("✅ ViewModel detected code: $code")

                    // Execute login logic
                    handleAuthCallback(code)

                    // Clear Buffer to avoid duplication
                    DeepLinkBuffer.consumeDeepLink()
                }
            }
        }
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
                val currentTab = _state.value.currentTab
                _state.update {
                    it.copy(refreshingStatus = it.refreshingStatus + (currentTab to true))
                }
                // Execute refresh (force reload)
                refreshCurrentTab()
            }
            is GalleryIntent.LoadMore -> {
                loadNextPage()
            }
            is GalleryIntent.Login ->  {
                login()
            }
        }
    }

    private fun login() {
        val loginUrl = getLoginUrl()
        viewModelScope.launch {
            _sideEffect.send(GallerySideEffect.OpenUrl(loginUrl))
        }
    }

    private fun handleAuthCallback(code: String) {
        val unsplashOAuthCode = UnsplashOAuthCode(
            clientId = getUnsplashAccessKey(),
            clientSecret = getUnsplashSecretKey(),
            redirectUri = Environment.AUTH_REDIRECT_URL,
            code = code
        )

        handleUseCaseCall(
            useCase = { exchangeOAuthUseCase(unsplashOAuthCode) },
            onLoading = {
                _state.update { it.copy(isAuthenticating = true) }
            },
            onSuccess = { data ->
                // Handle successful login
                viewModelScope.launch {
                    accessTokenProvider.setOAuthToken(data.tokenType, data.accessToken)

                    _state.update { it.copy(isAuthenticating = false) }
                    _sideEffect.send(GallerySideEffect.ShowSnackbar("Login Successful!"))

                    // Trigger refresh to update UI with logged-in state (e.g., User Profile, Likes)
                    fetchMeProfile()
                }
            },
            onError = { errorMessage ->
                viewModelScope.launch {
                    _state.update { it.copy(isAuthenticating = false) }
                    _sideEffect.send(GallerySideEffect.ShowSnackbar("Login Failed: $errorMessage"))
                }
            }
        )
    }

    private fun fetchMeProfile() {
        if (_state.value.meProfile != null) return

        handleUseCaseCall(
            useCase = { meUseCase(Unit) },
            onLoading = { },
            onSuccess = { me ->
                _state.update { it.copy(meProfile = me) }

                refreshCurrentTab()
            },
            onError = { errorMessage ->
                println("Fetch profile failed: $errorMessage")
            }

        )
    }

    private fun refreshCurrentTab() {
        when (_state.value.currentTab) {
            GalleryTab.Photos -> fetchPhotos(1)
            GalleryTab.Collections -> fetchCollections(1)
            GalleryTab.Topics -> fetchTopics(1)
        }
    }

    private fun loadNextPage() {
        val currentTab = _state.value.currentTab
        // Check if the Tab is currently loading
        if (loadingStatus[currentTab] == true) return

        // If refreshing, do not trigger load more to avoid data inconsistency
        val isRefreshing = _state.value.refreshingStatus[currentTab] ?: false
        if (isRefreshing) return

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
     * @param currentSubState Current state of the field (used for Loading check only)
     * @param getOldList Lambda to retrieve the *latest* list data at the moment of update
     * @param useCase API call logic
     * @param mapper Data transformation logic (Domain -> UI)
     * @param stateReducer State update logic (specify which field of state to update)
     * @param onSuccessUpdatePage Update current page number
     */
    private fun <T, R> fetchCategory(
        tab: GalleryTab,
        targetPage: Int,
        currentSubState: AppUiState<List<R>>,
        getOldList: (GalleryUiState) -> List<R>?, // Function to retrieve the latest data dynamically
        useCase: suspend () -> UseCaseResult<List<T>>,
        mapper: (List<T>) -> List<R>,
        distinctBy: (R) -> Any,
        stateReducer: (GalleryUiState, AppUiState<List<R>>, Boolean) -> GalleryUiState,
        onSuccessUpdatePage: () -> Unit
    ) {
        // Need to get the current Tab, or pass it as a parameter
        val currentTab = _state.value.currentTab
        // Prevent concurrent load-more requests.
        if (targetPage > 1 && loadingStatus.getValue(currentTab)) return

        // Set flag: If Page > 1, mark as loading more
        if (targetPage > 1) loadingStatus[currentTab] = true

        handleUseCaseCall(
            useCase = useCase,
            onLoading = {
                // Show full page loading (AppUiState.Loading) only when "Page 1" and "no old data"
                val hasOldData = targetPage > 1 || (currentSubState is AppUiState.Success && currentSubState.data.isNotEmpty())

                if (!hasOldData) {
                    _state.update {
                        stateReducer(it, AppUiState.Loading, false)
                    }
                }
            },
            onSuccess = { resultData ->
                val newItems = mapper(resultData)
                val isEndOfList = newItems.isEmpty()

                onSuccessUpdatePage()
                // Enter update block to get the "latest" state, instead of relying on the passed currentSubState
                _state.update { currentState ->
                    // 1. Dynamically retrieve old data (prevent Page 1 from being overwritten by Page 2 before it's written)
                    val oldList = if (targetPage > 1) {
                        getOldList(currentState) ?: emptyList()
                    } else {
                        emptyList()
                    }

                    // 2. Merge data
                    val finalData = (oldList + newItems).distinctBy(distinctBy)
                    val finalSubState = AppUiState.Success(finalData)

                    // Turn off the refresh status of a specific Tab
                    val newMap = currentState.refreshingStatus + (tab to false)

                    // 3. Update State and ensure Refresh loading indicator is closed
                    stateReducer(currentState, finalSubState, isEndOfList)
                        .copy(refreshingStatus = newMap)
                }

                // Unlock flag
                if (targetPage > 1) loadingStatus[currentTab] = false
            },
            onError = { exception ->
                _state.update { currentState ->
                    val newMap = currentState.refreshingStatus + (tab to false)
                    // Remember to close Refresh loading indicator even if an error occurs
                    stateReducer(currentState, AppUiState.Error(exception), false)
                        .copy(refreshingStatus = newMap)
                }

                // Unlock flag
                if (targetPage > 1) loadingStatus[currentTab] = false
            }
        )
    }

    private fun fetchPhotos(page: Int) {
        val params = GetPhotosParams(page = page, perPage = 10)

        fetchCategory(
            tab = GalleryTab.Photos,
            targetPage = page,
            currentSubState = _state.value.photosState,
            // Pass Lambda to retrieve the latest data during merge
            getOldList = { state -> (state.photosState as? AppUiState.Success)?.data },
            useCase = { getPhotosUseCase(params) },
            mapper = { list ->
                list.map {
                    GalleryPhoto(
                        it.id,
                        url = it.urls.small,
                        title = it.description ?: "",
                        userProfileImage = it.user.profileImage.small,
                        username = it.user.username,
                        name = it.user.name,
                        likes = it.likes,
                        blurhash = it.blurHash,
                        width = it.width,
                        height = it.height
                    )
                } },
            distinctBy = { it.id },
            stateReducer = { state, newState, isEnd ->
                state.copy(photosState = newState, photosEndOfList = isEnd)
            },
            onSuccessUpdatePage = { photosPage = page }
        )
    }

    private fun fetchCollections(page: Int) {
        val params = GetCollectionsParams(page = page, perPage = 10)

        fetchCategory(
            tab = GalleryTab.Collections,
            targetPage = page,
            currentSubState = _state.value.collectionsState,
            // Pass Lambda to retrieve the latest data during merge
            getOldList = { state -> (state.collectionsState as? AppUiState.Success)?.data },
            useCase = { getCollectionsUseCase(params) },
            mapper = { list ->
                list.map {
                    GalleryCollection(
                        id = it.id,
                        coverUrl = it.coverPhoto?.urls?.small,
                        title = it.title,
                        totalPhotos = it.totalPhotos,
                        userProfileImage = it.user.profileImage.small,
                        username = it.user.username,
                        name = it.user.name,
                        blurhash = it.coverPhoto?.blurHash,
                        width = it.coverPhoto?.width,
                        height = it.coverPhoto?.height,
                        previewPhotos =
                            it.previewPhotos?.map { previewPhoto ->
                                GalleryPreview(
                                    url = previewPhoto.urls.small,
                                    blurHash = previewPhoto.blurHash
                                )
                            }?: emptyList()
                    )
                } },
            distinctBy = { it.id },
            stateReducer = { state, newState, isEnd ->
                state.copy(collectionsState = newState, collectionsEndOfList = isEnd)
            },
            onSuccessUpdatePage = { collectionsPage = page }
        )
    }

    private fun fetchTopics(page: Int) {
        val params = GetTopicsParams(page = page, perPage = 10)

        fetchCategory(
            tab = GalleryTab.Topics,
            targetPage = page,
            currentSubState = _state.value.topicsState,
            // Pass Lambda to retrieve the latest data during merge
            getOldList = { state -> (state.topicsState as? AppUiState.Success)?.data },
            useCase = { getTopicsUseCase(params) },
            mapper = { list ->
                list.map {
                    GalleryTopic(
                        id = it.id,
                        coverUrl = it.coverPhoto.urls.small,
                        title = it.title,
                        username = it.coverPhoto.user.username,
                        name = it.coverPhoto.user.name,
                        description = it.description,
                        totalPhotos = it.totalPhotos,
                        blurhash = it.coverPhoto.blurHash,
                        width = it.coverPhoto.width,
                        height = it.coverPhoto.height
                    )
                } },
            distinctBy = { it.id },
            stateReducer = { state, newState, isEnd ->
                state.copy(topicsState = newState, topicsEndOfList = isEnd)
            },
            onSuccessUpdatePage = { topicsPage = page }
        )
    }

    private fun getLoginUrl(): String {
        return "https://unsplash.com/oauth/authorize" +
                "?client_id=${getUnsplashAccessKey()}" +
                "&redirect_uri=${Environment.AUTH_REDIRECT_URL}" +
                "&response_type=code" +
                "&scope=public"
    }
}
