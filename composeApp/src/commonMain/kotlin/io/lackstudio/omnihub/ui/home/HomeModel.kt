package io.lackstudio.omnihub.ui.home

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import io.lackstudio.omnihub.ui.navigation.Feature

data class OmniService(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val route: Feature // Route to navigate to when clicked
)

// Pre-defined list of current services
val omniServices = listOf(
    OmniService(
        title = "Gallery",
        description = "High-quality royalty-free photo search",
        icon = Icons.Filled.Photo,
        route = Feature.Gallery
    ),
    OmniService(
        title = "News",
        description = "Global real-time news aggregation",
        icon = Icons.Filled.Newspaper,
        route = Feature.News
    ),
    OmniService(
        title = "Stocks",
        description = "Stock market quotes and financial data",
        icon = Icons.AutoMirrored.Filled.ShowChart,
        route = Feature.Stocks
    )
)
