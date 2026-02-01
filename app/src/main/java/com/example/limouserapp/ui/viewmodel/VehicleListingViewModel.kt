package com.example.limouserapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.limouserapp.data.api.QuoteApi
import com.example.limouserapp.data.model.booking.RideData
import com.example.limouserapp.data.model.booking.VehicleListingRequest
import com.example.limouserapp.data.model.booking.LocationInfo
import com.example.limouserapp.data.model.booking.DistanceInfo
import com.example.limouserapp.data.model.booking.DurationInfo
import com.example.limouserapp.data.model.booking.OtherDetails
import com.example.limouserapp.data.model.booking.Vehicle
import com.example.limouserapp.data.service.DirectionsService
import com.example.limouserapp.data.service.FilterService
import com.example.limouserapp.ui.booking.components.FilterSelectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import com.example.limouserapp.ui.utils.DebugTags

/**
 * ViewModel for Vehicle Listing Screen
 * Calls /api/quote/vehicle-listing with selected master vehicle ID and filters
 */
@HiltViewModel
class VehicleListingViewModel @Inject constructor(
    private val quoteApi: QuoteApi,
    private val directionsService: DirectionsService,
    val filterService: FilterService
) : ViewModel() {
    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadVehicles(
        rideData: RideData,
        selectedMasterVehicleId: Int,
        filterSelection: FilterSelectionState
    ) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            Log.d(DebugTags.BookingProcess, "Requesting vehicle listing for master vehicle ID: $selectedMasterVehicleId")
            
            try {
                val isPickupAirport = rideData.pickupType.lowercase() == "airport"
                val isDropoffAirport = rideData.dropoffType.lowercase() == "airport"

                val locationInfo = buildLocationInfo(
                    serviceType = rideData.serviceType,
                    pickupLat = rideData.pickupLat,
                    pickupLong = rideData.pickupLong,
                    dropoffLat = rideData.destinationLat,
                    dropoffLong = rideData.destinationLong
                )

                // Convert filter selection to FiltersRequest
                // Always include the selected master vehicle ID in vehicle types (matches iOS logic)
                val updatedFilterSelection = filterSelection.copy(
                    selectedVehicleTypes = filterSelection.selectedVehicleTypes + selectedMasterVehicleId
                )
                val filtersRequest = updatedFilterSelection.toFiltersRequest()
                
                Log.d(DebugTags.BookingProcess, "Vehicle listing filters - Master Vehicle ID: $selectedMasterVehicleId, Vehicle Types: ${updatedFilterSelection.selectedVehicleTypes}")
                
                // Build request WITH FILTERS for vehicle listing API
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
                    dropoffAirportName = if (isDropoffAirport) rideData.selectedDestinationAirport.ifEmpty { null } else null,
                    dropoffAirportLat = if (isDropoffAirport) rideData.destinationLat else null,
                    dropoffAirportLong = if (isDropoffAirport) rideData.destinationLong else null,
                    dropoffAddress = if (!isDropoffAirport) rideData.destinationLocation else null,
                    dropoffAddressLat = if (!isDropoffAirport) rideData.destinationLat else null,
                    dropoffAddressLong = if (!isDropoffAirport) rideData.destinationLong else null,
                    noOfPassenger = rideData.noOfPassenger,
                    noOfLuggage = rideData.noOfLuggage,
                    locationInfo = locationInfo,
                    otherDetails = OtherDetails(
                        dropoffAirportName = if (isDropoffAirport) rideData.selectedDestinationAirport else null,
                        returnPickupAirportName = null
                    ),
                    filters = filtersRequest // Apply filters for vehicle listing API
                )

                Log.d(DebugTags.BookingProcess, "Calling vehicle listing API with filters: ${filtersRequest.hasAnyFilters()}")
                val response = quoteApi.getVehicleListing(req)
                
                if (response.success == true && response.data != null) {
                    // Sort vehicles by price in ascending order (matches user requirement)
                    val sortedVehicles = response.data.sortedBy { vehicle ->
                        val rateBreakdown = vehicle.getRateBreakdown(rideData.serviceType)
                        rateBreakdown?.grandTotal ?: rateBreakdown?.total ?: rateBreakdown?.subTotal ?: Double.MAX_VALUE
                    }
                    _vehicles.value = sortedVehicles
                    Log.d(DebugTags.BookingProcess, "Received ${response.data.size} vehicles, sorted by price ascending")
                } else {
                    _error.value = "Failed to load vehicles"
                    Log.e(DebugTags.BookingProcess, "Vehicle listing API returned success=false")
                }
            } catch (e: Exception) {
                Log.e(DebugTags.BookingProcess, "Error loading vehicles", e)
                _error.value = e.message ?: "An error occurred"
            } finally {
                _loading.value = false
            }
        }
    }

    private suspend fun buildLocationInfo(
        serviceType: String,
        pickupLat: Double?,
        pickupLong: Double?,
        dropoffLat: Double?,
        dropoffLong: Double?
    ): List<LocationInfo> {
        if (pickupLat == null || pickupLong == null || dropoffLat == null || dropoffLong == null) {
            return emptyList()
        }

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
        ) ?: Pair(0, 0)
        
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
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return (R * c).toInt()
    }
}

// Extension function to check if filters have any selection
private fun com.example.limouserapp.data.model.booking.FiltersRequest.hasAnyFilters(): Boolean {
    return vehicleType?.isNotEmpty() == true ||
            driverDresses?.isNotEmpty() == true ||
            driverLanguages?.isNotEmpty() == true ||
            driverGender?.isNotEmpty() == true ||
            amenities?.isNotEmpty() == true ||
            make?.isNotEmpty() == true ||
            model?.isNotEmpty() == true ||
            years?.isNotEmpty() == true ||
            colors?.isNotEmpty() == true ||
            interiors?.isNotEmpty() == true ||
            specialAmenities?.isNotEmpty() == true ||
            vehicleServiceArea?.isNotEmpty() == true ||
            affiliatePreferences?.isNotEmpty() == true
}

