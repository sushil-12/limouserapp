package com.example.limouserapp.data.api

import com.example.limouserapp.data.model.booking.FilterResponse
import retrofit2.http.GET

/**
 * API interface for filter endpoints
 */
interface FilterApi {
    @GET("api/quote/filters")
    suspend fun getFilters(): FilterResponse
}

