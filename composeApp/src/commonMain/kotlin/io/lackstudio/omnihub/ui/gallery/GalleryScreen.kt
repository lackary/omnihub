package io.lackstudio.omnihub.ui.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Topic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.lackstudio.omnihub.ui.navigation.Feature
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

// Define Tab Enum (place at the top of the file or in a separate file)
enum class PhotoTab(
    val title: String,
    val icon: ImageVector
) {
    List("Photos", Icons.Filled.PhotoLibrary),
    Collections("Collections", Icons.Filled.PhotoAlbum),
    Topics("Topics", Icons.Filled.Topic)
}

// Stateful Composable (Used for App navigation)
// Responsible for communicating with Koin and ViewModel
@Composable
fun GalleryScreen(
    onNavigateToFeature: (Feature) -> Unit,
    onBack: () -> Unit,
    viewModel: GalleryViewModel = koinViewModel()
) {
    // Collect ViewModel state
    val state by viewModel.state.collectAsState()

    // Forward state and events to pure UI Composable
    GalleryScreenContent(
        state = state,
        onEvent = viewModel::handleIntent, // Pass events back to ViewModel
        onNavigateToFeature = onNavigateToFeature,
        onBack = onBack
    )
}

// Stateless Composable (Pure UI)
// No ViewModel here, purely relies on passed parameters
// This function can be safely called by Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreenContent(
    state: GalleryUiState,          // Receive pure data state
    onEvent: (GalleryIntent) -> Unit, // Receive event callbacks
    onNavigateToFeature: (Feature) -> Unit,
    onBack: () -> Unit
) {
    // UI internal State (e.g., Pager, Scroll, Search) can be kept here
    val tabs = PhotoTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    var isSearching by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gallery") },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // If search logic is needed here, it can also be passed out via onEvent
                        isSearching = !isSearching
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search"
                        )
                    }
                },
            )
        },
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // TabRow logic
            PrimaryTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = { HorizontalDivider() }
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                            // If ViewModel needs to be notified of Tab changes:
                            // onEvent(GalleryIntent.SelectTab(index))
                        },
                        text = { Text(tab.title) },
                        icon = { Icon(tab.icon, contentDescription = null) }
                    )
                }
            }

            // Pager Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) { pageIndex ->
                when (tabs[pageIndex]) {
                    PhotoTab.List -> PhotosContent(state = state.photosState)
                    PhotoTab.Collections -> CollectionsContent(state = state.collectionsState)
                    PhotoTab.Topics -> TopicsContent(state = state.topicsState)
                }
            }
        }
    }
}

// --- Temporary sub-page Placeholders below (recommended to split into separate files later) ---

// Display photo list
@Composable
fun PhotosContent(state: PhotosState) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(state.items) { photo ->
                    println("photo url: ${photo.url}")
                    // Wrap with Card to make each item look like a card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Image placed at the top
                            AsyncImage(
                                model = photo.url,
                                contentDescription = photo.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight() // Height automatically adjusts to image aspect ratio
                                // Note: Card already has rounded corners, so no clip needed here unless targeting specific edges
                                ,
                                contentScale = ContentScale.FillWidth
                            )

                            // Title placed below the image
                            Text(
                                text = photo.title,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(16.dp) // Give text some padding
                            )
                        }
                    }
                }
            }
        }
    }
}

// Display collection list
@Composable
fun CollectionsContent(state: CollectionsState) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(state.items) { collection ->
                    println("collection url: ${collection.coverUrl}")
                    ListItem(
                        headlineContent = { Text(collection.title) },
                        supportingContent = { Text("${collection.totalPhotos} photos") },
                        leadingContent = {
                            // 👇 Use Coil to display cover image
                            AsyncImage(
                                model = collection.coverUrl,
                                contentDescription = collection.title,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    )
                    HorizontalDivider() // Add a divider
                }
            }
        }
    }
}

// Display topic list
@Composable
fun TopicsContent(state: TopicsState) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(state.items) { topic ->
                    println("topic url: ${topic.coverUrl}")
                    ListItem(
                        headlineContent = { Text(topic.title) },
                        supportingContent = { Text(topic.description, maxLines = 1) },
                        leadingContent = {
                            // 👇 Use Coil to display wide image
                            AsyncImage(
                                model = topic.coverUrl,
                                contentDescription = topic.title,
                                modifier = Modifier
                                    .size(80.dp, 56.dp) // Wider aspect ratio
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    )
                }
            }
        }
    }
}

// Add this function specifically for preview
@Preview
@Composable
fun GalleryScreenPreview() {
    // Create dummy data
    val dummyState = GalleryUiState(
        photosState = PhotosState(
            isLoading = false,
            items = listOf(
                GalleryPhoto("1", "https://picsum.photos/seed/photo0/300/400", "Preview Photo 1"),
                GalleryPhoto("2", "https://picsum.photos/seed/photo1/300/400", "Preview Photo 2")
            )
        )
    )

    GalleryScreenContent(
        state = dummyState, // Pass in dummy data
        onEvent = {},       // Empty event handling
        onNavigateToFeature = {},
        onBack = {}
    )
}
