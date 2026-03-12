package io.lackstudio.omnihub.compose.ui.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.lackstudio.omnifeed.ui.state.AppUiState
import io.lackstudio.omnihub.compose.ui.components.CommonTopBarActions
import io.lackstudio.omnihub.compose.ui.components.ExpandableText
import io.lackstudio.omnihub.compose.ui.components.WebLinkAction
import io.lackstudio.omnihub.compose.ui.navigation.Feature
import io.lackstudio.omnihub.compose.ui.navigation.rememberGalleryNavigator
import io.lackstudio.omnihub.compose.utils.UnsplashLinks
import io.lackstudio.omnihub.compose.utils.logging.rememberLogger
import io.lackstudio.omnihub.compose.utils.toCompactDisplayString
import kotlinx.coroutines.launch
import omnihub.compose.generated.resources.Res
import omnihub.compose.generated.resources.ic_instagram
import omnihub.compose.generated.resources.ic_unsplash
import omnihub.compose.generated.resources.ic_x_twitter
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun UserDetailScreen(
    username: String,
    onBack: () -> Unit,
    onNavigateToFeature: (Feature) -> Unit,
    viewModel: UserViewModel = koinViewModel(),
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val logger = rememberLogger("UserDetailScreen")
    val navigator = rememberGalleryNavigator(onNavigateToFeature)

    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(username) {
        viewModel.handleIntent(UserDetailIntent.LoadData(username))
    }

    val isCurrentTabRefreshing = state.refreshingStatus[state.currentTab] ?: false
    val onRefreshAction = {
        // Only send the event if the current Tab is not refreshing
        if (!isCurrentTabRefreshing) {
            viewModel.handleIntent(UserDetailIntent.Refresh)
        }
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = {
                    Text(
                        text = username,
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
                    CommonTopBarActions(
                        isRefreshing = isCurrentTabRefreshing,
                        onRefresh = onRefreshAction,
                        appendActions = {
                            WebLinkAction(
                                url = UnsplashLinks.userProfile(username),
                                icon = painterResource(Res.drawable.ic_unsplash),
                                contentDescription = "View on Unsplash"
                            )
                        }
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            UserDetailContent(
                state = state,
                onItemClick = navigator::navigateToPhoto,
                onUserClick = navigator::navigateToUser,
                onEvent = { event ->
                    logger.d { "UserDetailScreen: onEvent: $event" }
                    viewModel.handleIntent(event)
                }, // Pass it down
                onNavigateToFeature = onNavigateToFeature,
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
    onItemClick: (GalleryDisplayable) -> Unit,
    onUserClick: (String) -> Unit,
    onEvent: (UserDetailIntent) -> Unit,
    onNavigateToFeature: (Feature) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val tabs = UserTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    val photosGridState = rememberLazyStaggeredGridState()
    val collectionsGridState = rememberLazyStaggeredGridState()
    val likesGridState = rememberLazyStaggeredGridState()

    LaunchedEffect(state.currentTab) {
        if (pagerState.currentPage != state.currentTab.ordinal) {
            pagerState.animateScrollToPage(state.currentTab.ordinal)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val targetTab = UserTab.getByIndex(pagerState.currentPage)
        if (state.currentTab != targetTab) {
            onEvent(UserDetailIntent.SelectTab(targetTab))
        }
    }

    // --- Core: Nested Scroll State and Logic ---
    val density = LocalDensity.current
    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    var totalOverlayHeightPx by remember { mutableFloatStateOf(0f) }
    var headerOffsetPx by remember { mutableFloatStateOf(0f) }

    // Dynamically get the GridState of the currently displayed Tab
    val activeTabGridState by remember {
        derivedStateOf {
            when (UserTab.getByIndex(pagerState.currentPage)) {
                UserTab.Photos -> photosGridState
                UserTab.Collections -> collectionsGridState
                UserTab.Likes -> likesGridState
            }
        }
    }

    // Extract the math logic for calculating Header expand/collapse into a shared core!
    val consumeHeaderDelta: (Float, Boolean) -> Float = { delta, isPostScroll ->
        var consumed = 0f
        if (delta < 0 && headerHeightPx > 0f) {
            // Scroll up: Collapse the Header
            val oldOffset = headerOffsetPx
            val newOffset = (oldOffset + delta).coerceIn(-headerHeightPx, 0f)
            headerOffsetPx = newOffset
            consumed = newOffset - oldOffset
        } else if (delta > 0 && headerHeightPx > 0f) {
            // Scroll down: Expand the Header
            val isAtTop = activeTabGridState.firstVisibleItemIndex == 0 &&
                    activeTabGridState.firstVisibleItemScrollOffset <= 15

            // Check if the Header has left the top (i.e., not fully collapsed)!
            // This variable updates synchronously with no delay.
            val isHeaderNotClosed = headerOffsetPx > -headerHeightPx

            // As long as the Header is even slightly exposed, we must "unconditionally intercept" all downward scroll signals!
            // This ensures that during high-speed scrolling, signals won't leak to the list and be consumed by Overscroll.
            if (isPostScroll || isAtTop || isHeaderNotClosed) {
                val oldOffset = headerOffsetPx
                val newOffset = (oldOffset + delta).coerceIn(-headerHeightPx, 0f)
                headerOffsetPx = newOffset
                consumed = newOffset - oldOffset
            }
        }
        consumed
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Pre-scroll interception
                val consumedY = consumeHeaderDelta(available.y, false)
                return Offset(0f, consumedY)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // Post-scroll safety net
                val consumedY = consumeHeaderDelta(available.y, true)
                return Offset(0f, consumedY)
            }

            // 🔥 Ultimate move: Capture the "inertial momentum (Fling)" after releasing a fast scroll and implement "Auto-Snap"
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (headerHeightPx > 0f) {
                    if (available.y > 0f && headerOffsetPx < 0f) {
                        // 1. Fast scroll down: Residual momentum after the list hits the top -> smoothly pull the Header all the way down
                        animate(
                            initialValue = headerOffsetPx,
                            targetValue = 0f,
                            initialVelocity = available.y
                        ) { value, _ -> headerOffsetPx = value }
                        return Velocity(0f, available.y)

                    } else if (available.y < 0f && headerOffsetPx > -headerHeightPx) {
                        // 2. Fast scroll up: Header not fully collapsed yet -> smoothly collapse the Header entirely
                        animate(
                            initialValue = headerOffsetPx,
                            targetValue = -headerHeightPx,
                            initialVelocity = available.y
                        ) { value, _ -> headerOffsetPx = value }
                        return Velocity(0f, available.y)

                    } else if (headerOffsetPx < 0f && headerOffsetPx > -headerHeightPx) {
                        // 3. Slow scroll and release: No strong momentum, but Header is stuck halfway -> auto-snap to the nearest state
                        val targetOffset = if (headerOffsetPx > -headerHeightPx / 2) 0f else -headerHeightPx
                        animate(
                            initialValue = headerOffsetPx,
                            targetValue = targetOffset
                        ) { value, _ -> headerOffsetPx = value }
                    }
                }
                return super.onPostFling(consumed, available)
            }
        }
    }

    // Bind a dedicated scroll channel for the Header (implementing a complete Pre -> Grid -> Post flow manually)
    val headerScrollState = rememberScrollableState { delta ->
        // a. Pre-scroll: When hovering over the Header, ask the Header to consume the scroll first
        val consumedByHeaderPre = consumeHeaderDelta(delta, false)

        // b. Handover: Pass the remaining delta to the underlying image list
        val leftoverPre = delta - consumedByHeaderPre
        val consumedByGrid = if (leftoverPre != 0f) {
            activeTabGridState.dispatchRawDelta(leftoverPre)
        } else {
            0f
        }

        // c. Post-scroll: If the list can't consume it all (meaning it hit the boundary), ask the Header again! (isPostScroll = true)
        val leftoverPost = leftoverPre - consumedByGrid
        if (leftoverPost != 0f) {
            consumeHeaderDelta(leftoverPost, true)
        }

        // Unconditionally return the delta to trick the OS, keeping the gesture smooth and uninterrupted
        delta
    }

    val isRefreshing = state.refreshingStatus[state.currentTab] ?: false
    SafePullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { onEvent(UserDetailIntent.Refresh) },
        modifier = Modifier.fillMaxSize()
    ) {
        // Use Box to overlap Header/TabRow on top of the list, and attach nestedScroll
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
        ) {
            // ==========================================
            // 1. Bottom layer: Pager and List
            // ==========================================
            // Dynamically calculate the TabRow height to use as bottom compensation
            val tabRowHeightDp = with(density) { (totalOverlayHeightPx - headerHeightPx).toDp() }
            // Declare a Lambda to provide the offset amount, ensuring Recomposition is not triggered during scrolling
            val listOffsetProvider = { totalOverlayHeightPx + headerOffsetPx }

            // Declare an animated scroll-to-top action to smoothly reset the offset to 0f
            val onScrollToTopAction: () -> Unit = {
                coroutineScope.launch {
                    animate(
                        initialValue = headerOffsetPx,
                        targetValue = 0f
                    ) { value, _ ->
                        headerOffsetPx = value
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Top
            ) { pageIndex ->
                val tab = UserTab.getByIndex(pageIndex)
                val isEndOfList = state.endOfListStatus[tab] ?: false
                val isRefreshing = state.refreshingStatus[tab] ?: false
                val currentError = state.appendError[tab]?.takeIf { it.isNotBlank() }

                when (tab) {
                    UserTab.Photos -> {
                        GalleryPagedSection(
                            state = state.photosState,
                            isRefreshing = isRefreshing,
                            onRefresh = null,
                            isEndOfList = isEndOfList,
                            appendError = currentError,
                            emptyMessage = "No photos uploaded",
                            onLoadMore = { onEvent(UserDetailIntent.LoadMore) },
                            contentPaddingTop = 0.dp, // Pass in top Padding so the images are not covered
                            contentPaddingBottom = tabRowHeightDp,
                            listOffsetY = listOffsetProvider,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onItemClick,
                            onUserClick = onUserClick,
                            onScrollToTop = onScrollToTopAction,
                            gridState = photosGridState
                        )
                    }
                    UserTab.Collections -> {
                        GalleryPagedSection(
                            state = state.collectionsState,
                            isRefreshing = isRefreshing,
                            onRefresh = null,
                            isEndOfList = isEndOfList,
                            appendError = currentError,
                            emptyMessage = "No collections found",
                            onLoadMore = { onEvent(UserDetailIntent.LoadMore) },
                            contentPaddingTop = 0.dp,
                            contentPaddingBottom = tabRowHeightDp,
                            listOffsetY = listOffsetProvider,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = { item ->
                                onNavigateToFeature(Feature.Collection(item.displayId, item.displayTitle))
                            },
                            onUserClick = onUserClick,
                            onScrollToTop = onScrollToTopAction,
                            gridState = collectionsGridState
                        )
                    }
                    UserTab.Likes -> {
                        GalleryPagedSection(
                            state = state.likesState,
                            isRefreshing = isRefreshing,
                            onRefresh = null,
                            isEndOfList = isEndOfList,
                            appendError = currentError,
                            emptyMessage = "No liked photos",
                            onLoadMore = { onEvent(UserDetailIntent.LoadMore) },
                            contentPaddingTop = 0.dp,
                            contentPaddingBottom = tabRowHeightDp,
                            listOffsetY = listOffsetProvider,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onItemClick = onItemClick,
                            onUserClick = onUserClick,
                            onScrollToTop = onScrollToTopAction,
                            gridState = likesGridState
                        )
                    }
                }
            }

            // ==========================================
            // 2. Top layer: Floating Header and TabRow
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // Push the entire Header area up based on the scroll state
                    .offset { IntOffset(x = 0, y = headerOffsetPx.roundToInt()) }
                    .background(MaterialTheme.colorScheme.surface) // Add a background color to prevent the underlying images from showing through
                    .onGloballyPositioned { coordinates ->
                        totalOverlayHeightPx = coordinates.size.height.toFloat()
                    }
                    .scrollable(state = headerScrollState, orientation = Orientation.Vertical)
            ) {
                // Header area
                Box(
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        // Only measure the height of the UserHeader here
                        headerHeightPx = coordinates.size.height.toFloat()
                    }
                ) {
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
                            UserHeader(user = infoState.data)
                        }
                    }
                }

                // TabRow area
                // When the Header is completely pushed off the screen, the TabRow will perfectly stick to the top of the screen, creating a Sticky Header effect!
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
                                coroutineScope.launch { pagerState.animateScrollToPage(index) }
                            },
                            text = { Text(tab.title) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserHeader(
    user: UserProfile,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isLargeScreen = maxWidth > 600.dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
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
                    Spacer(modifier = Modifier.height(8.dp))
                    // Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        UserStatItem("Photos", user.totalPhotos)
                        Spacer(modifier = Modifier.width(24.dp))
                        UserStatItem("Collections", user.totalCollections)
                        Spacer(modifier = Modifier.width(24.dp))
                        UserStatItem("Likes", user.totalLikes)

                        if (isLargeScreen) {
                            Spacer(modifier = Modifier.weight(1f))
                            SocialLinkItems(user = user, modifier = Modifier.wrapContentWidth())
                        }
                    }

                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "@${user.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(8.dp))

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

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Bio
            user.bio?.let { bio ->
                ExpandableText(text = bio)
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 4. Social Links
            if (!isLargeScreen) {
                SocialLinkItems(user = user, modifier = modifier)
            }
        }
    }

}

@Composable
fun SocialLinkItems(
    user: UserProfile,
    modifier: Modifier
) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Start
    ) {
        // Unsplash
        SocialLinkButton(
            icon = painterResource(Res.drawable.ic_unsplash),
            text = "Unsplash"
        ) {
            uriHandler.openUri(UnsplashLinks.userProfile(user.username))
        }
        Spacer(modifier = Modifier.width(8.dp))
        // public web
        user.portfolioUrl?.let { portfolioUrl ->
            SocialLinkButton(
                icon = rememberVectorPainter(Icons.Filled.Public),
                text = ""
            ) {
                uriHandler.openUri(portfolioUrl)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        // Instagram
        user.instagramUsername?.let { ig ->
            SocialLinkButton(
                icon = painterResource(Res.drawable.ic_instagram),
                text = ""
            ) {
                uriHandler.openUri("https://instagram.com/$ig")
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        // X/Twitter
        user.twitterUsername?.let { tw ->
            SocialLinkButton(
                icon = painterResource(Res.drawable.ic_x_twitter),
                text = ""
            ) {
                uriHandler.openUri("https://twitter.com/$tw")
            }
        }
    }
}

@Composable
fun UserStatItem(label: String, count: Long) {
    Column (horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toCompactDisplayString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.width(4.dp))
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

// -----------------------------
// Previews
// -----------------------------
private val mockUserProfile = UserProfile(
    id = "u1",
    username = "mock_user",
    name = "Mock User",
    avatarUrl = null,
    bio = "Travel photographer & storyteller. Capturing moments from around the world.",
    location = "Taipei, Taiwan",
    totalLikes = 1204,
    totalPhotos = 45,
    totalCollections = 12,
    instagramUsername = "mock_insta",
    twitterUsername = "mock_tweets",
    portfolioUrl = "https://example.com"
)

private val mockUserPhotos = List(6) { index ->
    MockGalleryDisplayable(
        displayId = "photo_$index",
        displayTitle = "Photo $index",
        displayUsername = "mock_user",
        displayLikes = index * 15
    )
}

private val mockUserCollections = List(6) { index ->
    MockGalleryDisplayable(
        displayId = "collection_$index",
        displayTitle = "collection $index",
        displayUsername = "mock_user",
    )
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Preview(name = "User Content(Mobile) - Success", widthDp = 360, heightDp = 640, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Preview(name = "User Content(Desktop) - Success", widthDp = 1024, heightDp = 768, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun UserDetailContentPreview() {
    MaterialTheme {
        SharedTransitionLayout {
            AnimatedVisibility(visible = true) {
                UserDetailContent(
                    state = UserDetailUiState(
                        infoState = AppUiState.Success(mockUserProfile),
                        photosState = AppUiState.Success(mockUserPhotos),
                        collectionsState = AppUiState.Success(mockUserCollections),
                        likesState = AppUiState.Success(mockUserPhotos),
                        currentTab = UserTab.Photos,
                        endOfListStatus = mapOf(
                            UserTab.Photos to false,
                            UserTab.Collections to false,
                            UserTab.Likes to false
                        )
                    ),
                    onItemClick = {},
                    onUserClick = {},
                    onEvent = {},
                    onNavigateToFeature = {},
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedVisibility
                )
            }
        }
    }
}
