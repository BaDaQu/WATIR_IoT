package com.example.watir_iot_app.feature.joystick

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

    // Paleta Kolorów z Logo WATIR
    val watirNavy = Color(0xFF102A43)
    val watirBlue = Color(0xFF2EB4E6)
    val watirGreen = Color(0xFF549E39)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "Sterowanie Ręczne",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = watirNavy
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Joystick - dodaliśmy rozmiar (size) żeby go powiększyć
        VirtualJoystick(
            modifier = Modifier.size(240.dp),
            onMove = { x, y ->
                watirViewModel.onJoystickMoved(x, y)
            },
            onStop = {
                watirViewModel.onJoystickStopped()
            }
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Wybierz roślinę do kalibracji",
            style = MaterialTheme.typography.titleMedium,
            color = watirNavy
        )

        Spacer(modifier = Modifier.height(12.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp) // Piękne marginesy boczne
        ) {
            OutlinedTextField(
                value = selectedPlant?.name ?: "Wybierz profil",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                    .fillMaxWidth(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                    focusedBorderColor = watirBlue, // Niebieski akcent przy kliknięciu
                    unfocusedBorderColor = watirNavy.copy(alpha = 0.5f)
                ),
                shape = MaterialTheme.shapes.medium
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                plantProfiles.forEach { plant ->
                    DropdownMenuItem(
                        text = { Text(plant.name, color = watirNavy) },
                        onClick = {
                            selectedPlant = plant
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp), // Zrównane marginesy z dropdownem
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    // Miejsce na akcję podlewania
                },
                modifier = Modifier
                    .weight(0.4f) // Zajmuje 40% szerokości
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = watirBlue),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Podlej", fontWeight = FontWeight.Bold)
            }

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
                modifier = Modifier
                    .weight(0.6f) // Zajmuje 60% szerokości (bo ma dłuższy tekst)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = watirGreen, // Kolor sukcesu / rośliny
                    disabledContainerColor = Color.LightGray
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Zapisz pozycję", fontWeight = FontWeight.Bold)
            }
        }
    }
}