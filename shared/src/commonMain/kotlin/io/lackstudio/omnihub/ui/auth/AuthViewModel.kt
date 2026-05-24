package io.lackstudio.omnihub.ui.auth

import androidx.lifecycle.viewModelScope
import io.lackstudio.omnifeed.ui.viewmodel.BaseViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel : BaseViewModel() {

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
            AuthContract.Event.OnLoginClicked -> {
                loginWithEmail()
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
        // Domain and Data layer are not implemented yet, so we just simulate or leave placeholders
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            // TODO: Call LoginWithEmailUseCase
            
            // For now, just stop loading after a delay to show UI works
            kotlinx.coroutines.delay(1000)
            _state.update { it.copy(isLoading = false) }
        }
    }

    private fun loginWithGoogle() {
        // Domain and Data layer are not implemented yet
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            // TODO: Call LoginWithGoogleUseCase
            
            // For now, just stop loading after a delay to show UI works
            kotlinx.coroutines.delay(1000)
            _state.update { it.copy(isLoading = false) }
        }
    }
}
