package io.lackstudio.omnihub.ui.gallery

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.lackstudio.omnifeed.ui.state.AppUiState
import io.lackstudio.omnihub.ui.components.ExpandableText
import io.lackstudio.omnihub.ui.navigation.Feature // Remember to import Feature
import io.lackstudio.omnihub.utils.UnsplashLinks
import io.lackstudio.omnihub.utils.toCompactDisplayString
import kotlinx.coroutines.launch
import omnihub.composeapp.generated.resources.Res
import omnihub.composeapp.generated.resources.ic_instagram
import omnihub.composeapp.generated.resources.ic_unsplash
import omnihub.composeapp.generated.resources.ic_x_twitter
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
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
                onEvent = viewModel::handleIntent,
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
    onEvent: (UserDetailIntent) -> Unit,
    onNavigateToFeature: (Feature) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val tabs = UserTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

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

            when (tab) {
                UserTab.Photos -> {
                    UserPhotosSection(
                        state = state.photosState,
                        isEndOfList = isEndOfList,
                        onLoadMore = { onEvent(UserDetailIntent.LoadMore) },
                        onNavigateToFeature = onNavigateToFeature,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                }
                UserTab.Collections -> {
                    UserCollectionsSection(
                        state = state.collectionsState,
                        isEndOfList = isEndOfList,
                        onLoadMore = { onEvent(UserDetailIntent.LoadMore) },
                        onNavigateToFeature = onNavigateToFeature
                    )
                }

                UserTab.Likes -> {
                    UserPhotosSection(
                        state = state.likesState,
                        isEndOfList = isEndOfList,
                        onLoadMore = { onEvent(UserDetailIntent.LoadMore) },
                        onNavigateToFeature = onNavigateToFeature,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun UserPhotosSection(
    state: AppUiState<List<GalleryDisplayable>>,
    isEndOfList: Boolean,
    onLoadMore: () -> Unit,
    onNavigateToFeature: (Feature) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    when (state) {
        is AppUiState.Loading, AppUiState.Idle -> {
            Box(modifier = Modifier.fillMaxSize().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is AppUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("Failed to load photos", color = MaterialTheme.colorScheme.error)
            }
        }
        is AppUiState.Success -> {
            val photos = state.data
            if (photos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("No photos uploaded", color = Color.Gray)
                }
            } else {
                PhotoList(
                    photos = photos,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    isEndOfList = isEndOfList,
                    onLoadMore = onLoadMore,
                    onPhotoClick = { id, url ->
                        onNavigateToFeature(Feature.Photo(id, url))
                    },
                    onUserClick = { username ->
                        onNavigateToFeature(Feature.User(username))
                    }
                )
            }
        }
    }
}

@Composable
fun UserCollectionsSection(
    state: AppUiState<List<GalleryDisplayable>>,
    isEndOfList: Boolean,
    onLoadMore: () -> Unit,
    onNavigateToFeature: (Feature) -> Unit
) {
    when (state) {
        is AppUiState.Loading, AppUiState.Idle -> {
            Box(modifier = Modifier.fillMaxSize().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is AppUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("Failed to load collections", color = MaterialTheme.colorScheme.error)
            }
        }
        is AppUiState.Success -> {
            val collections = state.data
            if (collections.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("No collections found", color = Color.Gray)
                }
            } else {
                CollectionList(
                    collections = collections,
                    isEndOfList = isEndOfList,
                    onLoadMore = onLoadMore,
                    onCollectionClick = { id, title ->
                        onNavigateToFeature(Feature.Collection(id, title))
                    },
                    onUserClick = {}
                )
            }
        }
    }
}

@Composable
fun UserHeader(user: UserProfile) {
    val uriHandler = LocalUriHandler.current

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

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            UserStatItem("Photos", user.totalPhotos)
            UserStatItem("Collections", user.totalCollections)
            UserStatItem("Likes", user.totalLikes)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Bio
        user.bio?.let { bio ->
            ExpandableText(text = bio)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 4. Social Links
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                    onEvent = {},
                    onNavigateToFeature = {},
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedVisibility
                )
            }
        }
    }
}
