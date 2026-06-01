package io.lackstudio.omnihub.ui.account

import androidx.lifecycle.viewModelScope
import io.lackstudio.omnifeed.auth.domain.usecase.SignInWithEmailUseCase
import io.lackstudio.omnifeed.auth.domain.usecase.SignInWithGoogleUseCase
import io.lackstudio.omnifeed.core.common.error.getFriendlyMessage
import io.lackstudio.omnifeed.ui.viewmodel.BaseViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val signInWithEmailUseCase: SignInWithEmailUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase
) : BaseViewModel() {

    private val _state = MutableStateFlow(LoginContract.State())
    val state = _state.asStateFlow()

    private val _sideEffect = Channel<LoginContract.Effect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

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
            _state.update { it.copy(isLoading = true, error = null) }
            signInWithEmailUseCase(email, password)
                .onSuccess {
                    _state.update { it.copy(isLoading = false) }
                    _sideEffect.send(LoginContract.Effect.NavigateBack)
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.getFriendlyMessage()) }
                }
        }
    }

    private fun loginWithGoogle() {
        viewModelScope.launch {
            _sideEffect.send(LoginContract.Effect.ShowGoogleSignIn)
        }
    }

    fun onGoogleSignInResult(idToken: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            signInWithGoogleUseCase(idToken)
                .onSuccess {
                    _state.update { it.copy(isLoading = false) }
                    _sideEffect.send(LoginContract.Effect.NavigateBack)
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.getFriendlyMessage()) }
                }
        }
    }
}
