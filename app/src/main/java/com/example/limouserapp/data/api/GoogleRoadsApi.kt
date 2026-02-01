package com.example.limouserapp.data.api

import com.google.android.gms.maps.model.LatLng
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Google Roads API for snapping GPS points to roads
 * https://developers.google.com/maps/documentation/roads/snap-to-roads
 */
interface GoogleRoadsApi {
    /**
     * Snap GPS points to roads
     * @param path Pipe-separated lat,lng pairs (e.g., "lat1,lng1|lat2,lng2")
     * @param interpolate Whether to interpolate a path to include all points forming the full road-geometry
     * @param apiKey Google Maps API key
     */
    @GET("/v1/snapToRoads")
    suspend fun snapToRoads(
        @Query("path") path: String,
        @Query("interpolate") interpolate: Boolean = true,
        @Query("key") apiKey: String
    ): SnapToRoadsResponse
}

data class SnapToRoadsResponse(
    @SerializedName("snappedPoints") val snappedPoints: List<SnappedPoint>?,
    @SerializedName("warningMessage") val warningMessage: String?
)

data class SnappedPoint(
    @SerializedName("location") val location: LocationPoint,
    @SerializedName("originalIndex") val originalIndex: Int?,
    @SerializedName("placeId") val placeId: String?
)

data class LocationPoint(
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double
) {
    fun toLatLng(): LatLng = LatLng(latitude, longitude)
}
