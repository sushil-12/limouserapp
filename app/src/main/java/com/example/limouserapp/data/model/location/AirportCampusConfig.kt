package com.example.limouserapp.data.model.location

import com.google.gson.annotations.SerializedName

/**
 * Airport/Campus configuration model
 * Defines polygons, bounding boxes, and terminal POIs for special routing
 */
data class AirportCampusConfig(
    @SerializedName("sites") val sites: List<Site>
)

data class Site(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("code") val code: String?,
    @SerializedName("type") val type: String, // "airport" or "campus"
    @SerializedName("boundingBox") val boundingBox: BoundingBox,
    @SerializedName("polygon") val polygon: List<LatLngPoint>,
    @SerializedName("terminalPOIs") val terminalPOIs: List<TerminalPOI>,
    @SerializedName("preferredPOI") val preferredPOI: String?
) {
    fun getPreferredPOI(): TerminalPOI? {
        return preferredPOI?.let { poiId ->
            terminalPOIs.firstOrNull { it.id == poiId }
        } ?: terminalPOIs.firstOrNull()
    }
}

data class BoundingBox(
    @SerializedName("northeast") val northeast: LatLngPoint,
    @SerializedName("southwest") val southwest: LatLngPoint
)

data class LatLngPoint(
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double
)

data class TerminalPOI(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("entranceName") val entranceName: String,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("description") val description: String
)
