package com.example.watir_iot_app.viewmodel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.watir_iot_app.data.model.MoveRequest
import com.example.watir_iot_app.data.model.TelemetryHistoryResponse
import com.example.watir_iot_app.network.APIClients
import com.example.watir_iot_app.network.WatirAPI
import com.example.watir_iot_app.data.model.PlantProfile
import com.example.watir_iot_app.data.model.PlantProfileRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

class WatirViewModel : ViewModel(){

    private var watirAPI: WatirAPI? = null

    private val _telemetryHistory = mutableStateOf(TelemetryHistoryResponse())
    val telemetryHistory: State<TelemetryHistoryResponse> = _telemetryHistory

    private val _plantProfiles = mutableStateOf<List<PlantProfile>>(emptyList())
    val plantProfiles: State<List<PlantProfile>> = _plantProfiles

    private val _isConnected = mutableStateOf(false)
    val isConnected: State<Boolean> = _isConnected

    private val throttleIntervalMs = 200L
    private val lastSendTimestamps = mutableMapOf<String, Long>()

    private var currentX = 90
    private var currentY = 90

    private var joystickDeltaX = 0f
    private var joystickDeltaY = 0f

    private var joystickJob: Job? = null

    fun onJoystickMoved(xPercent: Float, yPercent: Float) {
        joystickDeltaX = xPercent
        joystickDeltaY = yPercent

        if (joystickJob == null || !joystickJob!!.isActive) {
            startJoystickLoop()
        }
    }

    fun onJoystickStopped() {
        joystickDeltaX = 0f
        joystickDeltaY = 0f
        joystickJob?.cancel()
    }

    private fun startJoystickLoop() {
        joystickJob = viewModelScope.launch {
            while (isActive) {
                var changed = false

                if (Math.abs(joystickDeltaX) > 0.2f) {
                    val step = if (joystickDeltaX > 0) -2 else 2
                    currentX = (currentX + step).coerceIn(0, 180)
                    changed = true
                }

                if (Math.abs(joystickDeltaY) > 0.2f) {
                    val step = if (joystickDeltaY > 0) 2 else -2
                    currentY = (currentY + step).coerceIn(0, 180)
                    changed = true
                }

                if (changed) {
                    sendMove("X", currentX)
                    sendMove("Y", currentY)
                }

                delay(50)
            }
        }
    }

    fun connectToServer(ipAddress: String) {
        viewModelScope.launch {
            try {
                val api = APIClients.createClientAPI(ipAddress)
                val response = api.getTelemetryHistory()
                watirAPI = api
                _telemetryHistory.value = response
                _isConnected.value = true
                fetchPlants()

            } catch (e: Exception) {
                watirAPI = null
                _isConnected.value = false
                println("Failed to connect: ${e.message}")
            }
        }
    }

    fun fetchHistory() {
        watirAPI?.let { api ->
            viewModelScope.launch {
                try {
                    val response = api.getTelemetryHistory()

                    _telemetryHistory.value = response

                } catch (e: Exception) {
                    println("Failed to fetch telemetry history: ${e.message}")
                }
            }
        }
    }

    fun fetchPlants() {
        watirAPI?.let { api ->
            viewModelScope.launch {
                try {
                    val response = api.getPlants()
                    if (response.status == "success") {
                        _plantProfiles.value = response.data
                    }
                } catch (e: Exception) {
                    println("Failed to fetch plants: ${e.message}")
                }
            }
        }
    }

    fun applyPlantProfile(
        id: Int,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        watirAPI?.let { api ->
            viewModelScope.launch {
                try {
                    val response = api.applyPlantProfile(id)
                    if (response.status == "success") {
                        onSuccess(response.message)
                    } else {
                        onError(response.message)
                    }
                } catch (e: Exception) {
                    onError(e.message ?: "Unknown error")
                }
            }
        } ?: onError("Not connected to server")
    }

    fun createPlantProfile(
        name: String,
        moistureThreshold: Int,
        autoWatering: Boolean,
        checkIntervalMs: Int,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        watirAPI?.let { api ->
            viewModelScope.launch {
                try {
                    val req =
                        PlantProfileRequest(
                            name,
                            moistureThreshold,
                            autoWatering,
                            checkIntervalMs,
                            90,
                            90
                        )
                    val response = api.createPlantProfile(req)
                    if (response.status == "success") {
                        onSuccess(response.message ?: "Profile created")
                        fetchPlants()
                    } else {
                        onError(response.message ?: "Failed to create profile")
                    }
                } catch (e: Exception) {
                    onError(e.message ?: "Unknown error")
                }
            }
        } ?: onError("Not connected to server")
    }

    fun updatePlantProfile(
        id: Int, name: String,
        moistureThreshold: Int,
        autoWatering: Boolean,
        checkIntervalMs: Int,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val existing = _plantProfiles.value.find { it.id == id }
        val panToSave = existing?.pan ?: currentX
        val tiltToSave = existing?.tilt ?: currentY

        watirAPI?.let { api ->
            viewModelScope.launch {
                try {
                    val req = PlantProfileRequest(
                        name,
                        moistureThreshold,
                        autoWatering,
                        checkIntervalMs,
                        panToSave,
                        tiltToSave
                    )
                    val response = api.updatePlantProfile(id, req)
                    if (response.status == "success") {
                        onSuccess(response.message ?: "Profile updated")
                        fetchPlants()
                    } else {
                        onError(response.message ?: "Failed to update profile")
                    }
                } catch (e: Exception) {
                    onError(e.message ?: "Unknown error")
                }
            }
        } ?: onError("Not connected to server")
    }

    fun deletePlantProfile(
        id: Int,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        watirAPI?.let { api ->
            viewModelScope.launch {
                try {
                    val response = api.deletePlantProfile(id)
                    if (response.status == "success") {
                        onSuccess(response.message ?: "Profile deleted")
                        fetchPlants()
                    } else {
                        onError(response.message ?: "Failed to delete profile")
                    }
                } catch (e: Exception) {
                    onError(e.message ?: "Unknown error")
                }
            }
        } ?: onError("Not connected to server")
    }

    fun saveCurrentPositionForPlant(
        id: Int,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val existing = _plantProfiles.value.find { it.id == id }

        if (existing == null) {
            onError("Profile with ID $id not found")
            return
        }

        watirAPI?.let { api ->
            viewModelScope.launch {
                try {
                    val req = PlantProfileRequest(
                        name = existing.name,
                        moisture_threshold = existing.moisture_threshold,
                        auto_watering = existing.auto_watering,
                        check_interval_ms = existing.check_interval_ms,
                        pan = currentX,
                        tilt = currentY
                    )
                    val response = api.updatePlantProfile(id, req)
                    if (response.status == "success") {
                        fetchPlants()
                        onSuccess("Position ${currentX}/${currentY} saved for ${existing.name}")
                    } else {
                        onError("Failed to save position: ${response.message}")
                    }
                } catch (e: Exception) {
                    onError("Network error: ${e.message}")
                }
            }
        } ?: onError("Not connected to server")
    }

    fun sendMove(axis: String, value: Int) {
        val now = System.currentTimeMillis()
        val lastSent = lastSendTimestamps[axis] ?: 0L
        if (now - lastSent < throttleIntervalMs) return
        lastSendTimestamps[axis] = now

        watirAPI?.let { api ->
            viewModelScope.launch {
                try {
                    api.sendMove(MoveRequest(axis = axis, value = value))
                } catch (e: Exception) {
                    println("Failed to send move command: ${e.message}")
                }
            }
        }
    }
}