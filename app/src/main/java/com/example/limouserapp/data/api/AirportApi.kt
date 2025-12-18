package com.example.limouserapp.data.api

import com.example.limouserapp.data.model.booking.AirportResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Airport API service interface
 */
interface AirportApi {
    
    /**
     * Search airports
     * Endpoint: /api/mobile-data?only_airports=true
     */
    @GET("api/mobile-data")
    suspend fun searchAirports(
        @Query("only_airports") onlyAirports: Boolean = true,
        @Query("search") search: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
    ): AirportResponse
}

