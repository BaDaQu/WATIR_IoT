package com.example.watir_iot_app.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.watir_iot_app.feature.charts.ChartsScreen
import com.example.watir_iot_app.feature.dashboard.DashboardScreen
import com.example.watir_iot_app.feature.joystick.JoystickScreen
import com.example.watir_iot_app.feature.settings.SettingsScreen
import com.example.watir_iot_app.feature.splash.SplashScreen
import com.example.watir_iot_app.viewmodel.WatirViewModel

@Composable
fun AppNavigation(watirViewModel: WatirViewModel){
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isConnected by watirViewModel.isConnected
    val latestData = watirViewModel.telemetryHistory.value.data.firstOrNull()
    val hasWaterError = latestData?.water_error == true

    val bottomBarItems = listOf(
        Screen.Dashboard,
        Screen.Charts,
        Screen.Joystick,
        Screen.Settings
    )

    val showBottomBar = currentRoute != Screen.Splash.route

    Scaffold(
        topBar = {
            if (showBottomBar) {
                if (!isConnected) {
                    ErrorBanner(message = "Urządzenie nieosiągalne (Offline)")
                } else if (hasWaterError) {
                    ErrorBanner(message = "ALARM: Brak wody w zbiorniku!")
                }
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomBarItems.forEach { screen ->
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            startDestination = Screen.Splash.route
        ) {
            composable(Screen.Charts.route) { ChartsScreen(watirViewModel) }
            composable(Screen.Dashboard.route) { DashboardScreen(watirViewModel) }
            composable(Screen.Joystick.route) { JoystickScreen(watirViewModel) }
            composable(Screen.Settings.route) { SettingsScreen(watirViewModel) }
            composable(Screen.Splash.route) { SplashScreen(navController) }
        }
    }
}

@Composable
fun ErrorBanner(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFD32F2F))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}