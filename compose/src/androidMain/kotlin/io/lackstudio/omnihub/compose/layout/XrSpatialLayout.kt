package io.lackstudio.omnihub.compose.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.xr.compose.spatial.ContentEdge
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.MovePolicy
import androidx.xr.compose.subspace.SpatialCurvedRow
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.SpatialSpacer
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.width
import io.lackstudio.omnihub.compose.ui.App
import io.lackstudio.omnihub.compose.ui.gallery.PhotoStackScreen
import io.lackstudio.omnihub.compose.ui.gallery.StackedPhoto
import io.lackstudio.omnihub.compose.ui.gallery.UserDetailScreen
import io.lackstudio.omnihub.compose.ui.navigation.XrNavEvent
import io.lackstudio.omnihub.compose.ui.navigation.getAppNavItems
import io.lackstudio.omnihub.compose.utils.LocalXrNavigation

@Composable
fun XrSpatialLayout() {
    val photoStack = remember { mutableStateListOf<StackedPhoto>() }
    var currentPhotoIndex by remember { mutableIntStateOf(0) }
    var selectedUsername by remember { mutableStateOf<String?>(null) }

    // Declare NavController at the top level so the Orbiter can use it
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val navItems = getAppNavItems(currentDestination)

    CompositionLocalProvider(
        LocalXrNavigation provides { event ->
            when(event) {
                is XrNavEvent.NavigateToPhoto -> {
//                    selectedUsername = null
                    val newPhoto = StackedPhoto(event.id, event.thumbUrl, event.ratio)
                    if (!photoStack.any { it.id == event.id }) photoStack.add(newPhoto)
                    currentPhotoIndex = photoStack.indexOfFirst { it.id == event.id }
                }
                is XrNavEvent.NavigateToUser -> {
                    selectedUsername = event.username
                }
            }
        }
    ) {
        Subspace {
            // Significantly widen the curve radius to 2000.dp to flatten the curve
            SpatialCurvedRow(curveRadius = 2000.dp) {
                val hasUser = selectedUsername != null
                val hasStack = photoStack.isNotEmpty()

                // Set safe total width and gap
                val safeTotalWidth = 1248.dp // Absolute safe limit for 3D rendering
                val gap = 24.dp
                val panelHeight = 800.dp

                val (targetUserWidth, targetMainWidth, targetPhotoWidth) = when {
                    hasUser && hasStack -> {
                        // Scenario A: All three panels open (User:Main:Photo = 1:1:2)
                        val availableWidth = safeTotalWidth - (gap * 2) // 1248 - 48 = 1200
                        val unit = availableWidth / 4                   // 1200 / 4 = 300
                        Triple(unit * 1, unit * 1, unit * 2)            // User: 300, Main: 300, Photo: 600
                    }
                    !hasUser && hasStack -> {
                        // Scenario B: Only Main + Photo (Main:Photo = 1:2)
                        val availableWidth = safeTotalWidth - gap       // 1248 - 24 = 1224
                        val unit = availableWidth / 3                   // 1224 / 3 = 408
                        Triple(0.dp, unit * 1, unit * 2)                // Main: 408, Photo: 816
                    }
                    hasUser && !hasStack -> {
                        // Scenario C: Only User + Main (User:Main = 1:1)
                        val availableWidth = safeTotalWidth - gap       // 1248 - 24 = 1224
                        val unit = availableWidth / 2                   // 1224 / 2 = 612
                        Triple(unit * 1, unit * 1, 0.dp)                // User: 612, Main: 612
                    }
                    else -> {
                        // Scenario D: Only Main panel
                        Triple(0.dp, 800.dp, 0.dp)                      // Provide a comfortable default width for the main panel
                    }
                }

//                // Add smooth transition animations (This makes the XR experience feel premium instantly!)
//                val userPanelWidth by animateDpAsState(
//                    targetValue = targetUserWidth,
//                    animationSpec = tween(500),
//                    label = "userWidth"
//                )
//                val mainPanelWidth by animateDpAsState(
//                    targetValue = targetMainWidth,
//                    animationSpec = tween(500),
//                    label = "mainWidth"
//                )
//                val photoPanelWidth by animateDpAsState(
//                    targetValue = targetPhotoWidth,
//                    animationSpec = tween(500),
//                    label = "photoWidth"
//                )

                val userPanelWidth = targetUserWidth
                val mainPanelWidth = targetMainWidth
                val photoPanelWidth = targetPhotoWidth

                // --- Left (User Panel) ---
                if (hasUser) {
                    SpatialPanel(
                        modifier = SubspaceModifier.width(userPanelWidth).height(panelHeight),
                        dragPolicy = MovePolicy(),
//                        resizePolicy = ResizePolicy()
                    ) {
                        SharedTransitionLayout {
                            AnimatedVisibility(visible = true) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    key(selectedUsername) {
                                        UserDetailScreen(
                                            username = selectedUsername!!,
                                            onBack = { selectedUsername = null },
                                            onNavigateToFeature = { },
                                            sharedTransitionScope = this@SharedTransitionLayout,
                                            animatedVisibilityScope = this@AnimatedVisibility
                                        )
                                    }
                                }
                            }
                        }
                    }
                    SpatialSpacer(modifier = SubspaceModifier.width(gap))
                }

                // --- Center: Main Application ---
                SpatialPanel(
                    modifier = SubspaceModifier.width(mainPanelWidth).height(panelHeight),
                    dragPolicy = MovePolicy(),
//                    resizePolicy = ResizePolicy()
                ) {

                    // 100% manually controlled bottom floating Orbiter.
                    // In alpha14, although EnableXrComponentOverrides can automatically adapt the
                    // NavigationSuiteScaffold's navigationRail / navigationBar inside the App,
                    // it completely fails to control the SpatialPanel's dimensions.
                    // The SpatialPanel will be automatically forced to its maximum size.
                    Orbiter(
                        position = ContentEdge.Bottom,
                        offset = 80.dp, // Floating gap. Setting it to 48.dp still covers the main panel
                        alignment = Alignment.CenterHorizontally
                    ) {
                        // Define a comfortable touch target width for each navigation bar button
                        val singleItemWidth = 90.dp
                        // Dynamically calculate the total width of the capsule
                        val capsuleWidth = singleItemWidth * navItems.size

                        NavigationBar(
                            modifier = Modifier
                                .width(capsuleWidth) // Tech-inspired capsule shape
                                .clip(RoundedCornerShape(32.dp)),
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        ) {

                            navItems.forEach { item ->
                                NavigationBarItem(
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) },
                                    selected = item.isSelected,
                                    onClick = {
                                        // Navigate to the corresponding route on click
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                    // Call the App and forcefully hide the internal 2D navigation bar
                    App(
                        navController = navController,
                        showNavigationBar = false
                    )

                }
                // --- Right (Photo Panel) ---
                if (hasStack) {
                    SpatialSpacer(modifier = SubspaceModifier.width(gap))
                    SpatialPanel(
                        modifier = SubspaceModifier.width(photoPanelWidth).height(panelHeight),
                        dragPolicy = MovePolicy(),
//                        resizePolicy = ResizePolicy()
                    ) {
                        SharedTransitionLayout {
                            AnimatedVisibility(visible = true) {
                                PhotoStackScreen(
                                    photos = photoStack,
                                    currentIndex = currentPhotoIndex,
                                    onIndexChanged = { newIndex -> currentPhotoIndex = newIndex },
                                    onClosePhoto = { closedId ->
                                        val indexToRemove = photoStack.indexOfFirst { it.id == closedId }
                                        if (indexToRemove != -1) {
                                            photoStack.removeAt(indexToRemove)
                                            if (currentPhotoIndex >= photoStack.size) {
                                                currentPhotoIndex = maxOf(0, photoStack.size - 1)
                                            }
                                        }
                                    },
                                    onNavigateToFeature = { },
                                    sharedTransitionScope = this@SharedTransitionLayout,
                                    animatedVisibilityScope = this@AnimatedVisibility
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
