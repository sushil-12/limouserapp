package com.example.limouserapp.ui.booking.comprehensivebooking

import android.util.Log
import com.example.limouserapp.data.model.booking.RideData
import com.example.limouserapp.data.model.booking.ExtraStopRequest
import com.example.limouserapp.data.model.booking.Airport
import com.example.limouserapp.data.model.booking.RateBreakdown
import com.example.limouserapp.data.model.booking.Vehicle
import com.example.limouserapp.ui.utils.DebugTags
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper functions for booking operations
 */

fun getServiceTypeDisplayName(serviceType: String): String {
    return when (serviceType.lowercase()) {
        "one_way" -> "One Way"
        "round_trip" -> "Round Trip"
        "charter_tour" -> "Charter Tour"
        else -> serviceType
    }
}

fun getTransferTypeDisplayName(rideData: RideData): String {
    val pickupType = rideData.pickupType.lowercase()
    val dropoffType = rideData.dropoffType.lowercase()
    
    return when {
        pickupType == "city" && dropoffType == "city" -> "City to City"
        pickupType == "city" && dropoffType == "airport" -> "City to Airport"
        pickupType == "airport" && dropoffType == "city" -> "Airport to City"
        pickupType == "airport" && dropoffType == "airport" -> "Airport to Airport"
        pickupType == "city" && dropoffType == "cruise" -> "City to Cruise Port"
        pickupType == "cruise" && dropoffType == "city" -> "Cruise Port to City"
        else -> "City to City"
    }
}

/**
 * Helper function to reverse transfer type for return trip (matches iOS getReversedTransferType)
 */
fun getReversedTransferType(outbound: String): String {
    return when (outbound) {
        "City to City" -> "City to City"
        "City to Airport" -> "Airport to City"
        "Airport to City" -> "City to Airport"
        "Airport to Airport" -> "Airport to Airport"
        "City to Cruise Port" -> "Cruise Port to City"
        "Cruise Port to City" -> "City to Cruise Port"
        "Airport to Cruise Port" -> "Cruise Port to Airport"
        "Cruise Port to Airport" -> "Airport to Cruise Port"
        "Cruise Port to Cruise Port" -> "Cruise Port to Cruise Port"
        else -> "City to City"
    }
}

fun formatDate(dateString: String): String {
    return try {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val date = dateFormat.parse(dateString)
        if (date != null) {
            displayDateFormat.format(date)
        } else {
            dateString
        }
    } catch (e: Exception) {
        dateString
    }
}

fun formatTime(timeString: String): String {
    return try {
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val displayTimeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val time = timeFormat.parse(timeString)
        if (time != null) {
            displayTimeFormat.format(time)
        } else {
            timeString
        }
    } catch (e: Exception) {
        timeString
    }
}

/**
 * Prefill extra stops from edit data (matches iOS prefillExtraStopsFromEditData)
 * Returns Pair of (outboundExtraStops, returnExtraStops)
 */
