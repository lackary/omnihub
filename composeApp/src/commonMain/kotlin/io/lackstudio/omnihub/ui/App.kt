package io.lackstudio.omnihub.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavDestination.Companion.hasRoute
import io.lackstudio.omnihub.ui.account.AccountScreen
import io.lackstudio.omnihub.ui.home.HomeScreen
import io.lackstudio.omnihub.ui.navigation.Feature
import io.lackstudio.omnihub.ui.navigation.Screen
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    val navController = rememberNavController()
    // Get current route state, used to determine BottomBar selection state
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry.value?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    // Check if the current route is Screen.Home
                    selected = currentDestination?.hasRoute<Screen.Home>() == true,
                    onClick = {
                        navController.navigate(Screen.Home) {
                            // Avoid excessive stacking, pop up to the start destination when Home is clicked
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Account") },
                    label = { Text("Account") },
                    selected = currentDestination?.hasRoute<Screen.Account>() == true,
                    onClick = {
                        navController.navigate(Screen.Account) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home,
            // The top section is handled by each Screen's internal TopAppBar
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // 1. Home Screen
            composable<Screen.Home> {
                HomeScreen(
                    onNavigateToFeature = { feature ->
                        navController.navigate(feature)
                    }
                )
            }

            // 2. Account Screen (Simple Example)
            composable<Screen.Account> {
                AccountScreen(
                    onNavigateToFeature = { feature ->
                        navController.navigate(feature)
                    }
                )
            }

            // 3. Definitions for each Feature page
            composable<Feature.Photos> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Gallery API Page")
                }
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
