package com.example.limouserapp.data.service

import com.example.limouserapp.data.api.DirectionsApi
import com.example.limouserapp.data.model.directions.DirectionsResponse
import com.example.limouserapp.data.model.directions.Route
import com.example.limouserapp.data.model.directions.Leg
import com.example.limouserapp.data.model.directions.DistanceInfo as DirectionsDistanceInfo
import com.example.limouserapp.data.model.directions.DurationInfo as DirectionsDurationInfo
import com.example.limouserapp.data.network.NetworkConfig
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service to calculate road distance and duration using Google Maps Directions API
 * Matches iOS VehicleService.calculateDistance implementation
 */
@Singleton
class DirectionsService @Inject constructor(
    private val directionsApi: DirectionsApi
) {
    companion object {
        private const val TAG = "DirectionsService"
    }
    
    /**
     * Calculate road distance and duration between two coordinates
     * @param pickupLat Pickup latitude
     * @param pickupLong Pickup longitude
     * @param dropoffLat Dropoff latitude
     * @param dropoffLong Dropoff longitude
     * @param waypoints Optional list of waypoint coordinates (lat, lng pairs)
     * @return Pair of (distance in meters, duration in seconds), or null if route calculation fails
     */
    suspend fun calculateDistance(
        pickupLat: Double,
        pickupLong: Double,
        dropoffLat: Double,
        dropoffLong: Double,
        waypoints: List<Pair<Double, Double>>? = null
    ): Pair<Int, Int>? {
        // Validate coordinates
        if (pickupLat == 0.0 && pickupLong == 0.0 || dropoffLat == 0.0 && dropoffLong == 0.0) {
            return null
        }
        
        val origin = "$pickupLat,$pickupLong"
        val destination = "$dropoffLat,$dropoffLong"
        val waypointsString = waypoints?.joinToString("|") { "${it.first},${it.second}" }
        
        return try {
            val response = directionsApi.getDirections(
                origin = origin,
                destination = destination,
                waypoints = waypointsString,
                key = NetworkConfig.GOOGLE_PLACES_API_KEY
            )
            
            if (response.status == "OK" && response.routes.isNotEmpty()) {
                val route = response.routes.first()
                
                // Check for invalid legs (distance = 0)
                val hasInvalidLeg = route.legs.any { it.distance.value == 0 }
                if (hasInvalidLeg) {
                    return null
                }
                
                // Sum up distance and duration from all legs
                var totalDistance = 0
                var totalDuration = 0
                for (leg in route.legs) {
                    totalDistance += leg.distance.value
                    totalDuration += leg.duration.value
                }
                
                if (totalDistance > 0 && totalDuration > 0) {
                    Pair(totalDistance, totalDuration)
                } else {
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    
    /**
     * Format distance for display
     * Matches iOS formatDistance implementation
     */
    fun formatDistance(distanceMeters: Int): Pair<String, Int> {
        return if (distanceMeters >= 1000) {
            val km = distanceMeters / 1000.0
            Pair(String.format("%.1f km", km), distanceMeters)
        } else {
            Pair("${distanceMeters}m", distanceMeters)
        }
    }
    
    /**
     * Format duration for display
     * Matches iOS formatDuration implementation
     */
    fun formatDuration(durationSeconds: Int): Pair<String, Int> {
        val hours = durationSeconds / 3600
        val minutes = (durationSeconds % 3600) / 60
        
        return if (hours > 0) {
            Pair("$hours hours $minutes mins", durationSeconds)
        } else {
            Pair("$minutes mins", durationSeconds)
        }
    }
    
    /**
     * Validate if a route between two addresses is possible by car
     * Returns null if route is valid, or an error message if invalid
     * @param pickupLat Pickup latitude
     * @param pickupLong Pickup longitude
     * @param dropoffLat Dropoff latitude
     * @param dropoffLong Dropoff longitude
     * @return Pair of (isValid: Boolean, errorMessage: String?)
     */
    suspend fun validateRoute(
        pickupLat: Double,
        pickupLong: Double,
        dropoffLat: Double,
        dropoffLong: Double
    ): Pair<Boolean, String?> {
        // Validate coordinates
        if (pickupLat == 0.0 && pickupLong == 0.0) {
            return Pair(false, "Invalid pickup location coordinates")
        }
        if (dropoffLat == 0.0 && dropoffLong == 0.0) {
            return Pair(false, "Invalid dropoff location coordinates")
        }
        
        val origin = "$pickupLat,$pickupLong"
        val destination = "$dropoffLat,$dropoffLong"
        
        Log.d(TAG, "🔍 Validating route: Origin=$origin, Destination=$destination")
        
        return try {
            val response = directionsApi.getDirections(
                origin = origin,
                destination = destination,
                waypoints = null,
                key = NetworkConfig.GOOGLE_PLACES_API_KEY
            )
            
            Log.d(TAG, "📥 Route validation response: Status=${response.status}, Error=${response.errorMessage}")
            
            when (response.status) {
                "OK" -> {
                    if (response.routes.isEmpty()) {
                        Pair(false, "No route found between these locations")
                    } else {
                        // Check if route has valid legs with distance > 0
                        val route = response.routes.first()
                        val hasValidLegs = route.legs.any { it.distance.value > 0 }
                        if (hasValidLegs) {
                            Pair(true, null)
                        } else {
                            Pair(false, "Route is not possible by car between these locations")
                        }
                    }
                }
                "ZERO_RESULTS" -> {
                    Pair(false, "No route found. These locations cannot be reached by car (e.g., different countries separated by ocean)")
                }
                "NOT_FOUND" -> {
                    Pair(false, "One or both locations could not be found")
                }
                "ROUTE_NOT_FOUND" -> {
                    Pair(false, "No route found between these locations")
                }
                else -> {
                    val errorMsg = response.errorMessage ?: "Unable to validate route"
                    Log.w(TAG, "⚠️ Route validation failed: ${response.status} - $errorMsg")
                    Pair(false, errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Route validation request failed: ${e.message}", e)
            // Don't fail validation on network errors - let it pass and show error later
            Pair(true, null)
        }
    }
    
    /**
     * Get route polyline between two coordinates
     * Returns list of LatLng points representing the route
     * @param originLat Origin latitude
     * @param originLong Origin longitude
     * @param destLat Destination latitude
     * @param destLong Destination longitude
     * @return List of LatLng points representing the route, or empty list if failed
     */
    suspend fun getRoutePolyline(
        originLat: Double,
        originLong: Double,
        destLat: Double,
        destLong: Double
    ): List<LatLng> {
        // Validate coordinates
        if (originLat == 0.0 && originLong == 0.0) {
            Log.e(TAG, "❌ Invalid origin coordinates: (0.0, 0.0)")
            return emptyList()
        }
        if (destLat == 0.0 && destLong == 0.0) {
            Log.e(TAG, "❌ Invalid destination coordinates: (0.0, 0.0)")
            return emptyList()
        }
        
        val origin = "$originLat,$originLong"
        val destination = "$destLat,$destLong"
        
        Log.d(TAG, "🗺️ Getting route polyline: Origin=$origin, Destination=$destination")
        
        return try {
            val response = directionsApi.getDirections(
                origin = origin,
                destination = destination,
                waypoints = null,
                key = NetworkConfig.GOOGLE_PLACES_API_KEY
            )
            
            if (response.status == "OK" && response.routes.isNotEmpty()) {
                val route = response.routes.first()
                val polyline = route.overviewPolyline?.points
                
                if (polyline != null && polyline.isNotBlank()) {
                    val decodedPoints = PolyUtil.decode(polyline)
                    Log.d(TAG, "✅ Route polyline decoded: ${decodedPoints.size} points")
                    decodedPoints
                } else {
                    Log.w(TAG, "⚠️ Route has no polyline data")
                    emptyList()
                }
            } else {
                Log.w(TAG, "❌ Failed to get route polyline: ${response.status} - ${response.errorMessage}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting route polyline: ${e.message}", e)
            emptyList()
        }
    }
    
    /**
     * Get route with both distance/duration and polyline
     * @return Triple of (distance in meters, duration in seconds, polyline points), or null if route calculation fails
     */
    suspend fun getRouteWithPolyline(
        originLat: Double,
        originLong: Double,
        destLat: Double,
        destLong: Double
    ): Triple<Int, Int, List<LatLng>>? {
        // Validate coordinates
        if (originLat == 0.0 && originLong == 0.0 || destLat == 0.0 && destLong == 0.0) {
            return null
        }
        
        val origin = "$originLat,$originLong"
        val destination = "$destLat,$destLong"
        
        return try {
            val response = directionsApi.getDirections(
                origin = origin,
                destination = destination,
                waypoints = null,
                key = NetworkConfig.GOOGLE_PLACES_API_KEY
            )
            
            if (response.status == "OK" && response.routes.isNotEmpty()) {
                val route = response.routes.first()
                
                // Calculate distance and duration
                var totalDistance = 0
                var totalDuration = 0
                
                for (leg in route.legs) {
                    if (leg.distance.value == 0 || leg.duration.value == 0) {
                        return null
                    }
                    totalDistance += leg.distance.value
                    totalDuration += leg.duration.value
                }
                
                if (totalDistance <= 0 || totalDuration <= 0) {
                    return null
                }
                
                // Get polyline - must be road-snapped
                val polyline = route.overviewPolyline?.points
                val decodedPoints = if (polyline != null && polyline.isNotBlank()) {
                    PolyUtil.decode(polyline)
                } else {
                    return null // No polyline means no route
                }
                
                if (decodedPoints.isEmpty()) {
                    return null
                }
                
                Triple(totalDistance, totalDuration, decodedPoints)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

