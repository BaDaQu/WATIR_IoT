package com.example.watir_iot_app.feature.joystick

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.watir_iot_app.feature.joystick.components.VirtualJoystick
import com.example.watir_iot_app.viewmodel.WatirViewModel

@Composable
fun JoystickScreen(watirViewModel: WatirViewModel) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally){
        VirtualJoystick(
            onMove = { x, y ->
                watirViewModel.onJoystickMoved(x, y)
            },
            onStop = {
                watirViewModel.onJoystickStopped()
            }
        )
        Spacer(modifier = Modifier.height(48.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = {

            }) {Text("Podlej") }

            Button(onClick = {

            }) {Text("Zapisz pozycję") }
        }
    }
}