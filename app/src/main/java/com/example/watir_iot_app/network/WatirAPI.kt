package com.example.watir_iot_app.network

import com.example.watir_iot_app.data.model.MoveRequest
import com.example.watir_iot_app.data.model.TelemetryHistoryResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface WatirAPI {
    @GET("api/telemetry")
    suspend fun getTelemetryHistory(): TelemetryHistoryResponse

    @POST("api/move")
    suspend fun sendMove(@Body request: MoveRequest)
}