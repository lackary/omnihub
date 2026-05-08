package io.lackstudio.omnihub.compose.layout

import android.app.Activity
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
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Quaternion
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.ActivityPanelEntity
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
    val session = LocalSession.current

    val density = LocalDensity.current
    val mainPanelWidth = 1000.dp
    val panelHeight = 800.dp
    val sidePanelWidth = 1000.dp

    CompositionLocalProvider(
        LocalXrNavigation provides { event ->
            session?.let { xrSession ->
                // 1000.dp.value returns Float type 1000f
                val sideWidthPx = with(density) { sidePanelWidth.roundToPx() }
                val sideHeightPx = with(density) { panelHeight.roundToPx() }
                val panelSize = IntSize2d(sideWidthPx, sideHeightPx)

                // Main panel (Native API): Convert dp directly to meters (1000dp -> 1.0m)
                val mainPhysicalWidth = mainPanelWidth.value / 1000f

                // Side panel (Legacy API): The system forces pixels to be treated as physical size, so we must calculate using Px!
                // (e.g., 1800px will be forced by the system to render as 1.8m; we must face this reality)
                val sidePhysicalWidth = sideWidthPx / 1000f

                // 10cm gap
                val gapInMeters = 0.1f

                // (Main screen half) + (Side screen half) + (Gap)
                val offsetX = (mainPhysicalWidth / 2f) + (sidePhysicalWidth / 2f) // + gapInMeters

                println("Calculated side panel pixels: $sideWidthPx px, Physical offset: $offsetX m")

                // Determine Intent, panel name, and initial pose based on the event
                val (intent, panelName, launchPose) = when(event) {
                    is XrNavEvent.NavigateToPhoto -> {
                        val i = Intent().setClassName(context.packageName, "${context.packageName}.PhotoStackActivity").apply {
                            putExtra("PHOTO_ID", event.id)
                        }
                        val p = Pose(Vector3(offsetX, 0f, 0.15f), Quaternion.fromEulerAngles(0f, -25f, 0f))
                        Triple(i, "PhotoStackPanel", p)
                    }
                    is XrNavEvent.NavigateToUser -> {
                        val i = Intent().setClassName(context.packageName, "${context.packageName}.UserDetailActivity").apply {
                            putExtra("USERNAME", event.username)
                        }
                        val p = Pose(Vector3(-offsetX, 0f, 0.15f), Quaternion.fromEulerAngles(0f, 25f, 0f))
                        Triple(i, "UserDetailPanel", p)
                    }
                }

                // Create ActivityPanelEntity
                val activityPanelEntity = ActivityPanelEntity.create(
                    session = xrSession,
                    pixelDimensions = panelSize,
                    name = panelName,
                    pose = launchPose
                )

                activityPanelEntity.startActivity(intent)
            }
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
