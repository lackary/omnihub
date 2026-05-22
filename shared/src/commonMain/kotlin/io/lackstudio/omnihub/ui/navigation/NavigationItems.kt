package io.lackstudio.omnihub.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute

data class NavItem(
    val label: String,
    val icon: ImageVector,
    val route: Any,
    val isSelected: Boolean
)

@Composable
fun getAppNavItems(navDestination: NavDestination?): List<NavItem> {
    return listOf(
        NavItem(
            label = "Home",
            icon = Icons.Filled.Home,
            route = Screen.Home,
            isSelected = navDestination?.hasRoute<Screen.Settings>() == true
        ),
        NavItem(
            label = "Settings",
            icon = Icons.Filled.Settings,
            route = Screen.Settings,
            isSelected = navDestination?.hasRoute<Screen.Settings>() == true
        )
    )
}
