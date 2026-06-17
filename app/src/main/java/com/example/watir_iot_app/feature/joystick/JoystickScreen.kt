package com.example.watir_iot_app.feature.joystick

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.watir_iot_app.data.model.PlantProfile
import com.example.watir_iot_app.feature.joystick.components.VirtualJoystick
import com.example.watir_iot_app.viewmodel.WatirViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoystickScreen(watirViewModel: WatirViewModel) {

    val context = LocalContext.current
    val plantProfiles by watirViewModel.plantProfiles

    var expanded by remember { mutableStateOf(false) }
    var selectedPlant by remember { mutableStateOf<PlantProfile?>(null) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally)
    {

        Text("Sterowanie Ręczne", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        VirtualJoystick(
            onMove = { x, y ->
                watirViewModel.onJoystickMoved(x, y)
            },
            onStop = {
                watirViewModel.onJoystickStopped()
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text("Wybierz roślinę do kalibracji", style = MaterialTheme.typography.labelLarge)

        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedPlant?.name ?: "Wybierz profil",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                    .fillMaxWidth(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                plantProfiles.forEach { plant ->
                    DropdownMenuItem(
                        text = { Text(plant.name) },
                        onClick = {
                            selectedPlant = plant
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = {

            }) {Text("Podlej") }

            Button(
                onClick = {
                    selectedPlant?.let { plant ->
                        watirViewModel.saveCurrentPositionForPlant(
                            id = plant.id,
                            onSuccess = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() },
                            onError = { err -> Toast.makeText(context, err, Toast.LENGTH_SHORT).show() }
                        )
                    }
                },
                enabled = selectedPlant != null,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                contentPadding = PaddingValues(12.dp)
            ) {
                Text("Zapisz pozycje dla rośliny")
            }
        }
    }
}