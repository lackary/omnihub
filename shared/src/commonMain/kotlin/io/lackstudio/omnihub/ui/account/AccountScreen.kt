package io.lackstudio.omnihub.ui.account

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.lackstudio.omnifeed.auth.domain.model.AuthProvider
import io.lackstudio.omnifeed.auth.utils.AuthManager
import io.lackstudio.omnihub.platform.rememberPlatformContext
import io.lackstudio.omnihub.ui.components.PasswordTextField
import io.lackstudio.omnihub.ui.components.responsiveDialog
import io.lackstudio.omnihub.utils.Environment
import kotlinx.coroutines.launch
import omnihub.shared.generated.resources.Res
import omnihub.shared.generated.resources.ic_google
import omnihub.shared.generated.resources.ic_unsplash
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AccountScreen(
    onNavigateToLogin: () -> Unit,
    viewModel: AccountViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sideEffect = viewModel.sideEffect
    val authManager: AuthManager = koinInject()
    val scope = rememberCoroutineScope()
    val context = rememberPlatformContext()

    LaunchedEffect(sideEffect) {
        sideEffect.collect { effect ->
            when (effect) {
                AccountContract.Effect.NavigateToLogin -> onNavigateToLogin()
                AccountContract.Effect.ShowDeleteConfirmation -> {
                    /* Handled via state.showDeleteDialog */
                }
                AccountContract.Effect.ShowGoogleSignIn -> {
                    scope.launch {
                        val tokens = authManager.signInWithGoogle(context)
                        if (tokens != null) {
                            viewModel.onGoogleSignInResult(tokens.idToken, tokens.accessToken)
                        } else {
                            // Reset loading state if cancelled
                            // viewModel.onCancel...
                        }
                    }
                }
            }
        }
    }

    AccountScreenContent(
        state = state,
        onEvent = viewModel::handleIntent
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreenContent(
    state: AccountContract.State,
    onEvent: (AccountContract.Event) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Scaffold(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Account",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 400.dp)
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    val user = state.user
                    if (user != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (user.photoUrl != null) {
                                AsyncImage(
                                    model = user.photoUrl,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.size(80.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = user.email ?: "No Email",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = user.username ?: "No Name",
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                    IconButton(
                                        onClick = { onEvent(AccountContract.Event.OnEditUsernameClicked) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Username",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val hasPassword = user.authProviders.containsKey(AuthProvider.PASSWORD.id)
                        Button(
                            onClick = { onEvent(AccountContract.Event.OnEditPasswordClicked) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(if (hasPassword) "Change Password" else "Set Password")
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Google Linking row
                        ServiceLinkRow(
                            label = "Google",
                            icon = Res.drawable.ic_google,
                            isLinked = user.isAuthProviderLinked(AuthProvider.GOOGLE.id),
                            isLoading = state.isLoading && state.loadingSource == AccountContract.LoadingSource.GOOGLE,
                            onLink = { onEvent(AccountContract.Event.OnLinkWithGoogleClicked) }
                        )

                        // Unsplash Linking row
                        ServiceLinkRow(
                            label = "Unsplash",
                            icon = Res.drawable.ic_unsplash,
                            isLinked = user.isCustomServiceLinked(Environment.SERVICE_UNSPLASH),
                            isLoading = state.isLoading && state.loadingSource == AccountContract.LoadingSource.UNSPLASH,
                            canUnlink = true,
                            onLink = { onEvent(AccountContract.Event.OnLinkWithUnsplashClicked) },
                            onUnlink = { onEvent(AccountContract.Event.OnUnlinkUnsplashClicked) }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { onEvent(AccountContract.Event.OnLogoutClicked) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            enabled = !state.isLoading
                        ) {
                            if (state.isLoading && state.loadingSource == AccountContract.LoadingSource.LOGOUT) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Logout")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        state.error?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        TextButton(
                            onClick = { onEvent(AccountContract.Event.OnDeleteAccountClicked) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            enabled = !state.isLoading
                        ) {
                            if (state.isLoading && state.loadingSource == AccountContract.LoadingSource.DELETE) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.error,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Delete Account")
                                }
                            }
                        }
                    } else {
                        Text("Not logged in")
                    }

                    if (state.showEditUsernameDialog) {
                        AlertDialog(
                            onDismissRequest = { onEvent(AccountContract.Event.OnDismissEditUsernameDialog) },
                            modifier = Modifier.responsiveDialog(),
                            properties = DialogProperties(usePlatformDefaultWidth = false),
                            title = { Text("Edit Username") },
                            text = {
                                Column {
                                    OutlinedTextField(
                                        value = state.editUsername,
                                        onValueChange = { onEvent(AccountContract.Event.OnUpdateUsernameChanged(it)) },
                                        label = { Text("Username") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = { onEvent(AccountContract.Event.OnUpdateUsername) },
                                    enabled = state.editUsername.isNotBlank()
                                ) {
                                    Text("Update")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { onEvent(AccountContract.Event.OnDismissEditUsernameDialog) }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    if (state.showEditPasswordDialog) {
                        val isChangePassword = user?.authProviders?.containsKey(AuthProvider.PASSWORD.id) == true
                        AlertDialog(
                            onDismissRequest = { onEvent(AccountContract.Event.OnDismissEditPasswordDialog) },
                            modifier = Modifier.responsiveDialog(),
                            properties = DialogProperties(usePlatformDefaultWidth = false),
                            title = { Text(if (isChangePassword) "Change Password" else "Set Password") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (isChangePassword) {
                                        PasswordTextField(
                                            value = state.editOldPassword,
                                            onValueChange = { onEvent(AccountContract.Event.OnUpdateOldPasswordChanged(it)) },
                                            label = "Old Password",
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    PasswordTextField(
                                        value = state.editNewPassword,
                                        onValueChange = { onEvent(AccountContract.Event.OnUpdateNewPasswordChanged(it)) },
                                        label = "New Password",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    PasswordTextField(
                                        value = state.editConfirmPassword,
                                        onValueChange = { onEvent(AccountContract.Event.OnUpdateConfirmPasswordChanged(it)) },
                                        label = "Confirm Password",
                                        modifier = Modifier.fillMaxWidth(),
                                        imeAction = ImeAction.Done,
                                        onFocusChanged = { focusState ->
                                            if (!focusState.isFocused) {
                                                onEvent(AccountContract.Event.OnConfirmPasswordBlur)
                                            }
                                        },
                                        isError = state.confirmPasswordError != null,
                                        supportingText = state.confirmPasswordError
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = { onEvent(AccountContract.Event.OnUpdatePassword) },
                                    enabled = state.editNewPassword.isNotBlank() && 
                                            state.editConfirmPassword.isNotBlank() && 
                                            state.editNewPassword == state.editConfirmPassword &&
                                            (!isChangePassword || state.editOldPassword.isNotBlank())
                                ) {
                                    Text("Confirm")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { onEvent(AccountContract.Event.OnDismissEditPasswordDialog) }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    if (state.showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { onEvent(AccountContract.Event.OnDismissDeleteDialog) },
                            modifier = Modifier.responsiveDialog(),
                            properties = DialogProperties(usePlatformDefaultWidth = false),
                            title = { Text("Delete Account") },
                            text = { Text("Are you sure you want to delete your account? This action cannot be undone.") },
                            confirmButton = {
                                TextButton(
                                    onClick = { onEvent(AccountContract.Event.OnConfirmDeleteAccount) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Delete")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { onEvent(AccountContract.Event.OnDismissDeleteDialog) }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceLinkRow(
    label: String,
    icon: DrawableResource,
    isLinked: Boolean,
    isLoading: Boolean,
    canUnlink: Boolean = false,
    onLink: () -> Unit,
    onUnlink: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }

        if (isLinked) {
            Button(
                onClick = onUnlink,
                enabled = canUnlink && !isLoading,
                colors = if (canUnlink) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                shape = MaterialTheme.shapes.medium
            ) {
                if (isLoading && canUnlink) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (canUnlink) "Unlink" else "Linked")
                }
            }
        } else {
            Button(
                onClick = onLink,
                enabled = !isLoading,
                shape = MaterialTheme.shapes.medium
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Link")
                }
            }
        }
    }
}
