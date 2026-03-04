package io.lackstudio.omnihub.compose.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DotsIndicator(
    totalDots: Int,
    selectedIndex: Int,
    modifier: Modifier = Modifier
) {
    if (totalDots <= 1) return // Do not show dots if there is only one photo

    val maxVisibleDots = 7

    // Calculate the start index of the sliding window
    val start = if (totalDots <= maxVisibleDots) {
        0
    } else {
        when {
            selectedIndex < 3 -> 0 // Near the start
            selectedIndex > totalDots - 4 -> totalDots - maxVisibleDots // Near the end
            else -> selectedIndex - 3 // Sliding in the middle
        }
    }

    // The actual number of dots to render (maximum 7)
    val visibleDots = minOf(totalDots, maxVisibleDots)

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        for (i in 0 until visibleDots) {
            val actualIndex = start + i
            val isSelected = actualIndex == selectedIndex

            // If the dot is at the edge and there are still hidden photos,
            // shrink it slightly to create a visual cue of infinite extension
            val isEdge = totalDots > maxVisibleDots &&
                    ((i == 0 && start > 0) || (i == visibleDots - 1 && start < totalDots - maxVisibleDots))

            val targetSize = when {
                isSelected -> 8.dp
                isEdge -> 4.dp // Shrink at the edge to imply more photos
                else -> 6.dp
            }

            val targetAlpha = if (isSelected) 1f else 0.4f

            // Apply smooth animations
            val size by animateDpAsState(targetValue = targetSize, label = "dotSize_$i")
            val alpha by animateFloatAsState(targetValue = targetAlpha, label = "dotAlpha_$i")

            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = alpha))
            )
        }
    }
}
