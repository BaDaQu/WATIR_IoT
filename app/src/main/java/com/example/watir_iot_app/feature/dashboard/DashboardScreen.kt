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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.watir_iot_app.feature.dashboard.components.SensorCard
import com.example.watir_iot_app.viewmodel.WatirViewModel

@Composable
fun DashboardScreen(viewModel: WatirViewModel) {
    val latestData = viewModel.telemetryHistory.value.data.firstOrNull()

    val humidityText = latestData?.humidity?.let { "$it%" } ?: "--%"
    val soilMoistureText = latestData?.soil_moisture?.let { "$it%" } ?: "--%"
    val tempText = latestData?.temp?.let { "$it°C" } ?: "--°C"

    val pumpText = if (latestData?.pump_active == true) "Pracuje" else "Czuwa"

    val waterText = if (latestData?.water_error == true) {
        "Brak wody!"
    } else {
        latestData?.water_level_cm?.let { "${it} cm" } ?: "Brak danych"
    }

    // Paleta WATIR
    val watirNavy = Color(0xFF102A43)
    val watirBlue = Color(0xFF2EB4E6)
    val watirGreen = Color(0xFF549E39)
    val watirDarkRed = Color(0xFFB71C1C) // Ciemny, elegancki czerwony dla termometru

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Twój Mikroklimat",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = watirNavy,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        SensorCard(
            title = "Temperatura",
            value = tempText,
            icon = Icons.Default.Thermostat,
            iconTint = watirDarkRed,
            modifier = Modifier.fillMaxWidth()
        )
        SensorCard(
            title = "Wilgotność powietrza",
            value = humidityText,
            icon = Icons.Default.WaterDrop,
            iconTint = watirBlue,
            modifier = Modifier.fillMaxWidth()
        )
        SensorCard(
            title = "Wilgotność gleby",
            value = soilMoistureText,
            icon = Icons.Default.Eco,
            iconTint = watirGreen,
            modifier = Modifier.fillMaxWidth()
        )
        SensorCard(
            title = "Poziom wody",
            value = waterText,
            icon = Icons.Default.Waves,
            iconTint = watirBlue,
            valueColor = if (latestData?.water_error == true) Color(0xFFD32F2F) else watirNavy,
            modifier = Modifier.fillMaxWidth()
        )
        SensorCard(
            title = "Stan Pompy",
            value = pumpText,
            icon = Icons.Default.PowerSettingsNew,
            iconTint = if (latestData?.pump_active == true) watirGreen else Color.Gray,
            modifier = Modifier.fillMaxWidth()
        )
    }
}