fun prefillExtraStopsFromEditData(
    editData: com.example.limouserapp.data.model.booking.EditReservationData,
    serviceType: String
): Pair<List<ExtraStop>, List<ExtraStop>> {
    Log.d(DebugTags.BookingProcess, "🔄 PREFILLING EXTRA STOPS FROM EDIT DATA:")
    Log.d(DebugTags.BookingProcess, "Extra Stops: ${editData.extraStops}")
    Log.d(DebugTags.BookingProcess, "Return Extra Stops: ${editData.returnExtraStops}")
    
    val outboundStops = mutableListOf<ExtraStop>()
    val returnStops = mutableListOf<ExtraStop>()
    
    // Parse extra stops from edit data
    if (!editData.extraStops.isNullOrEmpty()) {
        try {
            val extraStopsString = editData.extraStops.trim()
            if (extraStopsString.startsWith("[")) {
                val jsonArray = JSONArray(extraStopsString)
                Log.d(DebugTags.BookingProcess, "  Parsing extra stops as JSON array with ${jsonArray.length()} stops")
                
                for (i in 0 until jsonArray.length()) {
                    val stopData = jsonArray.getJSONObject(i)
                    val address = stopData.optString("address", "")
                    if (address.isNotEmpty()) {
                        val newStop = ExtraStop(
                            address = address,
                            latitude = stopData.optDouble("latitude", Double.NaN).takeIf { !it.isNaN() },
                            longitude = stopData.optDouble("longitude", Double.NaN).takeIf { !it.isNaN() },
                            isLocationSelected = true
                        )
                        outboundStops.add(newStop)
                        Log.d(DebugTags.BookingProcess, "  Added extra stop: $address")
                    }
                }
            } else {
                Log.d(DebugTags.BookingProcess, "  Parsing extra stops as comma-separated string")
                val stopAddresses = extraStopsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                for (address in stopAddresses) {
                    val newStop = ExtraStop(
                        address = address,
                        isLocationSelected = true
                    )
                    outboundStops.add(newStop)
                    Log.d(DebugTags.BookingProcess, "  Added extra stop: $address")
                }
            }
        } catch (e: Exception) {
            Log.e(DebugTags.BookingProcess, "Error parsing extra stops", e)
        }
    }
    
    // For round trips, also handle return extra stops
    if (serviceType == "Round Trip" && !editData.returnExtraStops.isNullOrEmpty()) {
        try {
            val returnExtraStopsString = editData.returnExtraStops.trim()
            if (returnExtraStopsString.startsWith("[")) {
                val jsonArray = JSONArray(returnExtraStopsString)
                Log.d(DebugTags.BookingProcess, "  Parsing return extra stops as JSON array with ${jsonArray.length()} stops")
                
                for (i in 0 until jsonArray.length()) {
                    val stopData = jsonArray.getJSONObject(i)
                    val address = stopData.optString("address", "")
                    if (address.isNotEmpty()) {
                        val newStop = ExtraStop(
                            address = address,
                            latitude = stopData.optDouble("latitude", Double.NaN).takeIf { !it.isNaN() },
                            longitude = stopData.optDouble("longitude", Double.NaN).takeIf { !it.isNaN() },
                            isLocationSelected = true
                        )
                        returnStops.add(newStop)
                        Log.d(DebugTags.BookingProcess, "  Added return extra stop: $address")
                    }
                }
            } else {
                Log.d(DebugTags.BookingProcess, "  Parsing return extra stops as comma-separated string")
                val stopAddresses = returnExtraStopsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                for (address in stopAddresses) {
                    val newStop = ExtraStop(
                        address = address,
                        isLocationSelected = true
                    )
                    returnStops.add(newStop)
                    Log.d(DebugTags.BookingProcess, "  Added return extra stop: $address")
                }
            }
        } catch (e: Exception) {
            Log.e(DebugTags.BookingProcess, "Error parsing return extra stops", e)
        }
    }
    
    Log.d(DebugTags.BookingProcess, "  Total outbound extra stops prefilled: ${outboundStops.size}")
    Log.d(DebugTags.BookingProcess, "  Total return extra stops prefilled: ${returnStops.size}")
    
    return Pair(outboundStops, returnStops)
}

/**
 * Check if two coordinates are approximately equal (matches iOS coordinatesApproximatelyEqual)
 */
fun coordinatesApproximatelyEqual(
    coord1: Pair<Double, Double>,
    coord2: Pair<Double, Double>,
    tolerance: Double = 0.002
): Boolean {
    val distance = calculateDistance(coord1.first, coord1.second, coord2.first, coord2.second)
    val toleranceMeters = tolerance * 111000
    return distance < toleranceMeters
}

/**
 * Normalize location text for comparison (matches iOS normalizeLocationText)
 */
fun normalizeLocationText(text: String): String {
    return text.trim()
        .replace(Regex("\\s+"), " ")
        .replace(Regex("[,;]"), " ")
        .trim()
        .uppercase()
}

