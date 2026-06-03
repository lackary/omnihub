package io.lackstudio.omnihub.ui.account

import androidx.lifecycle.viewModelScope
import io.lackstudio.omnifeed.auth.domain.usecase.SignUpWithEmailUseCase
import io.lackstudio.omnifeed.core.common.error.getFriendlyMessage
import io.lackstudio.omnifeed.ui.viewmodel.BaseViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegisterViewModel(
    private val signUpWithEmailUseCase: SignUpWithEmailUseCase,
) : BaseViewModel() {

    private val emailRegex = Regex("""^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$""")

    private val _state = MutableStateFlow(RegisterContract.State())
    val state = _state.asStateFlow()

    private val _sideEffect = Channel<RegisterContract.Effect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    fun handleIntent(event: RegisterContract.Event) {
        when (event) {
            is RegisterContract.Event.OnUsernameChanged -> {
                _state.update { it.copy(username = event.username, error = null) }
            }
            is RegisterContract.Event.OnEmailChanged -> {
                _state.update { it.copy(email = event.email, emailError = null, error = null) }
            }
            RegisterContract.Event.OnEmailBlur -> {
                if (state.value.email.isNotEmpty() && !validateEmail(state.value.email)) {
                    _state.update { it.copy(emailError = "Invalid email format") }
                }
            }
            is RegisterContract.Event.OnPasswordChanged -> {
                _state.update { it.copy(password = event.password, passwordError = null, error = null) }
            }
            RegisterContract.Event.OnPasswordBlur -> {
                _state.update {
                    var newState = it
                    if (it.password.isNotEmpty() && it.password.length < 6) {
                        newState = newState.copy(passwordError = "Password must be at least 6 characters")
                    }
                    if (it.confirmPassword.isNotEmpty() && it.password != it.confirmPassword) {
                        newState = newState.copy(confirmPasswordError = "Passwords do not match")
                    }
                    newState
                }
            }
            is RegisterContract.Event.OnConfirmPasswordChanged -> {
                _state.update {
                    val isMatchSoFar = it.password.startsWith(event.password)
                    it.copy(
                        confirmPassword = event.password,
                        confirmPasswordError = if (isMatchSoFar) null else "Passwords do not match",
                        error = null
                    )
                }
            }
            RegisterContract.Event.OnConfirmPasswordBlur -> {
                _state.update {
                    if (it.confirmPassword.isNotEmpty() && it.password != it.confirmPassword) {
                        it.copy(confirmPasswordError = "Passwords do not match")
                    } else {
                        it.copy(confirmPasswordError = null)
                    }
                }
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

    private fun validateEmail(email: String): Boolean {
        return emailRegex.matches(email)
    }

    private fun registerWithEmail() {
        val stateValue = state.value
        val email = stateValue.email
        val username = stateValue.username
        val password = stateValue.password
        val confirmPassword = stateValue.confirmPassword

        // Reset errors
        _state.update {
            it.copy(
                emailError = null,
                passwordError = null,
                confirmPasswordError = null,
                error = null
            )
        }

        if (email.isBlank() || password.isBlank() || confirmPassword.isBlank() || username.isBlank()) {
            _state.update { it.copy(error = "All fields are required") }
            return
        }

        if (!validateEmail(email)) {
            _state.update { it.copy(emailError = "Invalid email format") }
            return
        }

        if (password.length < 6) {
            _state.update { it.copy(passwordError = "Password must be at least 6 characters") }
            return
        }

        if (password != confirmPassword) {
            _state.update { it.copy(confirmPasswordError = "Passwords do not match") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            signUpWithEmailUseCase(email, password, username)
                .onSuccess {
                    _state.update { it.copy(isLoading = false) }
                    _sideEffect.send(RegisterContract.Effect.NavigateBack)
                }
                .onFailure { error ->
                    val message = error.getFriendlyMessage()
                    _state.update { it.copy(isLoading = false, error = message) }
                    _sideEffect.send(RegisterContract.Effect.ShowError(message))
                }
        }
    }
}
