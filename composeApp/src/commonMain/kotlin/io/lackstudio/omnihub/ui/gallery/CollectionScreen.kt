package io.lackstudio.omnihub.ui.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.lackstudio.omnifeed.ui.state.AppUiState
import io.lackstudio.omnihub.ui.components.ExpandableText
import io.lackstudio.omnihub.ui.navigation.Feature
import io.lackstudio.omnihub.utils.UnsplashLinks
import omnihub.composeapp.generated.resources.Res
import omnihub.composeapp.generated.resources.back
import omnihub.composeapp.generated.resources.ic_unsplash
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CollectionDetailScreen(
    collectionId: String,
    title: String,
    onBack: () -> Unit,
    onNavigateToFeature: (Feature) -> Unit,
    viewModel: CollectionViewModel = koinViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(collectionId) {
         viewModel.handleIntent(CollectionDetailIntent.LoadData(collectionId))
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
                },
                actions = {
                    IconButton(onClick = {
                        uriHandler.openUri(UnsplashLinks.collection(collectionId))
                    }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_unsplash),
                            contentDescription = "View on Unsplash",
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
                onNavigateToPhoto = { id, url ->
                    onNavigateToFeature(Feature.Photo(id, url))
                },
                onNavigateToUser = { username ->
                    onNavigateToFeature(Feature.User(username))
                },
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
    onNavigateToUser: (String) -> Unit,
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
                    CollectionHeader(
                        info = infoState.data,
                        onUserClick = onNavigateToUser
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
            }
        }

        // --- Photo List Section ---
        // Handle state and type conversion.
        // We need to convert AppUiState<List<CollectionPhoto>> to AppUiState<List<GalleryDisplayable>>,
        // or convert the data directly in the Success state.
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
                    // Use the shared PhotoList component.
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
fun CollectionHeader(
    info: Collection,
    onUserClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // User Info (Collection Creator)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.small)
                    .clickable { onUserClick(info.username) }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = info.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )

                    val attributionString = buildAnnotatedString {
                        append("on ")
                        withLink(LinkAnnotation.Url(UnsplashLinks.userProfile(info.username))) {
                            withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                                append("Unsplash")
                            }
                        }
                    }

                    Text(
                        text = attributionString,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Total Photos count (Optional)
            Text(
                text = "${info.totalPhotos} images",
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
            ExpandableText(text = desc)
        }
    }
}

// -----------------------------
// Previews
// -----------------------------
private val mockCollectionInfo = Collection(
    id = "c1",
    title = "Minimalist Winter",
    description = "A curated collection of minimalist winter photography. Snow, ice, and silence.",
    totalPhotos = 42,
    username = "mockuser",
    name = "Mock Curator",
    avatarUrl = null // Preview will show a light gray circle
)

@Preview(name = "Mobile", widthDp = 360, showBackground = true)
@Preview(name = "Desktop", widthDp = 1024, showBackground = true)
@Composable
private fun CollectionHeaderPreview() {
    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            CollectionHeader(
                info = mockCollectionInfo,
                onUserClick = {}
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Preview(name = "Content(Mobile) - Success", widthDp = 360, heightDp = 640, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Preview(name = "Content(Desktop) - Success", widthDp = 1024, heightDp = 768, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun CollectionDetailContentPreview() {

    val mockCollection = Collection(
        id = "c1",
        title = "Minimalist Winter",
        description = "A curated collection of minimalist winter photography. Snow, ice, and silence.",
        totalPhotos = 42,
        username = "mock_curator",
        name = "Mock Curator",
        avatarUrl = null
    )

    val mockPhotos = List(6) { index ->
        MockGalleryDisplayable(
            displayId = "photo_$index",
            displayTitle = "Winter Scene $index",
            displayWidth = 1080,
            displayHeight = 1350, // 4:5 ratio
            displayLikes = index * 42,
            displayUsername = "photographer_$index"
        )
    }

    MaterialTheme {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(mockCollection.title) },
                            navigationIcon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                        )
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        CollectionDetailContent(
                            state = CollectionDetailUiState(
                                infoState = AppUiState.Success(mockCollection),
                                photosState = AppUiState.Success(mockPhotos)
                            ),
                            onNavigateToPhoto = { _, _ -> },
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
@Preview(name = "Content(Mobile) - Loading", widthDp = 360, heightDp = 640, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun CollectionDetailLoadingPreview() {
    MaterialTheme {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                CollectionDetailContent(
                    state = CollectionDetailUiState(
                        infoState = AppUiState.Loading,
                        photosState = AppUiState.Loading
                    ),
                    onNavigateToPhoto = { _, _ -> },
                    onNavigateToUser = {},
                    onLoadMore = {},
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedVisibility
                )
            }
        }
    }
}
