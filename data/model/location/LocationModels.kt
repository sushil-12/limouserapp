package com.example.limouserapp.data.model.location

import com.google.gson.annotations.SerializedName

/**
 * Google Places API autocomplete prediction
 */
data class PlacePrediction(
    @SerializedName("place_id")
    val placeId: String,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("structured_formatting")
    val structuredFormatting: StructuredFormatting,
    
    @SerializedName("types")
    val types: List<String>
)

/**
 * Structured formatting for place predictions
 */
data class StructuredFormatting(
    @SerializedName("main_text")
    val mainText: String,
    
    @SerializedName("secondary_text")
    val secondaryText: String
)

/**
 * Google Places API response wrapper
 */
data class PlacesResponse(
    @SerializedName("predictions")
    val predictions: List<PlacePrediction>,
    
    @SerializedName("status")
    val status: String
)

/**
 * Place details from Google Places API
 */
data class PlaceDetails(
    @SerializedName("place_id")
    val placeId: String,
    
    @SerializedName("formatted_address")
    val formattedAddress: String,
    
    @SerializedName("geometry")
    val geometry: Geometry,
    
    @SerializedName("address_components")
    val addressComponents: List<AddressComponent>
)

/**
 * Geometry information for places
 */
data class Geometry(
    @SerializedName("location")
    val location: Location
)

/**
 * Location coordinates
 */
data class Location(
    @SerializedName("lat")
    val latitude: Double,
    
    @SerializedName("lng")
    val longitude: Double
)

/**
 * Address component from Google Places
 */
data class AddressComponent(
    @SerializedName("long_name")
    val longName: String,
    
    @SerializedName("short_name")
    val shortName: String,
    
    @SerializedName("types")
    val types: List<String>
)

/**
 * Simplified location model for internal use
 */
data class LocationInfo(
    val placeId: String,
    val address: String,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String,
    val latitude: Double,
    val longitude: Double
)
