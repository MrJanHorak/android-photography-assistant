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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.janhorak.shutterdeck.calculators.presentation.AstroShutterScreen
import com.janhorak.shutterdeck.calculators.presentation.DepthOfFieldScreen
import com.janhorak.shutterdeck.calculators.presentation.DiffractionScreen
import com.janhorak.shutterdeck.calculators.presentation.EquivalentExposureScreen
import com.janhorak.shutterdeck.calculators.presentation.FieldOfViewScreen
import com.janhorak.shutterdeck.calculators.presentation.FocusStackingScreen
import com.janhorak.shutterdeck.calculators.presentation.GuideNumberScreen
import com.janhorak.shutterdeck.calculators.presentation.MacroScreen
import com.janhorak.shutterdeck.calculators.presentation.NdFilterScreen
import com.janhorak.shutterdeck.calculators.presentation.PrintSizeScreen
import com.janhorak.shutterdeck.calculators.presentation.Sunny16Screen
import com.janhorak.shutterdeck.calculators.presentation.SunMoonPositionScreen
import com.janhorak.shutterdeck.calculators.presentation.SunTimesScreen
import com.janhorak.shutterdeck.film.presentation.FilmDevelopmentScreen
import com.janhorak.shutterdeck.film.presentation.FilmPushPullScreen
import com.janhorak.shutterdeck.film.presentation.FilmReciprocityScreen
import com.janhorak.shutterdeck.film.presentation.FilmScreen
import com.janhorak.shutterdeck.film.presentation.FilmRollDetailScreen
import com.janhorak.shutterdeck.film.presentation.FilmRollsScreen
import com.janhorak.shutterdeck.film.presentation.FilmStocksScreen
import com.janhorak.shutterdeck.gear.presentation.GearInventoryScreen
import com.janhorak.shutterdeck.home.HomeScreen
import com.janhorak.shutterdeck.metering.presentation.LightMeterScreen
import com.janhorak.shutterdeck.planner.presentation.LocationsScreen
import com.janhorak.shutterdeck.planner.presentation.PlannerScreen
import com.janhorak.shutterdeck.planner.presentation.ShootDetailScreen
import com.janhorak.shutterdeck.planner.presentation.ShootsScreen
import com.janhorak.shutterdeck.settings.SettingsScreen
import com.janhorak.shutterdeck.utilities.presentation.CompositionOverlayScreen
import com.janhorak.shutterdeck.utilities.presentation.GrayCardScreen
import com.janhorak.shutterdeck.utilities.presentation.SpiritLevelScreen

/** Root composable: app bar + bottom navigation hosting the navigation graph. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShutterDeckRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val topLevelRoutes = TopLevelDestination.entries.map { it.route }.toSet()
    val showBackButton = currentRoute != null && currentRoute !in topLevelRoutes
    val chromeHiddenRoutes = setOf(
        Routes.GRAY_CARD,
        Routes.SPIRIT_LEVEL,
        Routes.COMPOSITION_OVERLAYS,
    )
    val hideChrome = currentRoute in chromeHiddenRoutes

    Scaffold(
        topBar = {
            if (!hideChrome) {
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
            }
        },
        bottomBar = {
            if (!hideChrome) {
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
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.TOOLS,
            modifier = if (hideChrome) Modifier else Modifier.padding(innerPadding),
        ) {
            composable(Routes.TOOLS) {
                HomeScreen(onToolClick = { route -> navController.navigate(route) })
            }
            composable(Routes.LIGHT_METER) {
                LightMeterScreen()
            }
            composable(Routes.DEPTH_OF_FIELD) {
                DepthOfFieldScreen()
            }
            composable(Routes.ND_FILTER) {
                NdFilterScreen()
            }
            composable(Routes.FIELD_OF_VIEW) {
                FieldOfViewScreen()
            }
            composable(Routes.ASTRO_SHUTTER) {
                AstroShutterScreen()
            }
            composable(Routes.PRINT_SIZE) {
                PrintSizeScreen()
            }
            composable(Routes.FOCUS_STACK) {
                FocusStackingScreen()
            }
            composable(Routes.SUNNY_16) {
                Sunny16Screen()
            }
            composable(Routes.GUIDE_NUMBER) {
                GuideNumberScreen()
            }
            composable(Routes.EQUIVALENT_EXPOSURE) {
                EquivalentExposureScreen()
            }
            composable(Routes.SPIRIT_LEVEL) {
                SpiritLevelScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.GRAY_CARD) {
                GrayCardScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.COMPOSITION_OVERLAYS) {
                CompositionOverlayScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.MACRO) {
                MacroScreen()
            }
            composable(Routes.DIFFRACTION) {
                DiffractionScreen()
            }
            composable(Routes.SUN_TIMES) {
                SunTimesScreen()
            }
            composable(Routes.SUN_MOON_POSITION) {
                SunMoonPositionScreen()
            }
            composable(Routes.PLANNER) {
                PlannerScreen(onOpen = { route -> navController.navigate(route) })
            }
            composable(Routes.LOCATIONS) {
                LocationsScreen()
            }
            composable(Routes.SHOOTS) {
                ShootsScreen(onOpenShoot = { id -> navController.navigate(Routes.shootDetail(id)) })
            }
            composable(
                route = Routes.SHOOT_DETAIL,
                arguments = listOf(navArgument(Routes.SHOOT_ID_ARG) { type = NavType.LongType }),
            ) {
                ShootDetailScreen()
            }
            composable(Routes.GEAR) {
                GearInventoryScreen()
            }
            composable(Routes.FILM) {
                FilmScreen(onOpen = { route -> navController.navigate(route) })
            }
            composable(Routes.FILM_STOCKS) {
                FilmStocksScreen()
            }
            composable(Routes.FILM_ROLLS) {
                FilmRollsScreen(onOpenRoll = { rollId -> navController.navigate(Routes.filmRollDetailRoute(rollId)) })
            }
            composable(Routes.FILM_DEVELOPMENT) {
                FilmDevelopmentScreen()
            }
            composable(Routes.FILM_PUSH_PULL) {
                FilmPushPullScreen()
            }
            composable(Routes.FILM_RECIPROCITY) {
                FilmReciprocityScreen()
            }
            composable(
                route = Routes.FILM_ROLL_DETAIL,
                arguments = listOf(navArgument(Routes.FILM_ROLL_ID_ARG) { type = NavType.LongType }),
            ) {
                FilmRollDetailScreen()
            }
            composable(Routes.MORE) {
                SettingsScreen()
            }
        }
    }
}
