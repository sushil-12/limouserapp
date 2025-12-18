package com.example.limouserapp.data.api

import com.example.limouserapp.data.model.directions.DirectionsResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Google Maps Directions API interface
 * Used to calculate accurate road distance and duration between two points
 */
interface DirectionsApi {
    
    /**
     * Get directions between origin and destination
     * @param origin Origin coordinates in format "lat,lng"
     * @param destination Destination coordinates in format "lat,lng"
     * @param waypoints Optional waypoints in format "lat1,lng1|lat2,lng2|..."
     * @param key Google Maps API key
     * @return Directions response with route information
     */
    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("waypoints") waypoints: String? = null,
        @Query("key") key: String
    ): DirectionsResponse
}

