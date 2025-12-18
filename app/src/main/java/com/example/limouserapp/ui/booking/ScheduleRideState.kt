package com.example.limouserapp.ui.booking

import com.example.limouserapp.data.PlacePrediction
import com.example.limouserapp.data.model.booking.BookingType
import com.example.limouserapp.data.model.booking.LocationCoordinate
import com.example.limouserapp.data.model.booking.RideType

/**
 * State data class for Schedule Ride Bottom Sheet
 * Consolidates all state variables to reduce parameter passing
 */
data class ScheduleRideState(
    val selectedRideType: RideType = RideType.ONE_WAY,
    val selectedBookingType: BookingType = BookingType.CITY_FBO,
    val selectedDestinationType: BookingType = BookingType.AIRPORT,
    val pickupLocation: String = "",
    val destinationLocation: String = "",
    val selectedPickupAirport: String = "",
    val selectedDestinationAirport: String = "",
    val pickupAirportSearch: String = "",
    val destinationAirportSearch: String = "",
    val pickupCoordinate: LocationCoordinate? = null,
    val destinationCoordinate: LocationCoordinate? = null,
    val showPickupSuggestions: Boolean = false,
    val showDestinationSuggestions: Boolean = false,
    val pickupPredictions: List<PlacePrediction> = emptyList(),
    val destinationPredictions: List<PlacePrediction> = emptyList(),
    val airportSuggestions: List<String> = emptyList(),
    val isLoadingAirports: Boolean = false,
    val focusedField: String? = null,
    val selectedHours: String = "2 hours minimum"
)

