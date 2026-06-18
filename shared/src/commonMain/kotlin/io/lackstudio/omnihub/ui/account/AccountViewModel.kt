package io.lackstudio.omnihub.ui.account

import androidx.lifecycle.viewModelScope
import io.lackstudio.omnifeed.auth.domain.usecase.DeleteAccountUseCase
import io.lackstudio.omnifeed.auth.domain.usecase.LinkWithGoogleUseCase
import io.lackstudio.omnifeed.auth.domain.usecase.LinkWithCustomServiceUseCase
import io.lackstudio.omnifeed.auth.domain.usecase.UnlinkCustomServiceUseCase
import io.lackstudio.omnifeed.auth.domain.usecase.ObserveUserUseCase
import io.lackstudio.omnifeed.auth.domain.usecase.SignOutUseCase
import io.lackstudio.omnifeed.core.common.error.getFriendlyMessage
import io.lackstudio.omnifeed.ui.viewmodel.BaseViewModel
import io.lackstudio.omnifeed.unsplash.domain.usecase.ExchangeOAuthUseCase
import io.lackstudio.omnifeed.unsplash.domain.model.OAuthCode as UnsplashOAuthCode
import io.lackstudio.omnifeed.auth.AuthManager
import io.lackstudio.omnifeed.auth.DeepLinkBuffer
import io.lackstudio.omnihub.platform.getUnsplashAccessKey
import io.lackstudio.omnihub.platform.getUnsplashSecretKey
import io.lackstudio.omnihub.utils.Environment
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccountViewModel(
    private val authManager: AuthManager,
    private val observeUserUseCase: ObserveUserUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val linkWithGoogleUseCase: LinkWithGoogleUseCase,
    private val exchangeOAuthUseCase: ExchangeOAuthUseCase,
    private val linkWithCustomServiceUseCase: LinkWithCustomServiceUseCase,
    private val unlinkCustomServiceUseCase: UnlinkCustomServiceUseCase,
) : BaseViewModel() {

    private val _state = MutableStateFlow(AccountContract.State())
    val state = _state.asStateFlow()

    private val _sideEffect = Channel<AccountContract.Effect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    private var lastUsedRedirectUri: String? = null

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
            DeepLinkBuffer.deepLinkUrl.collect { url ->
                if (url != null && url.contains("code=")) {
                    val code = url.substringAfter("code=").substringBefore("&")
                    logger.d { "✅ AccountViewModel detected code: $code" }
                    handleUnsplashCallback(code)
                    DeepLinkBuffer.consumeDeepLink()
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
                    _sideEffect.send(AccountContract.Effect.ShowGoogleSignIn)
                }
            }
            AccountContract.Event.OnLinkWithUnsplashClicked -> {
                linkWithUnsplash()
            }
            AccountContract.Event.OnUnlinkUnsplashClicked -> {
                unlinkUnsplash()
            }
        }
    }

    private fun unlinkUnsplash() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            unlinkCustomServiceUseCase(Environment.SERVICE_UNSPLASH)
                .onSuccess { user ->
                    _state.update { it.copy(isLoading = false, user = user) }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.getFriendlyMessage()) }
                }
        }
    }

    private fun linkWithUnsplash() {
        val redirectUri = authManager.getRedirectUrl()
        lastUsedRedirectUri = redirectUri
        val authUrl = "https://unsplash.com/oauth/authorize" +
                "?client_id=${getUnsplashAccessKey()}" +
                "&response_type=code" +
                "&scope=public+read_user" +
                "&redirect_uri=$redirectUri"
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
            onLoading = { _state.update { it.copy(isLoading = true, error = null) } },
            useCase = { exchangeOAuthUseCase(unsplashOAuthCode) },
            onSuccess = { data ->
                viewModelScope.launch {
                    linkWithCustomServiceUseCase(Environment.SERVICE_UNSPLASH, data.accessToken)
                        .onSuccess { user ->
                            _state.update { it.copy(isLoading = false, user = user) }
                        }
                        .onFailure { error ->
                            _state.update { it.copy(isLoading = false, error = error.getFriendlyMessage()) }
                        }
                }
            },
            onError = { errorMessage ->
                _state.update { it.copy(isLoading = false, error = errorMessage) }
            }
        )
    }

    fun onGoogleSignInResult(idToken: String, accessToken: String? = null) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            linkWithGoogleUseCase(idToken, accessToken)
                .onSuccess { user ->
                    _state.update { it.copy(isLoading = false, user = user) }
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.getFriendlyMessage()) }
                }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            signOutUseCase()
                .onSuccess {
                    _state.update { it.copy(isLoading = false) }
                    _sideEffect.send(AccountContract.Effect.NavigateToLogin)
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    private fun deleteAccount() {
        viewModelScope.launch {
            logger.i { "deleteAccount initiated" }
            _state.update { it.copy(isLoading = true) }
            deleteAccountUseCase()
                .onSuccess {
                    logger.i { "deleteAccount SUCCESS, navigating to Login" }
                    _state.update { it.copy(isLoading = false) }
                    _sideEffect.send(AccountContract.Effect.NavigateToLogin)
                }
                .onFailure { error ->
                    logger.e { "deleteAccount FAILURE: ${error.message}" }
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
}
