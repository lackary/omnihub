package io.lackstudio.omnihub.compose.ui.gallery

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key // ★ Ensure this is imported
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer // ★ Ensure this is imported
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.lackstudio.omnihub.compose.ui.components.DotsIndicator
import io.lackstudio.omnihub.compose.ui.navigation.Feature
import kotlin.math.abs

data class StackedPhoto(
    val id: String,
    val thumbUrl: String,
    val ratio: Float
)

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun PhotoStackScreen(
    photos: List<StackedPhoto>,
    currentIndex: Int,
    onIndexChanged: (Int) -> Unit,
    onClosePhoto: (String) -> Unit,
    onNavigateToFeature: (Feature) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    if (photos.isEmpty()) return

    Box(modifier = Modifier.fillMaxSize()) {

        photos.forEachIndexed { index, photo ->
            // Add key to ensure Compose doesn't mix up animation states for each photo
            key(photo.id) {
                val distance = index - currentIndex

                val zIndex = if (distance == 0) 10f else (10 - abs(distance)).toFloat()
                val targetOffsetX = (distance * -150).dp
                val targetScale = if (distance == 0) 1f else 0.85f - (abs(distance) * 0.05f)
                val targetAlpha = if (distance == 0) 1f else 1f - (abs(distance) * 0.3f)

                // Add label to help debug animations in development tools
                val animatedOffsetX by animateDpAsState(targetValue = targetOffsetX, label = "offset_${photo.id}")
                val animatedScale by animateFloatAsState(targetValue = targetScale, label = "scale_${photo.id}")
                val animatedAlpha by animateFloatAsState(targetValue = targetAlpha.coerceIn(0f, 1f), label = "alpha_${photo.id}")

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(zIndex) // Keep zIndex on the outer layer to handle layer ordering
                        // Use graphicsLayer to force hardware rendering for alpha and scale, ensuring 100% effectiveness
                        .graphicsLayer {
                            translationX = animatedOffsetX.toPx()
                            scaleX = animatedScale
                            scaleY = animatedScale
                            alpha = animatedAlpha
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.85f)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        PhotoDetailScreen(
                            id = photo.id,
                            thumbUrl = photo.thumbUrl,
                            onBack = { onClosePhoto(photo.id) },
                            onNavigateToFeature = onNavigateToFeature,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope
                        )

                        // Transparent interception mask
                        // When the photo is not in the center, cover it with a transparent Box to intercept all clicks
                        // Prevent users from accidentally touching buttons inside background photos
                        if (distance != 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { onIndexChanged(index) } // Click to switch focus directly
                            )
                        }
                    }
                }
            }
        }

        // --- Left button: View new photo (currentIndex + 1) ---
        if (currentIndex < photos.lastIndex) {
            IconButton(
                onClick = { onIndexChanged(currentIndex + 1) },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(16.dp)
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .zIndex(20f)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Next Photo", tint = Color.White)
            }
        }

        // --- Right button: View old photo (currentIndex - 1) ---
        if (currentIndex > 0) {
            IconButton(
                onClick = { onIndexChanged(currentIndex - 1) },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(16.dp)
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .zIndex(20f)
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Previous Photo", tint = Color.White)
            }
        }

        // ★ Add pagination indicator (Dots)
        DotsIndicator(
            totalDots = photos.size,
            selectedIndex = currentIndex,
            modifier = Modifier
                .align(Alignment.BottomCenter) // Align to the bottom center
                .padding(bottom = 32.dp)       // Leave some space from the bottom
                .zIndex(20f)                   // Ensure it's on the top layer
        )
    }
}