/**
 * Extract country from address (matches iOS extractCountryFromAddress)
 */
fun extractCountryFromAddress(address: String): String? {
    val components = address.split(",", ";")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    
    for (component in components.reversed()) {
        if (component.any { it.isLetter() }) {
            return component
        }
    }
    
    return null
}

/**
 * Normalize country name for comparison (matches iOS normalizeCountry)
 */
fun normalizeCountry(country: String?): String? {
    if (country.isNullOrBlank()) return null
    
    var normalized = country.trim()
        .replace(".", "")
        .replace(",", "")
        .replace(";", "")
        .replace("  ", " ")
        .uppercase()
    
    val synonyms = mapOf(
        "USA" to "UNITED STATES",
        "US" to "UNITED STATES",
        "U.S.A" to "UNITED STATES",
        "U.S." to "UNITED STATES",
        "UK" to "UNITED KINGDOM",
        "U.K." to "UNITED KINGDOM"
    )
    
    return synonyms[normalized] ?: normalized
}

/**
 * Check if pickup and dropoff countries are different (matches iOS checkCountryMismatch)
 */
fun checkCountryMismatch(
    pickupLocation: String,
    dropoffLocation: String
): Boolean {
    val pickupCountry = extractCountryFromAddress(pickupLocation)
    val dropoffCountry = extractCountryFromAddress(dropoffLocation)
    
    val normalizedPickup = normalizeCountry(pickupCountry)
    val normalizedDropoff = normalizeCountry(dropoffCountry)
    
    if (normalizedPickup != null && normalizedDropoff != null) {
        return normalizedPickup != normalizedDropoff
    }
    
    return false
}

/**
 * Get effective outbound pickup coordinate based on transfer type
 */
fun getEffectiveOutboundPickupCoordinate(
    transferType: String,
    pickupLat: Double?,
    pickupLong: Double?,
    selectedPickupAirport: Airport?,
    selectedPickupCruise: String? = null
): Pair<Double?, Double?> {
    return when {
        transferType.contains("Airport", ignoreCase = true) && transferType.startsWith("Airport", ignoreCase = true) -> {
            if (selectedPickupAirport != null && selectedPickupAirport.lat != null && selectedPickupAirport.long != null) {
                Pair(selectedPickupAirport.lat, selectedPickupAirport.long)
            } else {
                Pair(pickupLat, pickupLong)
            }
        }
        transferType.contains("Cruise", ignoreCase = true) && transferType.startsWith("Cruise", ignoreCase = true) -> {
            Pair(pickupLat, pickupLong)
        }
        else -> {
            Pair(pickupLat, pickupLong)
        }
    }
}

/**
 * Get effective outbound dropoff coordinate based on transfer type
 */
fun getEffectiveOutboundDropoffCoordinate(
    transferType: String,
    dropoffLat: Double?,
    dropoffLong: Double?,
    selectedDropoffAirport: Airport?,
    selectedDropoffCruise: String? = null
): Pair<Double?, Double?> {
    return when {
        transferType.contains("Airport", ignoreCase = true) && transferType.endsWith("Airport", ignoreCase = true) -> {
            if (selectedDropoffAirport != null && selectedDropoffAirport.lat != null && selectedDropoffAirport.long != null) {
                Pair(selectedDropoffAirport.lat, selectedDropoffAirport.long)
            } else {
                Pair(dropoffLat, dropoffLong)
            }
        }
        transferType.contains("Cruise", ignoreCase = true) && transferType.endsWith("Cruise", ignoreCase = true) -> {
            Pair(dropoffLat, dropoffLong)
        }
        else -> {
            Pair(dropoffLat, dropoffLong)
        }
    }
}

/**
 * Get outbound pickup latitude
 */
fun getOutboundPickupLatitude(
    transferType: String,
    rideData: RideData,
    selectedPickupAirport: Airport?
): Double? {
    val (lat, _) = getEffectiveOutboundPickupCoordinate(
        transferType,
        rideData.pickupLat,
        rideData.pickupLong,
        selectedPickupAirport
    )
    return lat
}

