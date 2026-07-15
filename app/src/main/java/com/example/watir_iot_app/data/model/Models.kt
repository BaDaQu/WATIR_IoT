package com.example.watir_iot_app.data.model

data class TelemetryHistoryResponse(
    val status: String = "",
    val data: List<TelemetryDbRow> = emptyList()
)

data class MoveRequest(
    val axis: String,
    val value: Int
)

data class DirectionRequest(
    val direction: String
)

data class EspDirectionResponse(
    val status: String,
    val message: String,
    val pan: Int? = null,
    val tilt: Int? = null
)

data class DirectionResponse(
    val status: String,
    val message: String,
    val esp_response: EspDirectionResponse? = null
)

data class TelemetryDbRow(
    val id: Int,
    val device_id: String,
    val timestamp: Long,
    val temp: Float,
    val humidity: Int,
    val soil_moisture_1: Int,
    val soil_moisture_2: Int,
    val water_level_cm: Int,
    val water_error: Boolean,
    val pump_active: Boolean,
    val pan: Int,
    val tilt: Int
)

data class PlantProfile(
    val id: Int,
    val name: String,
    val moisture_threshold: Int,
    val auto_watering: Boolean,
    val check_interval_ms: Int,
    val pan: Int,
    val tilt: Int,
    val sensor: Int,
    val pump_power: Int
)

data class PlantProfileListResponse(
    val status: String,
    val data: List<PlantProfile>
)

data class PlantProfileApplyResponse(
    val status: String,
    val message: String,
    val applied_profile: PlantProfile? = null
)

data class PlantProfileRequest(
    val name: String,
    val moisture_threshold: Int,
    val auto_watering: Boolean,
    val check_interval_ms: Int,
    val pan: Int,
    val tilt: Int,
    val sensor: Int,
    val pump_power: Int
)

data class GlobalSettings(
    val id: Int = 1,
    val min_temp_block: Int,
    val max_temp_force: Int,
    val min_air_humidity_force: Int
)

data class GlobalSettingsRequest(
    val min_temp_block: Int,
    val max_temp_force: Int,
    val min_air_humidity_force: Int
)

data class GlobalSettingsResponse(
    val status: String,
    val message: String? = null,
    val data: GlobalSettings? = null
)

data class PlantProfileResponse(
    val status: String,
    val message: String? = null,
    val data: PlantProfile? = null,
    val deleted: PlantProfile? = null
)

data class PumpRequest(
    val device_id: String = "WATIR_01",
    val power: Int,
    val duration: Int? = null,
    val pan: Int? = null,
    val tilt: Int? = null
)