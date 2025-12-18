package com.example.limouserapp.data.model.directions

import com.google.gson.annotations.SerializedName

/**
 * Google Maps Directions API response models
 */
data class DirectionsResponse(
    @SerializedName("status") val status: String,
    @SerializedName("routes") val routes: List<Route> = emptyList(),
    @SerializedName("error_message") val errorMessage: String? = null
)

data class Route(
    @SerializedName("legs") val legs: List<Leg> = emptyList()
)

data class Leg(
    @SerializedName("distance") val distance: DistanceInfo,
    @SerializedName("duration") val duration: DurationInfo
)

data class DistanceInfo(
    @SerializedName("text") val text: String,
    @SerializedName("value") val value: Int // in meters
)

data class DurationInfo(
    @SerializedName("text") val text: String,
    @SerializedName("value") val value: Int // in seconds
)

