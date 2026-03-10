package io.lackstudio.omnihub.compose.ui.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
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
import io.lackstudio.omnihub.compose.ui.navigation.XrNavEvent
import io.lackstudio.omnihub.compose.ui.navigation.rememberGalleryNavigator
import io.lackstudio.omnihub.compose.utils.LocalXrNavigation
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

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { },
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
                onItemClick = navigator::navigateToPhoto,
                onUserClick = navigator::navigateToUser,
                onEvent = { event ->
                    logger.d { "UserDetailScreen: onEvent: $event" }
                    viewModel.handleIntent(event)
                },
                onNavigateToFeature = onNavigateToFeature, // Pass it down
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
    val xrNav = LocalXrNavigation.current

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

    Column(modifier = Modifier.fillMaxSize()) {

        // --- Header Section ---
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
            }
        }

        // --- Tab Row ---
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

        // --- Pager Content ---
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
                        onRefresh = { onEvent(UserDetailIntent.Refresh) },
                        isEndOfList = isEndOfList,
                        appendError = currentError,
                        emptyMessage = "No photos uploaded",
                        onLoadMore = { onEvent(UserDetailIntent.LoadMore) },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onItemClick = onItemClick,
                        onUserClick = onUserClick
                    )
                }
                UserTab.Collections -> {
                    GalleryPagedSection(
                        state = state.collectionsState,
                        isRefreshing = isRefreshing,
                        onRefresh = { onEvent(UserDetailIntent.Refresh) },
                        isEndOfList = isEndOfList,
                        appendError = currentError,
                        emptyMessage = "No collections found",
                        onLoadMore = { onEvent(UserDetailIntent.LoadMore) },
                        onItemClick = { item ->
                            onNavigateToFeature(Feature.Collection(item.displayId, item.displayTitle))
                        },
                        onUserClick = { username ->
                            if (xrNav != null) xrNav(XrNavEvent.NavigateToUser(username))
                            else onNavigateToFeature(Feature.User(username))
                        }
                    )
                }

                UserTab.Likes -> {
                    GalleryPagedSection(
                        state = state.likesState,
                        isRefreshing = isRefreshing,
                        onRefresh = { onEvent(UserDetailIntent.Refresh) },
                        isEndOfList = isEndOfList,
                        appendError = currentError,
                        emptyMessage = "No liked photos",
                        onLoadMore = { onEvent(UserDetailIntent.LoadMore) },
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onItemClick = { item ->
                            val ratio = item.displayWidth / item.displayHeight.toFloat()
                            val url = item.displayImageUrl ?: ""
                            if (xrNav != null) xrNav(XrNavEvent.NavigateToPhoto(item.displayId, url, ratio))
                            else onNavigateToFeature(Feature.Photo(item.displayId, url))
                        },
                        onUserClick = { username ->
                            if (xrNav != null) xrNav(XrNavEvent.NavigateToUser(username))
                            else onNavigateToFeature(Feature.User(username))
                        }
                    )
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

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Stats Row
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
    Row(verticalAlignment = Alignment.CenterVertically) {
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
