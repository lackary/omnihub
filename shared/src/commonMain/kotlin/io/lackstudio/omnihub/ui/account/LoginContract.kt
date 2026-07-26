package io.lackstudio.omnihub.ui.account

interface LoginContract {
    data class State(
        val isLoading: Boolean = false,
        val loadingSource: LoadingSource? = null,
        val email: String = "",
        val password: String = "",
        val error: String? = null
    )

    enum class LoadingSource {
        EMAIL, GOOGLE, UNSPLASH
    }

    sealed interface Event {
        data class OnEmailChanged(val email: String) : Event
        data class OnPasswordChanged(val password: String) : Event
        data object OnLoginClicked : Event
        data object OnGoogleLoginClicked : Event
        data object OnUnsplashLoginClicked : Event
        data object OnSignUpClicked : Event
        data object OnBackClicked : Event
    }

    sealed interface Effect {
        data object LoginSuccess : Effect
        data object NavigateBack : Effect
        data object NavigateToRegister : Effect
        data class ShowError(val message: String) : Effect
        data object ShowGoogleSignIn : Effect
    }
}
