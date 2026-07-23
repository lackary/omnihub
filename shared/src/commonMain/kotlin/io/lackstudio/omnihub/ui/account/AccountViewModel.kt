package io.lackstudio.omnihub.ui.account

import androidx.lifecycle.viewModelScope
import io.lackstudio.omnifeed.auth.domain.model.AuthProvider
import io.lackstudio.omnifeed.auth.domain.model.User
import io.lackstudio.omnifeed.auth.utils.AuthManager
import io.lackstudio.omnifeed.core.network.error.RemoteException
import io.lackstudio.omnifeed.core.network.error.StructuredApiException
import io.lackstudio.omnifeed.auth.utils.DeepLinkBuffer
import io.lackstudio.omnifeed.auth.utils.OAuthUrlFactory
import io.lackstudio.omnifeed.auth.domain.usecase.*
import io.lackstudio.omnifeed.core.common.error.getFriendlyMessage
import io.lackstudio.omnifeed.core.domain.usecase.UseCaseResult
import io.lackstudio.omnifeed.core.utils.maskId
import io.lackstudio.omnifeed.core.network.oauth.AccessTokenProvider
import io.lackstudio.omnifeed.ui.viewmodel.BaseViewModel
import io.lackstudio.omnifeed.unsplash.domain.model.OAuthToken
import io.lackstudio.omnifeed.unsplash.domain.usecase.ExchangeOAuthUseCase
import io.lackstudio.omnifeed.unsplash.domain.model.OAuthCode as UnsplashOAuthCode
import io.lackstudio.omnifeed.unsplash.utils.Environment.OAUTH_AUTHORIZE as UNSPLASH_OAUTH_AUTHORIZE
import io.lackstudio.omnihub.platform.getUnsplashAccessKey
import io.lackstudio.omnihub.platform.getUnsplashSecretKey
import io.lackstudio.omnihub.utils.Environment
import io.lackstudio.omnihub.utils.ValidationUtils
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.milliseconds

