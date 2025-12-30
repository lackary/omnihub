package io.lackstudio.omnihub.ui.gallery

import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import io.lackstudio.omnifeed.ui.state.AppUiState
import io.lackstudio.omnihub.platform.isPullToRefreshSupported
import io.lackstudio.omnihub.ui.navigation.Feature
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import omnihub.composeapp.generated.resources.Res
import omnihub.composeapp.generated.resources.back
import omnihub.composeapp.generated.resources.gallery_title
import omnihub.composeapp.generated.resources.refresh
import omnihub.composeapp.generated.resources.search
import org.jetbrains.compose.resources.stringResource
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
    val coroutineScope = rememberCoroutineScope() // Used to handle Tab click animation
    val snackbarHostState = remember { SnackbarHostState() }
    var isSearching by remember { mutableStateOf(false) }

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

    // Init tab index
    val initialPage = remember(state.currentTab) { state.currentTab.ordinal }
    // create pager state
    val pagerState = rememberPagerState(
        initialPage = initialPage, // Initial position
        pageCount = { GalleryTab.entries.size }
    )
    // Detect if the user is "dragging" the Pager with their finger
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
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

    // Create FocusRequester
    val focusRequester = remember { FocusRequester() }
    // Automatically request focus when entering the screen
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Get whether the current Tab is refreshing
    // Since state.refreshingStatus is a Map, if not found, default to false
    val isCurrentTabRefreshing = state.refreshingStatus[state.currentTab] ?: false
    val onRefreshAction = {
        // Only send the event if the current Tab is not refreshing
        if (!isCurrentTabRefreshing) {
            onEvent(GalleryIntent.Refresh)
        }
    }

    Scaffold(
        // Add keyboard listener
        // This allows refreshing by pressing F5 or Ctrl+R when this Screen gains focus
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
            if (keyEvent.type == KeyEventType.KeyDown) {
                //  F5
                if (keyEvent.key == Key.F5) {
                    onRefreshAction()
                    return@onPreviewKeyEvent true
                }
                // Support Ctrl+R (Windows/Linux) or Cmd+R (Mac)
                if (keyEvent.key == Key.R && (keyEvent.isCtrlPressed || keyEvent.isMetaPressed)) {
                    onRefreshAction()
                    return@onPreviewKeyEvent true
                }
            }
            false
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.gallery_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back)
                        )
                    }
                },
                actions = {
                    if (!isPullToRefreshSupported) {
                        IconButton(
                            onClick = { onRefreshAction() },
                            // Disable button while refreshing to prevent multiple clicks
                            enabled = !isCurrentTabRefreshing
                        ) {
                            if (isCurrentTabRefreshing) {
                                // You can change the button to a spinner, or just gray it out
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = stringResource(Res.string.refresh)
                                )
                            }
                        }
                    }

                    IconButton(onClick = {
                        // If search logic is needed here, it can also be passed out via onEvent
                        isSearching = !isSearching
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(Res.string.search)
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
                        isRefreshing = state.refreshingStatus[GalleryTab.Photos] ?: false,
                        onRefresh = { onEvent(GalleryIntent.Refresh) },
                        isEndOfList = state.photosEndOfList,
                        onLoadMore = { onEvent(GalleryIntent.LoadMore) }
                    )
                    GalleryTab.Collections -> CollectionsContent(
                        state = state.collectionsState,
                        isRefreshing = state.refreshingStatus[GalleryTab.Collections] ?: false,
                        onRefresh = { onEvent(GalleryIntent.Refresh) },
                        isEndOfList = state.collectionsEndOfList,
                        onLoadMore = { onEvent(GalleryIntent.LoadMore) }
                    )
                    GalleryTab.Topics -> TopicsContent(
                        state = state.topicsState,
                        isRefreshing = state.refreshingStatus[GalleryTab.Topics] ?: false,
                        onRefresh = { onEvent(GalleryIntent.Refresh) },
                        isEndOfList = state.topicsEndOfList,
                        onLoadMore = { onEvent(GalleryIntent.LoadMore) }
                    )
                }
            }
        }
    }
}

// --- Content Sub-pages (Updated with onLoadMore) ---
@Composable
fun PhotosContent(
    state: AppUiState<List<GalleryPhoto>>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    isEndOfList: Boolean,
    onLoadMore: () -> Unit
) {
    SafePullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (state) {
                is AppUiState.Idle, is AppUiState.Loading -> CircularProgressIndicator()
                is AppUiState.Error -> Text(
                    "Error: ${state.message}",
                    color = MaterialTheme.colorScheme.error
                )

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
}

@Composable
fun CollectionsContent(
    state: AppUiState<List<GalleryCollection>>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    isEndOfList: Boolean,
    onLoadMore: () -> Unit
) {
    SafePullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
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
}

@Composable
fun TopicsContent(
    state: AppUiState<List<GalleryTopic>>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    isEndOfList: Boolean,
    onLoadMore: () -> Unit
) {
    SafePullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (state) {
                is AppUiState.Idle, is AppUiState.Loading -> CircularProgressIndicator()
                is AppUiState.Error -> Text(
                    "Error: ${state.message}",
                    color = MaterialTheme.colorScheme.error
                )

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
