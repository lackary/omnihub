package io.lackstudio.omnihub.compose.layout

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.spatial.ContentEdge
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.MovePolicy
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.width
import io.lackstudio.omnihub.compose.ui.App
import io.lackstudio.omnihub.compose.ui.navigation.XrNavigationController
import io.lackstudio.omnihub.compose.ui.navigation.getAppNavItems
import io.lackstudio.omnihub.compose.utils.LocalXrNavigation

@Composable
fun XrAppLayout() {
    val context = LocalContext.current
    val session = LocalSession.current
    val density = LocalDensity.current

    // 🚀 Listen for global navigation requests from other activities/panels
    LaunchedEffect(session) {
        XrNavigationController.navRequests.collect { event ->
            println("[Debug XR] Received proxy request in Main: $event")
            XrNavigationController.navigate(context, session, density, event)
        }
    }

    // Declare NavController at the top level so the Orbiter can use it
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val navItems = getAppNavItems(currentDestination)

    CompositionLocalProvider(
        LocalXrNavigation provides { event ->
            println("[Debug XR] LocalXrNavigation event: $event")
            XrNavigationController.navigate(context, session, density, event)
        }
    ) {
        Subspace {
            // Main Application Panel at Center (0,0,0)
            val panelHeight = 800.dp
            val mainPanelWidth = 1000.dp

            SpatialPanel(
                modifier = SubspaceModifier.width(mainPanelWidth).height(panelHeight),
                dragPolicy = MovePolicy()
            ) {
                Orbiter(
                    position = ContentEdge.Bottom,
                    offset = 80.dp,
                    alignment = Alignment.CenterHorizontally
                ) {
                    val singleItemWidth = 90.dp
                    val capsuleWidth = singleItemWidth * navItems.size

                    NavigationBar(
                        modifier = Modifier
                            .width(capsuleWidth)
                            .clip(RoundedCornerShape(32.dp)),
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    ) {
                        navItems.forEach { item ->
                            NavigationBarItem(
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
                }
                App(
                    navController = navController,
                    showNavigationBar = false
                )
            }
        }
    }
}
