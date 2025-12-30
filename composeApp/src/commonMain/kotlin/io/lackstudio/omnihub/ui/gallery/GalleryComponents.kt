package io.lackstudio.omnihub.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterNone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.brys.compose.blurhash.BlurHashImage
import io.lackstudio.omnihub.platform.isPullToRefreshSupported // Variable defined recently
import io.lackstudio.omnihub.ui.extensions.pagingGridItems
import io.lackstudio.omnihub.ui.extensions.pagingItems

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
    // Note: We use remember because the Brush object is recreated on every Recomposition.
    // If the list is scrolled rapidly, this would create a large number of short-lived objects.
    val overlayBrush = remember {
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
            startY = 400f
        )
    }
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
                    .background(overlayBrush)
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
