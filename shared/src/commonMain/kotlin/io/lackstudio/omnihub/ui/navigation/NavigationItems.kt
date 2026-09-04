package io.lackstudio.omnihub.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute

import io.lackstudio.omnifeed.auth.domain.model.User

data class NavItem(
    val label: String,
    val icon: ImageVector,
    val route: Any,
    val isSelected: Boolean,
    val photoUrl: String? = null
)

@Composable
fun getAppNavItems(
    navDestination: NavDestination?,
    pagerIndex: Int = 0,
    user: User? = null
): List<NavItem> {
    val isMainTabs = navDestination?.hasRoute<Screen.MainTabs>() == true
    val isAnyFeature = navDestination?.let {
        it.hasRoute<Feature.Gallery>() ||
                it.hasRoute<Feature.Photo>() ||
                it.hasRoute<Feature.Collection>() ||
                it.hasRoute<Feature.Topic>() ||
                it.hasRoute<Feature.User>() ||
                it.hasRoute<Feature.News>() ||
                it.hasRoute<Feature.Stocks>()
    } == true

    return listOf(
        NavItem(
            label = "Home",
            icon = Icons.Filled.Home,
            route = Screen.Home,
            isSelected = (isMainTabs && pagerIndex == 0) ||
                    navDestination?.hasRoute<Screen.Home>() == true ||
                    isAnyFeature
        ),
        NavItem(
            label = "Settings",
            icon = Icons.Filled.Settings,
            route = Screen.Settings,
            isSelected = (isMainTabs && pagerIndex == 1) || navDestination?.hasRoute<Screen.Settings>() == true
        ),
        NavItem(
            label = "Account",
            icon = Icons.Filled.Person,
            route = Screen.Account,
            isSelected = (isMainTabs && pagerIndex == 2) ||
                    navDestination?.hasRoute<Screen.Account>() == true ||
                    navDestination?.hasRoute<Screen.Login>() == true ||
                    navDestination?.hasRoute<Screen.Register>() == true,
            photoUrl = user?.photoUrl
        )
    )
}
