package io.lackstudio.omnihub.compose.ui.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import coil3.compose.AsyncImage
import io.lackstudio.omnifeed.ui.state.AppUiState
import io.lackstudio.omnihub.compose.platform.isPullToRefreshSupported
import io.lackstudio.omnihub.compose.ui.components.MonitorErrorStates
import io.lackstudio.omnihub.compose.ui.navigation.Feature
import io.lackstudio.omnihub.compose.ui.navigation.XrNavEvent
import io.lackstudio.omnihub.compose.utils.LocalXrNavigation
import io.lackstudio.omnihub.compose.utils.logging.rememberLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import omnihub.compose.generated.resources.Res
import omnihub.compose.generated.resources.back
import omnihub.compose.generated.resources.gallery_title
import omnihub.compose.generated.resources.refresh
import omnihub.compose.generated.resources.search
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

const val tag = "GalleryScreen"

// Stateful Composable (Used for App navigation)
// Responsible for communicating with Koin and ViewModel
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GalleryScreen(
    onNavigateToFeature: (Feature) -> Unit,
    onBack: () -> Unit,
    viewModel: GalleryViewModel = koinViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val logger = rememberLogger(tag)
    // Collect ViewModel state
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sideEffectFlow = viewModel.sideEffect

    // Triggered when the page becomes "Resume" (visible and interactive)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        logger.d { "App returned to foreground, you can perform desired actions" }
    }

    LaunchedEffect(Unit) {
        logger.d { "Screen Launched" }
    }

    DisposableEffect(Unit) {
        onDispose {
            logger.d { "Screen Disposed" }
        }
    }

    MonitorErrorStates(
        tag = tag,
        "Photos" to state.photosState,
        "Collections" to state.collectionsState,
        "Topics" to state.topicsState,
    )

    // SideEffect Log
    LaunchedEffect(sideEffectFlow) {
        sideEffectFlow.collect { effect ->
            logger.d { "SideEffect: $effect" }
        }
    }

    // Forward state and events to pure UI Composable
    GalleryScreenContent(
        state = state,
        // Pass SideEffect Flow (receive ViewModel one-time events)
        sideEffectFlow = sideEffectFlow,
        onEvent = { event ->
            logger.d { "Event: $event" }
            viewModel.handleIntent(event)

        }, // Pass events back to ViewModel
        onNavigateToFeature = onNavigateToFeature,
        onBack = onBack,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope
    )
}

