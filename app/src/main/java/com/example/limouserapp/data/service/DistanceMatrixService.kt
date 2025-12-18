package com.example.limouserapp.data.service

import com.example.limouserapp.data.api.DistanceMatrixApi
import com.example.limouserapp.data.model.distancematrix.DistanceMatrixResponse
import com.example.limouserapp.data.model.distancematrix.DistanceMatrixRow
import com.example.limouserapp.data.model.distancematrix.DistanceMatrixElement
import com.example.limouserapp.data.network.NetworkConfig
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service to calculate distance and duration using Google Maps Distance Matrix API
 * Matches web app's DistanceMatrixService implementation for initial quote validation
 */
@Singleton
class DistanceMatrixService @Inject constructor(
    private val distanceMatrixApi: DistanceMatrixApi
) {
    companion object {
        private const val TAG = "DistanceMatrixService"
    }
    
    /**
     * Calculate distance matrix between multiple origins and destinations
     * Matches web app's calculateDistance() function in home.component.ts
     * @param origins List of origin coordinates (lat, lng pairs)
     * @param destinations List of destination coordinates (lat, lng pairs)
     * @return Result containing list of distance/duration pairs for each origin-destination combination
     */
    suspend fun calculateDistanceMatrix(
        origins: List<Pair<Double, Double>>,
        destinations: List<Pair<Double, Double>>
    ): Result<List<Pair<Int, Int>>> {
        // Validate inputs
        if (origins.isEmpty() || destinations.isEmpty()) {
            Log.e(TAG, "❌ Invalid inputs: origins or destinations list is empty")
            return Result.failure(IllegalArgumentException("Origins and destinations must not be empty"))
        }
        
        // Format origins and destinations as strings
        val originsString = origins.joinToString("|") { "${it.first},${it.second}" }
        val destinationsString = destinations.joinToString("|") { "${it.first},${it.second}" }
        
        Log.d(TAG, "🗺️ GOOGLE MAPS DISTANCE MATRIX API:")
        Log.d(TAG, "Origins: $originsString (${origins.size} locations)")
        Log.d(TAG, "Destinations: $destinationsString (${destinations.size} locations)")
        
        return try {
            Log.d(TAG, "📡 Making Google Distance Matrix API call...")
            val response = distanceMatrixApi.getDistanceMatrix(
                origins = originsString,
                destinations = destinationsString,
                mode = "driving",
                units = "metric",
                key = NetworkConfig.GOOGLE_PLACES_API_KEY
            )
            
            Log.d(TAG, "📥 API Response received:")
            Log.d(TAG, "  Status: ${response.status}")
            Log.d(TAG, "  Rows count: ${response.rows.size}")
            Log.d(TAG, "  Error message: ${response.errorMessage ?: "None"}")
            
            if (response.status == "OK" && response.rows.isNotEmpty()) {
                val results = mutableListOf<Pair<Int, Int>>()
                
                // Process each row (origin)
                for ((rowIndex, row) in response.rows.withIndex()) {
                    // Process each element (destination) in the row
                    for ((elementIndex, element) in row.elements.withIndex()) {
                        if (element.status == "OK" && element.distance != null && element.duration != null) {
                            val distance = element.distance.value
                            val duration = element.duration.value
                            
                            Log.d(TAG, "  Row $rowIndex, Element $elementIndex: distance=${distance}m (${element.distance.text}), duration=${duration}s (${element.duration.text})")
                            
                            // Check for invalid results (distance = 0)
                            if (distance == 0) {
                                Log.w(TAG, "❌ Invalid element detected (distance = 0) at row $rowIndex, element $elementIndex")
                                return Result.failure(IllegalStateException("Invalid location points: distance is zero"))
                            }
                            
                            results.add(Pair(distance, duration))
                        } else if (element.status == "ZERO_RESULTS") {
                            Log.w(TAG, "❌ ZERO_RESULTS for row $rowIndex, element $elementIndex")
                            return Result.failure(IllegalStateException("Invalid location points: no route found"))
                        } else {
                            Log.w(TAG, "❌ Error status '${element.status}' for row $rowIndex, element $elementIndex")
                            return Result.failure(IllegalStateException("Invalid location points: ${element.status}"))
                        }
                    }
                }
                
                val distanceKm = results.sumOf { it.first } / 1000.0
                Log.d(TAG, "✅ Total Distance: ${results.sumOf { it.first }} meters (${String.format("%.2f", distanceKm)} km)")
                Log.d(TAG, "✅ Total Duration: ${results.sumOf { it.second }} seconds")
                
                Result.success(results)
            } else {
                Log.w(TAG, "❌ Google Maps Distance Matrix API error:")
                Log.w(TAG, "  Status: ${response.status}")
                Log.w(TAG, "  Error message: ${response.errorMessage ?: "Unknown error"}")
                Log.w(TAG, "  Rows available: ${response.rows.size}")
                Result.failure(IllegalStateException("Distance Matrix API error: ${response.status} - ${response.errorMessage ?: "Unknown error"}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Google Maps Distance Matrix API request failed: ${e.message}", e)
            Log.e(TAG, "⚠️ Exception type: ${e.javaClass.simpleName}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * Calculate distance matrix for a single origin-destination pair
     * Convenience method for simple one-way trips
     * @param originLat Origin latitude
     * @param originLng Origin longitude
     * @param destinationLat Destination latitude
     * @param destinationLng Destination longitude
     * @return Result containing distance (meters) and duration (seconds) pair
     */
    suspend fun calculateDistance(
        originLat: Double,
        originLng: Double,
        destinationLat: Double,
        destinationLng: Double
    ): Result<Pair<Int, Int>> {
        val result = calculateDistanceMatrix(
            origins = listOf(Pair(originLat, originLng)),
            destinations = listOf(Pair(destinationLat, destinationLng))
        )
        
        return result.map { it.first() } // Return first (and only) result
    }
}


