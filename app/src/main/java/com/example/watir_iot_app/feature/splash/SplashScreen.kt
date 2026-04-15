package com.example.watir_iot_app.feature.splash

import android.R.attr.padding
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.watir_iot_app.R
import com.example.watir_iot_app.core.navigation.Screen
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavHostController) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.White),
        verticalArrangement = Arrangement.Center, // Środkuje zawartość w pionie
        horizontalAlignment = Alignment.CenterHorizontally // Środkuje zawartość w poziomie
    ) {
        Image(
            painterResource(id = R.drawable.logo),
            contentDescription = "Logo aplikacji",
            modifier = Modifier.size(200.dp),
        )
        Spacer(
            modifier = Modifier.height(16.dp)
        )
        Text(text = "Loading WATIR_IOT app")
    }
    LaunchedEffect(Unit) {
        delay(2000)
        navController.navigate(Screen.Dashboard.route){
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }
}