// Stateless Composable (Pure UI)
// No ViewModel here, purely relies on passed parameters
// This function can be safely called by Preview
@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun GalleryScreenContent(
    state: GalleryUiState,          // Receive pure data state
    sideEffectFlow: Flow<GallerySideEffect>, // Receive onetime event
    onEvent: (GalleryIntent) -> Unit, // Receive event callbacks
    onNavigateToFeature: (Feature) -> Unit,
    onBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    // UI internal State (e.g., Pager, Scroll, Search) can be kept here
    val tabs = GalleryTab.entries
    val coroutineScope = rememberCoroutineScope() // Used to handle Tab click animation
    val snackbarHostState = remember { SnackbarHostState() }
    var isSearching by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    val uriHandler = LocalUriHandler.current
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
                    is GallerySideEffect.OpenUrl -> {
                        uriHandler.openUri(effect.url)
                    }
                }
            }
    }
    val currentTab by rememberUpdatedState(state.currentTab)
    // create pager state
    val pagerState = rememberPagerState(
        initialPage = state.currentTab.ordinal, // Initial position
        pageCount = { GalleryTab.entries.size }
    )

    // Sync Pager state with ViewModel when user swipes
    LaunchedEffect(pagerState) {
        // We listen to both currentPage and isScrollInProgress
        snapshotFlow { pagerState.currentPage }
            .collect { page ->
                val targetTab = GalleryTab.getByIndex(page)
                if (currentTab != targetTab) {
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
                //  Support F5
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
                    IconButton(onClick = {
                        // If search logic is needed here, it can also be passed out via onEvent
                        isSearching = !isSearching
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = stringResource(Res.string.search)
                        )
                    }
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
                    IconButton(
                        onClick = {
                            state.meProfile?.let {
                                onNavigateToFeature(Feature.User(it.username))
                            }?: onEvent(GalleryIntent.Login)
                        }
                    ) {
                        Crossfade(targetState = state.meProfile) { profile ->
                            if (profile != null) {
                                AsyncImage(
                                    model = profile.profileImage.small,
                                    contentDescription = "My Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(32.dp)  // Set avatar display size
                                        .clip(CircleShape) // Clip to circle
                                )
                            } else {
                                if (state.isAuthenticating) {
                                    // If authenticating, show progress indicator instead of an icon
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp), // Same size as the Icon
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
                        onLoadMore = { onEvent(GalleryIntent.LoadMore) },
                        onNavigateToFeature = onNavigateToFeature,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                    GalleryTab.Collections -> CollectionsContent(
                        state = state.collectionsState,
                        isRefreshing = state.refreshingStatus[GalleryTab.Collections] ?: false,
                        onRefresh = { onEvent(GalleryIntent.Refresh) },
                        isEndOfList = state.collectionsEndOfList,
                        onLoadMore = { onEvent(GalleryIntent.LoadMore) },
                        onNavigateToFeature = onNavigateToFeature
                    )
                    GalleryTab.Topics -> TopicsContent(
                        state = state.topicsState,
                        isRefreshing = state.refreshingStatus[GalleryTab.Topics] ?: false,
                        onRefresh = { onEvent(GalleryIntent.Refresh) },
                        isEndOfList = state.topicsEndOfList,
                        onLoadMore = { onEvent(GalleryIntent.LoadMore) },
                        onNavigateToFeature = onNavigateToFeature
                    )
                }
            }
        }
    }
}

// --- Content Sub-pages (Updated with onLoadMore) ---
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhotosContent(
    state: AppUiState<List<GalleryPhoto>>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    isEndOfList: Boolean,
    onLoadMore: () -> Unit,
    onNavigateToFeature: (Feature) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val xrNav = LocalXrNavigation.current
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
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        isEndOfList = isEndOfList,
                        onLoadMore = onLoadMore,
                        onPhotoClick = { id, url, ratio ->
                            if (xrNav != null) {
                                xrNav(XrNavEvent.NavigateToPhoto(id, url, ratio))
                            } else {
                                onNavigateToFeature(Feature.Photo(id, url))
                            }
                        },
                        onUserClick = { id ->
                            onNavigateToFeature(Feature.User(id))
                        }
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
    onLoadMore: () -> Unit,
    onNavigateToFeature: (Feature) -> Unit,
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
                        onLoadMore = onLoadMore,
                        onCollectionClick = { id, title ->
                            onNavigateToFeature(Feature.Collection(id, title))
                        },
                        onUserClick = { id ->
                            onNavigateToFeature(Feature.User(id))
                        }
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
    onLoadMore: () -> Unit,
    onNavigateToFeature: (Feature) -> Unit,
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
                        onLoadMore,
                        onTopicClick = { id, title ->
                            onNavigateToFeature(Feature.Topic(idOrSlug = id, title = title ))
                        }
                    )
                }
            }
        }
    }
}

// Add this function specifically for preview
@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(name = "Mobile", widthDp = 360, heightDp = 640)
@Preview(name = "Desktop", widthDp = 1024, heightDp = 768)
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
                    name = "name1",
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
                    name = "name2",
                    1,
                    "",
                    0,
                    0
                )
            )
        )
    )

    // 1. Create shared transition layout
    SharedTransitionLayout {
        // 2. Create visibility scope (set to true to make it immediately visible)
        AnimatedVisibility(visible = true) {
            GalleryScreenContent(
                state = dummyState,
                sideEffectFlow = emptyFlow(),
                onEvent = {},
                onNavigateToFeature = {},
                onBack = {},
                // 3. Pass the Scope from the environment
                sharedTransitionScope = this@SharedTransitionLayout,
                animatedVisibilityScope = this // this refers to AnimatedVisibilityScope
            )
        }
    }
}
