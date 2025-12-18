package com.example.limouserapp.data.model.booking

import com.google.gson.annotations.SerializedName

/**
 * Airport API response model
 */
data class AirportResponse(
    @SerializedName("data") val data: AirportData,
    @SerializedName("pagination") val pagination: AirportPagination?
)

data class AirportData(
    @SerializedName("airportsData") val airportsData: List<Airport>
)

data class Airport(
    @SerializedName("id") val id: Int,
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String,
    @SerializedName("city") val city: String,
    @SerializedName("country") val country: String,
    @SerializedName("lat") val lat: Double?,
    @SerializedName("long") val long: Double?
) {
    /**
     * Display name for dropdowns (matches iOS format)
     */
    val displayName: String
        get() = "$code - $name"
}

data class AirportPagination(
    @SerializedName("current_page") val currentPage: Int,
    @SerializedName("last_page") val lastPage: Int,
    @SerializedName("has_next_page") val hasNextPage: Boolean
)

