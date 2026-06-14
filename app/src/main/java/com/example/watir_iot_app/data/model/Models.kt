package com.example.watir_iot_app.data.model

// ==========================================
// TELEMETRIA
// ==========================================

data class TelemetryHistoryResponse(
    val status: String = "",
    val data: List<TelemetryDbRow> = emptyList()
)

data class TelemetryDbRow(
    val id: Int = 0,
    val device_id: String = "",
    val timestamp: Long = 0,
    val temp: Float = 0f,
    val humidity: Int = 0,
    val soil_moisture: Int = 0,
    val water_level_cm: Int = 0,
    val water_error: Boolean = false,
    val pump_active: Boolean = false,
    val pan: Int = 90,
    val tilt: Int = 90
)

// ==========================================
// PROFILE ROŚLIN — Issue #25 / #28
// ==========================================

data class PlantProfile(
    val id: Int = 0,
    val name: String = "",
    val moisture_threshold: Int = 50,
    val auto_watering: Boolean = true,
    val check_interval_ms: Int = 10000
)

data class PlantProfileRequest(
    val name: String,
    val moisture_threshold: Int,
    val auto_watering: Boolean,
    val check_interval_ms: Int
)

data class PlantProfilesResponse(
    val status: String = "",
    val data: List<PlantProfile> = emptyList()
)

data class PlantProfileResponse(
    val status: String = "",
    val data: PlantProfile? = null
)

// ==========================================
// STANY UI — opakowują wynik każdego zapytania
// ==========================================

sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
