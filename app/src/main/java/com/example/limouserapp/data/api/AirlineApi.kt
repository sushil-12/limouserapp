package com.example.limouserapp.data.api

import com.example.limouserapp.data.model.booking.AirlineResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Airline API service interface
 */
interface AirlineApi {
    
    /**
     * Search airlines
     * Endpoint: /api/mobile-data?only_airlines=true
     */
    @GET("api/mobile-data")
    suspend fun searchAirlines(
        @Query("only_airlines") onlyAirlines: Boolean = true,
        @Query("search") search: String? = null
    ): AirlineResponse
}

