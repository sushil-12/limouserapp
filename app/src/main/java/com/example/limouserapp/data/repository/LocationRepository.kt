package com.example.limouserapp.data.repository

import com.example.limouserapp.data.api.LocationApi
import com.example.limouserapp.data.model.location.LocationInfo
import com.example.limouserapp.data.model.location.PlaceDetails
import com.example.limouserapp.data.model.location.PlacesResponse
import com.example.limouserapp.data.network.NetworkConfig
import com.example.limouserapp.data.network.error.ErrorHandler
import com.example.limouserapp.data.network.error.NetworkError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Location repository
 * Handles location-related data operations using Google Places API
 */
@Singleton
class LocationRepository @Inject constructor(
    private val locationApi: LocationApi,
    private val errorHandler: ErrorHandler
) {
    
    /**
     * Get place autocomplete suggestions
     */
    suspend fun getPlaceSuggestions(
        input: String
    ): Result<PlacesResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = locationApi.getPlacePredictions(
                    input = input,
                    key = NetworkConfig.GOOGLE_PLACES_API_KEY
                )
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }
    
    /**
     * Get detailed place information
     */
    suspend fun getPlaceDetails(
        placeId: String
    ): Result<PlaceDetails> {
        return withContext(Dispatchers.IO) {
            try {
                val response = locationApi.getPlaceDetails(
                    placeId = placeId,
                    key = NetworkConfig.GOOGLE_PLACES_API_KEY
                )
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(Exception(errorHandler.handleError(e)))
            }
        }
    }
    
    /**
     * Get simplified location information
     */
    suspend fun getLocationInfo(placeId: String): Result<LocationInfo> {
        return withContext(Dispatchers.IO) {
            try {
                val placeDetailsResult = getPlaceDetails(placeId)
                if (placeDetailsResult.isFailure) {
                    return@withContext Result.failure(placeDetailsResult.exceptionOrNull()!!)
                }
                
                val placeDetails = placeDetailsResult.getOrThrow()
                val locationInfo = extractLocationInfo(placeDetails)
                Result.success(locationInfo)
            } catch (e: Exception) {
                Result.failure(NetworkError.UnknownError(e))
            }
        }
    }
    
    /**
     * Extract location information from place details
     */
    private fun extractLocationInfo(placeDetails: PlaceDetails): LocationInfo {
        var city = ""
        var state = ""
        var postalCode = ""
        var country = ""
        
        // Extract information from address components
        placeDetails.result.address_components.forEach { component ->
            when {
                component.types.contains("locality") -> city = component.long_name
                component.types.contains("administrative_area_level_1") -> state = component.long_name
                component.types.contains("postal_code") -> postalCode = component.long_name
                component.types.contains("country") -> country = component.long_name
            }
        }
        
        return LocationInfo(
            placeId = placeDetails.result.place_id,
            formattedAddress = placeDetails.result.formatted_address,
            city = city,
            state = state,
            postalCode = postalCode,
            latitude = placeDetails.result.geometry.location.lat,
            longitude = placeDetails.result.geometry.location.lng
        )
    }
    
}
