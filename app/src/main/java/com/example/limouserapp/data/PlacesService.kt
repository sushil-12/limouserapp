package com.example.limouserapp.data

import android.content.Context
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AddressComponent
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import android.util.Log

class PlacesService(private val context: Context) {

    init {
        // Initialize Places API if not already initialized
        if (!Places.isInitialized()) {
            try {
                Places.initialize(context, "AIzaSyDjV38fI9kDAaVJKqEq2sdgLAHXQPC3Up4")
                Log.d("PlacesService", "Places API initialized successfully")
            } catch (e: Exception) {
                Log.e("PlacesService", "Failed to initialize Places API", e)
            }
        } else {
            Log.d("PlacesService", "Places API is already initialized")
        }
    }

    suspend fun getPlacePredictions(
        query: String,
        types: List<String> = listOf("address"),
        includeEstablishments: Boolean = true
    ): List<PlacePrediction> = withContext(Dispatchers.IO) {
        try {
            if (query.length < 2) return@withContext emptyList()

            Log.d("PlacesService", "Getting predictions for query: $query, includeEstablishments: $includeEstablishments")
            val token = AutocompleteSessionToken.newInstance()
            val requestBuilder = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .setSessionToken(token)
            
            // Support both geocode and establishment types like web app
            // If includeEstablishments is true, don't set TypeFilter (allows both addresses and establishments)
            // If false, use ADDRESS filter for backward compatibility
            if (!includeEstablishments) {
                requestBuilder.setTypeFilter(TypeFilter.ADDRESS)  // Backward compatibility: only addresses
            }
            // When includeEstablishments is true, no TypeFilter is set, allowing both geocode and establishment types
            
            val request = requestBuilder.build()

            val response = Places.createClient(context).findAutocompletePredictions(request).await()
            val predictions = response.autocompletePredictions.map { prediction ->
                PlacePrediction(
                    placeId = prediction.placeId,
                    primaryText = prediction.getPrimaryText(null).toString(),
                    secondaryText = prediction.getSecondaryText(null).toString(),
                    fullText = prediction.getFullText(null).toString()
                )
            }.take(10)  // Limit to 10 results for performance
            
            Log.d("PlacesService", "Found ${predictions.size} predictions")
            predictions
        } catch (e: Exception) {
            Log.e("PlacesService", "Error getting place predictions", e)
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getPlaceDetails(placeId: String): PlaceDetails? = withContext(Dispatchers.IO) {
        try {
            val request = FetchPlaceRequest.builder(
                placeId,
                listOf(
                    Place.Field.NAME,
                    Place.Field.ADDRESS,
                    Place.Field.ADDRESS_COMPONENTS,
                    Place.Field.LAT_LNG,
                    Place.Field.TYPES  // Add types field to match web app
                )
            ).build()

            val response = Places.createClient(context).fetchPlace(request).await()
            val place = response.place
            val addressComponents = place.addressComponents?.asList() ?: emptyList()
            val latLng = place.latLng
            // place.types is List<Place.Type>?, convert to List<String>
            val placeTypes = place.types?.map { it.name } ?: emptyList()

            // Log address components for debugging
            Log.d("PlacesService", "Address components: ${addressComponents.map { "${it.types} -> ${it.name}" }}")
            Log.d("PlacesService", "Place types: $placeTypes")

            // Extract granular address components using the 'name' property
            val streetNumber = addressComponents.find { it.types.contains("street_number") }?.name ?: ""
            val route = addressComponents.find { it.types.contains("route") }?.name ?: ""
            val city = addressComponents.find { it.types.contains("locality") }?.name
                ?: addressComponents.find { it.types.contains("postal_town") }?.name ?: ""
            val state = addressComponents.find { it.types.contains("administrative_area_level_1") }?.name ?: ""
            val postalCodeComponent = addressComponents.find { it.types.contains("postal_code") }
            val postalCode = postalCodeComponent?.name ?: ""
            val country = addressComponents.find { it.types.contains("country") }?.name ?: ""
            
            Log.d("PlacesService", "Extracted details - Address: ${place.address}, City: $city, State: $state, PostalCode: $postalCode, Country: $country")

            PlaceDetails(
                name = place.name ?: "",
                address = place.address ?: "",
                postalCode = postalCode,
                city = city,
                state = state,
                country = country,
                streetNumber = streetNumber,
                route = route,
                latitude = latLng?.latitude,
                longitude = latLng?.longitude,
                types = placeTypes
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Extract town/city from address components
     * Matches web app's getTown() function logic
     * @param addressComponents List of address components from Places API
     * @return Town/city name (long_name) or null if not found
     */
    fun getTown(addressComponents: List<AddressComponent>): String? {
        // First check for locality type
        val locality = addressComponents.find { it.types.contains("locality") }?.name
        if (locality != null && locality.isNotEmpty()) {
            return locality
        }
        
        // Fallback to postal_town if locality not found
        val postalTown = addressComponents.find { it.types.contains("postal_town") }?.name
        if (postalTown != null && postalTown.isNotEmpty()) {
            return postalTown
        }
        
        return null
    }

    /**
     * Check if two locations are in the same town/city
     * Matches web app's checkExtraStopInTown() function logic
     * @param location1 First location address string
     * @param location2 Second location address string
     * @return "in_town" if both locations are in same town/city, "out_town" if different, null if comparison fails
     */
    suspend fun checkExtraStopInTown(location1: String, location2: String): String? = withContext(Dispatchers.IO) {
        try {
            if (location1.isEmpty() || location2.isEmpty()) {
                Log.w("PlacesService", "checkExtraStopInTown: Empty location provided")
                return@withContext null
            }

            Log.d("PlacesService", "checkExtraStopInTown: Comparing '$location1' and '$location2'")
            
            // Use Places API to geocode both addresses
            // Get place details for location1
            val predictions1 = getPlacePredictions(location1, includeEstablishments = true)
            if (predictions1.isEmpty()) {
                Log.w("PlacesService", "checkExtraStopInTown: No predictions found for location1: $location1")
                return@withContext null
            }
            
            val placeDetails1 = getPlaceDetails(predictions1.first().placeId)
            if (placeDetails1 == null) {
                Log.w("PlacesService", "checkExtraStopInTown: Failed to get place details for location1")
                return@withContext null
            }
            
            // Get place details for location2
            val predictions2 = getPlacePredictions(location2, includeEstablishments = true)
            if (predictions2.isEmpty()) {
                Log.w("PlacesService", "checkExtraStopInTown: No predictions found for location2: $location2")
                return@withContext null
            }
            
            val placeDetails2 = getPlaceDetails(predictions2.first().placeId)
            if (placeDetails2 == null) {
                Log.w("PlacesService", "checkExtraStopInTown: Failed to get place details for location2")
                return@withContext null
            }
            
            // Extract towns using getTown() function
            // We need to get address components from place details
            // Since we already have city in PlaceDetails, we can use that directly
            val town1 = placeDetails1.city.ifEmpty { null }
            val town2 = placeDetails2.city.ifEmpty { null }
            
            if (town1 == null || town2 == null) {
                Log.w("PlacesService", "checkExtraStopInTown: Could not extract town for one or both locations")
                return@withContext null
            }
            
            val result = if (town1.equals(town2, ignoreCase = true)) {
                Log.d("PlacesService", "checkExtraStopInTown: Both locations are in the same town/city: $town1")
                "in_town"
            } else {
                Log.d("PlacesService", "checkExtraStopInTown: Locations are in different towns/cities: '$town1' vs '$town2'")
                "out_town"
            }
            
            result
        } catch (e: Exception) {
            Log.e("PlacesService", "checkExtraStopInTown: Error comparing locations", e)
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Check if two locations are in the same town/city using coordinates
     * Alternative method that uses coordinates instead of address strings
     * @param lat1 Latitude of first location
     * @param lng1 Longitude of first location
     * @param lat2 Latitude of second location
     * @param lng2 Longitude of second location
     * @return "in_town" if both locations are in same town/city, "out_town" if different, null if comparison fails
     */
    suspend fun checkExtraStopInTownByCoordinates(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double
    ): String? = withContext(Dispatchers.IO) {
        try {
            // Use reverse geocoding to get address components for both coordinates
            // For now, we'll use a simpler approach: fetch place details using coordinates
            // Note: This requires using Geocoding API or Places API reverse geocoding
            // For now, we'll return null and suggest using the address-based method
            Log.w("PlacesService", "checkExtraStopInTownByCoordinates: Coordinate-based comparison not yet implemented. Use checkExtraStopInTown() with address strings instead.")
            null
        } catch (e: Exception) {
            Log.e("PlacesService", "checkExtraStopInTownByCoordinates: Error comparing locations", e)
            null
        }
    }
}

data class PlacePrediction(
    val placeId: String,
    val primaryText: String,
    val secondaryText: String,
    val fullText: String
)

data class PlaceDetails(
    val name: String,
    val address: String,
    val postalCode: String,
    val city: String,
    val state: String,
    val country: String = "",
    val streetNumber: String = "",
    val route: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val types: List<String>? = null  // Place types for filtering/validation logic
)