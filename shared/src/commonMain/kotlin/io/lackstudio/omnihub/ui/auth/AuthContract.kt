package io.lackstudio.omnihub.ui.auth

interface AuthContract {
    data class State(
        val isLoading: Boolean = false,
        val email: String = "",
        val password: String = "",
        val confirmPassword: String = "",
        val isRegisterMode: Boolean = false,
        val error: String? = null
    )

    sealed interface Event {
        data class OnEmailChanged(val email: String) : Event
        data class OnPasswordChanged(val password: String) : Event
        data class OnConfirmPasswordChanged(val password: String) : Event
        data object OnToggleMode : Event
        data object OnLoginClicked : Event
        data object OnRegisterClicked : Event
        data object OnGoogleLoginClicked : Event
        data object OnBackClicked : Event
    }

    sealed interface Effect {
        data object NavigateBack : Effect
        data class ShowError(val message: String) : Effect
        data object ShowGoogleSignIn : Effect
    }
}
