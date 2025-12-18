package com.example.limouserapp.data.model.booking

import com.google.gson.annotations.SerializedName
import java.util.UUID

/**
 * Recent location model for pickup and dropoff locations
 */
data class RecentLocation(
    @SerializedName("id")
    val id: String = UUID.randomUUID().toString(),
    
    @SerializedName("address")
    val address: String,
    
    @SerializedName("latitude")
    val latitude: Double,
    
    @SerializedName("longitude")
    val longitude: Double,
    
    @SerializedName("type")
    val type: String, // "pickup" or "dropoff"
    
    @SerializedName("is_airport")
    val isAirport: Boolean = false,
    
    @SerializedName("last_used")
    val lastUsed: String = "",
    
    @SerializedName("airport_id")
    val airportId: String? = null,
    
    @SerializedName("airport_code")
    val airportCode: String? = null,
    
    @SerializedName("airport_name")
    val airportName: String? = null
) {
    /**
     * Get location coordinate
     */
    fun toLocationCoordinate(): LocationCoordinate {
        return LocationCoordinate(latitude = latitude, longitude = longitude)
    }
}

/**
 * Recent location data wrapper
 */
data class RecentLocationData(
    @SerializedName("locations")
    val locations: List<RecentLocation>,
    
    @SerializedName("count")
    val count: Int,
    
    @SerializedName("search")
    val search: String = "",
    
    @SerializedName("type")
    val type: String,
    
    @SerializedName("limit")
    val limit: Int
)

/**
 * Recent location response
 */
data class RecentLocationResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: RecentLocationData,
    
    @SerializedName("timestamp")
    val timestamp: String,
    
    @SerializedName("code")
    val code: Int
)