/**
 * Get outbound pickup longitude
 */
fun getOutboundPickupLongitude(
    transferType: String,
    rideData: RideData,
    selectedPickupAirport: Airport?
): Double? {
    val (_, lng) = getEffectiveOutboundPickupCoordinate(
        transferType,
        rideData.pickupLat,
        rideData.pickupLong,
        selectedPickupAirport
    )
    return lng
}

/**
 * Get outbound dropoff latitude
 */
fun getOutboundDropoffLatitude(
    transferType: String,
    rideData: RideData,
    selectedDropoffAirport: Airport?
): Double? {
    val (lat, _) = getEffectiveOutboundDropoffCoordinate(
        transferType,
        rideData.destinationLat,
        rideData.destinationLong,
        selectedDropoffAirport
    )
    return lat
}

/**
 * Get outbound dropoff longitude
 */
fun getOutboundDropoffLongitude(
    transferType: String,
    rideData: RideData,
    selectedDropoffAirport: Airport?
): Double? {
    val (_, lng) = getEffectiveOutboundDropoffCoordinate(
        transferType,
        rideData.destinationLat,
        rideData.destinationLong,
        selectedDropoffAirport
    )
    return lng
}

/**
 * Get return pickup latitude
 */
fun getReturnPickupLatitude(
    returnTransferType: String,
    returnPickupLat: Double?,
    returnPickupLong: Double?,
    selectedReturnPickupAirport: Airport?
): Double? {
    val (lat, _) = getEffectiveOutboundPickupCoordinate(
        returnTransferType,
        returnPickupLat,
        returnPickupLong,
        selectedReturnPickupAirport
    )
    return lat
}

/**
 * Get return pickup longitude
 */
fun getReturnPickupLongitude(
    returnTransferType: String,
    returnPickupLat: Double?,
    returnPickupLong: Double?,
    selectedReturnPickupAirport: Airport?
): Double? {
    val (_, lng) = getEffectiveOutboundPickupCoordinate(
        returnTransferType,
        returnPickupLat,
        returnPickupLong,
        selectedReturnPickupAirport
    )
    return lng
}

/**
 * Get return dropoff latitude
 */
fun getReturnDropoffLatitude(
    returnTransferType: String,
    returnDropoffLat: Double?,
    returnDropoffLong: Double?,
    selectedReturnDropoffAirport: Airport?
): Double? {
    val (lat, _) = getEffectiveOutboundDropoffCoordinate(
        returnTransferType,
        returnDropoffLat,
        returnDropoffLong,
        selectedReturnDropoffAirport
    )
    return lat
}

/**
 * Get return dropoff longitude
 */
fun getReturnDropoffLongitude(
    returnTransferType: String,
    returnDropoffLat: Double?,
    returnDropoffLong: Double?,
    selectedReturnDropoffAirport: Airport?
): Double? {
    val (_, lng) = getEffectiveOutboundDropoffCoordinate(
        returnTransferType,
        returnDropoffLat,
        returnDropoffLong,
        selectedReturnDropoffAirport
    )
    return lng
}

/**
 * Validate extra stop (matches iOS validateExtraStop)
 */
