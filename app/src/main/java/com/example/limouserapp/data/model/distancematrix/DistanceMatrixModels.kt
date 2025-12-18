package com.example.limouserapp.data.model.distancematrix

import com.google.gson.annotations.SerializedName

/**
 * Google Maps Distance Matrix API response models
 * Matches web app's DistanceMatrixService response structure
 */
data class DistanceMatrixResponse(
    @SerializedName("status") val status: String,
    @SerializedName("rows") val rows: List<DistanceMatrixRow> = emptyList(),
    @SerializedName("error_message") val errorMessage: String? = null,
    @SerializedName("origin_addresses") val originAddresses: List<String> = emptyList(),
    @SerializedName("destination_addresses") val destinationAddresses: List<String> = emptyList()
)

data class DistanceMatrixRow(
    @SerializedName("elements") val elements: List<DistanceMatrixElement> = emptyList()
)

data class DistanceMatrixElement(
    @SerializedName("status") val status: String,
    @SerializedName("distance") val distance: DistanceInfo? = null,
    @SerializedName("duration") val duration: DurationInfo? = null
)

data class DistanceInfo(
    @SerializedName("text") val text: String,
    @SerializedName("value") val value: Int // in meters
)

data class DurationInfo(
    @SerializedName("text") val text: String,
    @SerializedName("value") val value: Int // in seconds
)


