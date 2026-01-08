package io.lackstudio.omnihub.ui.gallery

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.lackstudio.omnifeed.ui.state.AppUiState
import io.lackstudio.omnihub.ui.extensions.pagingStaggeredGridItems
import omnihub.composeapp.generated.resources.Res
import omnihub.composeapp.generated.resources.app_name
import omnihub.composeapp.generated.resources.back
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CollectionDetailScreen(
    collectionId: String,
    title: String,
    onBack: () -> Unit,
    onNavigateToPhoto: (String, String) -> Unit,
    viewModel: CollectionViewModel = koinViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(collectionId) {
         viewModel.handleIntent(CollectionDetailIntent.LoadData(collectionId)) // temp id = 8961198
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CollectionDetailContent(
                state = state,
                onNavigateToPhoto = onNavigateToPhoto,
                onLoadMore = {
                    viewModel.handleIntent(CollectionDetailIntent.LoadMorePhotos)
                },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CollectionDetailContent(
    state: CollectionDetailUiState,
    onNavigateToPhoto: (String, String) -> Unit,
    onLoadMore: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    Column(modifier = Modifier.fillMaxSize()) {
        when (val infoState = state.infoState) {
            is AppUiState.Loading, AppUiState.Idle -> {
                // Simple Header Loading placeholder
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
            is AppUiState.Error -> {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        text = "Failed to load info: ${infoState.message}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            is AppUiState.Success -> {
                Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                    CollectionHeader(info = infoState.data)
                }
                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
            }
        }

        val scrollState = rememberLazyStaggeredGridState()

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(minSize = 300.dp), // Adjusted to match Gallery
            modifier = Modifier.fillMaxSize(),
            state = scrollState,
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp
        ) {
            // --- Photo List ---
            when (val photosState = state.photosState) {
                is AppUiState.Loading, AppUiState.Idle -> {
                    // Loading state for initial list load
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is AppUiState.Error -> {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Text("Failed to load photos: ${photosState.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
                is AppUiState.Success -> {
                    val photos = photosState.data
                    if (photos.isEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Box(modifier = Modifier.height(100.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("No photos in this collection", color = Color.Gray)
                            }
                        }
                    } else {
                        // Use paging extension
                        pagingStaggeredGridItems(
                            items = photos,
                            isEndOfList = state.isPhotosEndOfList,
                            onLoadMore = onLoadMore,
                            key = { it.id }
                        ) { photo ->
                            // Convert CollectionPhoto to GalleryDisplayable for GalleryCard use
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

            // Handle Load More indicator (handled inside pagingStaggeredGridItems, or added here)
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
fun CollectionHeader(info: Collection) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // User Info (Collection Creator)
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = info.avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = info.name, // Use Collection's user name
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            // Total Photos count (Optional)
            Text(
                text = "${info.totalPhotos} photos",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant

            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Expandable Description
        val mockDesc =
            "A curated collection of minimalist photography. Less is more. " +
            "This description is long enough to test the expand functionality on mobile devices."

        info.description?.let { desc ->
            ExpandableText(
                text = desc,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ExpandableText(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
    collapsedMaxLines: Int = 1
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isClickable by remember { mutableStateOf(false) }

    Box(modifier = modifier
        .clickable(enabled = isClickable) { isExpanded = !isExpanded }
        .animateContentSize()
    ) {
        Text(
            text = text,
            style = style,
            maxLines = if (isExpanded) Int.MAX_VALUE else collapsedMaxLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { textLayoutResult ->
                // Allow expansion only if text exceeds collapsedMaxLines
                if (!isExpanded && textLayoutResult.hasVisualOverflow) {
                    isClickable = true
                }
            }
        )
    }
}

// Extension to map CollectionPhoto to GalleryDisplayable for GalleryCard
private fun CollectionPhoto.toGalleryDisplayable(): GalleryDisplayable {
    return object : GalleryDisplayable {
        override val displayId: String = id
        override val displayImageUrl: String = url
        override val displayTitle: String = title ?: ""
        override val displayUserAvatar: String? = userProfileImage
        override val displayUsername: String = username
        override val displayLikes: Int = likes
        override val displayCount: Int = 0
        override val displayBlurHash: String = blurhash
        override val displayWidth: Int = width
        override val displayHeight: Int = height
        override val displayPreviewPhotos: List<GalleryPreview> = emptyList()
    }
}
