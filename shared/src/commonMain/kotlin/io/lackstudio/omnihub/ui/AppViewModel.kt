package io.lackstudio.omnihub.ui

import androidx.lifecycle.viewModelScope
import io.lackstudio.omnifeed.auth.domain.model.User
import io.lackstudio.omnifeed.auth.domain.usecase.ObserveUserUseCase
import io.lackstudio.omnifeed.ui.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AppViewModel(
    observeUserUseCase: ObserveUserUseCase
) : BaseViewModel() {
    val user: StateFlow<User?> = observeUserUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val isLoggedIn: StateFlow<Boolean> = user
        .map { it != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
}
