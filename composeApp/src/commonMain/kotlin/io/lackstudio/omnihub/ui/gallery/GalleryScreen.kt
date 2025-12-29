package io.lackstudio.omnihub.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import coil3.compose.AsyncImage
import com.brys.compose.blurhash.BlurHashImage
import io.lackstudio.omnifeed.ui.state.AppUiState
import io.lackstudio.omnihub.ui.extensions.pagingGridItems
import io.lackstudio.omnihub.ui.extensions.pagingItems
import io.lackstudio.omnihub.ui.navigation.Feature
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

// Stateful Composable (Used for App navigation)
// Responsible for communicating with Koin and ViewModel
@Composable
fun GalleryScreen(
    onNavigateToFeature: (Feature) -> Unit,
    onBack: () -> Unit,
    viewModel: GalleryViewModel = koinViewModel()
) {
    // Triggered when the page becomes "Resume" (visible and interactive)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        println("App returned to foreground, you can perform desired actions")
    }

    // Collect ViewModel state
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Forward state and events to pure UI Composable
    GalleryScreenContent(
        state = state,
        // Pass SideEffect Flow (receive ViewModel one-time events)
        sideEffectFlow = viewModel.sideEffect,
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
    sideEffectFlow: Flow<GallerySideEffect>, // Receive onetime event
    onEvent: (GalleryIntent) -> Unit, // Receive event callbacks
    onNavigateToFeature: (Feature) -> Unit,
    onBack: () -> Unit
) {
    // UI internal State (e.g., Pager, Scroll, Search) can be kept here
    val tabs = GalleryTab.entries
    val initialPage = remember(state.currentTab) { state.currentTab.ordinal }
    val pagerState = rememberPagerState(
        initialPage = initialPage, // Initial position
        pageCount = { GalleryTab.entries.size }
    )
    val coroutineScope = rememberCoroutineScope() // Used to handle Tab click animation
    val snackbarHostState = remember { SnackbarHostState() }
    var isSearching by remember { mutableStateOf(false) }
    // Detect if the user is "dragging" the Pager with their finger
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()

    val lifecycleOwner = LocalLifecycleOwner.current

    // Observe SideEffect and ensure Snackbar events are received only when in foreground
    // Use flowWithLifecycle to bind lifecycle
    // Safety: When the App goes to background (STOPPED), collection is automatically "paused" to avoid wasting resources on UI actions.
    // Auto-resume: When the App returns to foreground (STARTED), it automatically "continues" collecting previously buffered events.
    // State selection: Use STARTED to mean "receive as long as the user can see the App" (including split-screen, multi-window mode).
    // (If RESUMED is used, the App without focus in split-screen won't receive messages, leading to poor UX)
    LaunchedEffect(sideEffectFlow, lifecycleOwner) {
        sideEffectFlow
            .flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .collect { effect ->
            when (effect) {
                is GallerySideEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = effect.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    // Listen to Pager (swipe -> update VM)
    LaunchedEffect(pagerState) {
        // We listen to both currentPage and isScrollInProgress
        snapshotFlow { pagerState.currentPage }
            .collect { page ->
                val targetTab = GalleryTab.getByIndex(page)
                if (state.currentTab != targetTab) {
                    onEvent(GalleryIntent.SelectTab(targetTab))
                }
            }
    }

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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
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
                        },
                        text = { Text(tab.title) },
                        icon = { Icon(tab.icon, contentDescription = null) }
                    )
                }
            }

            // It handles gesture detection and shows the spinner when isRefreshing = true
            PullToRefreshBox(
                isRefreshing = state.isRefreshing, // From ViewModel
                onRefresh = { onEvent(GalleryIntent.Refresh) }, // Trigger ViewModel action
                modifier = Modifier.weight(1f)
            ) {
                // Pager Content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top, // Ensure content starts from the top
                    // Keep the state of 1 page on each side to avoid resetting scroll position when switching
                    beyondViewportPageCount = 1
                ) { pageIndex ->
                    when (tabs[pageIndex]) {
                        GalleryTab.Photos -> PhotosContent(
                            state = state.photosState,
                            isEndOfList = state.photosEndOfList,
                            onLoadMore = { onEvent(GalleryIntent.LoadMore) }
                        )
                        GalleryTab.Collections -> CollectionsContent(
                            state = state.collectionsState,
                            isEndOfList = state.collectionsEndOfList,
                            onLoadMore = { onEvent(GalleryIntent.LoadMore) }
                        )
                        GalleryTab.Topics -> TopicsContent(
                            state = state.topicsState,
                            isEndOfList = state.topicsEndOfList,
                            onLoadMore = { onEvent(GalleryIntent.LoadMore) }
                        )
                    }
                }
            }
        }
    }
}

