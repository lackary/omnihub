package io.lackstudio.omnihub.compose.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding // Manual padding is not needed, NavigationSuiteScaffold will handle it
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.lackstudio.omnihub.compose.auth.DeepLinkBuffer
import io.lackstudio.omnihub.compose.ui.settings.SettingsScreen
import io.lackstudio.omnihub.compose.ui.gallery.CollectionDetailScreen
import io.lackstudio.omnihub.compose.ui.gallery.GalleryScreen
import io.lackstudio.omnihub.compose.ui.gallery.PhotoDetailScreen
import io.lackstudio.omnihub.compose.ui.gallery.TopicDetailScreen
import io.lackstudio.omnihub.compose.ui.gallery.UserDetailScreen
import io.lackstudio.omnihub.compose.ui.home.HomeScreen
import io.lackstudio.omnihub.compose.ui.navigation.Feature
import io.lackstudio.omnihub.compose.ui.navigation.Screen
import kotlinx.coroutines.launch

// Helper data class (place at bottom of file or in a separate file)
data class NavItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val route: Any, // Or use your custom Screen type
    val isSelected: Boolean
)

// --- Animation Constants ---
private const val ANIM_DURATION = 300

// [Standard Page] Slide in (enter from the right)
private fun AnimatedContentTransitionScope<*>.slideIn() =
    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(ANIM_DURATION))

// [Standard Page] Slide out (exit to the left)
private fun AnimatedContentTransitionScope<*>.slideOut() =
    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(ANIM_DURATION))

// [Standard Page] Pop in (return from the left)
private fun AnimatedContentTransitionScope<*>.slidePopIn() =
    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(ANIM_DURATION))

// [Standard Page] Pop out (exit to the right - Back)
private fun AnimatedContentTransitionScope<*>.slidePopOut() =
    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(ANIM_DURATION))

