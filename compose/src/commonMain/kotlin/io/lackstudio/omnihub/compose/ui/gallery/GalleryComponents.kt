package io.lackstudio.omnihub.compose.ui.gallery

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Size
import com.brys.compose.blurhash.BlurHashImage
import io.lackstudio.omnihub.compose.platform.isPullToRefreshSupported // Variable defined recently
import io.lackstudio.omnihub.compose.ui.extensions.pagingGridItems
import io.lackstudio.omnihub.compose.ui.extensions.pagingStaggeredGridItems
import io.lackstudio.omnihub.compose.utils.UnsplashLinks
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafePullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (isPullToRefreshSupported) {
        // Supported platforms (Android/iOS): Use actual pull-to-refresh
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = modifier,
        ) {
            content()
        }
    } else {
        // Unsupported platforms (Wasm/Desktop): Display content directly, without any wrapper
        Box(modifier = modifier) {
            content()
        }
    }
}

// --- List Components ---
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PhotoList(
    photos: List<GalleryDisplayable>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    isEndOfList: Boolean,
    onLoadMore: () -> Unit,
    onPhotoClick: (String, String, Float) -> Unit,
    onUserClick: (String) -> Unit = {}
) {
    // Use Staggered Grid State
    val state = rememberLazyStaggeredGridState()

    LazyVerticalStaggeredGrid(
        // Key setting: Adaptive
        // Set minimum width (e.g., 300.dp or 180.dp)
        // - On mobile (width < 600dp), it automatically becomes 1 or 2 columns
        // - On Desktop/Web (large width), it automatically becomes 3, 4, 5... columns
        // Value can be adjusted based on design, smaller value means more columns
        columns = StaggeredGridCells.Adaptive(minSize = 300.dp),
        modifier = Modifier.fillMaxSize(),
        state = state,
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp
    ) {
        // Use the newly added extension
        pagingStaggeredGridItems(
            items = photos,
            isEndOfList = isEndOfList,
            onLoadMore = onLoadMore,
            key = { it.displayId }
        ) { item ->
            GalleryCard(
                item = item,
                onClick = {
                    val ratio = item.displayWidth / item.displayHeight.toFloat()
                    onPhotoClick(item.displayId, item.displayImageUrl?: "", ratio)
                },
                onUserClick = { item.displayUsername?.let { onUserClick(it) } },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope)
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CollectionList(
    collections: List<GalleryDisplayable>,
    isEndOfList: Boolean,
    onLoadMore: () -> Unit,
    onCollectionClick: (String, String) -> Unit,
    onUserClick: (String) -> Unit = {}
) {
    // Use Staggered Grid State
    val state = rememberLazyStaggeredGridState()

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 300.dp),
        modifier = Modifier.fillMaxSize(),
        state = state,
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp
    ) {
        pagingStaggeredGridItems(
            items = collections,
            isEndOfList = isEndOfList,
            onLoadMore = onLoadMore,
            key = { it.displayId }
        ) { collection ->
            GalleryCard(
                item = collection,
                onClick = { onCollectionClick(collection.displayId, collection.displayTitle) },
                onUserClick = {
                    collection.displayUsername?.let {
                        onUserClick(it)
                    }
                }
            )
        }
    }
}

@Composable
fun TopicList(
    topics: List<GalleryTopic>,
    isEndOfList: Boolean,
    onLoadMore: () -> Unit,
    onTopicClick: (String, String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 200.dp),
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
            TopicCard(
                topic = topic,
                onClick = { onTopicClick(topic.id, topic.title) }
            )
        }
    }
}

