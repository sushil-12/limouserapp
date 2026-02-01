package com.example.limouserapp.data.service

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.example.limouserapp.R
import com.example.limouserapp.data.model.location.AirportCampusConfig
import com.example.limouserapp.data.model.location.Site
import com.example.limouserapp.data.model.location.TerminalPOI
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for detecting airports/campuses and providing terminal POIs
 * Uses polygon point-in-polygon algorithm for accurate detection
 */
@Singleton
class AirportCampusService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var config: AirportCampusConfig? = null
    private val gson = Gson()

    init {
        loadConfig()
    }

    private fun loadConfig() {
        try {
            val inputStream: InputStream = context.resources.openRawResource(R.raw.airport_campus_config)
            val json = inputStream.bufferedReader().use { it.readText() }
            config = gson.fromJson(json, AirportCampusConfig::class.java)
        } catch (e: Exception) {
            Timber.tag("AirportCampusService").e(e, "Failed to load airport/campus config")
            config = AirportCampusConfig(emptyList())
        }
    }

    /**
     * Check if a location is inside any airport/campus polygon
     */
    fun isInsideAirportOrCampus(location: LatLng): Site? {
        val sites = config?.sites ?: return null
        return sites.firstOrNull { site ->
            isPointInPolygon(location, site.polygon.map { LatLng(it.lat, it.lng) })
        }
    }

    /**
     * Get preferred terminal POI for a site
     */
    fun getPreferredPOI(site: Site): TerminalPOI? {
        return site.getPreferredPOI()
    }

    /**
     * Get terminal POI message for display
     */
    fun getTerminalMessage(site: Site): String {
        val poi = site.getPreferredPOI()
        return poi?.description ?: "Arrived at Terminal — follow signs to pickup area"
    }

    /**
     * Point-in-polygon test using ray casting algorithm
     * Returns true if point is inside polygon
     */
    private fun isPointInPolygon(point: LatLng, polygon: List<LatLng>): Boolean {
        if (polygon.size < 3) return false

        var inside = false
        var j = polygon.size - 1

        for (i in polygon.indices) {
            val pi = polygon[i]
            val pj = polygon[j]

            if (((pi.latitude > point.latitude) != (pj.latitude > point.latitude)) &&
                (point.longitude < (pj.longitude - pi.longitude) * (point.latitude - pi.latitude) /
                        (pj.latitude - pi.latitude) + pi.longitude)) {
                inside = !inside
            }
            j = i
        }

        return inside
    }

    /**
     * Quick bounding box check before polygon test (performance optimization)
     */
    fun isInBoundingBox(location: LatLng, site: Site): Boolean {
        val box = site.boundingBox
        return location.latitude >= box.southwest.lat &&
                location.latitude <= box.northeast.lat &&
                location.longitude >= box.southwest.lng &&
                location.longitude <= box.northeast.lng
    }
}
