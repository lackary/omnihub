package io.lackstudio.omnihub.compose.layout

import android.content.Intent
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.xr.compose.spatial.ContentEdge
import androidx.xr.compose.spatial.Orbiter
import androidx.xr.compose.spatial.Subspace
import androidx.xr.compose.subspace.MovePolicy
import androidx.xr.compose.subspace.SpatialCurvedRow
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.width
import io.lackstudio.omnihub.compose.ui.App
import io.lackstudio.omnihub.compose.ui.navigation.XrNavEvent
import io.lackstudio.omnihub.compose.ui.navigation.getAppNavItems
import io.lackstudio.omnihub.compose.utils.LocalXrNavigation

@Composable
fun XrSpatialLayout() {
    val context = LocalContext.current

    // Declare NavController at the top level so the Orbiter can use it
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val navItems = getAppNavItems(currentDestination)

    CompositionLocalProvider(
        LocalXrNavigation provides { event ->
            val intent = when(event) {
                is XrNavEvent.NavigateToPhoto -> {
                    // Start PhotoStackActivity
                    // Note: The Activity name should match what's defined in androidApp
                    Intent().setClassName(context.packageName, "${context.packageName}.PhotoStackActivity").apply {
                        putExtra("PHOTO_ID", event.id)
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                            Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
                        )
                    }

                }
                is XrNavEvent.NavigateToUser -> {
                    // Start UserDetailActivity
                    Intent().setClassName(context.packageName, "${context.packageName}.UserDetailActivity").apply {
                        putExtra("USERNAME", event.username)
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_MULTIPLE_TASK or
                            Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT
                        )
                    }
                }
            }
            context.startActivity(intent)
        }
    ) {
        Subspace {
            // Significantly widen the curve radius to 2000.dp to flatten the curve
            SpatialCurvedRow(curveRadius = 2000.dp) {
                val panelHeight = 800.dp
                val mainPanelWidth = 1000.dp

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
                    App(
                        navController = navController,
                        showNavigationBar = false
                    )

                }
            }
        }
    }
}
