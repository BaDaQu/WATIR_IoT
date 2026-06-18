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
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SensorCard("Temperatura", tempText, Icons.Default.Thermostat, modifier = Modifier.fillMaxWidth())
        SensorCard("Wilgotność powietrza", humidityText, Icons.Default.WaterDrop, modifier = Modifier.fillMaxWidth())
        SensorCard("Wilgotność gleby", soilMoistureText, Icons.Default.Eco, modifier = Modifier.fillMaxWidth())
        SensorCard("Poziom wody", waterText, Icons.Default.Waves, modifier = Modifier.fillMaxWidth())
        SensorCard("Stan Pompy", pumpText, Icons.Default.PowerSettingsNew, modifier = Modifier.fillMaxWidth())
    }
}