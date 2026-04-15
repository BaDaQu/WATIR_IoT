package com.example.watir_iot_app.core.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.watir_iot_app.feature.charts.ChartsScreen
import com.example.watir_iot_app.feature.dashboard.DashboardScreen
import com.example.watir_iot_app.feature.joystick.JoystickScreen
import com.example.watir_iot_app.feature.settings.SettingsScreen
import com.example.watir_iot_app.feature.splash.SplashScreen

@Composable
fun AppNavigation(){
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarItems = listOf(
        Screen.Dashboard,
        Screen.Charts,
        Screen.Joystick,
        Screen.Settings
    )

// Pasek pokazujemy tylko, gdy nie jesteśmy na ekranie Splash
    val showBottomBar = currentRoute != Screen.Splash.route
    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomBarItems.forEach { screen ->
                        // Dla każdego elementu na liście tworzymy ikonkę w pasku
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = screen.icon!!,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    // Ten kod zapobiega otwieraniu ekranu 100 razy, jak ktoś klika w ikonę jak szalony
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) {innerPadding ->
        NavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            startDestination = Screen.Splash.route)
        {
            composable(Screen.Charts.route) { ChartsScreen() }
            composable(Screen.Dashboard.route) { DashboardScreen() }
            composable(Screen.Joystick.route) { JoystickScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
            composable(Screen.Splash.route) { SplashScreen(navController) }
        }
    }
}