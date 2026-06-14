package com.example.watir_iot_app.network

import com.example.watir_iot_app.data.model.PlantProfile
import com.example.watir_iot_app.data.model.PlantProfileRequest
import com.example.watir_iot_app.data.model.PlantProfileResponse
import com.example.watir_iot_app.data.model.PlantProfilesResponse
import com.example.watir_iot_app.data.model.TelemetryHistoryResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface WatirAPI {

    // ==========================================
    // TELEMETRIA
    // ==========================================

    // Pobiera historię logów, opcjonalnie filtrując po urządzeniu
    // Przykład: GET /api/telemetry?device_id=WATIR_01&limit=50
    @GET("api/telemetry")
    suspend fun getTelemetryHistory(
        @Query("device_id") deviceId: String? = null,
        @Query("limit") limit: Int? = null
    ): TelemetryHistoryResponse

    // Statystyki retencji bazy
    @GET("api/telemetry/stats")
    suspend fun getTelemetryStats(): Map<String, Any>

    // ==========================================
    // PROFILE ROŚLIN — CRUD dla issue #28
    // ==========================================

    // Pobiera wszystkie profile z bazy
    @GET("api/plants")
    suspend fun getAllPlants(): PlantProfilesResponse

    // Pobiera jeden profil po ID
    @GET("api/plants/{id}")
    suspend fun getPlantById(@Path("id") id: Int): PlantProfileResponse

    // Tworzy nowy profil
    @POST("api/plants")
    suspend fun createPlant(@Body plant: PlantProfileRequest): PlantProfileResponse

    // Aktualizuje istniejący profil
    @PUT("api/plants/{id}")
    suspend fun updatePlant(
        @Path("id") id: Int,
        @Body plant: PlantProfileRequest
    ): PlantProfileResponse

    // Usuwa profil po ID
    @DELETE("api/plants/{id}")
    suspend fun deletePlant(@Path("id") id: Int): Map<String, Any>
}
