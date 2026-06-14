package com.example.watir_iot_app.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.watir_iot_app.data.model.PlantProfile
import com.example.watir_iot_app.data.model.PlantProfileRequest
import com.example.watir_iot_app.data.model.TelemetryHistoryResponse
import com.example.watir_iot_app.data.model.UiState
import com.example.watir_iot_app.network.APIClients
import com.example.watir_iot_app.network.WatirAPI
import kotlinx.coroutines.launch

class WatirViewModel : ViewModel() {

    // ==========================================
    // STAN POŁĄCZENIA
    // ==========================================

    private var watirAPI: WatirAPI? = null

    private val _isConnected = mutableStateOf(false)
    val isConnected: State<Boolean> = _isConnected

    private val _serverIp = mutableStateOf("")
    val serverIp: State<String> = _serverIp

    // ==========================================
    // STANY UI
    // ==========================================

    private val _telemetryState = mutableStateOf<UiState<TelemetryHistoryResponse>>(UiState.Idle)
    val telemetryState: State<UiState<TelemetryHistoryResponse>> = _telemetryState

    private val _plantsState = mutableStateOf<UiState<List<PlantProfile>>>(UiState.Idle)
    val plantsState: State<UiState<List<PlantProfile>>> = _plantsState

    private val _plantActionState = mutableStateOf<UiState<PlantProfile>>(UiState.Idle)
    val plantActionState: State<UiState<PlantProfile>> = _plantActionState

    // Zachowane dla kompatybilności z SettingsScreen
    private val _telemetryHistory = mutableStateOf(TelemetryHistoryResponse())
    val telemetryHistory: State<TelemetryHistoryResponse> = _telemetryHistory

    // ==========================================
    // POŁĄCZENIE Z SERWEREM
    // ==========================================

    // Łączy się z serwerem i od razu pobiera dane testowe
    // żeby potwierdzić że połączenie działa
    fun connectToServer(ipAddress: String) {
        viewModelScope.launch {
            _telemetryState.value = UiState.Loading
            try {
                val api = APIClients.createClientAPI(ipAddress)
                val response = api.getTelemetryHistory(limit = 1)

                watirAPI = api
                _serverIp.value = ipAddress
                _isConnected.value = true
                _telemetryHistory.value = response
                _telemetryState.value = UiState.Success(response)

            } catch (e: Exception) {
                watirAPI = null
                _isConnected.value = false
                _telemetryState.value = UiState.Error(
                    "Nie można połączyć z $ipAddress: ${e.message}"
                )
            }
        }
    }

    // ==========================================
    // TELEMETRIA
    // ==========================================

    fun fetchHistory(deviceId: String? = null, limit: Int = 50) {
        val api = watirAPI ?: run {
            _telemetryState.value = UiState.Error("Brak połączenia z serwerem")
            return
        }

        viewModelScope.launch {
            _telemetryState.value = UiState.Loading
            try {
                val response = api.getTelemetryHistory(
                    deviceId = deviceId,
                    limit = limit
                )
                _telemetryHistory.value = response
                _telemetryState.value = UiState.Success(response)
            } catch (e: Exception) {
                _isConnected.value = false
                _telemetryState.value = UiState.Error("Błąd pobierania danych: ${e.message}")
            }
        }
    }

    // ==========================================
    // CRUD PROFILI ROŚLIN — Issue #28
    // ==========================================

    fun fetchAllPlants() {
        val api = watirAPI ?: run {
            _plantsState.value = UiState.Error("Brak połączenia z serwerem")
            return
        }

        viewModelScope.launch {
            _plantsState.value = UiState.Loading
            try {
                val response = api.getAllPlants()
                _plantsState.value = UiState.Success(response.data)
            } catch (e: Exception) {
                _plantsState.value = UiState.Error("Błąd pobierania profili: ${e.message}")
            }
        }
    }

    fun createPlant(
        name: String,
        moistureThreshold: Int,
        autoWatering: Boolean,
        checkIntervalMs: Int
    ) {
        val api = watirAPI ?: run {
            _plantActionState.value = UiState.Error("Brak połączenia z serwerem")
            return
        }

        viewModelScope.launch {
            _plantActionState.value = UiState.Loading
            try {
                val response = api.createPlant(
                    PlantProfileRequest(
                        name = name,
                        moisture_threshold = moistureThreshold,
                        auto_watering = autoWatering,
                        check_interval_ms = checkIntervalMs
                    )
                )
                response.data?.let {
                    _plantActionState.value = UiState.Success(it)
                    fetchAllPlants() // odśwież listę po dodaniu
                } ?: run {
                    _plantActionState.value = UiState.Error("Serwer nie zwrócił danych")
                }
            } catch (e: Exception) {
                _plantActionState.value = UiState.Error("Błąd tworzenia profilu: ${e.message}")
            }
        }
    }

    fun updatePlant(
        id: Int,
        name: String,
        moistureThreshold: Int,
        autoWatering: Boolean,
        checkIntervalMs: Int
    ) {
        val api = watirAPI ?: run {
            _plantActionState.value = UiState.Error("Brak połączenia z serwerem")
            return
        }

        viewModelScope.launch {
            _plantActionState.value = UiState.Loading
            try {
                val response = api.updatePlant(
                    id = id,
                    plant = PlantProfileRequest(
                        name = name,
                        moisture_threshold = moistureThreshold,
                        auto_watering = autoWatering,
                        check_interval_ms = checkIntervalMs
                    )
                )
                response.data?.let {
                    _plantActionState.value = UiState.Success(it)
                    fetchAllPlants() // odśwież listę po zmianie
                } ?: run {
                    _plantActionState.value = UiState.Error("Serwer nie zwrócił danych")
                }
            } catch (e: Exception) {
                _plantActionState.value = UiState.Error("Błąd aktualizacji profilu: ${e.message}")
            }
        }
    }

    fun deletePlant(id: Int) {
        val api = watirAPI ?: run {
            _plantActionState.value = UiState.Error("Brak połączenia z serwerem")
            return
        }

        viewModelScope.launch {
            _plantActionState.value = UiState.Loading
            try {
                api.deletePlant(id)
                _plantActionState.value = UiState.Idle
                fetchAllPlants() // odśwież listę po usunięciu
            } catch (e: Exception) {
                _plantActionState.value = UiState.Error("Błąd usuwania profilu: ${e.message}")
            }
        }
    }

    // Czyści stan akcji (np. po zamknięciu dialogu)
    fun resetPlantActionState() {
        _plantActionState.value = UiState.Idle
    }
}
