package com.example.watir_iot_app.feature.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.watir_iot_app.data.model.PlantProfile
import com.example.watir_iot_app.viewmodel.WatirViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: WatirViewModel) {
    val context = LocalContext.current
    var ipInput by remember { mutableStateOf("192.168.1.50") }

    val plants by viewModel.plantProfiles
    var expanded by remember { mutableStateOf(false) }
    var selectedPlant by remember { mutableStateOf<PlantProfile?>(null) }

    var showDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editId by remember { mutableStateOf(0) }
    
    var nameInput by remember { mutableStateOf("") }
    var thresholdInput by remember { mutableStateOf("") }
    var intervalInput by remember { mutableStateOf("") }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (isEditing) "Edytuj profil" else "Nowy profil") },
            text = {
                Column {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Nazwa") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = thresholdInput,
                        onValueChange = { thresholdInput = it },
                        label = { Text("Próg wilgotności (%)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = intervalInput,
                        onValueChange = { intervalInput = it },
                        label = { Text("Interwał sprawdzania (ms)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val threshold = thresholdInput.toIntOrNull() ?: 50
                        val interval = intervalInput.toIntOrNull() ?: 10000
                        if (isEditing) {
                            viewModel.updatePlantProfile(
                                id = editId,
                                name = nameInput,
                                moistureThreshold = threshold,
                                autoWatering = true,
                                checkIntervalMs = interval,
                                onSuccess = {
                                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                    showDialog = false
                                },
                                onError = {
                                    Toast.makeText(context, "Błąd: $it", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            viewModel.createPlantProfile(
                                name = nameInput,
                                moistureThreshold = threshold,
                                autoWatering = true,
                                checkIntervalMs = interval,
                                onSuccess = {
                                    Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                    showDialog = false
                                },
                                onError = {
                                    Toast.makeText(context, "Błąd: $it", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                ) {
                    Text("Zapisz")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }

    Column(modifier = Modifier.padding(16.dp)) {

        OutlinedTextField(
            value = ipInput,
            onValueChange = { ipInput = it },
            label = { Text("Server IP") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.connectToServer(ipInput) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Connect")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (viewModel.isConnected.value) "Status: Connected"
            else "Status: Disconnected"
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Profile Roślin")
            IconButton(onClick = {
                isEditing = false
                nameInput = ""
                thresholdInput = ""
                intervalInput = "10000"
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj profil")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedPlant?.name ?: "Wybierz profil",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    plants.forEach { plant ->
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

            if (selectedPlant != null) {
                IconButton(onClick = {
                    selectedPlant?.let { plant ->
                        isEditing = true
                        editId = plant.id
                        nameInput = plant.name
                        thresholdInput = plant.moisture_threshold.toString()
                        intervalInput = plant.check_interval_ms.toString()
                        showDialog = true
                    }
                }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edytuj profil")
                }
                IconButton(onClick = {
                    selectedPlant?.let { plant ->
                        viewModel.deletePlantProfile(
                            id = plant.id,
                            onSuccess = {
                                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                                selectedPlant = null
                            },
                            onError = { Toast.makeText(context, "Błąd: $it", Toast.LENGTH_SHORT).show() }
                        )
                    }
                }) {
                    Icon(Icons.Default.Delete, contentDescription = "Usuń profil")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                selectedPlant?.let { plant ->
                    viewModel.applyPlantProfile(
                        id = plant.id,
                        onSuccess = { msg ->
                            Toast.makeText(context, "Sukces: $msg", Toast.LENGTH_SHORT).show()
                        },
                        onError = { err ->
                            Toast.makeText(context, "Błąd: $err", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            },
            enabled = selectedPlant != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Zastosuj profil na ESP32")
        }
    }
}