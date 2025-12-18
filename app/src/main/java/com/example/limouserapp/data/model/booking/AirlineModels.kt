package com.example.limouserapp.data.model.booking

import com.google.gson.annotations.SerializedName

/**
 * Airline API response model
 */
data class AirlineResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: AirlineData,
    @SerializedName("message") val message: String
)

data class AirlineData(
    @SerializedName("airlinesData") val airlinesData: List<Airline>
)

data class Airline(
    @SerializedName("id") val id: Int,
    @SerializedName("code") val code: String,
    @SerializedName("name") val name: String,
    @SerializedName("country") val country: String?
) {
    /**
     * Display name for dropdowns (matches iOS format)
     */
    val displayName: String
        get() = "$code - $name"
    
    /**
     * Full display name with country (matches iOS fullDisplayName)
     */
    val fullDisplayName: String
        get() = if (country != null) {
            "$code - $name, $country"
        } else {
            "$code - $name"
        }
    
    /**
     * Search text for filtering (matches iOS searchText)
     */
    val searchText: String
        get() = "$code $name ${country ?: ""}".lowercase()
}

