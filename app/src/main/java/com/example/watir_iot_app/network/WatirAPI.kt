package com.example.watir_iot_app.network

import com.example.watir_iot_app.data.model.TelemetryHistoryResponse
import retrofit2.http.GET

interface WatirAPI {
    @GET("api/telemetry")
    suspend fun getTelemetryHistory(): TelemetryHistoryResponse
}