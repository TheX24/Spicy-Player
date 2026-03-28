package com.omar.musica.settings.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import com.omar.musica.settings.SettingsScreen
import com.omar.musica.settings.ResetSettingsScreen
import com.omar.musica.settings.SettingsViewModel
import com.omar.musica.ui.model.UserPreferencesUi


const val SETTINGS_NAVIGATION_GRAPH = "settings_graph"
const val SETTINGS_ROUTE = "settings_route"
const val RESET_SETTINGS_ROUTE = "reset_settings_route"


fun NavGraphBuilder.settingsGraph(
    contentModifier: MutableState<Modifier>,
    navController: NavHostController,
    onBackPressed: () -> Unit,
    enterAnimationFactory:
        (String, AnimatedContentTransitionScope<NavBackStackEntry>) -> EnterTransition,
    exitAnimationFactory:
        (String, AnimatedContentTransitionScope<NavBackStackEntry>) -> ExitTransition,
    popEnterAnimationFactory:
        (String, AnimatedContentTransitionScope<NavBackStackEntry>) -> EnterTransition,
    popExitAnimationFactory:
        (String, AnimatedContentTransitionScope<NavBackStackEntry>) -> ExitTransition,
) {

    navigation(
        route = SETTINGS_NAVIGATION_GRAPH,
        startDestination = SETTINGS_ROUTE
    ) {
        composable(
            SETTINGS_ROUTE,
            enterTransition = {
                enterAnimationFactory(SETTINGS_ROUTE, this)
            },
            exitTransition = {
                exitAnimationFactory(SETTINGS_ROUTE, this)
            },
            popEnterTransition = {
                popEnterAnimationFactory(SETTINGS_ROUTE, this)
            },
            popExitTransition = {
                popExitAnimationFactory(SETTINGS_ROUTE, this)
            }
        ) {
            val viewModel: SettingsViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            val scanDirectory by viewModel.scanDirectory.collectAsState()
            val scanProgress by viewModel.scanProgress.collectAsState()
            val scanHistory by viewModel.scanHistory.collectAsState()
            val updateStatus by viewModel.updateStatus.collectAsState()

            SettingsScreen(
                modifier = contentModifier.value,
                state = state,
                scanDirectory = scanDirectory,
                scanProgress = scanProgress,
                scanHistory = scanHistory,
                updateStatus = updateStatus,
                settingsCallbacks = viewModel,
                onBackPressed = onBackPressed,
                onNavigateToReset = { navController.navigate(RESET_SETTINGS_ROUTE) }
            )
        }

        composable(
            RESET_SETTINGS_ROUTE,
            enterTransition = { enterAnimationFactory(RESET_SETTINGS_ROUTE, this) },
            exitTransition = { exitAnimationFactory(RESET_SETTINGS_ROUTE, this) },
            popEnterTransition = { popEnterAnimationFactory(RESET_SETTINGS_ROUTE, this) },
            popExitTransition = { popExitAnimationFactory(RESET_SETTINGS_ROUTE, this) }
        ) {
            val viewModel: SettingsViewModel = hiltViewModel()
            val state by viewModel.state.collectAsState()
            val userPreferences = (state as? com.omar.musica.settings.SettingsState.Loaded)?.userPreferences ?: UserPreferencesUi()
            ResetSettingsScreen(
                userPreferences = userPreferences,
                settingsCallbacks = viewModel,
                onBack = { navController.popBackStack() } 
            )
        }
    }

}