class AccountViewModel(
    private val authManager: AuthManager,
    private val observeUserUseCase: ObserveUserUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val linkWithGoogleUseCase: LinkWithGoogleUseCase,
    private val exchangeOAuthUseCase: ExchangeOAuthUseCase,
    private val linkWithCustomServiceUseCase: LinkWithCustomServiceUseCase,
    private val unlinkCustomServiceUseCase: UnlinkCustomServiceUseCase,
    private val accessTokenProvider: AccessTokenProvider,
    private val updateUsernameUseCase: UpdateUsernameUseCase,
    private val updatePasswordUseCase: UpdatePasswordUseCase,
    private val reauthenticateWithEmailUseCase: ReauthenticateWithEmailUseCase,
    private val reauthenticateWithGoogleUseCase: ReauthenticateWithGoogleUseCase,
    private val reauthenticateWithCustomServiceUseCase: ReauthenticateWithCustomServiceUseCase,
) : BaseViewModel() {

    private val _state = MutableStateFlow(AccountContract.State())
    val state = _state.asStateFlow()

    private val _sideEffect = Channel<AccountContract.Effect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    private var lastUsedRedirectUri: String? = null
    private var pendingAction: (suspend () -> Unit)? = null
    private var preAuthToken: String? = null

    init {
        observeUser()
        observeDeepLink()
    }

    private fun observeUser() {
        viewModelScope.launch {
            observeUserUseCase().collect { user ->
                val currentToken = state.value.user?.idToken
                val newToken = user?.idToken
                if (currentToken != newToken) {
                    logger.i { "State Token Updated: old=${currentToken.maskId()}, new=${newToken.maskId()}" }
                }
                
                logger.d { "observeUser: user=${user?.id}, photoUrl=${user?.photoUrl}, isLoading=${state.value.isLoading}" }
                _state.update { it.copy(user = user) }
                if (user == null && !state.value.isLoading) {
                    logger.i { "observeUser: user is null, navigating to Login" }
                    _sideEffect.send(AccountContract.Effect.NavigateToLogin)
                }
            }
        }
    }

    private fun observeDeepLink() {
        viewModelScope.launch {
            combine(
                DeepLinkBuffer.deepLinkUrl,
                state.map { it.user }.distinctUntilChanged()
            ) { url, user ->
                if (url != null && url.contains("code=") && user != null) {
                    url
                } else {
                    null
                }
            }.collect { url ->
                url?.let {
                    val code = it.substringAfter("code=").substringBefore("&")
                    logger.d { "✅ AccountViewModel detected code: $code (Linking Mode)" }
                    // CONSUME IMMEDIATELY
                    DeepLinkBuffer.consumeDeepLink()
                    handleUnsplashCallback(code)
                }
            }
        }
    }

    fun handleIntent(event: AccountContract.Event) {
        when (event) {
            AccountContract.Event.OnLogoutClicked -> logout()
            AccountContract.Event.OnDeleteAccountClicked -> {
                _state.update { it.copy(showDeleteDialog = true) }
            }
            AccountContract.Event.OnDismissDeleteDialog -> {
                _state.update { it.copy(showDeleteDialog = false) }
            }
            AccountContract.Event.OnConfirmDeleteAccount -> {
                _state.update { it.copy(showDeleteDialog = false) }
                deleteAccount()
            }
            AccountContract.Event.OnLinkWithGoogleClicked -> {
                viewModelScope.launch {
                    _state.update { it.copy(isLoading = true, loadingSource = AccountContract.LoadingSource.GOOGLE) }
                    _sideEffect.send(AccountContract.Effect.ShowGoogleSignIn)
                }
            }
            AccountContract.Event.OnLinkWithUnsplashClicked -> {
                linkWithUnsplash()
            }
            AccountContract.Event.OnUnlinkUnsplashClicked -> {
                unlinkUnsplash()
            }
            AccountContract.Event.OnEditUsernameClicked -> {
                _state.update { it.copy(
                    showEditUsernameDialog = true,
                    editUsername = it.user?.username ?: ""
                ) }
            }
            is AccountContract.Event.OnUpdateUsernameChanged -> {
                _state.update { it.copy(editUsername = event.username) }
            }
            is AccountContract.Event.OnUpdateNewPasswordChanged -> {
                _state.update { it.copy(editNewPassword = event.password) }
            }
            is AccountContract.Event.OnUpdateConfirmPasswordChanged -> {
                _state.update {
                    val isMatch = ValidationUtils.isPasswordMatchSoFar(it.editNewPassword, event.password)
                    it.copy(
                        editConfirmPassword = event.password,
                        confirmPasswordError = if (isMatch) null else "Passwords do not match"
                    )
                }
            }
            AccountContract.Event.OnUpdateUsername -> {
                updateUsername(state.value.editUsername)
            }
            AccountContract.Event.OnUpdatePassword -> {
                updatePassword(state.value.editNewPassword)
            }
            AccountContract.Event.OnConfirmPasswordBlur -> {
                _state.update {
                    it.copy(confirmPasswordError = ValidationUtils.validatePasswords(it.editNewPassword, it.editConfirmPassword))
                }
            }
            AccountContract.Event.OnDismissEditUsernameDialog -> {
                _state.update { it.copy(showEditUsernameDialog = false) }
            }
            AccountContract.Event.OnTogglePasswordSection -> {
                _state.update { it.copy(
                    isPasswordSectionExpanded = !it.isPasswordSectionExpanded,
                    editNewPassword = "",
                    editConfirmPassword = "",
                    confirmPasswordError = null
                ) }
            }
            is AccountContract.Event.OnReAuthPasswordChanged -> {
                _state.update { it.copy(reAuthPassword = event.password) }
            }
            AccountContract.Event.OnConfirmReAuth -> {
                confirmReAuth()
            }
            AccountContract.Event.OnDismissReAuthDialog -> {
                _state.update { it.copy(showReAuthDialog = false, reAuthPassword = "", reAuthError = null) }
                pendingAction = null
            }
        }
    }

    private fun updateUsername(username: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadingSource = AccountContract.LoadingSource.UPDATE_USERNAME, showEditUsernameDialog = false) }
            updateUsernameUseCase(username)
                .onSuccess { user ->
                    _state.update { it.copy(isLoading = false, loadingSource = null, user = user) }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, loadingSource = null, error = error.getFriendlyMessage()) }
                }
        }
    }

    private fun updatePassword(password: String, oldPassword: String? = null) {
        performSensitiveAction(AccountContract.LoadingSource.UPDATE_PASSWORD) {
            updatePasswordUseCase(password, oldPassword).getOrThrow()
            _state.update { it.copy(
                isPasswordSectionExpanded = false,
                editNewPassword = "",
                editConfirmPassword = "",
                confirmPasswordError = null
            ) }
        }
    }


    private fun unlinkUnsplash() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadingSource = AccountContract.LoadingSource.UNSPLASH, error = null) }
            unlinkCustomServiceUseCase(Environment.SERVICE_UNSPLASH)
                .onSuccess { user ->
                    accessTokenProvider.resetToPublic()
                    _state.update { it.copy(isLoading = false, loadingSource = null, user = user) }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, loadingSource = null, error = error.getFriendlyMessage()) }
                }
        }
    }

    private fun linkWithUnsplash() {
        val redirectUri = authManager.getRedirectUrl()
        lastUsedRedirectUri = redirectUri
        val authUrl = OAuthUrlFactory.buildAuthUrl(
            baseUrl = UNSPLASH_OAUTH_AUTHORIZE,
            clientId = getUnsplashAccessKey(),
            redirectUri = redirectUri,
            scope = listOf("public", "read_user"),
            state = "web_popup"
        )

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadingSource = AccountContract.LoadingSource.UNSPLASH, error = null) }
            try {
                logger.d { "Attempting Unsplash link with popup: $authUrl" }
                val code = authManager.signInWithOAuthPopup(authUrl)
                if (code != null) {
                    logger.d { "AccountViewModel received code from popup: $code" }
                    handleUnsplashCallback(code)
                } else {
                    logger.d { "Popup closed or cancelled without code" }
                    _state.update { it.copy(isLoading = false, loadingSource = null) }
                }
            } catch (e: Exception) {
                if (e is UnsupportedOperationException) {
                    logger.d { "OAuth popup not supported on this platform, falling back to redirect flow" }
                    authManager.startLogin(authUrl)
                    // Keep isLoading = true, handleUnsplashCallback will be called via DeepLinkBuffer
                } else {
                    logger.e(e) { "Error during Unsplash popup linking" }
                    _state.update { it.copy(isLoading = false, loadingSource = null, error = e.getFriendlyMessage()) }
                }
            }
        }
    }

    private fun handleUnsplashCallback(code: String) {
        val redirectUriToUse = lastUsedRedirectUri ?: authManager.getRedirectUrl()
        val unsplashOAuthCode = UnsplashOAuthCode(
            clientId = getUnsplashAccessKey(),
            clientSecret = getUnsplashSecretKey(),
            redirectUri = redirectUriToUse,
            code = code
        )

        handleUseCaseCall(
            name = "exchangeOAuth",
            onLoading = { _state.update { it.copy(isLoading = true, loadingSource = AccountContract.LoadingSource.UNSPLASH, error = null) } },
            useCase = { exchangeOAuthUseCase(unsplashOAuthCode) },
            onSuccess = { data ->
                accessTokenProvider.setOAuthToken(data.tokenType, data.accessToken)
                
                // Plan A: Store the entire token object as JSON to preserve refresh_token for cloud sync
                val tokenJson = Json.encodeToString(
                    OAuthToken.serializer(),
                    data
                )

                if (pendingAction != null && state.value.reAuthType == AccountContract.ReAuthType.CUSTOM_SERVICE) {
                    viewModelScope.launch {
                        _state.update { it.copy(isLoading = true, loadingSource = AccountContract.LoadingSource.UNSPLASH, error = null) }
                        logger.d { "Re-authenticating Unsplash in background..." }
                        reauthenticateWithCustomServiceUseCase(Environment.SERVICE_UNSPLASH, tokenJson)
                            .onSuccess {
                                handleReAuthSuccess()
                            }
                            .onFailure { error ->
                                _state.update { it.copy(isLoading = false, loadingSource = null, error = error.getFriendlyMessage()) }
                            }
                    }
                } else {
                    performSensitiveAction(AccountContract.LoadingSource.UNSPLASH) {
                        logger.d { "Linking Unsplash account..." }
                        val user = linkWithCustomServiceUseCase(Environment.SERVICE_UNSPLASH, tokenJson).getOrThrow()
                        handleAuthSuccess(user)
                    }
                }
            },
            onError = { errorMessage ->
                _state.update { it.copy(isLoading = false, loadingSource = null, error = errorMessage) }
            }
        )
    }

    fun onGoogleSignInResult(idToken: String, accessToken: String? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadingSource = AccountContract.LoadingSource.GOOGLE, error = null) }
            
            if (pendingAction != null) {
                logger.d { "Re-authenticating Google in background..." }
                reauthenticateWithGoogleUseCase(idToken, accessToken)
                    .onSuccess {
                        handleReAuthSuccess()
                    }
                    .onFailure { error ->
                        logger.e(error) { "Google re-auth failed" }
                        _state.update { it.copy(isLoading = false, loadingSource = null, error = error.getFriendlyMessage()) }
                    }
            } else {
                logger.d { "Linking Google account..." }
                linkWithGoogleUseCase(idToken, accessToken)
                    .onSuccess { user ->
                        handleAuthSuccess(user)
                    }
                    .onFailure { error ->
                        logger.e(error) { "Google linking failed" }
                        _state.update { it.copy(isLoading = false, loadingSource = null, error = error.getFriendlyMessage()) }
                    }
            }
        }
    }

    private fun handleAuthSuccess(user: User) {
        _state.update { it.copy(isLoading = false, loadingSource = null, user = user) }
    }

    private fun confirmReAuth() {
        val reAuthType = state.value.reAuthType ?: return
        logger.d { "confirmReAuth: type=$reAuthType" }
        
        // Capture token IMMEDIATELY before starting re-auth to detect the change later
        preAuthToken = state.value.user?.idToken

        when (reAuthType) {
            AccountContract.ReAuthType.EMAIL -> {
                viewModelScope.launch {
                    // Prevent double submission and show loading
                    _state.update { it.copy(isReAuthLoading = true, reAuthError = null) }
                    reauthenticateWithEmailUseCase(state.value.reAuthPassword)
                        .onSuccess {
                            logger.i { "Email re-auth successful" }
                            _state.update { it.copy(isReAuthLoading = false) }
                            handleReAuthSuccess()
                        }
                        .onFailure { error ->
                            logger.e(error) { "Email re-auth failed" }
                            _state.update { it.copy(isReAuthLoading = false, reAuthError = error.getFriendlyMessage()) }
                        }
                }
            }
            AccountContract.ReAuthType.GOOGLE -> {
                logger.d { "Triggering Google re-auth via SideEffect" }
                _state.update { it.copy(showReAuthDialog = false) }
                viewModelScope.launch {
                    _sideEffect.send(AccountContract.Effect.ShowGoogleSignIn)
                }
            }
            AccountContract.ReAuthType.CUSTOM_SERVICE -> {
                logger.d { "Triggering Unsplash re-auth via OAuth" }
                _state.update { it.copy(showReAuthDialog = false) }
                linkWithUnsplash()
            }
        }
    }

    private fun handleReAuthSuccess() {
        logger.i { "handleReAuthSuccess: Clearing dialog and waiting for state sync..." }
        val action = pendingAction
        val lastSource = state.value.loadingSource
        val oldToken = preAuthToken

        _state.update { it.copy(
            showReAuthDialog = false,
            reAuthPassword = "",
            reAuthError = null,
            isLoading = true
        ) }

        if (action != null && lastSource != null) {
            viewModelScope.launch {
                try {
                    // Reactive Await: Wait for the token to actually change in the state
                    logger.d { "Waiting for fresh ID Token signal (pre=${oldToken.maskId()})" }
                    
                    val result = withTimeoutOrNull(5000.milliseconds) {
                        var count = 0
                        state.map { it.user }.first {
                            val currentToken = it?.idToken
                            count++
                            val isMatch = currentToken != null && currentToken != oldToken
                            logger.d { "Sync Check #$count: current=${currentToken.maskId()}, old=${oldToken.maskId()}, changed=$isMatch" }
                            isMatch
                        }
                    }

                    if (result != null) {
                        logger.i { "Fresh Token sync completed! Retrying action with source $lastSource..." }
                        pendingAction = null
                        preAuthToken = null
                        performSensitiveAction(lastSource, action)
                    } else {
                        logger.w { "State sync TIMEOUT! Retrying anyway as a last resort..." }
                        pendingAction = null
                        preAuthToken = null
                        performSensitiveAction(lastSource, action)
                    }
                } catch (e: Exception) {
                    logger.e(e) { "Error during state sync wait" }
                    _state.update { it.copy(isLoading = false, loadingSource = null) }
                }
            }
        } else {
            _state.update { it.copy(isLoading = false, loadingSource = null) }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadingSource = AccountContract.LoadingSource.LOGOUT) }
            signOutUseCase()
                .onSuccess {
                    accessTokenProvider.resetToPublic()
                    _state.update { it.copy(isLoading = false, loadingSource = null) }
                    _sideEffect.send(AccountContract.Effect.NavigateToLogin)
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, loadingSource = null, error = error.message) }
                }
        }
    }

    private fun deleteAccount() {
        performSensitiveAction(AccountContract.LoadingSource.DELETE) {
            deleteAccountUseCase().getOrThrow()
            accessTokenProvider.resetToPublic()
            _sideEffect.send(AccountContract.Effect.NavigateToLogin)
        }
    }

    private fun performSensitiveAction(
        loadingSource: AccountContract.LoadingSource,
        action: suspend () -> Unit
    ) {
        viewModelScope.launch {
            logger.d { "performSensitiveAction: source=$loadingSource" }
            _state.update { it.copy(isLoading = true, loadingSource = loadingSource, error = null) }
            try {
                action()
                logger.i { "performSensitiveAction: success for $loadingSource" }
                _state.update { it.copy(isLoading = false, loadingSource = null) }
                // CRITICAL: Clear all pending states upon success
                pendingAction = null
                preAuthToken = null
            } catch (e: Exception) {
                if (e.isReAuthRequired()) {
                    logger.w { "performSensitiveAction: RE-AUTH REQUIRED for $loadingSource" }
                    pendingAction = action
                    triggerReAuthFlow()
                } else {
                    logger.e(e) { "performSensitiveAction: failed for $loadingSource" }
                    _state.update { it.copy(isLoading = false, loadingSource = null, error = e.getFriendlyMessage()) }
                    pendingAction = null
                    preAuthToken = null
                }
            }
        }
    }

    private fun triggerReAuthFlow() {
        val user = state.value.user ?: return
        logger.i { "triggerReAuthFlow: user=${user.id}, providers=${user.authProviders.keys}, lastProvider=${user.lastSignInProvider}" }
        
        val hasGoogle = user.authProviders.containsKey(AuthProvider.GOOGLE.id)
        val hasPassword = user.authProviders.containsKey(AuthProvider.PASSWORD.id)
        val hasUnsplash = user.linkedServices.containsKey(Environment.SERVICE_UNSPLASH)

        // Smart Re-auth: Default to the provider used in the current session
        val reAuthType = when (user.lastSignInProvider) {
            "google.com" -> if (hasGoogle) AccountContract.ReAuthType.GOOGLE else null
            "password" -> if (hasPassword) AccountContract.ReAuthType.EMAIL else null
            "custom:unsplash" -> if (hasUnsplash) AccountContract.ReAuthType.CUSTOM_SERVICE else null
            else -> null
        } ?: when {
            // Fallback to static priority if session provider is unknown or missing
            hasGoogle -> AccountContract.ReAuthType.GOOGLE
            hasPassword -> AccountContract.ReAuthType.EMAIL
            hasUnsplash -> AccountContract.ReAuthType.CUSTOM_SERVICE
            else -> null
        }

        if (reAuthType != null) {
            _state.update { it.copy(
                showReAuthDialog = true,
                reAuthType = reAuthType,
                reAuthPassword = "",
                reAuthError = null,
                isLoading = false
            ) }
        } else {
            _state.update { it.copy(isLoading = false, loadingSource = null, error = "Authentication expired. Please log in again.") }
        }
    }

    private fun <T> UseCaseResult<T>.getOrThrow(): T {
        return when (this) {
            is UseCaseResult.Success -> data
            is UseCaseResult.Error -> throw exception
        }
    }

    private fun Throwable.isReAuthRequired(): Boolean {
        val msg = when (this) {
            is StructuredApiException -> structuredMessage ?: originalApiException.errorBody ?: message ?: ""
            is RemoteException.Api -> errorBody ?: message ?: ""
            else -> message ?: ""
        }
        val isRequired = msg.contains("CREDENTIAL_TOO_OLD_LOGIN_AGAIN") ||
                msg.contains("recent authentication") || // Matches Android SDK
                msg.contains("requires-recent-login") || // Matches Web/JS SDK
                msg.contains("INVALID_ID_TOKEN") ||
                msg.contains("USER_TOKEN_EXPIRED") ||
                msg.contains("TOKEN_EXPIRED") ||
                msg.contains("UNAUTHENTICATED")

        if (isRequired) {
            logger.w { "isReAuthRequired: TRUE for message: $msg" }
        } else {
            logger.d { "isReAuthRequired: FALSE for message: $msg" }
        }
        
        return isRequired
    }
}
