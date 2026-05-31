package io.lackstudio.omnihub.ui.auth

import androidx.lifecycle.viewModelScope
import io.lackstudio.omnifeed.auth.domain.usecase.SignInWithEmailUseCase
import io.lackstudio.omnifeed.auth.domain.usecase.SignInWithGoogleUseCase
import io.lackstudio.omnifeed.auth.domain.usecase.SignUpWithEmailUseCase
import io.lackstudio.omnifeed.ui.viewmodel.BaseViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val signInWithEmailUseCase: SignInWithEmailUseCase,
    private val signUpWithEmailUseCase: SignUpWithEmailUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase
) : BaseViewModel() {

    private val _state = MutableStateFlow(AuthContract.State())
    val state = _state.asStateFlow()

    private val _sideEffect = Channel<AuthContract.Effect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    fun handleIntent(event: AuthContract.Event) {
        when (event) {
            is AuthContract.Event.OnEmailChanged -> {
                _state.update { it.copy(email = event.email) }
            }
            is AuthContract.Event.OnPasswordChanged -> {
                _state.update { it.copy(password = event.password) }
            }
            is AuthContract.Event.OnConfirmPasswordChanged -> {
                _state.update { it.copy(confirmPassword = event.password) }
            }
            AuthContract.Event.OnToggleMode -> {
                _state.update { it.copy(isRegisterMode = !it.isRegisterMode, error = null) }
            }
            AuthContract.Event.OnLoginClicked -> {
                loginWithEmail()
            }
            AuthContract.Event.OnRegisterClicked -> {
                registerWithEmail()
            }
            AuthContract.Event.OnGoogleLoginClicked -> {
                loginWithGoogle()
            }
            AuthContract.Event.OnBackClicked -> {
                viewModelScope.launch {
                    _sideEffect.send(AuthContract.Effect.NavigateBack)
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
                    _sideEffect.send(AuthContract.Effect.NavigateBack)
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    private fun registerWithEmail() {
        val email = state.value.email
        val password = state.value.password
        val confirmPassword = state.value.confirmPassword

        if (email.isBlank() || password.isBlank()) {
            _state.update { it.copy(error = "Email and password cannot be empty") }
            return
        }

        if (password != confirmPassword) {
            _state.update { it.copy(error = "Passwords do not match") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            signUpWithEmailUseCase(email, password)
                .onSuccess {
                    _state.update { it.copy(isLoading = false) }
                    _sideEffect.send(AuthContract.Effect.NavigateBack)
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    private fun loginWithGoogle() {
        viewModelScope.launch {
            _sideEffect.send(AuthContract.Effect.ShowGoogleSignIn)
        }
    }

    fun onGoogleSignInResult(idToken: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            signInWithGoogleUseCase(idToken)
                .onSuccess {
                    _state.update { it.copy(isLoading = false) }
                    _sideEffect.send(AuthContract.Effect.NavigateBack)
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
}
