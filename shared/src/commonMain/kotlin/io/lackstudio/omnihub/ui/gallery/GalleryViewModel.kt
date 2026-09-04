package io.lackstudio.omnihub.ui.gallery

import androidx.lifecycle.viewModelScope
import io.lackstudio.omnifeed.core.domain.usecase.UseCaseResult
import io.lackstudio.omnifeed.core.network.oauth.AccessTokenProvider
import io.lackstudio.omnifeed.ui.state.AppUiState
import io.lackstudio.omnifeed.ui.viewmodel.BaseViewModel
import io.lackstudio.omnifeed.unsplash.domain.usecase.ExchangeOAuthUseCase
import io.lackstudio.omnifeed.unsplash.domain.model.OAuthCode as UnsplashOAuthCode
import io.lackstudio.omnifeed.unsplash.utils.Environment.OAUTH_AUTHORIZE as UNSPLASH_OAUTH_AUTHORIZE
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetCollectionsParams
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetCollectionsUseCase
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetMeUseCase
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetPhotosParams
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetPhotosUseCase
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetTopicsParams
import io.lackstudio.omnifeed.unsplash.domain.usecase.GetTopicsUseCase
import io.lackstudio.omnifeed.auth.domain.usecase.LinkWithCustomServiceUseCase
import io.lackstudio.omnifeed.auth.utils.AuthManager
import io.lackstudio.omnifeed.auth.utils.DeepLinkBuffer
import io.lackstudio.omnifeed.auth.utils.OAuthUrlFactory
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
    private val authManager: AuthManager,
    private val getPhotosUseCase: GetPhotosUseCase,
    private val getCollectionsUseCase: GetCollectionsUseCase,
    private val getTopicsUseCase: GetTopicsUseCase,
    private val exchangeOAuthUseCase: ExchangeOAuthUseCase,
    private val linkWithCustomServiceUseCase: LinkWithCustomServiceUseCase,
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

    private var lastUsedRedirectUri: String? = null

    // Flag to prevent duplicate loading (avoid triggering API multiple times on scroll)
    // Use Map to track loading status of each Tab
    private val internalLoadingMap = mutableMapOf<GalleryTab, Boolean>().withDefault { false }

    init {
        logger.d{"ViewModel init"}
        fetchPhotos(1)

        viewModelScope.launch {
            // Check if we have a persisted token at startup
            // This allows the UI to show the correct state (logged in vs logged out) immediately
            _state.update { it.copy(isAuthenticating = true) }
            val resolvedToken = accessTokenProvider.resolveToken()
            if (resolvedToken.type == "Bearer") {
                // If we have a Bearer token, we need to update the StateFlow
                // so that it triggers the fetchMeProfile logic
                accessTokenProvider.setOAuthToken(resolvedToken.type, resolvedToken.value)
            } else {
                // If no token, we are done "authenticating"
                _state.update { it.copy(isAuthenticating = false) }
            }

            accessTokenProvider.authToken.collect { token ->
                // public type is Client-ID
                // OAuth2 type is Bearer
                if (token.type == "Bearer") {
                    // If token type is Bear -> fetch user profile
                    fetchMeProfile()
                } else {
                    // If no token (e.g., just logged out) -> clear user profile
                    _state.update { it.copy(meProfile = null, isAuthenticating = false) }
                }
            }
        }

        viewModelScope.launch {
            DeepLinkBuffer.deepLinkUrl.collect { url ->
                if (url != null && url.contains("code=")) {
                    val code = url.substringAfter("code=").substringBefore("&")

                    logger.d{"✅ ViewModel detected code: $code"}

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
                logger.d { "handleIntent: SelectTab tab=${intent.tab}" }
                _state.update { it.copy(currentTab = intent.tab) }
                logger.d { "handleIntent: currentTab=${_state.value.currentTab}" }
                val shouldLoad = when(intent.tab) {
                    GalleryTab.Photos -> _state.value.photosState is AppUiState.Idle
                    GalleryTab.Collections -> _state.value.collectionsState is AppUiState.Idle
                    GalleryTab.Topics -> _state.value.topicsState is AppUiState.Idle
                }
                if (shouldLoad) {
                    loadContent(intent.tab, 1)
                }
            }
            is GalleryIntent.Refresh -> {
                // Enable Refresh indicator
                _state.update { currentState ->
                    logger.d { "handleIntent: Refresh" }
                    currentState.copy(
                        refreshingStatus = currentState.refreshingStatus +
                                (currentState.currentTab to true))
                }
                // Execute refresh (force reload)
                refreshCurrentTab()
            }
            is GalleryIntent.LoadMore -> {
                logger.d { "handleIntent: LoadMore" }
                loadNextPage()
            }
            is GalleryIntent.Login ->  {
                if (_state.value.isAuthenticating) return
                logger.d { "handleIntent: Login" }
                login(authManager.getRedirectUrl())
            }
        }
    }

    private fun login(redirectUri: String) {
        logger.d { "login: $redirectUri" }
        // Store it for token exchange later
        lastUsedRedirectUri = redirectUri

        // Build the authorization URL
        val authUrl = getAuthUrl(redirectUri)
        logger.d { "login: $authUrl" }

        authManager.startLogin(authUrl)
    }

    private fun handleAuthCallback(code: String) {
        val redirectUriToUse = lastUsedRedirectUri ?: authManager.getRedirectUrl()
        val unsplashOAuthCode = UnsplashOAuthCode(
            clientId = getUnsplashAccessKey(),
            clientSecret = getUnsplashSecretKey(),
            redirectUri =  redirectUriToUse,
            code = code
        )

        handleUseCaseCall(
            name = "exchangeOAuth",
            useCase = { exchangeOAuthUseCase(unsplashOAuthCode) },
            onLoading = {
                _state.update { it.copy(isAuthenticating = true) }
            },
            onSuccess = { data ->
                // Handle successful login
                viewModelScope.launch {
                    accessTokenProvider.setOAuthToken(data.tokenType, data.accessToken)

                    // Link with Firebase
                    linkWithCustomServiceUseCase(Environment.SERVICE_UNSPLASH, data.accessToken)

                    _state.update { it.copy(isAuthenticating = false) }
                    _sideEffect.send(GallerySideEffect.ShowSnackbar("Login Successful!"))
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
        if (_state.value.meProfile != null) {
            _state.update { it.copy(isAuthenticating = false) }
            return
        }

        handleUseCaseCall(
            name = "me",
            useCase = { meUseCase(Unit) },
            onLoading = {
                _state.update { it.copy(isAuthenticating = true) }
            },
            onSuccess = { me ->
                _state.update { it.copy(meProfile = me, isAuthenticating = false) }

                refreshCurrentTab()
            },
            onError = { errorMessage ->
                logger.e { "Fetch profile failed: $errorMessage" }
                _state.update { it.copy(isAuthenticating = false) }
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
        val currentState = _state.value
        val currentTab = currentState.currentTab
        // Check if the Tab is currently loading
        if (internalLoadingMap[currentTab] == true) return

        // If refreshing, do not trigger load more to avoid data inconsistency
        val isRefreshing = currentState.refreshingStatus[currentTab] ?: false
        if (isRefreshing) return

        // Check if end of list (EndOfList) is reached, if so, do not load
        val isEndOfList = currentState.endOfListStatus[currentTab] ?: false
        if (isEndOfList) return

        val currentPage = currentState.pages[currentTab] ?: 1
        val nextPage = currentPage + 1
        loadContent(currentTab, nextPage)
    }

    private fun loadContent(tab: GalleryTab, page: Int) {
        when(tab) {
            GalleryTab.Photos -> fetchPhotos(page)
            GalleryTab.Collections -> fetchCollections(page)
            GalleryTab.Topics -> fetchTopics(page)
        }
    }

    /**
     * Generic Helper function: Handle "Smart Loading" and "State Update" uniformly
     * @name Optional name for the UseCase
     * @param targetPage Target page number
     * @param getOldList Lambda to retrieve the *latest* list data at the moment of update
     * @param useCase API call logic
     * @param mapper Data transformation logic (Domain -> UI)
     * @param distinctBy
     * @param stateReducer State update logic (specify which field of state to update)
     */
    private fun <T, R> fetchCategory(
        name: String = "fetchCategory",
        tab: GalleryTab,
        targetPage: Int,
        getOldList: (GalleryUiState) -> List<R>?, // Function to retrieve the latest data dynamically
        useCase: suspend () -> UseCaseResult<List<T>>,
        mapper: (List<T>) -> List<R>,
        distinctBy: (R) -> Any,
        stateReducer: (GalleryUiState, AppUiState<List<R>>) -> GalleryUiState,
    ) {
        if (targetPage > 1 && internalLoadingMap.getValue(tab)) return
        if (targetPage > 1) internalLoadingMap[tab] = true

        handleUseCaseCall(
            name = name,
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

                // Enter update block to get the "latest" state, instead of relying on the passed currentSubState
                _state.update { currentState ->
                    // Get old data (if Page 1, clear it, representing Refresh)
                    val oldList = if (targetPage > 1) {
                        getOldList(currentState) ?: emptyList()
                    } else {
                        emptyList()
                    }

                    // Merge data
                    val finalData = (oldList + newItems).distinctBy(distinctBy)
                    val finalSubState = AppUiState.Success(finalData)

                    // Turn off the refresh status of a specific Tab
                    val newPagesMap = currentState.pages + (tab to targetPage)
                    val newRefreshMap = currentState.refreshingStatus + (tab to false)
                    val newEndOfListMap = currentState.endOfListStatus + (tab to isEndOfList)
                    val newErrorMap = currentState.appendError - tab

                    // Update State and ensure Refresh or EndOfList loading indicator is closed
                    stateReducer(currentState, finalSubState)
                        .copy(
                            refreshingStatus = newRefreshMap,
                            endOfListStatus = newEndOfListMap,
                            appendError = newErrorMap,
                            pages = newPagesMap
                        )
                }

                // Unlock flag
                if (targetPage > 1) internalLoadingMap[tab] = false
            },
            onError = { errorMessage ->
                logger.d{"errorMessage: $errorMessage"}
                _state.update { currentState ->
                    val newRefreshMap = currentState.refreshingStatus + (tab to false)
                    val oldList = getOldList(currentState)

                    if (!oldList.isNullOrEmpty()) {
                        val newErrorMap = currentState.appendError + (tab to errorMessage)
                        stateReducer(currentState, AppUiState.Success(oldList))
                            .copy(
                                refreshingStatus = newRefreshMap,
                                appendError = newErrorMap
                            )
                    } else {
                        // Only show a full-screen error when there is absolutely no data
                        // (e.g., no internet on the first app launch).
                        val newErrorMap = currentState.appendError - tab
                        stateReducer(currentState, AppUiState.Error(errorMessage))
                            .copy(
                                refreshingStatus = newRefreshMap,
                                appendError = newErrorMap
                            )
                    }
                }

                // Unlock flag
                if (targetPage > 1) internalLoadingMap[tab] = false
            }
        )
    }

    private fun fetchPhotos(page: Int) {
        val params = GetPhotosParams(page = page, perPage = 10)

        fetchCategory(
            name = "Photos",
            tab = GalleryTab.Photos,
            targetPage = page,
            // Pass Lambda to retrieve the latest data during merge
            getOldList = { state -> (state.photosState as? AppUiState.Success)?.data },
            useCase = { getPhotosUseCase(params) },
            mapper = { list ->
                list.map { photo ->
                    GalleryPhoto(
                        id = photo.id,
                        url = photo.urls.small,
                        title = photo.description ?: "",
                        userProfileImage = photo.user.profileImage.small,
                        username = photo.user.username,
                        name = photo.user.name,
                        likes = photo.likes,
                        blurhash = photo.blurHash,
                        width = photo.width,
                        height = photo.height
                    )
                } },
            distinctBy = { it.id },
            stateReducer = { state, newState ->
                state.copy(
                    photosState = newState,
                )
            },
        )
    }

    private fun fetchCollections(page: Int) {
        val params = GetCollectionsParams(page = page, perPage = 10)

        fetchCategory(
            name = "Collections",
            tab = GalleryTab.Collections,
            targetPage = page,
            // Pass Lambda to retrieve the latest data during merge
            getOldList = { state -> (state.collectionsState as? AppUiState.Success)?.data },
            useCase = { getCollectionsUseCase(params) },
            mapper = { list ->
                list.map { collection ->
                    GalleryCollection(
                        id = collection.id,
                        coverUrl = collection.coverPhoto?.urls?.small,
                        title = collection.title,
                        totalPhotos = collection.totalPhotos,
                        userProfileImage = collection.user.profileImage.small,
                        username = collection.user.username,
                        name = collection.user.name,
                        blurhash = collection.coverPhoto?.blurHash,
                        width = collection.coverPhoto?.width ?: 0,
                        height = collection.coverPhoto?.height ?: 0,
                        previewPhotos =
                            collection.previewPhotos?.map { previewPhoto ->
                                GalleryPreview(
                                    url = previewPhoto.urls.small,
                                    blurHash = previewPhoto.blurHash
                                )
                            }?: emptyList()
                    )
                } },
            distinctBy = { it.id },
            stateReducer = { state, newState ->
                state.copy(
                    collectionsState = newState,
                )
            },
        )
    }

    private fun fetchTopics(page: Int) {
        val params = GetTopicsParams(page = page, perPage = 20)

        fetchCategory(
            name = "Topics",
            tab = GalleryTab.Topics,
            targetPage = page,
            // Pass Lambda to retrieve the latest data during merge
            getOldList = { state -> (state.topicsState as? AppUiState.Success)?.data },
            useCase = { getTopicsUseCase(params) },
            mapper = { list ->
                list.map { topic ->
                    GalleryTopic(
                        id = topic.id,
                        coverUrl = topic.coverPhoto?.urls?.small,
                        title = topic.title,
                        username = topic.coverPhoto?.user?.username ?: "",
                        name = topic.coverPhoto?.user?.name?: "",
                        description = topic.description,
                        totalPhotos = topic.totalPhotos,
                        blurhash = topic.coverPhoto?.blurHash,
                        width = topic.coverPhoto?.width ?: 0,
                        height = topic.coverPhoto?.height ?: 0
                    )
                } },
            distinctBy = { it.id },
            stateReducer = { state, newState ->
                state.copy(
                    topicsState = newState,
                )
            },
        )
    }

    private fun getAuthUrl(redirectUrl: String): String {
        return OAuthUrlFactory.buildAuthUrl(
            baseUrl = UNSPLASH_OAUTH_AUTHORIZE,
            clientId = getUnsplashAccessKey(),
            redirectUri = redirectUrl,
            scope = listOf("public", "read_user")
        )
    }
}
