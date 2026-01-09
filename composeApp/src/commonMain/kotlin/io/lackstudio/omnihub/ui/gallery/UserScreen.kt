package io.lackstudio.omnihub.ui.gallery

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.lackstudio.omnifeed.ui.state.AppUiState
import io.lackstudio.omnihub.ui.components.ExpandableText
import io.lackstudio.omnihub.ui.extensions.pagingStaggeredGridItems
import omnihub.composeapp.generated.resources.Res
import omnihub.composeapp.generated.resources.ic_instagram
import omnihub.composeapp.generated.resources.ic_x_twitter
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun UserDetailScreen(
    username: String,
    onBack: () -> Unit,
    onNavigateToPhoto: (String, String) -> Unit,
    viewModel: UserViewModel = koinViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(username) {
        viewModel.handleIntent(UserDetailIntent.LoadData(username))
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { }, // Title is in the content header
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            UserDetailContent(
                state = state,
                onNavigateToPhoto = onNavigateToPhoto,
                onLoadMore = { viewModel.handleIntent(UserDetailIntent.LoadMorePhotos) },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun UserDetailContent(
    state: UserDetailUiState,
    onNavigateToPhoto: (String, String) -> Unit,
    onLoadMore: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    Column(modifier = Modifier.fillMaxSize()) {

        // --- Header Section (Fixed at top) ---
        when (val infoState = state.infoState) {
            is AppUiState.Loading, AppUiState.Idle -> {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AppUiState.Error -> {
                Text("Error: ${infoState.message}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }
            is AppUiState.Success -> {
                UserHeader(user = infoState.data)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }

        // --- Photo List Section ---
        val scrollState = rememberLazyStaggeredGridState()

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(minSize = 300.dp),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = scrollState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp
        ) {
            when (val photosState = state.photosState) {
                is AppUiState.Loading, AppUiState.Idle -> {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is AppUiState.Error -> {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Text("Failed to load photos", color = MaterialTheme.colorScheme.error)
                    }
                }
                is AppUiState.Success -> {
                    val photos = photosState.data
                    if (photos.isEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Box(modifier = Modifier.height(100.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("No photos uploaded", color = Color.Gray)
                            }
                        }
                    } else {
                        pagingStaggeredGridItems(
                            items = photos,
                            isEndOfList = state.isPhotosEndOfList,
                            onLoadMore = onLoadMore,
                            key = { it.id }
                        ) { photo ->
                            val displayItem = remember(photo) { photo.toGalleryDisplayable() }
                            GalleryCard(
                                item = displayItem,
                                onClick = { onNavigateToPhoto(photo.id, photo.url) },
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }
                    }
                }
            }

            if (state.isPhotosLoadingMore) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun UserHeader(user: UserProfile) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // 1. Avatar & Basic Info
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Location if available
                user.location?.let { loc ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = loc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            UserStatItem("Photos", user.totalPhotos)
            UserStatItem("Collections", user.totalCollections)
            UserStatItem("Likes", user.totalLikes)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Bio
        user.bio?.let { bio ->
            ExpandableText(text = bio, collapsedMaxLines = 2)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 4. Social Links
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            // Instagram (Construct URL)
            user.instagramUsername?.let { ig ->
                SocialLinkButton(icon = painterResource(Res.drawable.ic_instagram), text = "") {
                    uriHandler.openUri("https://instagram.com/$ig")
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            // Twitter (Construct URL)
            user.twitterUsername?.let { tw ->
                SocialLinkButton(icon = painterResource(Res.drawable.ic_x_twitter), text = "") {
                    uriHandler.openUri("https://twitter.com/$tw")
                }
            }
        }
    }
}

@Composable
fun UserStatItem(label: String, count: Long) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SocialLinkButton(icon: Painter, text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
        if (text.isNotBlank()) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

// Extension to map UserPhoto to GalleryDisplayable
private fun UserPhoto.toGalleryDisplayable(): GalleryDisplayable {
    return object : GalleryDisplayable {
        override val displayId: String = id
        override val displayImageUrl: String = url
        override val displayTitle: String = title ?: ""
        override val displayUserAvatar: String? = null // Profile page doesn't show user avatar on each photo
        override val displayUsername: String? = null
        override val displayLikes: Int = likes
        override val displayCount: Int = 0
        override val displayBlurHash: String? = blurhash
        override val displayWidth: Int = width
        override val displayHeight: Int = height
        override val displayPreviewPhotos: List<GalleryPreview> = emptyList()
    }
}
