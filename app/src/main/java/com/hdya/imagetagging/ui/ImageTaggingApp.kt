package com.hdya.imagetagging.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hdya.imagetagging.R
import com.hdya.imagetagging.data.AppDatabase
import com.hdya.imagetagging.data.PreferencesRepository
import com.hdya.imagetagging.ui.gallery.GalleryScreen
import com.hdya.imagetagging.ui.labels.LabelsScreen
import com.hdya.imagetagging.ui.settings.SettingsScreen

sealed class Screen(val route: String, val titleRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Gallery : Screen("gallery", R.string.gallery, Icons.Filled.Home)
    object Labels : Screen("labels", R.string.labels, Icons.Filled.Star)
    object Settings : Screen("settings", R.string.settings, Icons.Filled.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageTaggingApp(
    database: AppDatabase,
    preferencesRepository: PreferencesRepository
) {
    val navController = rememberNavController()
    val items = listOf(Screen.Gallery, Screen.Labels, Screen.Settings)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) }
            )
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(stringResource(screen.titleRes)) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Gallery.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Gallery.route) {
                GalleryScreen(
                    database = database,
                    preferencesRepository = preferencesRepository
                )
            }
            composable(Screen.Labels.route) {
                LabelsScreen(database = database)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    preferencesRepository = preferencesRepository,
                    database = database
                )
            }
        }
    }
}