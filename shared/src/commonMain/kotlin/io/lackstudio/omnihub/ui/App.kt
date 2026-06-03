package io.lackstudio.omnihub.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.lackstudio.omnihub.auth.DeepLinkBuffer
import io.lackstudio.omnihub.ui.extensions.navigateToFeatureSmart
import io.lackstudio.omnihub.ui.account.AccountScreen
import io.lackstudio.omnihub.ui.account.LoginScreen
import io.lackstudio.omnihub.ui.account.RegisterScreen
import io.lackstudio.omnihub.ui.settings.SettingsScreen
import io.lackstudio.omnihub.ui.gallery.CollectionDetailScreen
import io.lackstudio.omnihub.ui.gallery.GalleryScreen
import io.lackstudio.omnihub.ui.gallery.PhotoDetailScreen
import io.lackstudio.omnihub.ui.gallery.TopicDetailScreen
import io.lackstudio.omnihub.ui.gallery.UserDetailScreen
import io.lackstudio.omnihub.ui.home.HomeScreen
import io.lackstudio.omnihub.ui.navigation.Feature
import io.lackstudio.omnihub.ui.navigation.Screen
import io.lackstudio.omnihub.ui.navigation.getAppNavItems
import io.lackstudio.omnihub.utils.logging.AppLog
import io.lackstudio.omnihub.utils.LocalLogger
import io.lackstudio.omnihub.utils.logging.rememberLogger
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

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

// [Photo Details] Fade in/out
private fun fadeEnter() = fadeIn(tween(ANIM_DURATION))
private fun fadeExit() = fadeOut(tween(ANIM_DURATION))

