package com.example.limouserapp.data.service

import com.example.limouserapp.data.api.DirectionsApi
import com.example.limouserapp.data.model.directions.DirectionsResponse
import com.example.limouserapp.data.model.directions.Route
import com.example.limouserapp.data.model.directions.Leg
import com.example.limouserapp.data.model.directions.DistanceInfo as DirectionsDistanceInfo
import com.example.limouserapp.data.model.directions.DurationInfo as DirectionsDurationInfo
import com.example.limouserapp.data.network.NetworkConfig
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
     * @return Pair of (distance in meters, duration in seconds)
     */
    suspend fun calculateDistance(
        pickupLat: Double,
        pickupLong: Double,
        dropoffLat: Double,
        dropoffLong: Double,
        waypoints: List<Pair<Double, Double>>? = null
    ): Pair<Int, Int> {
        // Validate coordinates
        if (pickupLat == 0.0 && pickupLong == 0.0) {
            Log.e(TAG, "❌ Invalid pickup coordinates: (0.0, 0.0)")
            return fallbackToStraightLineDistance(pickupLat, pickupLong, dropoffLat, dropoffLong)
        }
        if (dropoffLat == 0.0 && dropoffLong == 0.0) {
            Log.e(TAG, "❌ Invalid dropoff coordinates: (0.0, 0.0)")
            return fallbackToStraightLineDistance(pickupLat, pickupLong, dropoffLat, dropoffLong)
        }
        
        val origin = "$pickupLat,$pickupLong"
        val destination = "$dropoffLat,$dropoffLong"
        val waypointsString = waypoints?.joinToString("|") { "${it.first},${it.second}" }
        
        Log.d(TAG, "🗺️ GOOGLE MAPS DIRECTIONS API:")
        Log.d(TAG, "Origin: $origin (lat=$pickupLat, lng=$pickupLong)")
        Log.d(TAG, "Destination: $destination (lat=$dropoffLat, lng=$dropoffLong)")
        if (waypointsString != null) {
            Log.d(TAG, "Waypoints: $waypointsString (${waypoints.size} stops)")
        }
        
        return try {
            Log.d(TAG, "📡 Making Google Directions API call...")
            val response = directionsApi.getDirections(
                origin = origin,
                destination = destination,
                waypoints = waypointsString,
                key = NetworkConfig.GOOGLE_PLACES_API_KEY
            )
            
            Log.d(TAG, "📥 API Response received:")
            Log.d(TAG, "  Status: ${response.status}")
            Log.d(TAG, "  Routes count: ${response.routes.size}")
            Log.d(TAG, "  Error message: ${response.errorMessage ?: "None"}")
            
            if (response.status == "OK" && response.routes.isNotEmpty()) {
                val route = response.routes.first()
                Log.d(TAG, "  Legs count: ${route.legs.size}")
                
                var totalDistance = 0
                var totalDuration = 0
                
                // Check for invalid legs (distance = 0)
                val invalidLeg: Leg? = route.legs.firstOrNull { leg: Leg -> leg.distance.value == 0 }
                if (invalidLeg != null) {
                    Log.w(TAG, "❌ Invalid leg detected in Google route (distance = 0)")
                    Log.w(TAG, "⚠️ Falling back to straight-line distance calculation")
                    return fallbackToStraightLineDistance(
                        pickupLat, pickupLong, dropoffLat, dropoffLong
                    )
                }
                
                // Sum up distance and duration from all legs
                for ((index, leg) in route.legs.withIndex()) {
                    Log.d(TAG, "  Leg $index: distance=${leg.distance.value}m (${leg.distance.text}), duration=${leg.duration.value}s (${leg.duration.text})")
                    totalDistance += leg.distance.value
                    totalDuration += leg.duration.value
                }
                
                val distanceKm = totalDistance / 1000.0
                Log.d(TAG, "✅ Road Distance: $totalDistance meters (${String.format("%.2f", distanceKm)} km)")
                Log.d(TAG, "✅ Road Duration: $totalDuration seconds")
                
                Pair(totalDistance, totalDuration)
            } else {
                Log.w(TAG, "❌ Google Maps Directions API error:")
                Log.w(TAG, "  Status: ${response.status}")
                Log.w(TAG, "  Error message: ${response.errorMessage ?: "Unknown error"}")
                Log.w(TAG, "  Routes available: ${response.routes.size}")
                Log.w(TAG, "⚠️ Falling back to straight-line distance calculation")
                fallbackToStraightLineDistance(pickupLat, pickupLong, dropoffLat, dropoffLong)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Google Maps Directions API request failed: ${e.message}", e)
            Log.e(TAG, "⚠️ Exception type: ${e.javaClass.simpleName}")
            e.printStackTrace()
            Log.w(TAG, "⚠️ Falling back to straight-line distance calculation")
            fallbackToStraightLineDistance(pickupLat, pickupLong, dropoffLat, dropoffLong)
        }
    }
    
    /**
     * Fallback to straight-line distance calculation using Haversine formula
     * Matches iOS fallbackToStraightLineDistance implementation
     */
    private fun fallbackToStraightLineDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Pair<Int, Int> {
        val R = 6371000.0 // Earth's radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val distance = (R * c).toInt()
        
        // Estimate duration: assume average speed of 60 km/h for city, 100 km/h for highway
        val averageSpeed = if (distance > 50000) 100000.0 else 60000.0 // meters per hour
        val duration = ((distance / averageSpeed) * 3600).toInt() // convert to seconds
        
        Log.w(TAG, "⚠️ Using fallback straight-line distance: $distance meters")
        return Pair(distance, duration)
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
}

