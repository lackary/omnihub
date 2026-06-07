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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
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

// [Tabs] Fade in/out
private fun tabEnterTransition() = fadeIn(tween(ANIM_DURATION))
private fun tabExitTransition() = fadeOut(tween(ANIM_DURATION))

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

    val pagerState = rememberPagerState { 3 }

    // 🔍 Global log for monitoring navigation destination changes
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            logger.d { "NavHost Real Destination: ${entry.destination.route}" }
        }
    }

    LaunchedEffect(isLoggedIn) {
        logger.d { "Current Login State: $isLoggedIn" }
    }

    // Get current layout info (Is it Rail or BottomBar?)
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val defaultLayoutType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(adaptiveInfo)

    // Check if the current destination is any Feature screen
    val isPhotoDetail = currentDestination?.hasRoute<Feature.Photo>() == true
    val isAnyFeature = currentDestination?.let {
        it.hasRoute<Feature.Gallery>() ||
                it.hasRoute<Feature.Photo>() ||
                it.hasRoute<Feature.Collection>() ||
                it.hasRoute<Feature.Topic>() ||
                it.hasRoute<Feature.User>() ||
                it.hasRoute<Feature.News>() ||
                it.hasRoute<Feature.Stocks>()
    } == true

    // Determine Layout Type:
    // 1. If explicitly hidden, hide it.
    // 2. If it's PhotoDetail (Lightbox), hide it on all platforms.
    // 3. If it's Mobile (NavigationBar) and we are in any sub-feature or secondary screen, hide it.
    // 4. Otherwise, use the default layout (shows Rail on Desktop).
    val layoutType = when {
        !showNavigationBar -> NavigationSuiteType.None
        isPhotoDetail -> NavigationSuiteType.None
        defaultLayoutType == NavigationSuiteType.NavigationBar && (isAnyFeature || currentDestination?.hasRoute<Screen.Register>() == true) -> NavigationSuiteType.None
        else -> defaultLayoutType
    }

    LaunchedEffect(Unit) {
        logger.i { "App Screen Launched" }
    }

    // Define your navigation items
    val navItems = getAppNavItems(currentDestination, pagerState.currentPage)

    // Resolve issue where web cannot redirect to Gallery page after successful OAuth2 login
    val startDestination: Any = remember {
        val hasAuthCode = DeepLinkBuffer.deepLinkUrl.value?.contains("code=") == true
        if (hasAuthCode)  Feature.Gallery else Screen.MainTabs
    }

    NavigationSuiteScaffold(
        layoutType = layoutType,
        navigationSuiteItems = {
            navItems.forEachIndexed { index, item ->

                val itemModifier = if ((layoutType == NavigationSuiteType.NavigationRail) && (index == 0)) {
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
                        val targetRoute = item.route
                        logger.d { "NavClick: target=$targetRoute, current=${currentDestination?.route}" }
                        val startDest = navController.graph.findStartDestination()
                        logger.d { "StartDestination ID: ${startDest.id}, Route: ${startDest.route}" }

                        if (targetRoute == Screen.Home || targetRoute == Screen.Settings || targetRoute == Screen.Account) {
                            val pageIndex = when (targetRoute) {
                                Screen.Home -> 0
                                Screen.Settings -> 1
                                else -> 2
                            }

                            if (currentDestination?.hasRoute<Screen.MainTabs>() == true) {
                                logger.d { "NavClick: Already in MainTabs, scrolling to $pageIndex" }
                                scope.launch {
                                    pagerState.animateScrollToPage(pageIndex)
                                }
                            } else {
                                logger.d { "NavClick: In Sub-Page, popping back to root and jumping to $pageIndex" }
                                // 1. Pop back directly to the root node (safest way)
                                navController.popBackStack(startDest.id, inclusive = false)

                                // 2. Sync Pager state
                                scope.launch {
                                    pagerState.scrollToPage(pageIndex)
                                }

                                // 3. If it's not MainTabs after popping (e.g. startDestination is not MainTabs), navigate to it.
                                if (navController.currentDestination?.hasRoute<Screen.MainTabs>() != true) {
                                    navController.navigate(Screen.MainTabs) {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        } else {
                            // Other non-Pager Feature page navigation
                            logger.d { "NavClick: Navigating to Feature: $targetRoute" }
                            navController.navigate(targetRoute) {
                                popUpTo(startDest.id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
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
                    // Main Tabs with Pager
                    composable<Screen.MainTabs>(
                        enterTransition = { tabEnterTransition() },
                        exitTransition = { tabExitTransition() },
                        popEnterTransition = { tabEnterTransition() },
                        popExitTransition = { tabExitTransition() }
                    ) {
                        logger.d { "Recomposing MainTabs, pagerState.currentPage=${pagerState.currentPage}" }
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1
                        ) { page ->
                            logger.d { "Displaying Pager page: $page" }
                            when (page) {
                                0 -> HomeScreen(onNavigateToFeature = onNavigate)
                                1 -> SettingsScreen(onNavigateToFeature = onNavigate)
                                2 -> {
                                    if (isLoggedIn) {
                                        AccountScreen(
                                            onNavigateToLogin = {
                                                // Login handled by state
                                            }
                                        )
                                    } else {
                                        LoginScreen(
                                            onNavigateToRegister = {
                                                navController.navigate(Screen.Register)
                                            },
                                            onLoginSuccess = {
                                                logger.d { "Login success triggered, isLoggedIn: $isLoggedIn" }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Register Screen
                    composable<Screen.Register>(
                        enterTransition = { slideIn() },
                        exitTransition = { slideOut() },
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
                        LaunchedEffect(Unit) {
                            logger.d { "Navigated to Gallery Composable (Effect)" }
                        }
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
