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
import kotlinx.coroutines.launch

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

    fun applyPlantProfile(id: Int, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
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

    fun createPlantProfile(name: String, moistureThreshold: Int, autoWatering: Boolean, checkIntervalMs: Int, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        watirAPI?.let { api ->
            viewModelScope.launch {
                try {
                    val req = com.example.watir_iot_app.data.model.PlantProfileRequest(name, moistureThreshold, autoWatering, checkIntervalMs)
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

    fun updatePlantProfile(id: Int, name: String, moistureThreshold: Int, autoWatering: Boolean, checkIntervalMs: Int, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        watirAPI?.let { api ->
            viewModelScope.launch {
                try {
                    val req = com.example.watir_iot_app.data.model.PlantProfileRequest(name, moistureThreshold, autoWatering, checkIntervalMs)
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

    fun deletePlantProfile(id: Int, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
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