// --- Content Sub-pages (Updated with onLoadMore) ---
@Composable
fun PhotosContent(
    state: AppUiState<List<GalleryPhoto>>,
    isEndOfList: Boolean,
    onLoadMore: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (state) {
            is AppUiState.Idle, is AppUiState.Loading -> CircularProgressIndicator()
            is AppUiState.Error -> Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
            is AppUiState.Success -> {
                if (state.data.isEmpty()) Text("No photos found.")
                else PhotoList(
                    state.data,
                    isEndOfList = isEndOfList,
                    onLoadMore
                )
            }
        }
    }
}

@Composable
fun CollectionsContent(
    state: AppUiState<List<GalleryCollection>>,
    isEndOfList: Boolean,
    onLoadMore: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (state) {
            is AppUiState.Idle, is AppUiState.Loading -> CircularProgressIndicator()
            is AppUiState.Error -> Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
            is AppUiState.Success -> {
                if (state.data.isEmpty()) Text("No collections found.")
                else CollectionList(
                    state.data,
                    isEndOfList = isEndOfList,
                    onLoadMore
                )
            }
        }
    }
}

@Composable
fun TopicsContent(
    state: AppUiState<List<GalleryTopic>>,
    isEndOfList: Boolean,
    onLoadMore: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (state) {
            is AppUiState.Idle, is AppUiState.Loading -> CircularProgressIndicator()
            is AppUiState.Error -> Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
            is AppUiState.Success -> {
                if (state.data.isEmpty()) Text("No topics found.")
                else TopicList(
                    state.data,
                    isEndOfList = isEndOfList,
                    onLoadMore
                )
            }
        }
    }
}

// --- List Components ---
@Composable
fun PhotoList(
    photos: List<GalleryPhoto>,
    isEndOfList: Boolean,
    onLoadMore: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        pagingItems(
            items = photos,
            isEndOfList = isEndOfList,
            onLoadMore = onLoadMore,
            key = { it.id } // Recommended to add id to improve LazyColumn performance
        ) { photo ->
            GalleryCard(photo)
        }
    }
}

@Composable
fun CollectionList(
    collections: List<GalleryCollection>,
    isEndOfList: Boolean,
    onLoadMore: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        pagingItems(
            items = collections,
            isEndOfList = isEndOfList,
            onLoadMore = onLoadMore,
            key = { it.id }
        ) { collection ->
            GalleryCard(collection)
        }
    }
}

@Composable
fun TopicList(
    topics: List<GalleryTopic>,
    isEndOfList: Boolean,
    onLoadMore: () -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        pagingGridItems(
            items = topics,
            isEndOfList = isEndOfList,
            onLoadMore = onLoadMore,
            key = { topic -> topic.id }
        ) { topic ->
            TopicCard(topic = topic)
        }
    }
}

