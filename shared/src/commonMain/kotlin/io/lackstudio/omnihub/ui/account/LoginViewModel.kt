package io.lackstudio.omnihub.ui.account

import androidx.lifecycle.viewModelScope
import io.lackstudio.omnifeed.auth.utils.AuthManager
import io.lackstudio.omnifeed.auth.utils.DeepLinkBuffer
import io.lackstudio.omnifeed.auth.utils.OAuthUrlFactory
import io.lackstudio.omnifeed.auth.domain.usecase.SignInWithCustomServiceUseCase
import io.lackstudio.omnifeed.auth.domain.usecase.SignInWithEmailUseCase
import io.lackstudio.omnifeed.auth.domain.usecase.SignInWithGoogleUseCase
import io.lackstudio.omnifeed.auth.domain.usecase.ObserveUserUseCase
import io.lackstudio.omnifeed.core.common.error.getFriendlyMessage
import io.lackstudio.omnifeed.core.network.oauth.AccessTokenProvider
import io.lackstudio.omnifeed.ui.viewmodel.BaseViewModel
import io.lackstudio.omnifeed.unsplash.domain.usecase.ExchangeOAuthUseCase
import io.lackstudio.omnifeed.unsplash.domain.model.OAuthCode as UnsplashOAuthCode
import io.lackstudio.omnifeed.unsplash.utils.Environment.OAUTH_AUTHORIZE as UNSPLASH_OAUTH_AUTHORIZE
import io.lackstudio.omnihub.platform.getUnsplashAccessKey
import io.lackstudio.omnihub.platform.getUnsplashSecretKey
import io.lackstudio.omnihub.utils.Environment
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authManager: AuthManager,
    private val signInWithEmailUseCase: SignInWithEmailUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val signInWithCustomServiceUseCase: SignInWithCustomServiceUseCase,
    private val exchangeOAuthUseCase: ExchangeOAuthUseCase,
    private val accessTokenProvider: AccessTokenProvider,
    private val observeUserUseCase: ObserveUserUseCase,
) : BaseViewModel() {

    private val _state = MutableStateFlow(LoginContract.State())
    val state = _state.asStateFlow()

    private val _sideEffect = Channel<LoginContract.Effect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    init {
        observeDeepLink()
    }

    private fun observeDeepLink() {
        viewModelScope.launch {
            combine(
                DeepLinkBuffer.deepLinkUrl,
                observeUserUseCase()
            ) { url, user ->
                if (url != null && url.contains("code=") && user == null) {
                    url
                } else {
                    null
                }
            }.collect { url ->
                url?.let {
                    val code = it.substringAfter("code=").substringBefore("&")
                    logger.d { "✅ LoginViewModel detected code: $code (Sign-in Mode)" }
                    // CONSUME IMMEDIATELY to prevent re-triggering if state changes quickly
                    DeepLinkBuffer.consumeDeepLink()
                    handleUnsplashCallback(code)
                }
            }
        }
    }

    fun handleIntent(event: LoginContract.Event) {
        when (event) {
            is LoginContract.Event.OnEmailChanged -> {
                _state.update { it.copy(email = event.email) }
            }
            is LoginContract.Event.OnPasswordChanged -> {
                _state.update { it.copy(password = event.password) }
            }
            LoginContract.Event.OnLoginClicked -> {
                loginWithEmail()
            }
            LoginContract.Event.OnGoogleLoginClicked -> {
                loginWithGoogle()
            }
            LoginContract.Event.OnUnsplashLoginClicked -> {
                loginWithUnsplash()
            }
            LoginContract.Event.OnSignUpClicked -> {
                viewModelScope.launch {
                    _sideEffect.send(LoginContract.Effect.NavigateToRegister)
                }
            }
            LoginContract.Event.OnBackClicked -> {
                viewModelScope.launch {
                    _sideEffect.send(LoginContract.Effect.NavigateBack)
                }
            }
        }
    }

    private fun loginWithEmail() {
        val email = state.value.email
        val password = state.value.password

        if (email.isBlank() || password.isBlank()) {
            _state.update { it.copy(error = "Email and password cannot be empty") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadingSource = LoginContract.LoadingSource.EMAIL, error = null) }
            signInWithEmailUseCase(email, password)
                .onSuccess {
                    _state.update { it.copy(isLoading = false, loadingSource = null) }
                    _sideEffect.send(LoginContract.Effect.LoginSuccess)
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, loadingSource = null, error = error.getFriendlyMessage()) }
                }
        }
    }

    private fun loginWithGoogle() {
        viewModelScope.launch {
            _sideEffect.send(LoginContract.Effect.ShowGoogleSignIn)
        }
    }

    private fun loginWithUnsplash() {
        val authUrl = OAuthUrlFactory.buildAuthUrl(
            baseUrl = UNSPLASH_OAUTH_AUTHORIZE,
            clientId = getUnsplashAccessKey(),
            redirectUri = authManager.getRedirectUrl(),
            scope = listOf("public", "read_user")
        )
        authManager.startLogin(authUrl)
    }

    private fun handleUnsplashCallback(code: String) {
        val unsplashOAuthCode = UnsplashOAuthCode(
            clientId = getUnsplashAccessKey(),
            clientSecret = getUnsplashSecretKey(),
            redirectUri = authManager.getRedirectUrl(),
            code = code
        )

        handleUseCaseCall(
            name = "exchangeOAuth",
            onLoading = { _state.update { it.copy(isLoading = true, loadingSource = LoginContract.LoadingSource.UNSPLASH, error = null) } },
            useCase = { exchangeOAuthUseCase(unsplashOAuthCode) },
            onSuccess = { data ->
                val serviceName = Environment.SERVICE_UNSPLASH
                viewModelScope.launch {
                    accessTokenProvider.setOAuthToken(data.tokenType, data.accessToken)
                    
                    signInWithCustomServiceUseCase(serviceName, data.accessToken)
                        .onSuccess {
                            logger.d { "Service $serviceName, login success!" }
                            _state.update { it.copy(isLoading = false, loadingSource = null) }
                            _sideEffect.send(LoginContract.Effect.LoginSuccess)
                        }
                        .onFailure { error ->
                            logger.e(error) { "Service $serviceName, login Failed!, error message: ${error.message}" }
                            _state.update { it.copy(isLoading = false, loadingSource = null, error = error.getFriendlyMessage()) }
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
            logger.i { "Google Login: Received tokens, starting Firebase sign-in..." }
            _state.update { it.copy(isLoading = true, loadingSource = LoginContract.LoadingSource.GOOGLE, error = null) }
            
            signInWithGoogleUseCase(idToken, accessToken)
                .onSuccess {
                    logger.i { "Google Login: Firebase sign-in SUCCESS!" }
                    _state.update { it.copy(isLoading = false, loadingSource = null) }
                    _sideEffect.send(LoginContract.Effect.LoginSuccess)
                }
                .onFailure { error ->
                    val message = error.getFriendlyMessage()
                    logger.e { "Google Login: Firebase sign-in FAILED: $message" }
                    _state.update { it.copy(isLoading = false, loadingSource = null, error = message) }
                }
        }
    }
}
