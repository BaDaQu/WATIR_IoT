package com.example.watir_iot_app.data.model

data class TelemetryHistoryResponse(
    val status: String = "",
    val data: List<TelemetryDbRow> = emptyList()
)

data class TelemetryDbRow(
    val id: Int,
    val device_id: String,
    val timestamp: Long,
    val temp: Float,
    val humidity: Int,
    val soil_moisture: Int,
    val water_level_cm: Int,
    val water_error: Boolean,
    val pump_active: Boolean,
    val pan: Int,
    val tilt: Int
)