@Composable
fun PlaceholderBlurHash(
    blurHash: String?,
    modifier: Modifier = Modifier,
    contentDescription: String = "",
    contentScale: ContentScale = ContentScale.Crop
) {
    blurHash?.let {
        BlurHashImage(
            hash = blurHash,
            contentDescription = contentDescription,
            modifier = modifier
        )
    }?: Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun GalleryCard(
    item: GalleryDisplayable,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    onClick: () -> Unit,
    onUserClick: (String) -> Unit = {}
) {
    // 1. Prepare image list
    val previews = item.displayPreviewPhotos
    // 2. Create Pager State
    val pagerState = rememberPagerState(pageCount = { previews.size })

    // Calculate image aspect ratio
    val aspectRatio = remember(item.displayWidth, item.displayHeight) {
        val w = item.displayWidth
        val h = item.displayHeight
        if (h > 0 && w > 0) {
            w.toFloat() / h.toFloat()
        } else {
            1f
        }
    }

    // Calculate Shared Element Modifier
    // If Scope exists, apply animation effect; otherwise return the original Modifier
    val sharedElementModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedElement(
                sharedContentState = rememberSharedContentState(key = "image-${item.displayId}"),
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    } else {
        Modifier
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color.Gray.copy(alpha = 0.2f),
        contentColor = Color.White
    ) {
        // Use Column: Image on top, text below
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        ) {
            // totalPhotos for collections and topics/:idOrSlug
            // but topics has another container
            val isCollection = item.displayCount > 0

            // --- Top section: Image area ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio).
                    then(sharedElementModifier),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Image content
                    GalleryCardImageContent(
                        previews = previews,
                        singleItem = item,
                        pagerState = pagerState
                    )

                    // Image count
                    if (isCollection) {
                        GalleryCountBadge(
                            count = item.displayCount,
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                        )
                    }

                    GalleryCardGradientOverlay()

                    // Control layer (Pager left/right arrows)
                    if (previews.size > 1) {
                        GalleryPagerNavigation(
                            pagerState = pagerState,
                            itemCount = previews.size,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // --- Bottom section: Info row (Username and Likes) ---
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                if (isCollection) {
                    Text(
                        text = item.displayTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        maxLines = 1,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    // Bottom Left: User info (Black text)
                    // Don't need display avatar and username for user screen
                    if (item.displayUserAvatar != null && item.displayUsername != null ) {
                        GalleryCardAttribution(
                            isCollection = isCollection,
                            avatarUrl = item.displayUserAvatar,
                            username = item.displayUsername,
                            name = item.displayName,
                            onAvatarClick = { item.displayUsername?.let { onUserClick(it) } }
                        )
                    } else {
                        // [User Page] Hide User Info, but need Spacer to keep layout consistent
                        // This ensures the LikeBadge on the right is pushed to the edge
                        Spacer(modifier = Modifier.weight(1f))
                    }


                    // Bottom Right: Likes (Black text + Red heart)
                    if (item.displayLikes > 0) {
                        GalleryLikeBadge(
                            likes = item.displayLikes,
                            modifier = Modifier
                                .padding(start = 8.dp)
                        )
                    }
                }
            }

            // Extra bottom spacing (Optional)
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

/**
 * Handles attribution display at the card bottom
 * For collections: Display "Created by [name] on Unsplash"
 * For photos: Display Avatar + Name
 */
@Composable
private fun GalleryCardAttribution(
    isCollection: Boolean,
    avatarUrl: String?,
    username: String?,
    name: String?,
    onAvatarClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // only photos
        if (!isCollection) {
            if (LocalInspectionMode.current) {
                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(Color.Gray))
            } else {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(24.dp) // Slightly smaller Avatar to accommodate multi-line text
                        .clip(CircleShape)
                        .clickable(onClick = onAvatarClick)
                        .background(Color.LightGray),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        if (isCollection) {
            // [Collection Mode]: Display "Created by Name on Unsplash"
            val annotatedString = buildAnnotatedString {
                append("Curated by ")
                username?.let {
                    // Name Link
                    withLink(LinkAnnotation.Url(UnsplashLinks.userProfile(username))) {
                        withStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append(name ?: username)
                        }
                    }
                }

                append(" on ")

                // Unsplash Link
                withLink(LinkAnnotation.Url(UnsplashLinks.home())) {
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append("Unsplash")
                    }
                }
            }

            Text(
                text = annotatedString,
                style = MaterialTheme.typography.labelSmall, // Use a smaller font
                color = Color.Gray, // Use gray to keep visual hierarchy lower than the title
                maxLines = 2
            )

        } else {
            val annotatedString = buildAnnotatedString {
                if (username != null) {
                    withLink(LinkAnnotation.Url(UnsplashLinks.userProfile(username))) {
                        withStyle(
                            SpanStyle(
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append(name ?: username)
                        }
                    }
                } else {
                    // display name if there is no username
                    withStyle(SpanStyle(color = Color.Black, fontWeight = FontWeight.SemiBold)) {
                        append(name ?: "")
                    }
                }
            }
            // [Photo Mode]: Maintain original display (Name Only)
            Text(
                text = annotatedString,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Handles image display logic:
 * Automatically determines whether to use Pager for carousel or single image
 */
@Composable
private fun GalleryCardImageContent(
    previews: List<GalleryPreview>,
    singleItem: GalleryDisplayable,
    pagerState: PagerState
) {
    if (LocalInspectionMode.current) {
        // Preview Mode
        Box(
            modifier = Modifier.fillMaxSize().background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Text("Image Preview", color = Color.DarkGray)
        }
        return
    }

    if (previews.size > 1) {
        // [Multiple Images Mode]
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val photo = previews[page]
            Box(modifier = Modifier.fillMaxSize()) {
                PlaceholderBlurHash(
                    blurHash = photo.blurHash,
                    modifier = Modifier.fillMaxSize()
                )
                AsyncImage(
                    model = photo.url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    } else {
        // [Single Image Mode]
        PlaceholderBlurHash(
            blurHash = singleItem.displayBlurHash,
            contentDescription = "blur hash",
            modifier = Modifier.fillMaxSize(),
        )
        AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(singleItem.displayImageUrl)
                .size(Size.ORIGINAL)
                .crossfade(true)
                .build(),
            contentDescription = singleItem.displayTitle,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

/**
 * Handles gradient overlay
 */
@Composable
private fun GalleryCardGradientOverlay() {
    val overlayBrush = remember {
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
            startY = 400f // Could consider using relative ratio here, but fixed height is usually sufficient inside a Card
        )
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(overlayBrush)
    )
}

/**
 * Handles carousel navigation (arrows + dots)
 */
@Composable
private fun GalleryPagerNavigation(
    pagerState: PagerState,
    itemCount: Int,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = modifier) {
        // Left Arrow
        if (pagerState.currentPage > 0) {
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp)
                    .size(28.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.3f),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev")
            }
        }

        // Right Arrow
        if (pagerState.currentPage < itemCount - 1) {
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
                    .size(28.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Black.copy(alpha = 0.3f),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
            }
        }

        // Bottom Dots
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(itemCount) { iteration ->
                val isSelected = pagerState.currentPage == iteration
                val color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f)
                val size = if (isSelected) 6.dp else 4.dp
                Box(
                    modifier = Modifier
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(size)
                )
            }
        }
    }
}

/**
 * Top right count badge (Collections)
 */
@Composable
private fun GalleryCountBadge(count: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = 0.6f),
        contentColor = Color.White
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.FilterNone,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Bottom right likes badge (Photos)
 */
@Composable
private fun GalleryLikeBadge(likes: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = likes.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Black,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = "Likes",
            modifier = Modifier.size(16.dp),
            tint = Color.Red
        )
    }
}

@Composable
fun TopicCard(
    topic: GalleryTopic,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable(onClick = onClick), // Limit height (adjust dp as needed)
        shape = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center // Title displayed in the center
        ) {

            PlaceholderBlurHash(
                topic.blurhash,
                contentDescription = "topic blur hash",
                modifier = Modifier.fillMaxSize()
            )

            // Background image
            AsyncImage(
                // Please confirm GalleryTopic data structure field name
                // (e.g. topic.coverUrl or topic.urls.small)
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

            topic.coverUrl?.let {
                Text(
                    text = "Cover by ${topic.name} on Unsplash",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                )
            }
        }
    }
}

// -----------------------------
// Previews
// -----------------------------
@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun PreviewGalleryUserInfo() {
    MaterialTheme {
        GalleryCardAttribution(
            isCollection = false,
            avatarUrl = null,
            username = "OmniHubDesigner",
            name = "OmniHub Designer",
            onAvatarClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
fun PreviewGalleryLikeBadge() {
    MaterialTheme {
        GalleryLikeBadge(
            likes = 1250,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(showBackground = true, widthDp = 300)
@Composable
fun PreviewGalleryCard_SinglePhoto() {
    // Mock single photo
    val mockItem = MockGalleryDisplayable(
        displayUsername = "Alice Photographer",
        displayLikes = 340,
        displayCount = 0, // Single photo has no count badge
        displayWidth = 400,
        displayHeight = 300
    )

    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            GalleryCard(item = mockItem, onClick = {})
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(showBackground = true, widthDp = 300)
@Composable
fun PreviewGalleryCardCollection() {
    // Mock Collection (Multiple images + Top right badge)
    val mockItem = MockGalleryDisplayable(
        displayUsername = "Bob Curator",
        displayLikes = 88,
        displayCount = 12, // Display 12 items
        displayWidth = 300,
        displayHeight = 400,
        // Mock two preview photos to trigger Pager logic (Images won't show in Preview, but structure is visible)
        displayPreviewPhotos = listOf(
            GalleryPreview("url1", null),
            GalleryPreview("url2", null)
        )
    )

    MaterialTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            GalleryCard(item = mockItem, onClick = {})
        }
    }
}

// --- Mock Data Helper for Previews ---
internal data class MockGalleryDisplayable(
    override val displayId: String = "mock_id",
    override val displayTitle: String = "Mock Title",
    override val displayWidth: Int = 1080,
    override val displayHeight: Int = 1920,
    override val displayLikes: Int = 100,
    override val displayUsername: String = "mock_user",
    override val displayName: String = "Mock User",
    override val displayUserAvatar: String? = null,
    override val displayImageUrl: String? = "https://source.unsplash.com/random/800x600?sig=${displayId}",
    override val displayBlurHash: String? = "L6PZfSi_.AyE_3t7t7R**0o#DgR4",
    override val displayCount: Int = 0,
    override val displayPreviewPhotos: List<GalleryPreview> = emptyList()
) : GalleryDisplayable
