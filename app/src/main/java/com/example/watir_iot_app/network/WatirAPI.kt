package com.example.watir_iot_app.network

import com.example.watir_iot_app.data.model.DirectionRequest
import com.example.watir_iot_app.data.model.MoveRequest
import com.example.watir_iot_app.data.model.PlantProfileApplyResponse
import com.example.watir_iot_app.data.model.PlantProfileListResponse
import com.example.watir_iot_app.data.model.TelemetryHistoryResponse
import com.example.watir_iot_app.data.model.PlantProfileRequest
import com.example.watir_iot_app.data.model.PlantProfileResponse
import com.example.watir_iot_app.data.model.PumpRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface WatirAPI {
    @GET("api/telemetry")
    suspend fun getTelemetryHistory(): TelemetryHistoryResponse

    @POST("api/move")
    suspend fun sendMove(@Body request: MoveRequest)

    @POST("api/move")
    suspend fun sendDirection(@Body request: DirectionRequest): com.example.watir_iot_app.data.model.DirectionResponse

    @GET("api/plants")
    suspend fun getPlants(): PlantProfileListResponse

    @POST("api/plants")
    suspend fun createPlantProfile(@Body request: PlantProfileRequest): PlantProfileResponse

    @PUT("api/plants/{id}")
    suspend fun updatePlantProfile(@Path("id") id: Int, @Body request: PlantProfileRequest): PlantProfileResponse

    @DELETE("api/plants/{id}")
    suspend fun deletePlantProfile(@Path("id") id: Int): PlantProfileResponse

    @POST("api/plants/{id}/apply")
    suspend fun applyPlantProfile(@Path("id") id: Int): PlantProfileApplyResponse

    @POST("api/pump")
    suspend fun triggerPump(@Body request: PumpRequest)

    @GET("api/settings")
    suspend fun getGlobalSettings(): com.example.watir_iot_app.data.model.GlobalSettingsResponse

    @POST("api/settings")
    suspend fun updateGlobalSettings(@Body request: com.example.watir_iot_app.data.model.GlobalSettingsRequest): com.example.watir_iot_app.data.model.GlobalSettingsResponse
}