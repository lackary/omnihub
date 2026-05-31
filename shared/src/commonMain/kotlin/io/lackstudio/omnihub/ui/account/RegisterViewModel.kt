package io.lackstudio.omnihub.ui.account

import androidx.lifecycle.viewModelScope
import io.lackstudio.omnifeed.auth.domain.usecase.SignUpWithEmailUseCase
import io.lackstudio.omnifeed.ui.viewmodel.BaseViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val signUpWithEmailUseCase: SignUpWithEmailUseCase
) : BaseViewModel() {

    private val _state = MutableStateFlow(RegisterContract.State())
    val state = _state.asStateFlow()

    private val _sideEffect = Channel<RegisterContract.Effect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    fun handleIntent(event: RegisterContract.Event) {
        when (event) {
            is RegisterContract.Event.OnUsernameChanged -> {
                _state.update { it.copy(username = event.username) }
            }
            is RegisterContract.Event.OnEmailChanged -> {
                _state.update { it.copy(email = event.email) }
            }
            is RegisterContract.Event.OnPasswordChanged -> {
                _state.update { it.copy(password = event.password) }
            }
            is RegisterContract.Event.OnConfirmPasswordChanged -> {
                _state.update { it.copy(confirmPassword = event.password) }
            }
            RegisterContract.Event.OnRegisterClicked -> {
                registerWithEmail()
            }
            RegisterContract.Event.OnLoginClicked -> {
                viewModelScope.launch {
                    _sideEffect.send(RegisterContract.Effect.NavigateToLogin)
                }
            }
            RegisterContract.Event.OnBackClicked -> {
                viewModelScope.launch {
                    _sideEffect.send(RegisterContract.Effect.NavigateBack)
                }
            }
        }
    }

    private fun registerWithEmail() {
        val email = state.value.email
        val username = state.value.username
        val password = state.value.password
        val confirmPassword = state.value.confirmPassword

        if (email.isBlank() || password.isBlank() || username.isBlank()) {
            _state.update { it.copy(error = "All fields are required") }
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
                    _sideEffect.send(RegisterContract.Effect.NavigateBack)
                }
                .onFailure { error ->
                    _state.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
}
