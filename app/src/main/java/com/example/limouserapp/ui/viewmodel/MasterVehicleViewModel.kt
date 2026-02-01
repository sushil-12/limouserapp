package com.example.limouserapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.limouserapp.data.api.QuoteApi
import com.example.limouserapp.data.model.booking.RideData
import com.example.limouserapp.data.model.booking.Vehicle
import com.example.limouserapp.data.model.booking.VehicleListingRequest
import com.example.limouserapp.data.model.booking.LocationInfo
import com.example.limouserapp.data.model.booking.DistanceInfo
import com.example.limouserapp.data.model.booking.DurationInfo
import com.example.limouserapp.data.model.booking.OtherDetails
import com.example.limouserapp.data.service.AirportService
import com.example.limouserapp.data.service.DirectionsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import com.example.limouserapp.ui.utils.DebugTags
import com.google.gson.GsonBuilder

/**
 * ViewModel for Master Vehicle Selection Screen
 * Calls /api/quote/master-vehicle-listing (no filters)
 */
@HiltViewModel
class MasterVehicleViewModel @Inject constructor(
    private val quoteApi: QuoteApi,
    val filterService: com.example.limouserapp.data.service.FilterService,
    private val airportService: AirportService,
    private val directionsService: DirectionsService
) : ViewModel() {
    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Cache for storing loaded vehicles to avoid re-fetching when navigating back
    private var cachedVehicles: MutableMap<String, List<Vehicle>> = mutableMapOf()
    private var lastRideDataKey: String? = null

    fun loadMasterVehicles(rideData: RideData) {
        viewModelScope.launch {
            // Create a cache key based on key ride data parameters
            val cacheKey = createCacheKey(rideData)

            // Check if we have cached data for this ride data
            val cachedData = cachedVehicles[cacheKey]
            if (cachedData != null && cacheKey == lastRideDataKey) {
                Log.d(DebugTags.BookingProcess, "Using cached master vehicles data for ride: $rideData")
                _vehicles.value = cachedData
                _loading.value = false
                return@launch
            }

            _loading.value = true
            _error.value = null
            Log.d(DebugTags.BookingProcess, "Requesting master vehicle listing for ride: $rideData")
            
            try {
                val isPickupAirport = rideData.pickupType.lowercase() == "airport"
                val isDropoffAirport = rideData.dropoffType.lowercase() == "airport"
                val isRoundTrip = rideData.serviceType.lowercase() == "round_trip"
                val isCharterTour = rideData.serviceType.lowercase() == "charter_tour"

                // Get airport IDs from airport names
                val pickupAirportId = if (isPickupAirport && rideData.selectedPickupAirport.isNotEmpty()) {
                    airportService.getAirportByDisplayName(rideData.selectedPickupAirport)?.id
                } else null
                
                val dropoffAirportId = if (isDropoffAirport && rideData.selectedDestinationAirport.isNotEmpty()) {
                    airportService.getAirportByDisplayName(rideData.selectedDestinationAirport)?.id
                } else null

                // For return trip: return pickup is the original dropoff, return dropoff is the original pickup
                val returnPickupAirportId = if (isDropoffAirport && rideData.selectedDestinationAirport.isNotEmpty()) {
                    airportService.getAirportByDisplayName(rideData.selectedDestinationAirport)?.id
                } else null
                
                val returnDropoffAirportId = if (isPickupAirport && rideData.selectedPickupAirport.isNotEmpty()) {
                    airportService.getAirportByDisplayName(rideData.selectedPickupAirport)?.id
                } else null

                val locationInfo = buildLocationInfo(
                    serviceType = rideData.serviceType,
                    pickupLat = rideData.pickupLat,
                    pickupLong = rideData.pickupLong,
                    dropoffLat = rideData.destinationLat,
                    dropoffLong = rideData.destinationLong
                )

                // Return trip fields - always provide them (API requirement)
                val returnPickupDate = rideData.pickupDate // Same as pickup date for now
                val returnPickupTime =  rideData.pickupDate // Default return timse
                
                // Return pickup: from dropoff location
                val returnPickupAirportName = if (isDropoffAirport) rideData.selectedDestinationAirport else null
                val returnPickupAirportLat = if (isDropoffAirport) rideData.destinationLat else null
                val returnPickupAirportLong = if (isDropoffAirport) rideData.destinationLong else null
                val returnPickupAddress = if (!isDropoffAirport) rideData.destinationLocation else null
                val returnPickupAddressLat = if (!isDropoffAirport) rideData.destinationLat else 0.0
                val returnPickupAddressLong = if (!isDropoffAirport) rideData.destinationLong else 0.0
                
                // Return dropoff: to pickup location
                val returnDropoffAirportName = if (isPickupAirport) rideData.selectedPickupAirport else null
                val returnDropoffAirportLat = if (isPickupAirport) rideData.pickupLat else 0.0
                val returnDropoffAirportLong = if (isPickupAirport) rideData.pickupLong else 0.0
                val returnDropoffAddress = if (!isPickupAirport) rideData.pickupLocation else null
                val returnDropoffAddressLat = if (!isPickupAirport) rideData.pickupLat else 0.0
                val returnDropoffAddressLong = if (!isPickupAirport) rideData.pickupLong else 0.0

                // Build request - NO FILTERS for master vehicle API
                val req = VehicleListingRequest(
                    serviceType = rideData.serviceType,
                    bookingHour = rideData.bookingHour,
                    pickupType = rideData.pickupType,
                    dropoffType = rideData.dropoffType,
                    pickupDate = rideData.pickupDate,
                    pickupTime = rideData.pickupTime,
                    pickupAirport = pickupAirportId,
                    pickupAirportName = if (isPickupAirport) rideData.selectedPickupAirport.ifEmpty { null } else null,
                    pickupAirportLat = if (isPickupAirport) rideData.pickupLat else null,
                    pickupAirportLong = if (isPickupAirport) rideData.pickupLong else null,
                    pickupAddress = if (!isPickupAirport) rideData.pickupLocation else null,
                    pickupAddressLat = if (!isPickupAirport) rideData.pickupLat else null,
                    pickupAddressLong = if (!isPickupAirport) rideData.pickupLong else null,
                    dropoffAirport = dropoffAirportId,
                    dropoffAirportName = if (isDropoffAirport) rideData.selectedDestinationAirport.ifEmpty { null } else null,
                    dropoffAirportLat = if (isDropoffAirport) rideData.destinationLat else null,
                    dropoffAirportLong = if (isDropoffAirport) rideData.destinationLong else null,
                    dropoffAddress = if (!isDropoffAirport) rideData.destinationLocation else null,
                    dropoffAddressLat = if (!isDropoffAirport) rideData.destinationLat else null,
                    dropoffAddressLong = if (!isDropoffAirport) rideData.destinationLong else null,
                    returnPickupDate = returnPickupDate,
                    returnPickupTime = returnPickupTime,
                    returnPickupAirport = returnPickupAirportId,
                    returnPickupAirportName = returnPickupAirportName,
                    returnPickupAirportLat = returnPickupAirportLat,
                    returnPickupAirportLong = returnPickupAirportLong,
                    returnPickupAddress = returnPickupAddress,
                    returnPickupAddressLat = returnPickupAddressLat,
                    returnPickupAddressLong = returnPickupAddressLong,
                    returnDropoffAirport = returnDropoffAirportId,
                    returnDropoffAirportName = returnDropoffAirportName,
                    returnDropoffAirportLat = returnDropoffAirportLat,
                    returnDropoffAirportLong = returnDropoffAirportLong,
                    returnDropoffAddress = returnDropoffAddress,
                    returnDropoffAddressLat = returnDropoffAddressLat,
                    returnDropoffAddressLong = returnDropoffAddressLong,
                    noOfPassenger = rideData.noOfPassenger,
                    noOfLuggage = rideData.noOfLuggage,
                    locationInfo = locationInfo,
                    otherDetails = OtherDetails(
                        pickupAirportName = if (isPickupAirport) rideData.selectedPickupAirport else null,
                        dropoffAirportName = if (isDropoffAirport) rideData.selectedDestinationAirport else null,
                        returnPickupAirportName = if (isDropoffAirport) rideData.selectedDestinationAirport else null,
                        returnDropoffAirportName = if (isPickupAirport) rideData.selectedPickupAirport else null
                    ),
                    filters = com.example.limouserapp.data.model.booking.FiltersRequest() // Empty filters for master API
                )

                // Log the complete payload as JSON
                val gsonPretty = GsonBuilder().setPrettyPrinting().create()
                val jsonPayload = gsonPretty.toJson(req)
                Log.d(DebugTags.BookingProcess, "===========================================")
                Log.d(DebugTags.BookingProcess, "MASTER VEHICLE LISTING REQUEST PAYLOAD:")
                Log.d(DebugTags.BookingProcess, "===========================================")
                Log.d(DebugTags.BookingProcess, jsonPayload)
                Log.d(DebugTags.BookingProcess, "===========================================")

                Log.d(DebugTags.BookingProcess, "Calling master vehicle listing API (NO FILTERS)")
                val response = quoteApi.getMasterVehicleListing(req)
                
                if (response.success == true && response.data != null) {
                    // Convert VehicleActual to Vehicle with price extracted from rate breakdowns
                    val vehicles = response.data.map { vehicleActual ->
                        vehicleActual.toVehicle(rideData.serviceType)
                    }
                    _vehicles.value = vehicles

                    // Cache the loaded vehicles
                    cachedVehicles[cacheKey] = vehicles
                    lastRideDataKey = cacheKey

                    Log.d(DebugTags.BookingProcess, "Received ${vehicles.size} master vehicles for service type: ${rideData.serviceType} (cached)")
                } else {
                    _error.value = "Failed to load vehicles"
                    Log.e(DebugTags.BookingProcess, "Master vehicle API returned success=false")
                }
            } catch (e: Exception) {
                Log.e(DebugTags.BookingProcess, "Error loading master vehicles", e)
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

        val isRoundTrip = serviceType.lowercase() == "round_trip"
        
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
        val R = 6371000.0 // meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return (R * c).toInt()
    }

    /**
     * Creates a cache key based on ride data parameters that affect vehicle results
     */
    private fun createCacheKey(rideData: RideData): String {
        return "${rideData.serviceType}_${rideData.pickupType}_${rideData.dropoffType}_${rideData.bookingHour}_${rideData.noOfPassenger}_${rideData.noOfLuggage}_${rideData.selectedPickupAirport}_${rideData.selectedDestinationAirport}"
    }

    /**
     * Clears the cache (useful for testing or when data becomes stale)
     */
    fun clearCache() {
        cachedVehicles.clear()
        lastRideDataKey = null
        Log.d(DebugTags.BookingProcess, "Master vehicle cache cleared")
    }
}

