package io.lackstudio.omnihub.ui.account

import io.lackstudio.omnifeed.auth.domain.model.User

interface AccountContract {
    data class State(
        val user: User? = null,
        val isLoading: Boolean = false,
        val loadingSource: LoadingSource? = null,
        val showDeleteDialog: Boolean = false,
        val showEditUsernameDialog: Boolean = false,
        val editUsername: String = "",
        val editNewPassword: String = "",
        val editConfirmPassword: String = "",
        val confirmPasswordError: String? = null,
        val isPasswordSectionExpanded: Boolean = false,
        val showReAuthDialog: Boolean = false,
        val reAuthType: ReAuthType? = null,
        val reAuthPassword: String = "",
        val reAuthError: String? = null,
        val error: String? = null
    )

    enum class LoadingSource {
        GOOGLE, UNSPLASH, LOGOUT, DELETE, UPDATE_USERNAME, UPDATE_PASSWORD, REAUTH
    }

    enum class ReAuthType {
        EMAIL, GOOGLE, CUSTOM_SERVICE
    }

    sealed interface Event {
        data object OnLogoutClicked : Event
        data object OnDeleteAccountClicked : Event
        data object OnDismissDeleteDialog : Event
        data object OnConfirmDeleteAccount : Event
        data object OnLinkWithGoogleClicked : Event
        data object OnLinkWithUnsplashClicked : Event
        data object OnUnlinkUnsplashClicked : Event
        data object OnEditUsernameClicked : Event
        data class OnUpdateUsernameChanged(val username: String) : Event
        data class OnUpdateNewPasswordChanged(val password: String) : Event
        data class OnUpdateConfirmPasswordChanged(val password: String) : Event
        data object OnUpdateUsername : Event
        data object OnUpdatePassword : Event
        data object OnConfirmPasswordBlur : Event
        data object OnDismissEditUsernameDialog : Event
        data object OnTogglePasswordSection : Event
        data class OnReAuthPasswordChanged(val password: String) : Event
        data object OnConfirmReAuth : Event
        data object OnDismissReAuthDialog : Event
    }

    sealed interface Effect {
        data object NavigateToLogin : Effect
        data object ShowDeleteConfirmation : Effect
        data object ShowGoogleSignIn : Effect
    }
}
