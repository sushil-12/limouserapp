package com.example.limouserapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.limouserapp.data.model.booking.RideData
import com.example.limouserapp.data.model.booking.Vehicle
import com.example.limouserapp.data.model.booking.VehicleListingRequest
import com.example.limouserapp.data.model.booking.LocationInfo
import com.example.limouserapp.data.model.booking.DistanceInfo
import com.example.limouserapp.data.model.booking.DurationInfo
import com.example.limouserapp.data.model.booking.OtherDetails
import com.example.limouserapp.data.service.QuoteService
import com.example.limouserapp.data.service.DirectionsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import com.example.limouserapp.ui.utils.DebugTags

@HiltViewModel
class VehicleSelectionViewModel @Inject constructor(
    private val quoteService: QuoteService,
    private val directionsService: DirectionsService
) : ViewModel() {
    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun loadVehicles(rideData: RideData) {
        viewModelScope.launch {
            _loading.value = true
            Log.d(DebugTags.BookingProcess, "Requesting vehicle quotes for ride: $rideData")
            val isPickupAirport = rideData.pickupType == "airport"
            val isDropoffAirport = rideData.dropoffType == "airport"

            val locationInfo = buildLocationInfo(
                serviceType = rideData.serviceType,
                pickupLat = rideData.pickupLat,
                pickupLong = rideData.pickupLong,
                dropoffLat = rideData.destinationLat,
                dropoffLong = rideData.destinationLong
            )

            val req = VehicleListingRequest(
                serviceType = rideData.serviceType,
                bookingHour = rideData.bookingHour,
                pickupType = rideData.pickupType,
                dropoffType = rideData.dropoffType,
                pickupDate = rideData.pickupDate,
                pickupTime = rideData.pickupTime,
                pickupAirport = null,
                pickupAirportName = if (isPickupAirport) rideData.selectedPickupAirport.ifEmpty { null } else null,
                pickupAirportLat = if (isPickupAirport) rideData.pickupLat else null,
                pickupAirportLong = if (isPickupAirport) rideData.pickupLong else null,
                pickupAddress = if (!isPickupAirport) rideData.pickupLocation else null,
                pickupAddressLat = if (!isPickupAirport) rideData.pickupLat else null,
                pickupAddressLong = if (!isPickupAirport) rideData.pickupLong else null,
                dropoffAirport = null,
                dropoffAirportName = if (isDropoffAirport) (rideData.selectedDestinationAirport.ifEmpty { "" }) else "",
                dropoffAirportLat = if (isDropoffAirport) rideData.destinationLat else null,
                dropoffAirportLong = if (isDropoffAirport) rideData.destinationLong else null,
                dropoffAddress = if (!isDropoffAirport) rideData.destinationLocation else null,
                dropoffAddressLat = if (!isDropoffAirport) rideData.destinationLat else null,
                dropoffAddressLong = if (!isDropoffAirport) rideData.destinationLong else null,
                noOfPassenger = rideData.noOfPassenger,
                noOfLuggage = rideData.noOfLuggage,
                locationInfo = locationInfo,
                otherDetails = OtherDetails(
                    dropoffAirportName = "",
                    returnPickupAirportName = ""
                )
            )

            // Log the exact JSON we will send
            try {
                val gson = com.google.gson.Gson()
                Log.d(DebugTags.BookingProcess, "VehicleListingRequest JSON: ${gson.toJson(req)}")
            } catch (_: Exception) {}
            val vehicles = quoteService.fetchVehicles(req)
            Log.d(DebugTags.BookingProcess, "Received ${vehicles.size} vehicles for selection")
            _vehicles.value = vehicles
            _loading.value = false
        }
    }

    private suspend fun buildLocationInfo(
        serviceType: String,
        pickupLat: Double?,
        pickupLong: Double?,
        dropoffLat: Double?,
        dropoffLong: Double?
    ): List<LocationInfo> {
        if (pickupLat == null || pickupLong == null || dropoffLat == null || dropoffLong == null) return emptyList()

        val isRoundTrip = serviceType == "round_trip"
        
        // Create outbound location info (pickup to dropoff)
        val outboundLocationInfo = createSingleLocationInfo(
            pickupLat, pickupLong, dropoffLat, dropoffLong
        )
        
        // For round trips, also create return location info (dropoff to pickup - reversed)
        if (isRoundTrip) {
            val returnLocationInfo = createSingleLocationInfo(
                dropoffLat, dropoffLong, pickupLat, pickupLong
            )
            return outboundLocationInfo + returnLocationInfo
        }
        
        return outboundLocationInfo
    }
    
    private suspend fun createSingleLocationInfo(
        pickupLat: Double,
        pickupLong: Double,
        dropoffLat: Double,
        dropoffLong: Double
    ): List<LocationInfo> {
        // Use Google Maps Directions API for accurate road distance and duration
        val (distanceMeters, durationSeconds) = directionsService.calculateDistance(
            pickupLat, pickupLong, dropoffLat, dropoffLong
        )
        
        // Format distance and duration
        val (distanceText, _) = directionsService.formatDistance(distanceMeters)
        val (durationText, _) = directionsService.formatDuration(durationSeconds)

        return listOf(
            LocationInfo(
                type = null,
                address = null,
                lat = null,
                long = null,
                distance = DistanceInfo(text = distanceText, value = distanceMeters),
                duration = DurationInfo(text = durationText, value = durationSeconds)
            )
        )
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val R = 6371000.0 // meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return (R * c).toInt()
    }
}



