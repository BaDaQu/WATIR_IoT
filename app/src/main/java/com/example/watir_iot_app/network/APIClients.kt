package com.example.watir_iot_app.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object APIClients {

    // Tworzy klienta Retrofit dla podanego adresu IP serwera.
    // Wywołaj connectToServer() w WatirViewModel zamiast używać tego bezpośrednio.
    fun createClientAPI(ipAddress: String): WatirAPI {

        val formattedUrl = if (ipAddress.startsWith("http")) {
            if (ipAddress.endsWith("/")) ipAddress else "$ipAddress/"
        } else {
            "http://$ipAddress:3000/"
        }

        return Retrofit.Builder()
            .baseUrl(formattedUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WatirAPI::class.java)
    }
}