fun validateExtraStop(
    stop: ExtraStop,
    pickupLocation: String,
    dropoffLocation: String,
    pickupLat: Double?,
    pickupLong: Double?,
    dropoffLat: Double?,
    dropoffLong: Double?,
    isReturnTrip: Boolean = false
): String? {
    val stopLat = stop.latitude
    val stopLng = stop.longitude
    
    if (stopLat == null || stopLng == null) {
        return null
    }
    
    val stopCoord = Pair(stopLat, stopLng)
    
    if (pickupLat != null && pickupLong != null) {
        val pickupCoord = Pair(pickupLat, pickupLong)
        if (coordinatesApproximatelyEqual(stopCoord, pickupCoord, tolerance = 0.002)) {
            return "Extra stop cannot be the same as pickup location. Please select a different location."
        }
    }
    
    if (dropoffLat != null && dropoffLong != null) {
        val dropoffCoord = Pair(dropoffLat, dropoffLong)
        if (coordinatesApproximatelyEqual(stopCoord, dropoffCoord, tolerance = 0.002)) {
            return "Extra stop cannot be the same as drop-off location. Please select a different location."
        }
    }
    
    val normalizedStopAddress = normalizeLocationText(stop.address)
    val normalizedPickupAddress = normalizeLocationText(pickupLocation)
    val normalizedDropoffAddress = normalizeLocationText(dropoffLocation)
    
    if (normalizedStopAddress.isNotEmpty() && normalizedStopAddress == normalizedPickupAddress) {
        return "Extra stop cannot be the same as pickup location. Please select a different location."
    }
    
    if (normalizedStopAddress.isNotEmpty() && normalizedStopAddress == normalizedDropoffAddress) {
        return "Extra stop cannot be the same as drop-off location. Please select a different location."
    }
    
    return null
}

/**
 * Calculate distance between two coordinates (simple haversine formula)
 */
fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6371000.0 // meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return earthRadius * c
}

/**
 * Convert ExtraStop list to ExtraStopRequest list for API
 * This version uses default "out_town" rate (backward compatibility)
 */
fun List<ExtraStop>.toExtraStopRequests(): List<ExtraStopRequest> {
    return this.filter { it.isLocationSelected && it.latitude != null && it.longitude != null }
        .map { stop ->
            ExtraStopRequest(
                address = stop.address,
                latitude = stop.latitude!!,
                longitude = stop.longitude!!,
                rate = "out_town",
                bookingInstructions = stop.bookingInstructions
            )
        }
}

/**
 * Convert ExtraStop list to ExtraStopRequest list for API with town comparison
 * Matches web app's checkExtraStopInTown() logic
 * @param placesService PlacesService instance for town comparison
 * @param pickupLocation Pickup location address string for comparison
 * @return List of ExtraStopRequest with calculated rates (in_town or out_town)
 */
suspend fun List<ExtraStop>.toExtraStopRequestsWithTownComparison(
    placesService: com.example.limouserapp.data.PlacesService,
    pickupLocation: String
): List<ExtraStopRequest> {
    return this.filter { it.isLocationSelected && it.latitude != null && it.longitude != null && it.address.isNotEmpty() }
        .map { stop ->
            // Determine rate by comparing extra stop location with pickup location
            val rate = try {
                val comparisonResult = placesService.checkExtraStopInTown(pickupLocation, stop.address)
                comparisonResult ?: "out_town" // Default to out_town if comparison fails
            } catch (e: Exception) {
                android.util.Log.e("BookingUtils", "Error checking town for extra stop: ${stop.address}", e)
                "out_town" // Default to out_town on error
            }
            
            ExtraStopRequest(
                address = stop.address,
                latitude = stop.latitude!!,
                longitude = stop.longitude!!,
                rate = rate,
                bookingInstructions = stop.bookingInstructions
            )
        }
}

/**
 * Helper function to check if there are extra stops
 */
fun hasExtraStops(editData: com.example.limouserapp.data.model.booking.EditReservationData?): Boolean {
    if (editData == null) return false
    
    val hasOutboundStops = !editData.extraStops.isNullOrEmpty() && 
                           editData.extraStops.trim().isNotEmpty() &&
                           editData.extraStops.trim() != "[]" &&
                           editData.extraStops.trim() != "null"
    
    val hasReturnStops = !editData.returnExtraStops.isNullOrEmpty() && 
                        editData.returnExtraStops.trim().isNotEmpty() &&
                        editData.returnExtraStops.trim() != "[]" &&
                        editData.returnExtraStops.trim() != "null"
    
    val result = hasOutboundStops || hasReturnStops
    Log.d(DebugTags.BookingProcess, "🔍 Checking extra stops:")
    Log.d(DebugTags.BookingProcess, "  Extra Stops: ${editData.extraStops}")
    Log.d(DebugTags.BookingProcess, "  Return Extra Stops: ${editData.returnExtraStops}")
    Log.d(DebugTags.BookingProcess, "  Has Extra Stops: $result")
    return result
}

