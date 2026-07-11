package com.iamsubho.drivesync.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.iamsubho.drivesync.presentation.browser.BrowserFilesScreen
import com.iamsubho.drivesync.presentation.browser.BrowserScreen
import com.iamsubho.drivesync.presentation.details.DetailsScreen
import com.iamsubho.drivesync.presentation.home.HomeScreen
import com.iamsubho.drivesync.presentation.settings.SettingsScreen
import com.iamsubho.drivesync.presentation.wizard.WizardScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenWizard = { navController.navigate(Routes.WIZARD) },
                onOpenDetails = { jobId -> navController.navigate(Routes.details(jobId)) },
                onOpenBrowser = { email -> navController.navigate(Routes.browser(email)) },
            )
        }
        composable(Routes.WIZARD) {
            WizardScreen(onExit = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.DETAILS,
            arguments = listOf(navArgument("jobId") { type = NavType.LongType }),
        ) {
            DetailsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.BROWSER,
            arguments = listOf(navArgument("email") { type = NavType.StringType }),
        ) { entry ->
            val email = entry.arguments?.getString("email").orEmpty()
            BrowserScreen(
                onBack = { navController.popBackStack() },
                onOpenFolder = { folderId, folderName ->
                    navController.navigate(Routes.browserFiles(email, folderId, folderName))
                },
            )
        }
        composable(
            route = Routes.BROWSER_FILES,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType },
                navArgument("folderId") { type = NavType.StringType },
                navArgument("folderName") {
                    type = NavType.StringType
                    defaultValue = "Drive folder"
                },
            ),
        ) {
            BrowserFilesScreen(onBack = { navController.popBackStack() })
        }
    }
}
