package io.lackstudio.omnihub.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.lackstudio.omnihub.platform.isPullToRefreshSupported
import omnihub.shared.generated.resources.Res
import omnihub.shared.generated.resources.search
import org.jetbrains.compose.resources.stringResource

@Composable
fun RowScope.CommonTopBarActions(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    prependActions: @Composable RowScope.() -> Unit = {},
    appendActions: @Composable RowScope.() -> Unit = {}
) {
    // Prepend actions area (e.g., Search)
    prependActions()

    // Universal action: Refresh button
    if (!isPullToRefreshSupported) {
        RefreshAction(
            isRefreshing = isRefreshing,
            onClick = onRefresh
        )
    }

    // Append actions area (e.g., WebLink, Avatar)
    appendActions()
}

@Composable
fun SearchAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = stringResource(Res.string.search)
        )
    }
}

@Composable
fun RefreshAction(
    isRefreshing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        enabled = !isRefreshing,
        modifier = modifier
    ) {
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onSurface
            )
        } else {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh"
            )
        }
    }
}

@Composable
fun WebLinkAction(
    url: String,
    icon: Painter,
    contentDescription: String = "View on Web",
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    IconButton(onClick = { uriHandler.openUri(url) }, modifier = modifier) {
        Icon(
            painter = icon,
            contentDescription = contentDescription,
        )
    }
}

@Composable
fun AvatarAction(
    avatarUrl: String?,
    isAuthenticating: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Crossfade(targetState = avatarUrl, label = "AvatarCrossfade") { currentUrl ->
            if (currentUrl != null) {
                AsyncImage(
                    model = currentUrl,
                    contentDescription = "My Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                )
            } else {
                if (isAuthenticating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = "Login"
                    )
                }
            }
        }
    }
}
