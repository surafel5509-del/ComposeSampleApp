package com.example.composeapp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.composeapp.feature.home.HomeScreen
import com.example.composeapp.feature.projects.ProjectsScreen
import com.example.composeapp.feature.settings.SettingsScreen

private object Routes {
    const val HOME = "home"
    const val PROJECTS = "projects"
    const val SETTINGS = "settings"
}

@Composable
fun MoreCutApp(navController: NavHostController) {
    Surface(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        NavHost(navController = navController, startDestination = Routes.HOME) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenProjects = { navController.navigate(Routes.PROJECTS) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onCreateProject = { navController.navigate(Routes.PROJECTS) },
                )
            }
            composable(Routes.PROJECTS) {
                ProjectsScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
