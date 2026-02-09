package app.lifelauncher

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.lifelauncher.data.AppListRepository
import app.lifelauncher.data.PreferencesRepository
import app.lifelauncher.ui.drawer.AppDrawerScreen
import app.lifelauncher.ui.home.HomeScreen
import app.lifelauncher.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Settings : Screen("settings")
    data object AppDrawer : Screen("drawer?selectFor={selectFor}") {
        fun createRoute(selectFor: String? = null) = "drawer?selectFor=${selectFor ?: ""}"
    }
}

@Composable
fun LifeLauncherContent() {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    val prefsRepo = remember { PreferencesRepository(context) }
    val appListRepo = remember { AppListRepository(context) }
    
    val hiddenApps by prefsRepo.hiddenApps.collectAsState(initial = emptySet())
    
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                prefsRepo = prefsRepo,
                appListRepo = appListRepo,
                onOpenDrawer = { selectFor ->
                    navController.navigate(Screen.AppDrawer.createRoute(selectFor))
                },
                onOpenSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                prefsRepo = prefsRepo,
                onNavigateToAppSelect = { selectFor ->
                    navController.navigate(Screen.AppDrawer.createRoute(selectFor))
                },
                onDismiss = { navController.popBackStack() }
            )
        }
        
        composable(Screen.AppDrawer.route) { backStackEntry ->
            val selectFor = backStackEntry.arguments?.getString("selectFor")?.ifBlank { null }
            
            AppDrawerScreen(
                appListRepo = appListRepo,
                prefsRepo = prefsRepo,
                hiddenApps = hiddenApps,
                selectFor = selectFor,
                onAppSelected = { app, forSlot ->
                    if (forSlot != null) {
                        // Selection mode - go back
                        navController.popBackStack()
                    } else {
                        // Launch mode
                        appListRepo.launchApp(app.packageName, app.className)
                    }
                },
                onDismiss = { navController.popBackStack() }
            )
        }
    }
}