@Composable
fun GalleryCard(item: GalleryDisplayable) {
    val aspectRatio = remember(item.displayWidth, item.displayHeight) {
        val w = item.displayWidth ?: 0
        val h = item.displayHeight ?: 0
        if (h > 0 && w > 0) {
            w.toFloat() / h.toFloat()
        } else {
            1f // Default aspect ratio to avoid crash due to division by zero
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentHeight()
        ) {
            if (LocalInspectionMode.current) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .height(200.dp)
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Image Preview", color = Color.DarkGray)
                }
            } else {

                // Place BlurHash at the bottom
                item.displayBlurHash?.let { hash ->
                    BlurHashImage(
                        hash = hash,
                        contentDescription = "blur hash",
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                // Place AsyncImage on top (it will cover the bottom layer after loading)
                AsyncImage(
                    model = item.displayImageUrl,
                    contentDescription = item.displayTitle,
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                    contentScale = ContentScale.Crop
                )
            }

            // Gradient shadow (makes text visible on light images)
            Box(
                modifier = Modifier
                    .matchParentSize() // Match parent size
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                            startY = 400f // Adjust the starting position of the gradient
                        )
                    )
            )

            // User information at bottom left (Avatar + Username)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart) // Align bottom start
                    .padding(12.dp), // Leave some padding
                verticalAlignment = Alignment.CenterVertically
            ) {
                // User Avatar
                if (LocalInspectionMode.current) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Gray)
                    )
                } else {
                    AsyncImage(
                        model = item.displayUserAvatar,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(32.dp)            // Set size
                            .clip(CircleShape)            // Clip to circle
                            .background(Color.LightGray), // Background color before loading
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(8.dp)) // Spacing

                // Username
                Text(
                    text = item.displayUsername?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White, // Use white text because of the black gradient background
                    fontWeight = FontWeight.SemiBold
                )
            }

            // the count at top right for collections (icon + number)
            if (item.displayCount > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd) // Align top end
                        .padding(8.dp), // Padding
                    shape = RoundedCornerShape(12.dp), // Rounded capsule shape
                    color = Color.Black.copy(alpha = 0.6f),  // Semi-transparent black background
                    contentColor = Color.White  // White text
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Add a small icon, e.g., stack icon
                        Icon(
                            imageVector = Icons.Filled.FilterNone, // Or PhotoLibrary
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))

                        // Count
                        Text(
                            text = item.displayCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // the likes at bottom right for photos (number + icon)
            if (item.displayLikes > 0) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd) // Align bottom left
                        .padding(12.dp),       // Keep same padding as User Info on the left
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    // Number
                    Text(
                        text = item.displayLikes.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Heart Icon
                    Icon(
                        imageVector = Icons.Filled.Favorite, // Or use Icons.Default.Favorite (Filled)
                        contentDescription = "Likes",
                        modifier = Modifier.size(16.dp), // Moderate size
                        tint = Color.White // White, because of the black gradient background
                    )
                }
            }
        }
    }
}

@Composable
fun TopicCard(topic: GalleryTopic) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp), // Limit height (adjust dp as needed)
        shape = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center // Title displayed in the center
        ) {

            topic.blurhash?.let { hash ->
                BlurHashImage(
                    hash = hash,
                    contentDescription = "",
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Background image
            AsyncImage(
                // Please confirm your GalleryTopic data structure field name (e.g. topic.coverUrl or topic.urls.small)
                model = topic.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Black semi-transparent overlay (makes white text clearer)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            )

            // Title text
            Text(
                text = topic.title,
                color = Color.White,
                fontWeight = FontWeight.Bold, // 4. Bold text
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

// Add this function specifically for preview
@Preview
@Composable
fun GalleryScreenPreview() {
    // Create dummy data
    val dummyState = GalleryUiState(
        photosState = AppUiState.Success(
            data = listOf(
                GalleryPhoto(
                    "1",
                    "https://picsum.photos/seed/photo0/300/400",
                    "Preview Photo 1",
                    "",
                    "username1",
                    1,
                    "",
                    0,
                    0),
                GalleryPhoto(
                    "2",
                    "https://picsum.photos/seed/photo1/300/400",
                    "Preview Photo 2",
                    "",
                    "username2",
                    1,
                    "",
                    0,
                    0
                )
            )
        )
    )

    GalleryScreenContent(
        state = dummyState, // Pass in dummy data
        sideEffectFlow = emptyFlow(),
        onEvent = {},       // Empty event handling
        onNavigateToFeature = {},
        onBack = {}
    )
}
