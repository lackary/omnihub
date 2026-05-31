package io.lackstudio.omnihub.ui.account

interface RegisterContract {
    data class State(
        val isLoading: Boolean = false,
        val username: String = "",
        val email: String = "",
        val password: String = "",
        val confirmPassword: String = "",
        val error: String? = null
    )

    sealed interface Event {
        data class OnUsernameChanged(val username: String) : Event
        data class OnEmailChanged(val email: String) : Event
        data class OnPasswordChanged(val password: String) : Event
        data class OnConfirmPasswordChanged(val password: String) : Event
        data object OnRegisterClicked : Event
        data object OnLoginClicked : Event
        data object OnBackClicked : Event
    }

    sealed interface Effect {
        data object NavigateBack : Effect
        data object NavigateToLogin : Effect
        data class ShowError(val message: String) : Effect
    }
}
