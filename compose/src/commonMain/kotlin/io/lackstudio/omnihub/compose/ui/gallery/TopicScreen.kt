package io.lackstudio.omnihub.compose.ui.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.lackstudio.omnifeed.ui.state.AppUiState
import io.lackstudio.omnihub.compose.ui.components.ExpandableText
import io.lackstudio.omnihub.compose.ui.navigation.Feature
import io.lackstudio.omnihub.compose.utils.LocalXrNavigation
import io.lackstudio.omnihub.compose.utils.UnsplashLinks
import io.lackstudio.omnihub.compose.utils.logging.rememberLogger
import omnihub.compose.generated.resources.Res
import omnihub.compose.generated.resources.ic_unsplash
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun TopicDetailScreen(
    topicId: String,
    title: String,
    onBack: () -> Unit,
    onNavigateToFeature: (Feature) -> Unit,
    viewModel: TopicViewModel = koinViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val logger = rememberLogger("TopicDetailScreen")
    val uriHandler = LocalUriHandler.current

    val state by viewModel.state.collectAsStateWithLifecycle()
    val xrNavigationOverride = LocalXrNavigation.current

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
                },
                actions = {
                    IconButton(onClick = {
                        logger.d { "View on Unsplash" }
                        uriHandler.openUri(UnsplashLinks.topic(topicId))
                    }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_unsplash),
                            contentDescription = "View on Unsplash"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            TopicDetailContent(
                state = state,
                onNavigateToPhoto = { id, url, ratio ->
                    if (xrNavigationOverride != null) {
                        xrNavigationOverride(id, url, ratio)
                    } else {
                        onNavigateToFeature(Feature.Photo(id, url))
                    }
                },
                onNavigateToUser = { username ->
                    onNavigateToFeature(Feature.User(username))
                },
                onLoadMore = {
                    logger.d { "onLoadMore" }
                    viewModel.handleIntent(TopicDetailIntent.LoadMorePhotos)
                },
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
    onNavigateToPhoto: (String, String, Float) -> Unit,
    onNavigateToUser: (String) -> Unit,
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
                    TopicHeader(
                        topic = infoState.data,
                        onUserClick = onNavigateToUser
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
            }
        }

        // --- Photo List Section ---
        // Handle state and type conversion
        // We need to convert AppUiState<List<TopicPhoto>> to AppUiState<List<GalleryDisplayable>>
        // Or convert data directly upon Success
        when (val photosState = state.photosState) {
            is AppUiState.Loading, AppUiState.Idle -> {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AppUiState.Error -> {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("Failed to load photos", color = MaterialTheme.colorScheme.error)
                }
            }
            is AppUiState.Success -> {
                val photos = photosState.data
                if (photos.isEmpty()) {
                    Box(modifier = Modifier.height(100.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No photos in this topic", color = Color.Gray)
                    }
                } else {
                    // Use the shared PhotoList
                    PhotoList(
                        photos = photos,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        isEndOfList = state.isPhotosEndOfList,
                        onLoadMore = onLoadMore,
                        onPhotoClick = onNavigateToPhoto,
                        onUserClick = onNavigateToUser
                    )
                }
            }
        }
    }
}

@Composable
fun TopicHeader(
    topic: Topic,
    onUserClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Dropdown Menu for Top Contributors
        if (topic.contributors.isNotEmpty()) {
            ContributorsDropdown(
                contributors = topic.contributors,
                onUserClick = onUserClick
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Description
        topic.description?.let { desc ->
            ExpandableText(text = desc)
        }
    }
}

@Composable
fun ContributorsDropdown(
    contributors: List<TopicContributor>,
    onUserClick: (String) -> Unit
) {
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
                        expanded = false
                        onUserClick(user.username)
                    }
                )
            }
        }
    }
}

// -----------------------------
// Previews
// -----------------------------

// Mock Data Helpers
private val mockContributors = listOf(
    TopicContributor(username = "alice_photo", name = "Alice Lens", avatarUrl = null),
    TopicContributor(username = "bob_art", name = "Bob Artist", avatarUrl = null)
)

private val mockTopic = Topic(
    id = "topic_1",
    title = "Wallpapers",
    description = "From epic drone shots to inspiring moments in nature.",
    contributors = mockContributors,
)

@Preview(name = "Header", showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun TopicHeaderPreview() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TopicHeader(
                topic = mockTopic,
                onUserClick = {}
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Preview(name = "Content(Mobile) - Success", widthDp = 360, heightDp = 640, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Preview(name = "Content(Desktop) - Success", widthDp = 1024, heightDp = 768, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun TopicDetailContentPreview() {

    val mockPhotos = List(6) { index ->
        MockGalleryDisplayable(
            displayId = "photo_$index",
            displayTitle = "Wallpaper $index",
            displayWidth = 1080,
            displayHeight = 1920,
            displayLikes = index * 88,
            displayUsername = "creator_$index"
        )
    }

    MaterialTheme {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(mockTopic.title) },
                            navigationIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                        )
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        TopicDetailContent(
                            state = TopicDetailUiState(
                                infoState = AppUiState.Success(mockTopic),
                                photosState = AppUiState.Success(mockPhotos),
                                isPhotosEndOfList = false
                            ),
                            onNavigateToPhoto = { _, _, _-> },
                            onNavigateToUser = {},
                            onLoadMore = {},
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedVisibility
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(name = "Content - Loading", widthDp = 360, heightDp = 640, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun TopicDetailLoadingPreview() {
    MaterialTheme {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                TopicDetailContent(
                    state = TopicDetailUiState(
                        infoState = AppUiState.Loading,
                        photosState = AppUiState.Loading,
                        isPhotosEndOfList = false
                    ),
                    onNavigateToPhoto = { _, _, _ -> },
                    onNavigateToUser = {},
                    onLoadMore = {},
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedVisibility
                )
            }
        }
    }
}
