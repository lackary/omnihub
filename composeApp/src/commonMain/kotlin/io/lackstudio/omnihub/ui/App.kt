package io.lackstudio.omnihub.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding // Manual padding is not needed, NavigationSuiteScaffold will handle it
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.lackstudio.omnihub.ui.account.AccountScreen
import io.lackstudio.omnihub.ui.gallery.GalleryScreen
import io.lackstudio.omnihub.ui.gallery.PhotoDetailScreen
import io.lackstudio.omnihub.ui.home.HomeScreen
import io.lackstudio.omnihub.ui.navigation.Feature
import io.lackstudio.omnihub.ui.navigation.Screen
import org.jetbrains.compose.ui.tooling.preview.Preview

// Helper data class (place at bottom of file or in a separate file)
data class NavItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: Any, // Or use your custom Screen type
    val isSelected: Boolean
)

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(name = "Mobile", widthDp = 360, heightDp = 640)
@Preview(name = "Desktop", widthDp = 1024, heightDp = 768)
@Composable
fun App() {
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry.value?.destination

    // Get current layout info (Is it Rail or BottomBar?)
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val layoutType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)

    // Define your navigation items
    val navItems = listOf(
        NavItem(
            label = "Home",
            icon = Icons.Default.Home,
            route = Screen.Home,
            isSelected = currentDestination?.hasRoute<Screen.Home>() == true
        ),
        NavItem(
            label = "Account",
            icon = Icons.Default.Person,
            route = Screen.Account,
            isSelected = currentDestination?.hasRoute<Screen.Account>() == true
        )
    )

    // Use NavigationSuiteScaffold instead of the original Scaffold
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            navItems.forEachIndexed { index, item ->

                // Logic: If in Rail mode and it is the first item (Home), add 16dp top padding
                val itemModifier = if (layoutType == NavigationSuiteType.NavigationRail && index == 0) {
                    Modifier.padding(top = 16.dp)
                } else {
                    Modifier
                }

                item(
                    modifier = itemModifier,
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = { Text(item.label) },
                    selected = item.isSelected,
                    onClick = {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) {
        SharedTransitionLayout {
            // Main content goes here (NavHost)
            // Note: No need to handle innerPadding like in standard Scaffold,
            // NavigationSuiteScaffold automatically handles the layout
            NavHost(
                navController = navController,
                startDestination = Screen.Home,
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Home Screen
                composable<Screen.Home> {
                    HomeScreen(
                        onNavigateToFeature = { feature -> navController.navigate(feature) }
                    )
                }

                // Account Screen
                composable<Screen.Account> {
                    AccountScreen(
                        onNavigateToFeature = { feature -> navController.navigate(feature) }
                    )
                }

                // Features
                composable<Feature.Gallery> {
                    GalleryScreen(
                        onNavigateToFeature = { feature -> navController.navigate(feature) },
                        onBack = { navController.popBackStack() },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable
                    )
                }

                composable<Feature.Photo> { backStackEntry ->
                    val route: Feature.Photo = backStackEntry.toRoute()

                    PhotoDetailScreen(
                        id = route.id,
                        thumbUrl = route.url,
                        onBack = { navController.popBackStack() },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable
                    )
                }

                composable<Feature.News> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("News API Page")
                    }
                }

                composable<Feature.Stocks> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Stocks API Page")
                    }
                }
            }
        }

    }
}
