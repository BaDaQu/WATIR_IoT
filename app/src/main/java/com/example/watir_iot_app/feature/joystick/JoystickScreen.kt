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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.watir_iot_app.R
import com.example.watir_iot_app.data.model.PlantProfile
import com.example.watir_iot_app.viewmodel.WatirViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoystickScreen(watirViewModel: WatirViewModel) {

    val context = LocalContext.current
    val plantProfiles by watirViewModel.plantProfiles

    var expanded by remember { mutableStateOf(false) }
    var selectedPlant by remember { mutableStateOf<PlantProfile?>(null) }

    var pumpPower by remember { mutableFloatStateOf(50f) }

    val watirGreen = Color(0xFF549E39)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = stringResource(R.string.joystick_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.pump_power_label, pumpPower.roundToInt()),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )

        Slider(
            value = pumpPower,
            onValueChange = {
                pumpPower = it
                val finalPumpPower = 45f + ((pumpPower - 1) * (60f - 45f))/(100f - 1f)
                watirViewModel.setPumpPower(finalPumpPower.roundToInt())
            },
            valueRange = 1f..100f,
            steps = 99,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- D-PAD: STRZAŁKI STERUJĄCE SERWAMI ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Strzałka GÓRA
            ArrowButton(
                icon = Icons.Default.KeyboardArrowUp,
                description = stringResource(R.string.direction_up),
                color = MaterialTheme.colorScheme.primary,
                onClick = { watirViewModel.moveServo("gora") }
            )

            // Strzałki LEWO i PRAWO
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                ArrowButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    description = stringResource(R.string.direction_left),
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { watirViewModel.moveServo("lewo") }
                )
                Spacer(modifier = Modifier.width(48.dp))
                ArrowButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    description = stringResource(R.string.direction_right),
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { watirViewModel.moveServo("prawo") }
                )
            }

            // Strzałka DÓŁ
            ArrowButton(
                icon = Icons.Default.KeyboardArrowDown,
                description = stringResource(R.string.direction_down),
                color = MaterialTheme.colorScheme.primary,
                onClick = { watirViewModel.moveServo("dol") }
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = stringResource(R.string.joystick_calibrate_hint),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            OutlinedTextField(
                value = selectedPlant?.name ?: stringResource(R.string.settings_select_profile),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                    .fillMaxWidth(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                    focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.primary
                ),
                shape = MaterialTheme.shapes.medium
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                plantProfiles.forEach { plant ->
                    DropdownMenuItem(
                        text = { Text(plant.name, color = MaterialTheme.colorScheme.primary) },
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
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    val finalPumpPower = 45f + ((pumpPower - 1) * (60f - 45f))/(100f - 1f)
                    watirViewModel.triggerPump(power = finalPumpPower.roundToInt(), duration = 1500)
                },
                modifier = Modifier
                    .weight(0.4f)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(stringResource(R.string.joystick_water_button), fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    selectedPlant?.let { plant ->
                        watirViewModel.saveCurrentPositionForPlant(
                            id = plant.id,
                            onSuccess = { msg -> Toast.makeText(context, context.getString(R.string.success_prefix, msg), Toast.LENGTH_SHORT).show() },
                            onError = { err -> Toast.makeText(context, context.getString(R.string.error_prefix, err), Toast.LENGTH_SHORT).show() }
                        )
                    }
                },
                enabled = selectedPlant != null,
                modifier = Modifier
                    .weight(0.6f)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = watirGreen,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(stringResource(R.string.joystick_save_pos_button), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ArrowButton(
    icon: ImageVector,
    description: String,
    color: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = color,
            contentColor = Color.White
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            modifier = Modifier.size(36.dp)
        )
    }
}
