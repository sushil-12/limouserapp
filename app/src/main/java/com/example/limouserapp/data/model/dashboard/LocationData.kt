package com.example.limouserapp.data.model.dashboard

import com.google.gson.annotations.SerializedName

/**
 * Represents location data with coordinates and address information
 */
data class LocationData(
    @SerializedName("latitude")
    val latitude: Double,
    
    @SerializedName("longitude")
    val longitude: Double,
    
    @SerializedName("address")
    val address: String,
    
    @SerializedName("city")
    val city: String? = null,
    
    @SerializedName("state")
    val state: String? = null,
    
    @SerializedName("country")
    val country: String? = null,
    
    @SerializedName("postal_code")
    val postalCode: String? = null,
    
    @SerializedName("formatted_address")
    val formattedAddress: String? = null
) {
    /**
     * Get the complete formatted address
     */
    val fullAddress: String
        get() = formattedAddress ?: address
    
    /**
     * Get a short address for display
     */
    val shortAddress: String
        get() = when {
            !city.isNullOrEmpty() && !state.isNullOrEmpty() -> "$city, $state"
            !city.isNullOrEmpty() -> city
            !state.isNullOrEmpty() -> state
            else -> address
        }
}

/**
 * Represents a car location on the map
 */
data class CarLocation(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("latitude")
    val latitude: Double,
    
    @SerializedName("longitude")
    val longitude: Double,
    
    @SerializedName("driver_id")
    val driverId: String? = null,
    
    @SerializedName("driver_name")
    val driverName: String? = null,
    
    @SerializedName("vehicle_type")
    val vehicleType: String? = null,
    
    @SerializedName("is_available")
    val isAvailable: Boolean = true,
    
    @SerializedName("last_updated")
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Represents map region for camera positioning
 */
data class MapRegion(
    val centerLatitude: Double,
    val centerLongitude: Double,
    val latitudeDelta: Double,
    val longitudeDelta: Double
) {
    companion object {
        /**
         * Default region for New York City
         */
        val DEFAULT_REGION = MapRegion(
            centerLatitude = 40.7128,
            centerLongitude = -74.0060,
            latitudeDelta = 0.01,
            longitudeDelta = 0.01
        )
        
        /**
         * Create a region from center point and span
         */
        fun create(
            centerLatitude: Double,
            centerLongitude: Double,
            span: Double = 0.01
        ): MapRegion {
            return MapRegion(
                centerLatitude = centerLatitude,
                centerLongitude = centerLongitude,
                latitudeDelta = span,
                longitudeDelta = span
            )
        }
    }
}

/**
 * Represents a map annotation for display
 */
data class MapAnnotation(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val title: String? = null,
    val snippet: String? = null,
    val type: AnnotationType = AnnotationType.CAR
)

/**
 * Types of map annotations
 */
enum class AnnotationType {
    CAR,
    PICKUP,
    DROPOFF,
    USER_LOCATION,
    DRIVER_LOCATION
}

/**
 * Location permission status
 */
enum class LocationPermissionStatus {
    GRANTED,
    DENIED,
    DENIED_FOREVER,
    NOT_DETERMINED
}

/**
 * Location service state
 */
enum class LocationServiceState {
    ENABLED,
    DISABLED,
    NOT_AVAILABLE
}
