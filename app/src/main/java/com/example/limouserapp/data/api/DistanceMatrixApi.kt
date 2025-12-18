package com.example.limouserapp.data.api

import com.example.limouserapp.data.model.distancematrix.DistanceMatrixResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Google Maps Distance Matrix API interface
 * Used to calculate distance and duration between multiple origins and destinations
 * Matches web app's DistanceMatrixService usage
 */
interface DistanceMatrixApi {
    
    /**
     * Get distance matrix between origins and destinations
     * @param origins List of origin coordinates in format "lat1,lng1|lat2,lng2|..."
     * @param destinations List of destination coordinates in format "lat1,lng1|lat2,lng2|..."
     * @param mode Travel mode (default: "driving")
     * @param units Unit system (default: "metric")
     * @param key Google Maps API key
     * @return Distance matrix response with distance and duration for each origin-destination pair
     */
    @GET("maps/api/distancematrix/json")
    suspend fun getDistanceMatrix(
        @Query("origins") origins: String,
        @Query("destinations") destinations: String,
        @Query("mode") mode: String = "driving",
        @Query("units") units: String = "metric",
        @Query("key") key: String
    ): DistanceMatrixResponse
}


