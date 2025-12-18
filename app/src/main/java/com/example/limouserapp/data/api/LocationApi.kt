package com.example.limouserapp.data.api

import com.example.limouserapp.data.model.location.PlaceDetails
import com.example.limouserapp.data.model.location.PlacesResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Location API service interface
 * Handles Google Places API integration for location services
 */
interface LocationApi {
    
    /**
     * Get place autocomplete suggestions
     * @param input User input for place search
     * @param key Google Places API key
     * @param types Type of places to search (e.g., "address")
     * @return List of place predictions
     */
    @GET("maps/api/place/autocomplete/json")
    suspend fun getPlacePredictions(
        @Query("input") input: String,
        @Query("key") key: String,
        @Query("types") types: String = "address"
    ): PlacesResponse
    
    /**
     * Get detailed information about a specific place
     * @param placeId Google Place ID
     * @param key Google Places API key
     * @param fields Specific fields to return
     * @return Detailed place information
     */
    @GET("maps/api/place/details/json")
    suspend fun getPlaceDetails(
        @Query("place_id") placeId: String,
        @Query("key") key: String,
        @Query("fields") fields: String = "place_id,formatted_address,geometry,address_components"
    ): PlaceDetails
}
