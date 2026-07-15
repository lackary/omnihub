package io.lackstudio.omnihub.ui.account

import androidx.lifecycle.viewModelScope
import io.lackstudio.omnifeed.auth.domain.model.User
import io.lackstudio.omnifeed.auth.utils.AuthManager
import io.lackstudio.omnifeed.core.network.error.StructuredApiException
import io.lackstudio.omnifeed.auth.utils.DeepLinkBuffer
import io.lackstudio.omnifeed.auth.utils.OAuthUrlFactory
import io.lackstudio.omnifeed.auth.domain.usecase.*
import io.lackstudio.omnifeed.core.common.error.getFriendlyMessage
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

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
) : BaseViewModel() {

    private val _state = MutableStateFlow(AccountContract.State())
    val state = _state.asStateFlow()

    private val _sideEffect = Channel<AccountContract.Effect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    private var lastUsedRedirectUri: String? = null
    private var pendingPasswordUpdate: String? = null

    init {
        observeUser()
        observeDeepLink()
    }

    private fun observeUser() {
        viewModelScope.launch {
            observeUserUseCase().collect { user ->
                logger.d { "observeUser: user=$user, isLoading=${state.value.isLoading}" }
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
            AccountContract.Event.OnEditPasswordClicked -> {
                _state.update { it.copy(
                    showEditPasswordDialog = true,
                    editOldPassword = "",
                    editNewPassword = "",
                    editConfirmPassword = "",
                    confirmPasswordError = null
                ) }
            }
            is AccountContract.Event.OnUpdateUsernameChanged -> {
                _state.update { it.copy(editUsername = event.username) }
            }
            is AccountContract.Event.OnUpdateOldPasswordChanged -> {
                _state.update { it.copy(editOldPassword = event.password) }
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
                updatePassword(state.value.editNewPassword, state.value.editOldPassword.takeIf { it.isNotBlank() })
            }
            AccountContract.Event.OnConfirmPasswordBlur -> {
                _state.update {
                    it.copy(confirmPasswordError = ValidationUtils.validatePasswords(it.editNewPassword, it.editConfirmPassword))
                }
            }
            AccountContract.Event.OnDismissEditUsernameDialog -> {
                _state.update { it.copy(showEditUsernameDialog = false) }
            }
            AccountContract.Event.OnDismissEditPasswordDialog -> {
                _state.update { it.copy(showEditPasswordDialog = false) }
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
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadingSource = AccountContract.LoadingSource.UPDATE_PASSWORD, showEditPasswordDialog = false, error = null) }
            updatePasswordUseCase(password, oldPassword)
                .onSuccess {
                    _state.update { it.copy(isLoading = false, loadingSource = null) }
                    pendingPasswordUpdate = null
                }
                .onFailure { error ->
                    if (error.isReAuthRequired()) {
                        logger.i { "Re-auth required for password update, triggering auto re-auth" }
                        pendingPasswordUpdate = password
                        triggerReAuth()
                    } else {
                        _state.update { it.copy(isLoading = false, loadingSource = null, error = error.getFriendlyMessage()) }
                        pendingPasswordUpdate = null
                    }
                }
        }
    }

    private fun Throwable.isReAuthRequired(): Boolean {
        val msg = if (this is StructuredApiException) {
            this.structuredMessage ?: this.message ?: ""
        } else {
            this.message ?: ""
        }
        return msg.contains("CREDENTIAL_TOO_OLD_LOGIN_AGAIN")
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
            scope = listOf("public", "read_user")
        )
        authManager.startLogin(authUrl)
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
                viewModelScope.launch {
                    accessTokenProvider.setOAuthToken(data.tokenType, data.accessToken)
                    
                    // Plan A: Store the entire token object as JSON to preserve refresh_token for cloud sync
                    val tokenJson = Json.encodeToString(
                        OAuthToken.serializer(),
                        data
                    )
                    
                    linkWithCustomServiceUseCase(Environment.SERVICE_UNSPLASH, tokenJson)
                        .onSuccess { user ->
                            handleAuthSuccess(user)
                        }
                        .onFailure { error ->
                            _state.update { it.copy(isLoading = false, loadingSource = null, error = error.getFriendlyMessage()) }
                            pendingPasswordUpdate = null
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
            linkWithGoogleUseCase(idToken, accessToken)
                .onSuccess { user ->
                    handleAuthSuccess(user)
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, loadingSource = null, error = error.getFriendlyMessage()) }
                    pendingPasswordUpdate = null
                }
        }
    }

    private fun handleAuthSuccess(user: User) {
        _state.update { it.copy(isLoading = false, loadingSource = null, user = user) }
        
        // Auto-resume pending password update if exists
        pendingPasswordUpdate?.let { password ->
            logger.i { "Resuming pending password update after successful re-authentication" }
            updatePassword(password, state.value.editOldPassword.takeIf { it.isNotBlank() })
            // Note: pendingPasswordUpdate is cleared inside updatePassword's onSuccess/onFailure
        }
    }

    private suspend fun triggerReAuth() {
        val user = state.value.user ?: return
        if (user.id.startsWith("custom:${Environment.SERVICE_UNSPLASH}")) {
            logger.i { "Triggering Unsplash re-auth" }
            linkWithUnsplash()
        } else {
            logger.i { "Triggering Google re-auth" }
            _sideEffect.send(AccountContract.Effect.ShowGoogleSignIn)
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
        viewModelScope.launch {
            logger.i { "deleteAccount initiated" }
            _state.update { it.copy(isLoading = true, loadingSource = AccountContract.LoadingSource.DELETE) }
            deleteAccountUseCase()
                .onSuccess {
                    logger.i { "deleteAccount SUCCESS, navigating to Login" }
                    accessTokenProvider.resetToPublic()
                    _state.update { it.copy(isLoading = false, loadingSource = null) }
                    _sideEffect.send(AccountContract.Effect.NavigateToLogin)
                }
                .onFailure { error ->
                    logger.e { "deleteAccount FAILURE: ${error.message}" }
                    _state.update { it.copy(isLoading = false, loadingSource = null, error = error.message) }
                }
        }
    }
}
