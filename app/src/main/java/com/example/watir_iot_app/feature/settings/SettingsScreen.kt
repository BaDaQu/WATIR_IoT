package com.example.watir_iot_app.feature.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import com.example.watir_iot_app.R
import com.example.watir_iot_app.data.model.PlantProfile
import com.example.watir_iot_app.data.model.GlobalSettingsRequest
import com.example.watir_iot_app.viewmodel.WatirViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: WatirViewModel) {
    val context = LocalContext.current
    var ipInput by remember { mutableStateOf("192.168.1.50") }

    val plants by viewModel.plantProfiles
    var expanded by remember { mutableStateOf(false) }
    var selectedPlant by remember { mutableStateOf<PlantProfile?>(null) }

    LaunchedEffect(plants) {
        selectedPlant?.let { current ->
            selectedPlant = plants.find { it.id == current.id } ?: current
        }
    }

    var showDialog by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editId by remember { mutableStateOf(0) }

    var nameInput by remember { mutableStateOf("") }
    var thresholdInput by remember { mutableStateOf("") }
    var intervalInput by remember { mutableStateOf("") }
    var sensorInput by remember { mutableStateOf(1) }
    var pumpPowerInput by remember { mutableStateOf("70") }

    val isDarkMode by viewModel.isDarkMode
    val currentLang by viewModel.language

    val globalSettings by viewModel.globalSettings
    var minTempInput by remember { mutableStateOf("5") }
    var maxTempInput by remember { mutableStateOf("35") }
    var minHumidityInput by remember { mutableStateOf("30") }

    LaunchedEffect(globalSettings) {
        globalSettings?.let {
            minTempInput = it.min_temp_block.toString()
            maxTempInput = it.max_temp_force.toString()
            minHumidityInput = it.min_air_humidity_force.toString()
        }
    }

    val watirNavy = Color(0xFF102A43)
    val watirBlue = Color(0xFF2EB4E6)
    val watirGreen = Color(0xFF549E39)
    val watirRed = Color(0xFFD32F2F)

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { 
                Text(
                    if (isEditing) stringResource(R.string.settings_edit_profile) else stringResource(R.string.settings_new_profile_dialog), 
                    style = MaterialTheme.typography.titleLarge
                ) 
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text(stringResource(R.string.settings_name_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = thresholdInput,
                        onValueChange = { thresholdInput = it },
                        label = { Text(stringResource(R.string.settings_moisture_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = intervalInput,
                        onValueChange = { intervalInput = it },
                        label = { Text(stringResource(R.string.settings_interval_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(R.string.soil_sensor_label), style = MaterialTheme.typography.labelLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.RadioButton(
                            selected = sensorInput == 1,
                            onClick = { sensorInput = 1 },
                            colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = watirNavy)
                        )
                        Text(stringResource(R.string.sensor_1_g1))
                        Spacer(modifier = Modifier.width(16.dp))
                        androidx.compose.material3.RadioButton(
                            selected = sensorInput == 2,
                            onClick = { sensorInput = 2 },
                            colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = watirNavy)
                        )
                        Text(stringResource(R.string.sensor_2_g2))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pumpPowerInput,
                        onValueChange = { pumpPowerInput = it },
                        label = { Text(stringResource(R.string.settings_pump_power_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val threshold = thresholdInput.toIntOrNull() ?: 50
                        val interval = intervalInput.toIntOrNull() ?: 10000
                        val power = pumpPowerInput.toIntOrNull() ?: 70
                        if (isEditing) {
                            viewModel.updatePlantProfile(
                                id = editId,
                                name = nameInput,
                                moistureThreshold = threshold,
                                autoWatering = true,
                                checkIntervalMs = interval,
                                sensor = sensorInput,
                                pumpPower = power,
                                onSuccess = {
                                    Toast.makeText(context, context.getString(R.string.success_prefix, it), Toast.LENGTH_SHORT).show()
                                    showDialog = false
                                },
                                onError = {
                                    Toast.makeText(context, context.getString(R.string.error_prefix, it), Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            viewModel.createPlantProfile(
                                name = nameInput,
                                moistureThreshold = threshold,
                                autoWatering = true,
                                checkIntervalMs = interval,
                                sensor = sensorInput,
                                pumpPower = power,
                                onSuccess = {
                                    Toast.makeText(context, context.getString(R.string.success_prefix, it), Toast.LENGTH_SHORT).show()
                                    showDialog = false
                                },
                                onError = {
                                    Toast.makeText(context, context.getString(R.string.error_prefix, it), Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                ) {
                    Text(stringResource(R.string.settings_save), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false }
                ) {
                    Text(stringResource(R.string.settings_cancel))
                }
            }
        )
    }

    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        
        Text(text = stringResource(R.string.nav_settings), style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Dark Mode Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.settings_dark_mode), style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = isDarkMode,
                onCheckedChange = { viewModel.toggleDarkMode(it) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Language Selector
        Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.bodyLarge)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.setLanguage("pl") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentLang == "pl") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (currentLang == "pl") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.lang_pl))
            }
            Button(
                onClick = { viewModel.setLanguage("en") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentLang == "en") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (currentLang == "en") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.lang_en))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = ipInput,
            onValueChange = { ipInput = it },
            label = { Text(stringResource(R.string.settings_server_ip)) },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { viewModel.connectToServer(ipInput) },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(stringResource(R.string.settings_connect), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (viewModel.isConnected.value) stringResource(R.string.settings_status_connected) else stringResource(R.string.settings_status_disconnected),
            color = if (viewModel.isConnected.value) watirGreen else watirRed,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.settings_plant_profiles), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = {
                isEditing = false
                nameInput = ""
                thresholdInput = ""
                intervalInput = "10000"
                sensorInput = 1
                pumpPowerInput = "70"
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.settings_add_profile))
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
                    value = selectedPlant?.name ?: stringResource(R.string.settings_select_profile),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true).fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
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
                        sensorInput = plant.sensor
                        pumpPowerInput = plant.pump_power.toString()
                        showDialog = true
                    }
                }) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.settings_edit_profile), tint = watirBlue)
                }
                IconButton(onClick = {
                    selectedPlant?.let { plant ->
                        viewModel.deletePlantProfile(
                            id = plant.id,
                            onSuccess = {
                                Toast.makeText(context, context.getString(R.string.success_prefix, it), Toast.LENGTH_SHORT).show()
                                selectedPlant = null
                            },
                            onError = { Toast.makeText(context, context.getString(R.string.error_prefix, it), Toast.LENGTH_SHORT).show() }
                        )
                    }
                }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.settings_delete_profile), tint = watirRed)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                selectedPlant?.let { plant ->
                    viewModel.applyPlantProfile(
                        id = plant.id,
                        onSuccess = { msg ->
                            Toast.makeText(context, context.getString(R.string.success_prefix, msg), Toast.LENGTH_SHORT).show()
                        },
                        onError = { err ->
                            Toast.makeText(context, context.getString(R.string.error_prefix, err), Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            },
            enabled = selectedPlant != null,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(stringResource(R.string.settings_apply_esp), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Text(stringResource(R.string.settings_climate_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = minTempInput,
                onValueChange = { minTempInput = it },
                label = { Text(stringResource(R.string.settings_climate_min_temp)) },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = maxTempInput,
                onValueChange = { maxTempInput = it },
                label = { Text(stringResource(R.string.settings_climate_max_temp)) },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = minHumidityInput,
                onValueChange = { minHumidityInput = it },
                label = { Text(stringResource(R.string.settings_climate_min_hum)) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val minTemp = minTempInput.toIntOrNull() ?: 5
                val maxTemp = maxTempInput.toIntOrNull() ?: 35
                val minHum = minHumidityInput.toIntOrNull() ?: 30
                viewModel.updateGlobalSettings(
                    GlobalSettingsRequest(minTemp, maxTemp, minHum),
                    onSuccess = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() },
                    onError = { Toast.makeText(context, "Błąd: $it", Toast.LENGTH_SHORT).show() }
                )
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(stringResource(R.string.settings_climate_save), fontWeight = FontWeight.Bold)
        }
    }
}