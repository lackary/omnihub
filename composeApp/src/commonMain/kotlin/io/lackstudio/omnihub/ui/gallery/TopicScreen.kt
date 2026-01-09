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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.lackstudio.omnifeed.ui.state.AppUiState
import io.lackstudio.omnihub.ui.components.ExpandableText
import io.lackstudio.omnihub.ui.extensions.pagingStaggeredGridItems
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TopicDetailScreen(
    topicId: String,
    title: String,
    onBack: () -> Unit,
    onNavigateToPhoto: (String, String) -> Unit,
    viewModel: TopicViewModel = koinViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(topicId) {
        viewModel.handleIntent(TopicDetailIntent.LoadData(topicId))
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TopicDetailContent(
                state = state,
                onNavigateToPhoto = onNavigateToPhoto,
                onLoadMore = { viewModel.handleIntent(TopicDetailIntent.LoadMorePhotos) },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TopicDetailContent(
    state: TopicDetailUiState,
    onNavigateToPhoto: (String, String) -> Unit,
    onLoadMore: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    Column(modifier = Modifier.fillMaxSize()) {

        // --- Header Section (Fixed at the top) ---
        when (val infoState = state.infoState) {
            is AppUiState.Loading, AppUiState.Idle -> {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AppUiState.Error -> {
                Text("Error: ${infoState.message}", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }
            is AppUiState.Success -> {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    TopicHeader(topic = infoState.data)
                }
                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
            }
        }

        // --- Photo List Section ---
        val scrollState = rememberLazyStaggeredGridState()

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Adaptive(minSize = 300.dp),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = scrollState,
            contentPadding = PaddingValues(8.dp),
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
                                Text("No photos in this topic", color = Color.Gray)
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
fun TopicHeader(topic: Topic) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 2. Dropdown Menu for Top Contributors
        if (topic.contributors.isNotEmpty()) {
            ContributorsDropdown(contributors = topic.contributors)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 3. Description
        topic.description?.let { desc ->
            ExpandableText(
                text = desc,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ContributorsDropdown(contributors: List<TopicContributor>) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        // Dropdown Trigger Button
        OutlinedButton(
            onClick = { expanded = true },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Top Contributors",
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }

        // The Menu
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            contributors.forEach { user ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(text = user.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(text = "@${user.username}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    },
                    leadingIcon = {
                        AsyncImage(
                            model = user.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray),
                            contentScale = ContentScale.Crop
                        )
                    },
                    onClick = {
                        // TODO: Handle user click if needed (e.g., navigate to user profile)
                        expanded = false
                    }
                )
            }
        }
    }
}

// Extension to map TopicPhoto to GalleryDisplayable
private fun TopicPhoto.toGalleryDisplayable(): GalleryDisplayable {
    return object : GalleryDisplayable {
        override val displayId: String = id
        override val displayImageUrl: String = url
        override val displayTitle: String = title ?: ""
        override val displayUserAvatar: String? = userProfileImage
        override val displayUsername: String = username
        override val displayLikes: Int = likes
        override val displayCount: Int = 0
        override val displayBlurHash: String? = blurhash
        override val displayWidth: Int = width
        override val displayHeight: Int = height
        override val displayPreviewPhotos: List<GalleryPreview> = emptyList()
    }
}