// Helper to determine screen index for tab animations
private fun getScreenIndex(entry: NavBackStackEntry?): Int {
    return when {
        entry?.destination?.hasRoute<Screen.Home>() == true -> 0
        entry?.destination?.hasRoute<Screen.Settings>() == true -> 1
        entry?.destination?.hasRoute<Screen.Login>() == true -> 2
        entry?.destination?.hasRoute<Screen.Register>() == true -> 2
        entry?.destination?.hasRoute<Screen.Account>() == true -> 2
        else -> 0
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview(name = "Mobile", widthDp = 360, heightDp = 640)
@Preview(name = "Desktop", widthDp = 1024, heightDp = 768)
@Composable
fun App(
    navController: NavHostController = rememberNavController(),
    showNavigationBar: Boolean = true
) {

    val rootLogger = remember { AppLog.withTag("App") }
    CompositionLocalProvider(
        LocalLogger provides rootLogger
    ) {
        AppScreen(navController = navController, showNavigationBar = showNavigationBar)
    }

}

@Composable
fun AppScreen(
    navController: NavHostController,
    showNavigationBar: Boolean,
    viewModel: AppViewModel = koinViewModel()
) {
    val logger = rememberLogger("AppScreen")
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()

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
    val layoutType = if (!showNavigationBar || isPhotoDetail) {
        NavigationSuiteType.None
    } else {
        defaultLayoutType
    }

    LaunchedEffect(Unit) {
        logger.i { "App Screen Launched" }
    }

    // Define your navigation items
    val navItems = getAppNavItems(currentDestination)

    // Resolve issue where web cannot redirect to Gallery page after successful OAuth2 login
    val startDestination: Any = remember {
        val hasAuthCode = DeepLinkBuffer.deepLinkUrl.value?.contains("code=") == true
        if (hasAuthCode)  Feature.Gallery else Screen.Home
    }

    NavigationSuiteScaffold(
        layoutType = layoutType,
        navigationSuiteItems = {
            navItems.forEachIndexed { index, item ->

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
                        val targetRoute = if (item.route == Screen.Account && !isLoggedIn) {
                            Screen.Login
                        } else {
                            item.route
                        }
                        navController.navigate(targetRoute) {
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
                containerColor = androidx.compose.ui.graphics.Color.Transparent
            ) { innerPadding ->
                val onNavigate: (Feature) -> Unit = { feature ->
                    val currentRoute = navController.currentDestination?.route
                    logger.d { "Navigation: $currentRoute -> $feature" }
                    handleAppNavigation(feature, navController, scope, snackbarHostState)
                }

                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Home Screen
                    composable<Screen.Home>(
                        enterTransition = {
                            val initialIndex = getScreenIndex(initialState)
                            val targetIndex = getScreenIndex(targetState)
                            if (targetIndex > initialIndex) {
                                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(ANIM_DURATION))
                            } else {
                                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(ANIM_DURATION))
                            }
                        },
                        exitTransition = {
                            val initialIndex = getScreenIndex(initialState)
                            val targetIndex = getScreenIndex(targetState)
                            if (targetIndex < initialIndex) {
                                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(ANIM_DURATION))
                            } else {
                                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(ANIM_DURATION))
                            }
                        }
                    ) {
                        HomeScreen(
                            onNavigateToFeature = onNavigate
                        )
                    }

                    // Settings Screen
                    composable<Screen.Settings>(
                        enterTransition = {
                            val initialIndex = getScreenIndex(initialState)
                            val targetIndex = getScreenIndex(targetState)
                            if (targetIndex > initialIndex) {
                                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(ANIM_DURATION))
                            } else {
                                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(ANIM_DURATION))
                            }
                        },
                        exitTransition = {
                            val initialIndex = getScreenIndex(initialState)
                            val targetIndex = getScreenIndex(targetState)
                            if (targetIndex < initialIndex) {
                                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(ANIM_DURATION))
                            } else {
                                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(ANIM_DURATION))
                            }
                        }
                    ) {
                        SettingsScreen(
                            onNavigateToFeature = onNavigate
                        )
                    }

                    // Login Screen
                    composable<Screen.Login>(
                        enterTransition = {
                            val initialIndex = getScreenIndex(initialState)
                            val targetIndex = getScreenIndex(targetState)
                            if (targetIndex > initialIndex) {
                                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(ANIM_DURATION))
                            } else {
                                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(ANIM_DURATION))
                            }
                        },
                        exitTransition = {
                            val initialIndex = getScreenIndex(initialState)
                            val targetIndex = getScreenIndex(targetState)
                            if (targetIndex < initialIndex) {
                                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(ANIM_DURATION))
                            } else {
                                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(ANIM_DURATION))
                            }
                        },
                        popEnterTransition = { slidePopIn() },
                        popExitTransition = { slidePopOut() }
                    ) {
                        LoginScreen(
                            onNavigateToRegister = {
                                navController.navigate(Screen.Register)
                            },
                            onLoginSuccess = {
                                navController.navigate(Screen.Account) {
                                    popUpTo(Screen.Login) { inclusive = true }
                                }
                            }
                        )
                    }

                    // Account Screen
                    composable<Screen.Account>(
                        enterTransition = { slideIn() },
                        exitTransition = { slideOut() },
                        popEnterTransition = { slidePopIn() },
                        popExitTransition = { slidePopOut() }
                    ) {
                        AccountScreen(
                            onNavigateToLogin = {
                                navController.navigate(Screen.Login) {
                                    popUpTo(Screen.Account) { inclusive = true }
                                }
                            },
                            onBack = {
                                navController.popBackStack()
                            }
                        )
                    }

                    // Register Screen
                    composable<Screen.Register>(
                        enterTransition = { slideIn() },
                        exitTransition = {
                            val initialIndex = getScreenIndex(initialState)
                            val targetIndex = getScreenIndex(targetState)
                            if (targetIndex < initialIndex) {
                                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(ANIM_DURATION))
                            } else {
                                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(ANIM_DURATION))
                            }
                        },
                        popEnterTransition = { slidePopIn() },
                        popExitTransition = { slidePopOut() }
                    ) {
                        RegisterScreen(
                            onNavigateToLogin = {
                                navController.popBackStack()
                            },
                            onBack = {
                                navController.popBackStack()
                            }
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
                            onBack = {
                                logger.d { "GalleryScreen Back button pressed" }
                                navController.popBackStack()
                            },
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
                            onBack = {
                                logger.d { "PhotoDetailScreen Back button pressed" }
                                navController.popBackStack()
                            },
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
                            onBack = {
                                logger.d { "CollectionDetailScreen Back button pressed" }
                                navController.popBackStack()
                            },
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
                            onBack = {
                                logger.d { "TopicDetailScreen Back button pressed" }
                                navController.popBackStack()
                            },
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
                            onBack = {
                                logger.d { "UserDetailScreen Back button pressed" }
                                navController.popBackStack()
                            },
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

private fun handleAppNavigation(
    feature: Feature,
    navController: NavHostController,
    scope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: SnackbarHostState
) {
    when (feature) {
        is Feature.News, is Feature.Stocks -> {
            scope.launch {
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
