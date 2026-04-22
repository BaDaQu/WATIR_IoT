package com.example.watir_iot_app.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Splash : Screen("splash", "Splash")
    object Dashboard : Screen("dashboard", "Home", Icons.Default.Home)
    object Charts : Screen("charts", "Wykresy", Icons.Default.Timeline)
    object Joystick : Screen("joystick", "Sterowanie", Icons.Default.VideogameAsset)
    object Settings : Screen("settings", "Ustawienia", Icons.Default.Settings)
}