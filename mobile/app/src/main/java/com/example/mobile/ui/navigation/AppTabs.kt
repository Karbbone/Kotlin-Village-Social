package com.example.mobile.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mobile.auth.AuthRepository
import com.example.mobile.cities.CitiesRepository
import com.example.mobile.network.NetworkModule
import com.example.mobile.ui.add.AddEventScreen
import com.example.mobile.ui.cities.CitiesScreen
import com.example.mobile.ui.feed.FeedScreen
import com.example.mobile.ui.profile.ProfileScreen

sealed class NavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Feed : NavItem(
        route = "feed",
        label = "Accueil",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Add : NavItem(
        route = "add",
        label = "Ajouter",
        selectedIcon = Icons.Filled.AddCircle,
        unselectedIcon = Icons.Outlined.AddCircle
    )

    data object Cities : NavItem(
        route = "cities",
        label = "Mes Villes",
        selectedIcon = Icons.Filled.LocationCity,
        unselectedIcon = Icons.Outlined.LocationCity
    )

    data object Profile : NavItem(
        route = "profile",
        label = "Profil",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
}

private val bottomItems = listOf(
    NavItem.Feed,
    NavItem.Add,
    NavItem.Cities,
    NavItem.Profile
)

@Composable
fun AppTabs(
    authRepo: AuthRepository,
    snackbarHostState: SnackbarHostState,
    citiesRepo: CitiesRepository,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val api = remember { NetworkModule.createApi(context, authRepo) }
    Scaffold(
        modifier = modifier,
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            NavigationBar {
                bottomItems.forEach { item ->
                    val selected = currentDestination.isTopLevelSelected(item)
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                // Stratégie recommandée pour bottom bar
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavItem.Feed.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(NavItem.Feed.route) { FeedScreen(citiesRepo = citiesRepo, api = api) }
            composable(NavItem.Add.route) { AddEventScreen(citiesRepo = citiesRepo, api = api, snackbarHostState = snackbarHostState) }
            composable(NavItem.Cities.route) {
                CitiesScreen(
                    api = api,
                    authRepo = authRepo,
                    citiesRepo = citiesRepo,
                    snackbarHostState = snackbarHostState
                )
            }
            composable(NavItem.Profile.route) { ProfileScreen(authRepo = authRepo, api = api, snackbarHostState = snackbarHostState) }
        }
    }
}


private fun NavDestination?.isTopLevelSelected(item: NavItem): Boolean =
    this?.hierarchy?.any { it.route == item.route } == true
