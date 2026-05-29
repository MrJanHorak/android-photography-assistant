package com.janhorak.shutterdeck.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.janhorak.shutterdeck.home.HomeScreen
import com.janhorak.shutterdeck.metering.presentation.LightMeterScreen
import com.janhorak.shutterdeck.settings.SettingsScreen
import com.janhorak.shutterdeck.ui.components.PlaceholderScreen

/** Root composable: app bar + bottom navigation hosting the navigation graph. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShutterDeckRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val topLevelRoutes = TopLevelDestination.entries.map { it.route }.toSet()
    val showBackButton = currentRoute != null && currentRoute !in topLevelRoutes

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleForRoute(currentRoute)) },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label,
                            )
                        },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.TOOLS,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.TOOLS) {
                HomeScreen(onToolClick = { route -> navController.navigate(route) })
            }
            composable(Routes.LIGHT_METER) {
                LightMeterScreen()
            }
            composable(Routes.PLANNER) {
                PlaceholderScreen(
                    title = "Planner",
                    message = "Golden hour, sun & moon position, locations, and shot lists are coming here.",
                )
            }
            composable(Routes.GEAR) {
                PlaceholderScreen(
                    title = "Gear",
                    message = "Inventory, maintenance logs, and packing lists are coming here.",
                )
            }
            composable(Routes.MORE) {
                SettingsScreen()
            }
        }
    }
}
