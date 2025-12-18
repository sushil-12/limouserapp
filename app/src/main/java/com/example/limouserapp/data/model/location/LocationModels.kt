package com.example.limouserapp.data.model.location

import kotlinx.serialization.Serializable

/**
 * Google Places API response models
 */

@Serializable
data class PlacesResponse(
    val predictions: List<PlacePrediction>,
    val status: String
)

@Serializable
data class PlacePrediction(
    val place_id: String,
    val description: String,
    val structured_formatting: StructuredFormatting
)

@Serializable
data class StructuredFormatting(
    val main_text: String,
    val secondary_text: String
)

@Serializable
data class PlaceDetails(
    val result: PlaceResult,
    val status: String
)

@Serializable
data class PlaceResult(
    val place_id: String,
    val formatted_address: String,
    val geometry: PlaceGeometry,
    val address_components: List<AddressComponent>
)

@Serializable
data class PlaceGeometry(
    val location: PlaceLocation
)

@Serializable
data class PlaceLocation(
    val lat: Double,
    val lng: Double
)

@Serializable
data class AddressComponent(
    val long_name: String,
    val short_name: String,
    val types: List<String>
)

@Serializable
data class LocationInfo(
    val placeId: String,
    val formattedAddress: String,
    val city: String?,
    val state: String?,
    val postalCode: String?,
    val latitude: Double,
    val longitude: Double
)
