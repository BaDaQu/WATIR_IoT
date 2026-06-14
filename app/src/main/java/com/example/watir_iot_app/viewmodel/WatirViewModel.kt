package com.example.watir_iot_app.viewmodel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.watir_iot_app.data.model.TelemetryHistoryResponse
import com.example.watir_iot_app.network.APIClients
import com.example.watir_iot_app.network.WatirAPI
import kotlinx.coroutines.launch

class WatirViewModel : ViewModel(){

    private var watirAPI: WatirAPI? = null

    private val _telemetryHistory = mutableStateOf(TelemetryHistoryResponse())
    val telemetryHistory: State<TelemetryHistoryResponse> = _telemetryHistory

    private val _isConnected = mutableStateOf(false)
    val isConnected: State<Boolean> = _isConnected

    fun connectToServer(ipAddress: String) {
        viewModelScope.launch {
            try {
                val api = APIClients.createClientAPI(ipAddress)

                val response = api.getTelemetryHistory()

                watirAPI = api
                _telemetryHistory.value = response
                _isConnected.value = true

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
}