package com.example.watir_iot_app.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.watir_iot_app.R

sealed class Screen(val route: String, val titleResId: Int, val icon: ImageVector? = null) {
    object Splash : Screen("splash", R.string.app_name)
    object Dashboard : Screen("dashboard", R.string.nav_home, Icons.Default.Home)
    object Charts : Screen("charts", R.string.nav_charts, Icons.Default.Timeline)
    object Joystick : Screen("joystick", R.string.nav_joystick, Icons.Default.VideogameAsset)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
}