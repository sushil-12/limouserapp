package com.example.limouserapp.data.model.booking

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Location coordinate model
 */
@Parcelize
data class LocationCoordinate(
    val latitude: Double,
    val longitude: Double,
    val countryCode: String? = null,
    val postalCode: String? = null
) : Parcelable

/**
 * Ride type enum
 */
enum class RideType(val displayName: String) {
    ONE_WAY("One Way"),
    ROUND_TRIP("Round Trip"),
    HOURLY("Hourly");
    
    fun toServiceType(): String {
        return when (this) {
            ONE_WAY -> "one_way"
            ROUND_TRIP -> "round_trip"
            HOURLY -> "charter_tour"
        }
    }
    
    companion object {
        val allCases = entries.toList()
        
        fun fromServiceType(serviceType: String): RideType {
            return when (serviceType.lowercase()) {
                "one_way" -> ONE_WAY
                "round_trip" -> ROUND_TRIP
                "charter_tour" -> HOURLY
                else -> ONE_WAY
            }
        }
    }
}

/**
 * Booking type options
 */
enum class BookingType(val displayName: String) {
    CITY_FBO("City/FBO Address"),
    AIRPORT("Airport"),
    CRUISE_PORT("Cruise Port");
    
    fun toPickupType(): String {
        return when (this) {
            CITY_FBO -> "city"
            AIRPORT -> "airport"
            CRUISE_PORT -> "cruise port"
        }
    }
    
    companion object {
        fun fromString(value: String): BookingType {
            return values().find { it.displayName == value } ?: CITY_FBO
        }
        
        fun fromPickupType(pickupType: String): BookingType {
            return when (pickupType.lowercase()) {
                "city" -> CITY_FBO
                "airport" -> AIRPORT
                "cruise", "cruise port" -> CRUISE_PORT
                else -> CITY_FBO
            }
        }
    }
}

/**
 * Ride data model for API requests
 */
data class RideData(
    val serviceType: String,
    val bookingHour: String,
    val pickupType: String,
    val dropoffType: String,
    val pickupDate: String,
    val pickupTime: String,
    val pickupLocation: String,
    val destinationLocation: String,
    val selectedPickupAirport: String,
    val selectedDestinationAirport: String,
    val noOfPassenger: Int,
    val noOfLuggage: Int,
    val noOfVehicles: Int,
    val pickupLat: Double?,
    val pickupLong: Double?,
    val destinationLat: Double?,
    val destinationLong: Double?,
    val pickupCountryCode: String? = null,
    val pickupPostalCode: String? = null,
    val destinationCountryCode: String? = null,
    val destinationPostalCode: String? = null,
    // Airline and flight fields
    val selectedPickupAirline: String? = null,
    val selectedDestinationAirline: String? = null,
    val pickupFlightNumber: String? = null,
    val dropoffFlightNumber: String? = null,
    val returnPickupFlightNumber: String? = null,
    val returnDropoffFlightNumber: String? = null,
    val originAirportCity: String? = null,
    // Return trip fields (for round trip)
    val returnPickupDate: String? = null,
    val returnPickupTime: String? = null,
    // Distance and duration from Google Maps Directions API (calculated once, reused)
    val distanceMeters: Double? = null,
    val durationSeconds: Int? = null
)