/**
 * Extension function to get rate breakdown from vehicle
 */
fun Vehicle.getRateBreakdown(serviceType: String): RateBreakdown? {
    return when (serviceType.lowercase()) {
        "one_way" -> rateBreakdownOneWay
        "round_trip" -> rateBreakdownRoundTrip
        "charter_tour" -> rateBreakdownCharterTour
        else -> null
    }
}

/**
 * Convert Android transfer type display name to web format (with underscores)
 * Matches web app's transfer_type format
 */
fun convertTransferTypeToWebFormat(transferType: String): String {
    return when (transferType) {
        "City to City" -> "city_to_city"
        "City to Airport" -> "city_to_airport"
        "Airport to City" -> "airport_to_city"
        "Airport to Airport" -> "airport_to_airport"
        "City to Cruise Port" -> "city_to_cruise"
        "Airport to Cruise Port" -> "airport_to_cruise"
        "Cruise Port to City" -> "cruise_to_city"
        "Cruise Port to Airport" -> "cruise_to_airport"
        else -> "city_to_city"
    }
}

/**
 * Get special instructions based on transfer type (matches web app logic)
 * Web app logic:
 * - If transfer_type includes "city_": City instructions
 * - If transfer_type includes "cruise_": Cruise instructions
 * - If transfer_type includes "airport_": Airport instructions
 */
fun getSpecialInstructionsForTransferType(transferType: String): String {
    val webFormat = convertTransferTypeToWebFormat(transferType)
    
    return when {
        // City pickup - default instructions
        webFormat.startsWith("city_") -> 
            "1. Driver - Text on location. Text the client a day before to confirm driver name , cell phone and booking details. Text client with ETA when en route"
        
        // Cruise pickup - cruise instructions
        webFormat.startsWith("cruise_") -> 
            "1. Pax - Text driver when docked.  2. Driver - Text the client a day before to confirm driver name , cell phone and booking details. Text client with ETA when en route. Text pax with pickup instructions when ship has arrived."
        
        // Airport pickup - airport instructions
        webFormat.startsWith("airport_") -> 
            "1. Pax - Text driver when landing.  2. Driver - Text the client a day before to confirm driver name , cell phone and booking details. Text client with ETA when en route. Text pax with pickup instructions when plane has arrived."
        
        else -> 
            "1. Driver - Text on location. Text the client a day before to confirm driver name , cell phone and booking details. Text client with ETA when en route"
    }
}

/**
 * Get meet & greet choice based on transfer type (matches web app logic)
 * Web app logic:
 * - If transfer_type includes "city_": meet_greet_choices = 1, "Driver - Text/call when on location"
 * - Otherwise: meet_greet_choices = 2, "Driver - Airport - Text/call after plane lands with curbside meet location"
 */
fun getMeetAndGreetForTransferType(transferType: String): String {
    val webFormat = convertTransferTypeToWebFormat(transferType)
    
    return if (webFormat.startsWith("city_")) {
        "Driver - Text/call when on location"
    } else {
        "Driver -  Airport - Text/call after plane lands with curbside meet location"
    }
}

/**
 * Check if transfer type starts with city (for meet & greet logic)
 */
fun transferTypeStartsWithCity(transferType: String): Boolean {
    val webFormat = convertTransferTypeToWebFormat(transferType)
    return webFormat.startsWith("city_")
}

/**
 * Check if transfer type ends with city (for return trip meet & greet logic)
 */
fun transferTypeEndsWithCity(transferType: String): Boolean {
    val webFormat = convertTransferTypeToWebFormat(transferType)
    return webFormat.endsWith("_city")
}

