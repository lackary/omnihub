package io.lackstudio.omnihub.ui.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.lackstudio.omnihub.utils.toCompactDisplayString
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun PhotoDetailInfoContent(detail: Photo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Text("Info", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))
        HorizontalDivider()

        detail.exif?.let { exif ->
            val subtitle = listOfNotNull(
                exif.aperture,
                exif.exposureTime,
                exif.iso?.let { "ISO $it" },
                exif.focalLength
            ).joinToString(" • ")

            val title = listOfNotNull(exif.make, exif.model).joinToString(" ").trim()
            val finalTitle = title.ifBlank { "Unknown Camera" }

            if (subtitle.isNotBlank() || title.isNotBlank()) {
                InfoRow(Icons.Default.CameraAlt, finalTitle, subtitle)
            }
        }

        detail.location?.displayString()?.let { locationStr ->
            val coords = if (detail.location.latitude != null && detail.location.longitude != null) {
                "${detail.location.latitude}, ${detail.location.longitude}"
            } else ""
            InfoRow(Icons.Default.LocationOn, locationStr, coords)
        }

        if (!detail.description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = detail.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Photo by ${detail.username}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun InfoRow(icon: ImageVector, title: String, subtitle: String) {
    Row(modifier = Modifier.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun PhotoMetadataOverlay(
    photo: Photo,
    modifier: Modifier = Modifier
) {
    var showUsername by remember { mutableStateOf(false) }

    // Unified width for Avatar and stats icons to ensure vertical alignment.
    // This ensures their "center points" are on the same vertical line
    val barWidth = 48.dp

    // Define vertical gradient (only used for the bar on the right)
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            Color.Black.copy(alpha = 0.4f), // Slightly darker in the middle
            Color.Black.copy(alpha = 0.7f)  // Darker at the bottom
        )
    )

    // Use Box for layer stacking
    // .height(IntrinsicSize.Min), allows Box height to be determined by the content height of child elements (Column)
    Box(
        modifier = modifier.height(IntrinsicSize.Min),
        contentAlignment = Alignment.BottomEnd // Overall alignment to the bottom right
    ) {
        // --- Layer 1: Background layer (fixed width, aligned right) ---
        // Background layer: Fixed width to maintain gradient position independent of username expansion.
        Box(
            modifier = Modifier
                .fillMaxHeight() // Fill parent container height (i.e., follow content height)
                .width(barWidth) // [Key] Fixed width, only wraps the Icon
                .align(Alignment.CenterEnd) // Locked to the right
                .background(brush = gradientBrush, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) // Add rounded corners for a smoother top
        )

        // --- Layer 2: Content layer ---
        Column(
            horizontalAlignment = Alignment.End, // Content aligned to the right
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar Row (includes the sliding Username)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                AnimatedVisibility(
                    visible = showUsername,
                    // Expand from the right (End) to left + Fade in
                    enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
                    // Shrink back to the right (End) + Fade out
                    exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Username style background
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f)) // Semi-transparent black background, ensures visibility
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = photo.username,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                        // Spacing between Username and Avatar
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }

                // Avatar (Click to toggle show/hide)
                photo.userAvatar?.let { avatarUrl ->
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(barWidth)
                            .clip(CircleShape)
                            .background(Color.Gray)
                            .clickable { showUsername = !showUsername }, // Click to toggle state
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // --- Views ---
            StatItem(
                icon = Icons.Filled.Visibility,
                value = photo.views.toCompactDisplayString(),
                width = barWidth
            )

            // --- Likes ---
            StatItem(
                icon = Icons.Filled.Favorite,
                value = photo.likes.toCompactDisplayString(),
                width = barWidth
            )

            // --- Downloads (Bottom) ---
            StatItem(
                icon = Icons.Filled.Download,
                value = photo.downloads.toCompactDisplayString(),
                width = barWidth
            )
        }
    }
}

@Composable
fun StatItem(
    icon: ImageVector,
    value: String,
    width: Dp
) {
    // Use Column to place the number below the Icon
    Column(
        modifier = Modifier.width(width),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(28.dp) // Icon slightly larger for better visibility
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall, // Smaller font suitable for placement below Icon
            color = Color.White,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * Preview: Photo Detail Info (Bottom Sheet Content)
 * Simulates white background (Surface)
 */
@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, name = "Detail Info Content")
@Composable
fun PhotoDetailInfoContentPreview() {
    val dummyPhoto = Photo(
        id = "1",
        fullUrl = "",
        username = "Photographer Name",
        userAvatar = "https://example.com/avatar.jpg",
        description = "This is a beautiful shot taken during the golden hour in the mountains. The lighting was perfect.",
        exif = PhotoExif(
            make = "Sony",
            model = "A7III",
            aperture = "f/2.8",
            exposureTime = "1/200s",
            iso = 100,
            focalLength = "35mm"
        ),
        location = PhotoLocation(
            city = "Kyoto",
            country = "Japan",
            latitude = 35.0116,
            longitude = 135.7681
        ),
        views = 12500,
        likes = 450,
        downloads = 88,
        createdAt = "2024-05-20T10:00:00Z"
    )

    MaterialTheme {
        // Simulates BottomSheet container environment
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
            PhotoDetailInfoContent(detail = dummyPhoto)
        }
    }
}

/**
 * Preview: Overlay Data (Overlay)
 * Simulates dark background (Because text is white)
 */
@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "Metadata Overlay (Dark)")
@Composable
fun PhotoMetadataOverlayPreview() {
    val dummyPhoto = Photo(
        id = "1",
        fullUrl = "",
        username = "TestUser",
        userAvatar = null, // Test case without avatar (or you can provide a url string)
        description = null,
        exif = null,
        location = null,
        views = 15400,    // Test 15.4k
        likes = 342,      // Test 342
        downloads = 1200000, // Test 1.2m
        createdAt = "2024-03-25T10:00:00Z"
    )

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.BottomEnd // Simulates being at the bottom end of the screen
        ) {
            PhotoMetadataOverlay(
                photo = dummyPhoto
            )
        }
    }
}

/**
 * Preview: Single Data Item (StatItem)
 */
@Preview(showBackground = true, backgroundColor = 0xFF333333, name = "Single Stat Item")
@Composable
fun StatItemPreview() {
    val contentWidth = 48.dp
    MaterialTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            StatItem(icon = Icons.Filled.Visibility, value = "12.5k", contentWidth)
            StatItem(icon = Icons.Filled.Favorite, value = "300", contentWidth)
        }
    }
}

/**
 * Preview: Single Info Row (InfoRow)
 */
@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, name = "Info Row")
@Composable
fun InfoRowPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            InfoRow(
                icon = Icons.Default.CameraAlt,
                title = "Sony A7IV",
                subtitle = "f/1.4 • 1/500s • ISO 100 • 50mm"
            )
            InfoRow(
                icon = Icons.Default.LocationOn,
                title = "Taipei, Taiwan",
                subtitle = "25.0330, 121.5654"
            )
        }
    }
}
