package com.example.watir_iot_app.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.watir_iot_app.R
import com.example.watir_iot_app.feature.dashboard.components.SensorCard
import com.example.watir_iot_app.viewmodel.WatirViewModel

@Composable
fun DashboardScreen(viewModel: WatirViewModel) {
    val latestData = viewModel.telemetryHistory.value.data.firstOrNull()

    val humidityText = latestData?.humidity?.let { "$it%" } ?: "--%"
    val soilMoistureText = latestData?.soil_moisture?.let { "$it%" } ?: "--%"
    val tempText = latestData?.temp?.let { "$it°C" } ?: "--°C"

    val pumpText = if (latestData?.pump_active == true) stringResource(R.string.pump_working) else stringResource(R.string.pump_standby)

    val waterText = if (latestData?.water_error == true) {
        stringResource(R.string.no_water)
    } else {
        latestData?.water_level_cm?.let { "${it} cm" } ?: stringResource(R.string.no_data)
    }

    val watirBlue = Color(0xFF2EB4E6)
    val watirGreen = Color(0xFF549E39)
    val watirDarkRed = Color(0xFFB71C1C)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.dashboard_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        SensorCard(
            title = stringResource(R.string.temp),
            value = tempText,
            icon = Icons.Default.Thermostat,
            iconTint = watirDarkRed,
            modifier = Modifier.fillMaxWidth()
        )
        SensorCard(
            title = stringResource(R.string.humidity_air),
            value = humidityText,
            icon = Icons.Default.WaterDrop,
            iconTint = watirBlue,
            modifier = Modifier.fillMaxWidth()
        )
        SensorCard(
            title = stringResource(R.string.humidity_soil),
            value = soilMoistureText,
            icon = Icons.Default.Eco,
            iconTint = watirGreen,
            modifier = Modifier.fillMaxWidth()
        )
        SensorCard(
            title = stringResource(R.string.water_level),
            value = waterText,
            icon = Icons.Default.Waves,
            iconTint = watirBlue,
            valueColor = if (latestData?.water_error == true) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
        SensorCard(
            title = stringResource(R.string.pump_status),
            value = pumpText,
            icon = Icons.Default.PowerSettingsNew,
            iconTint = if (latestData?.pump_active == true) watirGreen else Color.Gray,
            modifier = Modifier.fillMaxWidth()
        )
    }
}