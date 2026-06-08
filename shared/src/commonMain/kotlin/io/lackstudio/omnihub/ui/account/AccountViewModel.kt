package io.lackstudio.omnihub.ui.account

import androidx.lifecycle.viewModelScope
import io.lackstudio.omnifeed.auth.domain.usecase.DeleteAccountUseCase
import io.lackstudio.omnifeed.auth.domain.usecase.LinkWithGoogleUseCase
import io.lackstudio.omnifeed.auth.domain.usecase.ObserveUserUseCase
import io.lackstudio.omnifeed.auth.domain.usecase.SignOutUseCase
import io.lackstudio.omnifeed.core.common.error.getFriendlyMessage
import io.lackstudio.omnifeed.ui.viewmodel.BaseViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccountViewModel(
    private val observeUserUseCase: ObserveUserUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val linkWithGoogleUseCase: LinkWithGoogleUseCase
) : BaseViewModel() {

    private val _state = MutableStateFlow(AccountContract.State())
    val state = _state.asStateFlow()

    private val _sideEffect = Channel<AccountContract.Effect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()

    init {
        observeUser()
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
        }
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
