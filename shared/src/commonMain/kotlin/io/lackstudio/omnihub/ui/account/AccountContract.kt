package io.lackstudio.omnihub.ui.account

import io.lackstudio.omnifeed.auth.domain.model.User

interface AccountContract {
    data class State(
        val user: User? = null,
        val isLoading: Boolean = false,
        val loadingSource: LoadingSource? = null,
        val showDeleteDialog: Boolean = false,
        val error: String? = null
    )

    enum class LoadingSource {
        GOOGLE, UNSPLASH, LOGOUT, DELETE
    }

    sealed interface Event {
        data object OnLogoutClicked : Event
        data object OnDeleteAccountClicked : Event
        data object OnDismissDeleteDialog : Event
        data object OnConfirmDeleteAccount : Event
        data object OnLinkWithGoogleClicked : Event
        data object OnLinkWithUnsplashClicked : Event
        data object OnUnlinkUnsplashClicked : Event
    }

    sealed interface Effect {
        data object NavigateToLogin : Effect
        data object ShowDeleteConfirmation : Effect
        data object ShowGoogleSignIn : Effect
    }
}