// [Photo Details] Fade in/out (Since fadeIn/fadeOut are global functions, variables or functions could be used here; keeping it as functions for consistency)
private fun fadeEnter() = fadeIn(tween(ANIM_DURATION))
private fun fadeExit() = fadeOut(tween(ANIM_DURATION))

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(name = "Mobile", widthDp = 360, heightDp = 640)
@Preview(name = "Desktop", widthDp = 1024, heightDp = 768)
@Composable
fun App() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry.value?.destination


    // Get current layout info (Is it Rail or BottomBar?)
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val defaultLayoutType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)

    // Check if the current destination is the PhotoDetail screen
    val isPhotoDetail = currentDestination?.hasRoute<Feature.Photo>() == true
    // Determine Layout Type: If it's PhotoDetail, force hide the navigation bar (None)
    val layoutType = if (isPhotoDetail) {
        NavigationSuiteType.None
    } else {
        defaultLayoutType
    }

    // Define your navigation items
    val navItems = listOf(
        NavItem(
            label = "Home",
            icon = Icons.Filled.Home,
            route = Screen.Home,
            isSelected = currentDestination?.hasRoute<Screen.Home>() == true
        ),
        NavItem(
            label = "Settings",
            icon = Icons.Filled.Settings,
            route = Screen.Settings,
            isSelected = currentDestination?.hasRoute<Screen.Settings>() == true
        )
    )

    // Resolve issue where web cannot redirect to Gallery page after successful OAuth2 login
    val startDestination: Any = remember {
        val hasAuthCode = DeepLinkBuffer.deepLinkUrl.value?.contains("code=") == true
        // If an auth code is present, navigate directly to Gallery (letting ViewModel handle login);
        // otherwise, go to the Home screen.
        if (hasAuthCode)  Feature.Gallery else Screen.Home
    }

    // Use NavigationSuiteScaffold instead of the original Scaffold
    NavigationSuiteScaffold(
        layoutType = layoutType,
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
            Scaffold(
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                // Set to transparent to avoid obscuring the background
                containerColor = androidx.compose.ui.graphics.Color.Transparent
            ) { innerPadding ->
                val onNavigate: (Feature) -> Unit = { feature ->
                    handleAppNavigation(feature, navController, scope, snackbarHostState)
                }

                // Main content goes here (NavHost)
                // Note: No need to handle innerPadding like in standard Scaffold,
                // NavigationSuiteScaffold automatically handles the layout
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    // Home Screen
                    composable<Screen.Home> {
                        HomeScreen(
                            onNavigateToFeature = onNavigate
                        )
                    }

                    // Account Screen
                    composable<Screen.Settings> {
                        SettingsScreen(
                            onNavigateToFeature = onNavigate
                        )
                    }

                    // Features
                    composable<Feature.Gallery>(
                        enterTransition = { slideIn() },
                        exitTransition = { slideOut() },
                        popEnterTransition = { slidePopIn() },
                        popExitTransition = { slidePopOut() }
                    ) {
                        GalleryScreen(
                            onNavigateToFeature = onNavigate,
                            onBack = { navController.popBackStack() },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@composable
                        )
                    }

                    composable<Feature.Photo>(
                        enterTransition = { fadeEnter() },
                        exitTransition = { fadeExit() },
                        popEnterTransition = { fadeEnter() },
                        popExitTransition = { fadeExit() }
                    ) { backStackEntry ->
                        val route: Feature.Photo = backStackEntry.toRoute()

                        PhotoDetailScreen(
                            id = route.id,
                            thumbUrl = route.url,
                            onBack = { navController.popBackStack() },
                            onNavigateToFeature = onNavigate,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@composable
                        )
                    }

                    composable<Feature.Collection>(
                        enterTransition = { slideIn() },
                        exitTransition = { slideOut() },
                        popEnterTransition = { slidePopIn() },
                        popExitTransition = { slidePopOut() }
                    ) { backStackEntry ->
                        val route: Feature.Collection = backStackEntry.toRoute()

                        CollectionDetailScreen(
                            collectionId = route.id,
                            title = route.title,
                            onBack = { navController.popBackStack() },
                            onNavigateToFeature = onNavigate,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@composable
                        )
                    }

                    composable<Feature.Topic>(
                        enterTransition = { slideIn() },
                        exitTransition = { slideOut() },
                        popEnterTransition = { slidePopIn() },
                        popExitTransition = { slidePopOut() }
                    ) { backStackEntry ->
                        val route: Feature.Topic = backStackEntry.toRoute()

                        TopicDetailScreen(
                            topicId = route.idOrSlug,
                            title = route.title,
                            onBack = { navController.popBackStack() },
                            onNavigateToFeature = onNavigate,
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@composable
                        )
                    }

                    composable<Feature.User>(
                        enterTransition = { slideIn() },
                        exitTransition = { slideOut() },
                        popEnterTransition = { slidePopIn() },
                        popExitTransition = { slidePopOut() }
                    ) { backStackEntry ->
                        val route: Feature.User = backStackEntry.toRoute()
                        UserDetailScreen(
                            username = route.username,
                            onBack = { navController.popBackStack() },
                            onNavigateToFeature = onNavigate,
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
}

/**
 * Smart Navigation: Automatically check if we should "pop" to the previous page.
 * Used to prevent infinite loops like User -> Photo -> User -> Photo.
 */
private fun NavHostController.navigateToFeatureSmart(feature: Feature) {
    val previousEntry = this.previousBackStackEntry

    val shouldPop = try {
        when (feature) {
            // 1. Check User loops
            is Feature.User -> {
                val prev = previousEntry?.toRoute<Feature.User>()
                prev?.username == feature.username
            }
            // 2. Check Collection loops
            is Feature.Collection -> {
                val prev = previousEntry?.toRoute<Feature.Collection>()
                prev?.id == feature.id
            }
            // 3. Check Photo loops
            is Feature.Photo -> {
                val prev = previousEntry?.toRoute<Feature.Photo>()
                prev?.id == feature.id
            }
            // 4. Check Topic loops
            is Feature.Topic -> {
                val prev = previousEntry?.toRoute<Feature.Topic>()
                prev?.idOrSlug == feature.idOrSlug
            }
            else -> false
        }
    } catch (e: Exception) {
        // If the previous page's route type doesn't match the target (e.g., coming from Home),
        // toRoute might throw an exception.
        false
    }

    if (shouldPop) {
        // If the destination is the same as the previous page, pop back to create a "return to page" feel.
        this.popBackStack()
    } else {
        // Otherwise, navigate to the new page normally.
        this.navigate(feature)
    }
}

/**
 * Centralized navigation logic:
 * 1. Intercept unfinished features (News, Stocks) -> Show Snackbar
 * 2. Finished features -> Perform smart navigation
 */
private fun handleAppNavigation(
    feature: Feature,
    navController: NavHostController,
    scope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: SnackbarHostState
) {
    when (feature) {
        is Feature.News, is Feature.Stocks -> {
            scope.launch {
                // Clear old snackbar and show new one to prevent stacking
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(
                    message = "Coming Soon: ${feature::class.simpleName} is under development!",
                    withDismissAction = true
                )
            }
        }
        else -> {
            navController.navigateToFeatureSmart(feature)
        }
    }
}
