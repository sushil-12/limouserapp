package com.example.limouserapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.limouserapp.data.api.DashboardApi
import com.example.limouserapp.data.model.booking.*
import com.example.limouserapp.data.model.booking.CreateReservationRequest
import com.example.limouserapp.data.model.booking.CreateReservationResponse
import com.example.limouserapp.data.model.booking.RideData
import com.example.limouserapp.data.model.booking.Vehicle
import com.example.limouserapp.data.model.booking.AirportOption
import com.example.limouserapp.data.model.booking.AirlineOption
import com.example.limouserapp.data.service.BookingService
import com.example.limouserapp.data.service.BookingRatesService
import com.example.limouserapp.data.service.EditReservationService
import com.example.limouserapp.data.service.DirectionsService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import com.example.limouserapp.ui.utils.DebugTags
import com.google.gson.GsonBuilder
import com.example.limouserapp.ui.booking.comprehensivebooking.ExtraStop

/**
 * Service type enum to replace hardcoded strings
 */
enum class ServiceType(val value: String, val displayName: String) {
    ONE_WAY("one_way", "One Way"),
    ROUND_TRIP("round_trip", "Round Trip"),
    CHARTER_TOUR("charter_tour", "Charter Tour");

    companion object {
        fun fromValue(value: String): ServiceType {
            return entries.find { it.value == value } ?: ONE_WAY
        }

        fun fromDisplayName(displayName: String): ServiceType {
            return entries.find { it.displayName == displayName } ?: ONE_WAY
        }
    }
}

/**
 * Transfer type enum to replace hardcoded strings
 */
enum class TransferType(val pickupType: String, val dropoffType: String, val displayName: String) {
    CITY_TO_CITY("city", "city", "City to City"),
    CITY_TO_AIRPORT("city", "airport", "City to Airport"),
    AIRPORT_TO_CITY("airport", "city", "Airport to City"),
    AIRPORT_TO_AIRPORT("airport", "airport", "Airport to Airport"),
    CITY_TO_CRUISE_PORT("city", "cruise", "City to Cruise Port"),
    AIRPORT_TO_CRUISE_PORT("airport", "cruise", "Airport to Cruise Port"),
    CRUISE_PORT_TO_CITY("cruise", "city", "Cruise Port to City"),
    CRUISE_PORT_TO_AIRPORT("cruise", "airport", "Cruise Port to Airport"),
    CRUISE_PORT_TO_CRUISE_PORT("cruise", "cruise", "Cruise Port to Cruise Port");

    companion object {
        fun fromTypes(pickupType: String, dropoffType: String): TransferType {
            // Normalize types - handle "cruise port" -> "cruise"
            val normalizedPickup = pickupType.lowercase().replace("cruise port", "cruise").replace("cruise_port", "cruise").trim()
            val normalizedDropoff = dropoffType.lowercase().replace("cruise port", "cruise").replace("cruise_port", "cruise").trim()
            return entries.find { it.pickupType == normalizedPickup && it.dropoffType == normalizedDropoff } ?: CITY_TO_CITY
        }

        fun fromDisplayName(displayName: String): TransferType {
            return entries.find { it.displayName == displayName } ?: CITY_TO_CITY
        }
    }
}

/**
 * UI state holder for booking screen - replaces local state management
 */
data class BookingUiState(
    val rideData: RideData = RideData(
        serviceType = "one_way",
        bookingHour = "2 hours minimum",
        pickupType = "city",
        dropoffType = "city",
        pickupDate = "",
        pickupTime = "",
        pickupLocation = "",
        destinationLocation = "",
        selectedPickupAirport = "",
        selectedDestinationAirport = "",
        noOfPassenger = 1,
        noOfLuggage = 0,
        noOfVehicles = 1,
        pickupLat = null,
        pickupLong = null,
        destinationLat = null,
        destinationLong = null
    ),
    val vehicle: Vehicle? = null,
    val pickupDate: String = "",
    val pickupTime: String = "",
    val serviceType: ServiceType = ServiceType.ONE_WAY,
    val transferType: TransferType = TransferType.CITY_TO_CITY,
    val pickupLocation: String = "",
    val dropoffLocation: String = "",
    val passengerCount: String = "1",
    val luggageCount: String = "0",
    val extraStops: List<ExtraStop> = emptyList(),
    val profileData: com.example.limouserapp.data.model.dashboard.ProfileData? = null,
    val isLoading: Boolean = false,
    val validationErrors: List<String> = emptyList(),
    // Address validation errors (for Directions API validation)
    val addressValidationErrors: Map<String, String> = emptyMap(), // Key: "pickup_location" or "dropoff_location", Value: error message
    // Passenger information fields (editable, matches web app)
    val passengerName: String = "",
    val passengerEmail: String = "",
    val passengerMobile: String = "",
    // Cruise fields (for validation)
    val cruisePort: String = "",
    val cruiseShipName: String = "",
    val shipArrivalTime: String = "",
    val dropoffCruisePort: String = "",
    val dropoffCruiseShipName: String = "",
    val dropoffShipArrivalTime: String = "",
    // Return trip fields (for validation when service type is round trip)
    val returnPickupDate: String = "",
    val returnPickupTime: String = "",
    val returnPickupLocation: String = "",
    val returnDropoffLocation: String = "",
    val returnPickupLat: Double? = null,
    val returnPickupLong: Double? = null,
    val returnDropoffLat: Double? = null,
    val returnDropoffLong: Double? = null,
    val returnTransferType: TransferType? = null,
    val returnPickupAirport: String = "",
    val returnDropoffAirport: String = "",
    val returnPickupAirline: String = "",
    val returnDropoffAirline: String = "",
    val returnPickupFlightNumber: String = "",
    val returnOriginAirportCity: String = "",
    val returnCruisePort: String = "",
    val returnCruiseShipName: String = "",
    val returnShipArrivalTime: String = ""
) {
    val isFormValid: Boolean
        get() = validationErrors.isEmpty()
}

@HiltViewModel
class ReservationViewModel @Inject constructor(
    private val bookingService: BookingService,
    val dashboardApi: DashboardApi,
    private val bookingRatesService: BookingRatesService,
    private val editReservationService: EditReservationService,
    val airlineService: com.example.limouserapp.data.service.AirlineService,
    val airportService: com.example.limouserapp.data.service.AirportService,
    val directionsService: DirectionsService,
    val meetGreetService: com.example.limouserapp.data.service.MeetGreetService
) : ViewModel() {
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _result = MutableStateFlow<Result<CreateReservationResponse>?>(null)
    val result: StateFlow<Result<CreateReservationResponse>?> = _result
    
    // Store booking rates data (matches iOS bookingRatesService.ratesData)
    private val _bookingRatesData = MutableStateFlow<BookingRatesData?>(null)
    val bookingRatesData: StateFlow<BookingRatesData?> = _bookingRatesData
    
    // Store currency from booking rates response (matches iOS bookingRatesService.currency)
    private val _bookingRatesCurrency = MutableStateFlow<BookingRatesCurrency?>(null)
    val bookingRatesCurrency: StateFlow<BookingRatesCurrency?> = _bookingRatesCurrency
    
    private val _bookingRatesLoading = MutableStateFlow(false)
    val bookingRatesLoading: StateFlow<Boolean> = _bookingRatesLoading
    
    // Edit reservation data (matches iOS editReservationService.editData)
    private val _editData = MutableStateFlow<EditReservationData?>(null)
    val editData: StateFlow<EditReservationData?> = _editData
    
    private val _editLoading = MutableStateFlow(false)
    val editLoading: StateFlow<Boolean> = _editLoading
    
    private val _editError = MutableStateFlow<String?>(null)
    val editError: StateFlow<String?> = _editError
    
    // Update result for edit operations
    private val _updateResult = MutableStateFlow<Result<EditReservationUpdateResponse>?>(null)
    val updateResult: StateFlow<Result<EditReservationUpdateResponse>?> = _updateResult

    // UI State for comprehensive booking screen - unidirectional data flow
    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState

    /**
     * Fetch booking rates - matches iOS fetchBookingRates()
     * Should be called when:
     * - Screen loads
     * - Vehicle is selected
     * - Locations change
     * - Service type changes
     * - Number of vehicles changes
     * 
     * @param ride RideData with location and service information
     * @param vehicle Selected vehicle
     * @param isEditMode Whether we're in edit mode
     * @param editBookingId Booking ID if in edit mode
     * @param hasExtraStops Whether there are extra stops (defaults to false)
     */
    // Track last API call to prevent duplicate calls
    private var lastFetchBookingRatesCall: Long = 0
    private val DEBOUNCE_DELAY_MS = 300L // 300ms debounce delay
    
    // Track the serviceType of the currently loading call to detect changes
    private var currentLoadingServiceType: String? = null
    
    fun fetchBookingRates(
        ride: RideData, 
        vehicle: Vehicle,
        isEditMode: Boolean = false,
        editBookingId: Int? = null,
        hasExtraStops: Boolean = false,
        extraStops: List<com.example.limouserapp.data.model.booking.ExtraStopRequest> = emptyList(),
        returnExtraStops: List<com.example.limouserapp.data.model.booking.ExtraStopRequest> = emptyList(),
        returnPickupLat: Double? = null,
        returnPickupLong: Double? = null,
        returnDropoffLat: Double? = null,
        returnDropoffLong: Double? = null
    ) {
        viewModelScope.launch {
            // ==========================================
            // ENHANCED LOGGING: Trace serviceType at entry
            // ==========================================
            Log.d(DebugTags.BookingProcess, "===========================================")
            Log.d(DebugTags.BookingProcess, "🚀 fetchBookingRates CALLED - ENTRY POINT")
            Log.d(DebugTags.BookingProcess, "===========================================")
            Log.d(DebugTags.BookingProcess, "📥 PARAMETER ride.serviceType='${ride.serviceType}'")
            Log.d(DebugTags.BookingProcess, "📥 PARAMETER ride.bookingHour='${ride.bookingHour}'")
            Log.d(DebugTags.BookingProcess, "📥 PARAMETER ride.pickupLocation='${ride.pickupLocation}'")
            Log.d(DebugTags.BookingProcess, "📥 PARAMETER ride.pickupTime='${ride.pickupTime}'")
            Log.d(DebugTags.BookingProcess, "===========================================")
            
            // Determine the serviceType for this call
            val incomingServiceType = if (ride.serviceType in listOf("one_way", "round_trip", "charter_tour")) {
                ride.serviceType
            } else {
                ReservationRequestBuilder.mapServiceType(ride.serviceType)
            }
            
            // CRITICAL FIX: Check if already loading, but allow override if serviceType changed
            if (_bookingRatesLoading.value) {
                val currentLoadingType = currentLoadingServiceType
                if (currentLoadingType != null && currentLoadingType == incomingServiceType) {
                    // Same serviceType is already loading - skip this call
                    Log.d(DebugTags.BookingProcess, "⏭️ Skipping fetchBookingRates call - already loading")
                    Log.d(DebugTags.BookingProcess, "⚠️ WARNING: ServiceType='${ride.serviceType}' was passed but call was skipped (already loading same serviceType='$currentLoadingType')!")
                    return@launch
                } else {
                    // Different serviceType - this is important! Allow override
                    Log.d(DebugTags.BookingProcess, "===========================================")
                    Log.d(DebugTags.BookingProcess, "🔄 SERVICE TYPE CHANGED - OVERRIDING PREVIOUS CALL")
                    Log.d(DebugTags.BookingProcess, "===========================================")
                    Log.d(DebugTags.BookingProcess, "⚠️ Previous call serviceType: '$currentLoadingType'")
                    Log.d(DebugTags.BookingProcess, "⚠️ New call serviceType: '$incomingServiceType'")
                    Log.d(DebugTags.BookingProcess, "✅ Allowing new call to proceed (serviceType changed)")
                    Log.d(DebugTags.BookingProcess, "===========================================")
                    // Continue - we'll override the previous call
                }
            }
            
            // Debounce: Skip if called within DEBOUNCE_DELAY_MS (but only if same serviceType)
            val currentTime = System.currentTimeMillis()
            val timeSinceLastCall = currentTime - lastFetchBookingRatesCall
            if (timeSinceLastCall < DEBOUNCE_DELAY_MS && currentLoadingServiceType == incomingServiceType) {
                Log.d(DebugTags.BookingProcess, "===========================================")
                Log.d(DebugTags.BookingProcess, "⏭️ CALL DEBOUNCED - SKIPPING")
                Log.d(DebugTags.BookingProcess, "===========================================")
                Log.d(DebugTags.BookingProcess, "⚠️ WARNING: ServiceType='${ride.serviceType}' was passed but call was debounced!")
                Log.d(DebugTags.BookingProcess, "⚠️ Time since last call: ${timeSinceLastCall}ms (debounce threshold: ${DEBOUNCE_DELAY_MS}ms)")
                Log.d(DebugTags.BookingProcess, "⚠️ This call with serviceType='${ride.serviceType}' is being skipped!")
                Log.d(DebugTags.BookingProcess, "===========================================")
                return@launch
            }
            lastFetchBookingRatesCall = currentTime
            Log.d(DebugTags.BookingProcess, "✅ Call NOT debounced - proceeding with serviceType='${ride.serviceType}'")
            
            // Update tracking
            currentLoadingServiceType = incomingServiceType
            
            _bookingRatesLoading.value = true
            Log.d(DebugTags.BookingProcess, "===========================================")
            Log.d(DebugTags.BookingProcess, "🔄 FETCHING BOOKING RATES")
            Log.d(DebugTags.BookingProcess, "===========================================")
            Log.d(DebugTags.BookingProcess, "Edit Mode: $isEditMode")
            Log.d(DebugTags.BookingProcess, "Ride Data: serviceType=${ride.serviceType}, pickup=${ride.pickupLocation}, bookingHour=${ride.bookingHour}, pickupTime=${ride.pickupTime}")
            Log.d(DebugTags.BookingProcess, "Vehicle ID: ${vehicle.id}, Name: ${vehicle.name}")
            
            try {
                // ==========================================
                // ENHANCED LOGGING: Trace serviceType mapping
                // ==========================================
                Log.d(DebugTags.BookingProcess, "===========================================")
                Log.d(DebugTags.BookingProcess, "🔍 SERVICE TYPE MAPPING SECTION")
                Log.d(DebugTags.BookingProcess, "===========================================")
                Log.d(DebugTags.BookingProcess, "📥 INPUT: ride.serviceType='${ride.serviceType}'")
                Log.d(DebugTags.BookingProcess, "📥 INPUT: ride.bookingHour='${ride.bookingHour}'")
                
                // Map service type and transfer type
                // CRITICAL: Use ride.serviceType directly (it's already mapped in updatedRideData)
                // Log the service type to debug why it might be wrong
                Log.d(DebugTags.BookingProcess, "🔍 Mapping service type - ride.serviceType='${ride.serviceType}', ride.bookingHour='${ride.bookingHour}'")
                
                // CRITICAL: Don't remap if it's already correctly set - use ride.serviceType directly
                // Only map if it's in a different format (e.g., "oneway" -> "one_way")
                val serviceType = if (ride.serviceType in listOf("one_way", "round_trip", "charter_tour")) {
                    // Already in correct format, use directly
                    Log.d(DebugTags.BookingProcess, "✅ Service type already in correct format, using directly: '${ride.serviceType}'")
                    ride.serviceType
                } else {
                    // Need to map from other format
                    val mapped = ReservationRequestBuilder.mapServiceType(ride.serviceType)
                    Log.d(DebugTags.BookingProcess, "🔄 Mapped service type - input='${ride.serviceType}' -> output='$mapped'")
                    mapped
                }
                
                Log.d(DebugTags.BookingProcess, "📤 OUTPUT: Final service type for request: '$serviceType'")
                Log.d(DebugTags.BookingProcess, "===========================================")
                
                // CRITICAL: If serviceType is still "one_way" but ride.serviceType was "charter_tour", log error
                if (ride.serviceType == "charter_tour" && serviceType != "charter_tour") {
                    Log.e(DebugTags.BookingProcess, "===========================================")
                    Log.e(DebugTags.BookingProcess, "❌ CRITICAL ERROR: Service type mapping failed!")
                    Log.e(DebugTags.BookingProcess, "❌ ride.serviceType='${ride.serviceType}' but mapped serviceType='$serviceType'")
                    Log.e(DebugTags.BookingProcess, "❌ Expected: 'charter_tour', Got: '$serviceType'")
                    Log.e(DebugTags.BookingProcess, "===========================================")
                }
                
                // CRITICAL: If ride.serviceType is "charter_tour" but serviceType is "one_way", log error
                if (ride.serviceType == "charter_tour" && serviceType == "one_way") {
                    Log.e(DebugTags.BookingProcess, "===========================================")
                    Log.e(DebugTags.BookingProcess, "❌ CRITICAL ERROR: Service type was 'charter_tour' but became 'one_way'!")
                    Log.e(DebugTags.BookingProcess, "❌ ride.serviceType='${ride.serviceType}'")
                    Log.e(DebugTags.BookingProcess, "❌ serviceType variable='$serviceType'")
                    Log.e(DebugTags.BookingProcess, "❌ This should NEVER happen!")
                    Log.e(DebugTags.BookingProcess, "===========================================")
                }
                val transferType = ReservationRequestBuilder.mapTransferType(ride.pickupType, ride.dropoffType)
                
                // iOS Logic: In edit mode, check if we need to recalculate rates
                // Use reservation-rates API when: edit mode + one-way + no extra stops
                // Use booking-rates-vehicle API when: normal mode OR edit mode with round trip/extra stops
                val isEditModeRoundTrip = isEditMode && serviceType == "round_trip"
                val needsRecalculation = isEditModeRoundTrip || hasExtraStops
                
                if (isEditMode && !needsRecalculation && editBookingId != null) {
                    // In edit mode for one-way WITHOUT extra stops, use the reservation rates API (fetch existing rates)
                    Log.d(DebugTags.BookingProcess, "🔄 EDIT MODE (One Way, No Extra Stops): Using reservation-rates API for booking ID: $editBookingId")
                    val reservationRatesResult = bookingRatesService.fetchReservationRates(editBookingId)
                    
                    if (reservationRatesResult.isSuccess) {
                        val data = reservationRatesResult.getOrNull()?.data
                        _bookingRatesData.value = data
                        Log.d(DebugTags.BookingProcess, "✅ Reservation rates fetched successfully")
                        Log.d(DebugTags.BookingProcess, "  API Sub Total: ${data?.subTotal}")
                        Log.d(DebugTags.BookingProcess, "  API Grand Total: ${data?.grandTotal}")
                    } else {
                        Log.e(DebugTags.BookingProcess, "❌ Failed to fetch reservation rates: ${reservationRatesResult.exceptionOrNull()?.message}")
                        _bookingRatesData.value = null
                    }
                } else {
                    // Normal mode (create) OR edit mode with round trip/extra stops: use the regular booking rates API to recalculate
                    if (isEditMode) {
                        if (isEditModeRoundTrip) {
                            Log.d(DebugTags.BookingProcess, "🔄 EDIT MODE (Round Trip): Recalculating rates with booking-rates-vehicle API")
                        } else if (hasExtraStops) {
                            Log.d(DebugTags.BookingProcess, "🔄 EDIT MODE (One Way with Extra Stops): Recalculating rates with booking-rates-vehicle API")
                        }
                    } else {
                        Log.d(DebugTags.BookingProcess, "🔄 NORMAL MODE (CREATE): Using booking-rates-vehicle API")
                    }
                    
                    // ALWAYS recalculate distance before hitting API (matches iOS line 2398)
                    Log.d(DebugTags.BookingProcess, "📏 RECALCULATING DISTANCE BEFORE API CALL...")
                    Log.d(DebugTags.BookingProcess, "📍 RIDE COORDINATES CHECK:")
                    Log.d(DebugTags.BookingProcess, "   Pickup: Lat=${ride.pickupLat}, Long=${ride.pickupLong}, Location=${ride.pickupLocation}")
                    Log.d(DebugTags.BookingProcess, "   Dropoff: Lat=${ride.destinationLat}, Long=${ride.destinationLong}, Location=${ride.destinationLocation}")
                    
                    // Extract waypoints from extra stops (matches iOS calculateDistanceWithExtraStops)
                    val waypoints = extraStops
                        .filter { it.latitude != null && it.longitude != null }
                        .map { Pair(it.latitude!!, it.longitude!!) }
                        .takeIf { it.isNotEmpty() }
                    
                    Log.d(DebugTags.BookingProcess, "📍 Waypoints: ${waypoints?.size ?: 0} stops")
                    
                    val (distance, duration) = if (ride.pickupLat != null && ride.pickupLong != null && 
                        ride.destinationLat != null && ride.destinationLong != null) {
                        Log.d(DebugTags.BookingProcess, "✅ Coordinates available - calculating distance...")
                        val result = directionsService.calculateDistance(
                            ride.pickupLat, ride.pickupLong, 
                            ride.destinationLat, ride.destinationLong,
                            waypoints
                        )
                        Log.d(DebugTags.BookingProcess, "📏 RECALCULATED DISTANCE: ${result.first} meters (with ${waypoints?.size ?: 0} waypoints)")
                        Log.d(DebugTags.BookingProcess, "⏱️ RECALCULATED DURATION: ${result.second} seconds")
                        result
                    } else {
                        Log.w(DebugTags.BookingProcess, "⚠️ Missing coordinates - cannot calculate distance")
                        Log.w(DebugTags.BookingProcess, "   Pickup Lat: ${ride.pickupLat}, Long: ${ride.pickupLong}")
                        Log.w(DebugTags.BookingProcess, "   Dropoff Lat: ${ride.destinationLat}, Long: ${ride.destinationLong}")
                        Pair(0, 0)
                    }
                    
                    // CRITICAL FIX: For round trips, ALSO recalculate return distance (matches iOS line 2402-2406)
                    // Extract waypoints from return extra stops
                    val returnWaypoints = returnExtraStops
                        .filter { it.latitude != null && it.longitude != null }
                        .map { Pair(it.latitude!!, it.longitude!!) }
                        .takeIf { it.isNotEmpty() }
                    
                    // Use return trip coordinates if provided, otherwise fall back to reversed outbound coordinates
                    val returnPickupLatToUse = returnPickupLat ?: ride.destinationLat
                    val returnPickupLongToUse = returnPickupLong ?: ride.destinationLong
                    val returnDropoffLatToUse = returnDropoffLat ?: ride.pickupLat
                    val returnDropoffLongToUse = returnDropoffLong ?: ride.pickupLong
                    
                    val (returnDistance, returnDuration) = if (serviceType == "round_trip" && 
                        returnPickupLatToUse != null && returnPickupLongToUse != null &&
                        returnDropoffLatToUse != null && returnDropoffLongToUse != null) {
                        Log.d(DebugTags.BookingProcess, "🔄 RECALCULATING RETURN DISTANCE AS WELL")
                        Log.d(DebugTags.BookingProcess, "📍 Return Pickup: Lat=$returnPickupLatToUse, Long=$returnPickupLongToUse")
                        Log.d(DebugTags.BookingProcess, "📍 Return Dropoff: Lat=$returnDropoffLatToUse, Long=$returnDropoffLongToUse")
                        Log.d(DebugTags.BookingProcess, "📍 Return Waypoints: ${returnWaypoints?.size ?: 0}")
                        val result = directionsService.calculateDistance(
                            returnPickupLatToUse, returnPickupLongToUse,
                            returnDropoffLatToUse, returnDropoffLongToUse,
                            returnWaypoints
                        )
                        Log.d(DebugTags.BookingProcess, "📏 RECALCULATED RETURN DISTANCE: ${result.first} meters (with ${returnWaypoints?.size ?: 0} waypoints)")
                        Log.d(DebugTags.BookingProcess, "⏱️ RECALCULATED RETURN DURATION: ${result.second} seconds")
                        result
                    } else {
                        Log.w(DebugTags.BookingProcess, "⚠️ Cannot calculate return distance - missing coordinates")
                        Pair(0, 0)
                    }
                    
                    // Validate distance before API call
                    if (distance <= 0) {
                        Log.w(DebugTags.BookingProcess, "⚠️ WARNING: Distance is 0 or invalid. Rates may be incorrect.")
                    }
                    
                    // Build booking rates request
                    // CRITICAL: Clean up noOfHours - remove " hours minimum" and " hours" suffixes for charter tour
                    // Also handle the case where bookingHour might still have " hours minimum" suffix
                    val numberOfHours = if (serviceType == "charter_tour" && ride.bookingHour != null) {
                        val cleaned = ride.bookingHour.replace(" hours minimum", "").replace(" hours", "").trim()
                        if (cleaned.isEmpty()) "0" else cleaned
                    } else if (serviceType != "charter_tour" && ride.bookingHour != null && ride.bookingHour.contains("hours")) {
                        // For non-charter tours, if bookingHour has "hours", use default "0"
                        "0"
                    } else {
                        ride.bookingHour ?: "0"
                    }
                    
                    // CRITICAL: Log pickupTime before formatting to debug empty values
                    Log.d(DebugTags.BookingProcess, "🔍 Building BookingRatesRequest - ride.serviceType='${ride.serviceType}', mapped serviceType='$serviceType', ride.bookingHour='${ride.bookingHour}', cleaned numberOfHours='$numberOfHours'")
                    Log.d(DebugTags.BookingProcess, "🔍 Building BookingRatesRequest - ride.pickupTime='${ride.pickupTime}', ride.returnPickupTime='${ride.returnPickupTime}'")
                    
                    // CRITICAL: Use ride.pickupTime directly - if empty, log warning but don't fail
                    val formattedPickupTime = if (ride.pickupTime.isNotEmpty()) {
                        ReservationRequestBuilder.formatTimeForAPI(ride.pickupTime)
                    } else {
                        Log.w(DebugTags.BookingProcess, "⚠️ WARNING: ride.pickupTime is empty! Using empty string.")
                        ""
                    }
                    
                    val formattedReturnPickupTime = if (serviceType == "round_trip" && !ride.returnPickupTime.isNullOrEmpty()) {
                        ReservationRequestBuilder.formatTimeForAPI(ride.returnPickupTime)
                    } else if (ride.pickupTime.isNotEmpty()) {
                        ReservationRequestBuilder.formatTimeForAPI(ride.pickupTime) // Fallback to pickup time if return time not set
                    } else {
                        Log.w(DebugTags.BookingProcess, "⚠️ WARNING: ride.pickupTime is empty for returnPickupTime fallback! Using empty string.")
                        ""
                    }
                    
                    Log.d(DebugTags.BookingProcess, "🔍 Formatted times - pickupTime='$formattedPickupTime', returnPickupTime='$formattedReturnPickupTime'")
                    
                    // ==========================================
                    // ENHANCED LOGGING: Trace serviceType before creating request
                    // ==========================================
                    Log.d(DebugTags.BookingProcess, "===========================================")
                    Log.d(DebugTags.BookingProcess, "🏗️ CREATING BookingRatesRequest")
                    Log.d(DebugTags.BookingProcess, "===========================================")
                    Log.d(DebugTags.BookingProcess, "📥 ride.serviceType='${ride.serviceType}'")
                    Log.d(DebugTags.BookingProcess, "📥 serviceType variable='$serviceType'")
                    Log.d(DebugTags.BookingProcess, "📥 numberOfHours='$numberOfHours'")
                    Log.d(DebugTags.BookingProcess, "📥 ride.bookingHour='${ride.bookingHour}'")
                    Log.d(DebugTags.BookingProcess, "===========================================")
                    
                    // CRITICAL: Verify serviceType is correct before creating request
                    if (ride.serviceType == "charter_tour" && serviceType != "charter_tour") {
                        Log.e(DebugTags.BookingProcess, "===========================================")
                        Log.e(DebugTags.BookingProcess, "❌ CRITICAL ERROR BEFORE CREATING REQUEST!")
                        Log.e(DebugTags.BookingProcess, "❌ ride.serviceType='${ride.serviceType}'")
                        Log.e(DebugTags.BookingProcess, "❌ serviceType variable='$serviceType'")
                        Log.e(DebugTags.BookingProcess, "❌ Expected: 'charter_tour', Got: '$serviceType'")
                        Log.e(DebugTags.BookingProcess, "===========================================")
                    }
                    
                    val bookingRatesRequest = BookingRatesRequest(
                        vehicleId = vehicle.id,
                        transferType = transferType,
                        serviceType = serviceType,
                        numberOfVehicles = ride.noOfVehicles,
                        distance = distance,
                        returnDistance = returnDistance,
                        noOfHours = numberOfHours,
                        isMasterVehicle = vehicle.isMasterVehicle ?: false,
                        extraStops = extraStops,
                        returnExtraStops = returnExtraStops,
                        pickupTime = formattedPickupTime,
                        returnPickupTime = formattedReturnPickupTime,
                        returnVehicleId = vehicle.id,
                        returnAffiliateType = "affiliate"
                    )
                    
                    // ==========================================
                    // ENHANCED LOGGING: Verify serviceType in created request
                    // ==========================================
                    Log.d(DebugTags.BookingProcess, "===========================================")
                    Log.d(DebugTags.BookingProcess, "✅ BookingRatesRequest CREATED")
                    Log.d(DebugTags.BookingProcess, "===========================================")
                    Log.d(DebugTags.BookingProcess, "📤 bookingRatesRequest.serviceType='${bookingRatesRequest.serviceType}'")
                    Log.d(DebugTags.BookingProcess, "📤 bookingRatesRequest.noOfHours='${bookingRatesRequest.noOfHours}'")
                    Log.d(DebugTags.BookingProcess, "📤 bookingRatesRequest.pickupTime='${bookingRatesRequest.pickupTime}'")
                    Log.d(DebugTags.BookingProcess, "📤 bookingRatesRequest.transferType='${bookingRatesRequest.transferType}'")
                    
                    // CRITICAL: Verify the request has correct serviceType
                    if (ride.serviceType == "charter_tour" && bookingRatesRequest.serviceType != "charter_tour") {
                        Log.e(DebugTags.BookingProcess, "===========================================")
                        Log.e(DebugTags.BookingProcess, "❌ CRITICAL ERROR IN CREATED REQUEST!")
                        Log.e(DebugTags.BookingProcess, "❌ ride.serviceType='${ride.serviceType}'")
                        Log.e(DebugTags.BookingProcess, "❌ serviceType variable='$serviceType'")
                        Log.e(DebugTags.BookingProcess, "❌ bookingRatesRequest.serviceType='${bookingRatesRequest.serviceType}'")
                        Log.e(DebugTags.BookingProcess, "❌ Expected: 'charter_tour', Got: '${bookingRatesRequest.serviceType}'")
                        Log.e(DebugTags.BookingProcess, "===========================================")
                    }
                    Log.d(DebugTags.BookingProcess, "===========================================")
                    
                    Log.d(DebugTags.BookingProcess, "===========================================")
                    Log.d(DebugTags.BookingProcess, "📦 BOOKING RATES REQUEST PAYLOAD:")
                    Log.d(DebugTags.BookingProcess, "📦 BOOKING RATES REQUEST PAYLOAD:")

                    Log.d(DebugTags.BookingProcess, "===========================================")
                    Log.d(DebugTags.BookingProcess, "vehicleId: ${vehicle.id}")
                    Log.d(DebugTags.BookingProcess, "transferType: $transferType")
                    Log.d(DebugTags.BookingProcess, "serviceType: $serviceType")
                    Log.d(DebugTags.BookingProcess, "numberOfVehicles: ${ride.noOfVehicles}")
                    Log.d(DebugTags.BookingProcess, "distance: $distance meters (${distance / 1609.34} miles)")
                    Log.d(DebugTags.BookingProcess, "returnDistance: $returnDistance meters (${returnDistance / 1609.34} miles)")
                    Log.d(DebugTags.BookingProcess, "noOfHours: $numberOfHours")
                    Log.d(DebugTags.BookingProcess, "isMasterVehicle: ${vehicle.isMasterVehicle ?: false}")
                    Log.d(DebugTags.BookingProcess, "extraStops: ${extraStops.size} stops")
                    extraStops.forEachIndexed { index, stop ->
                        Log.d(DebugTags.BookingProcess, "  Stop ${index + 1}: ${stop.address}, Lat=${stop.latitude}, Long=${stop.longitude}")
                    }
                    Log.d(DebugTags.BookingProcess, "returnExtraStops: ${returnExtraStops.size} stops")
                    returnExtraStops.forEachIndexed { index, stop ->
                        Log.d(DebugTags.BookingProcess, "  Return Stop ${index + 1}: ${stop.address}, Lat=${stop.latitude}, Long=${stop.longitude}")
                    }
                    // Use the already-formatted times from above
                    Log.d(DebugTags.BookingProcess, "pickupTime: $formattedPickupTime")
                    Log.d(DebugTags.BookingProcess, "returnPickupTime: $formattedReturnPickupTime")
                    Log.d(DebugTags.BookingProcess, "returnVehicleId: ${vehicle.id}")
                    Log.d(DebugTags.BookingProcess, "returnAffiliateType: affiliate")
                    Log.d(DebugTags.BookingProcess, "===========================================")
                    
                    Log.d(DebugTags.BookingProcess, "🚀 CALLING BOOKING RATES API WITH DISTANCE: $distance meters")
                    Log.d(DebugTags.BookingProcess, "🚀 CALLING BOOKING RATES API WITH RETURN DISTANCE: $returnDistance meters")
                    Log.d(DebugTags.BookingProcess, "Calling bookingRatesService.fetchBookingRates()...")
                    val bookingRatesResult = bookingRatesService.fetchBookingRates(bookingRatesRequest)
                    
                    if (bookingRatesResult.isSuccess) {
                        val response = bookingRatesResult.getOrNull()
                        val data = response?.data
                        _bookingRatesData.value = data
                        _bookingRatesCurrency.value = response?.currency // Store currency from response
                        Log.d(DebugTags.BookingProcess, "✅ Booking rates fetched successfully")
                        Log.d(DebugTags.BookingProcess, "  API Sub Total: ${data?.subTotal}")
                        Log.d(DebugTags.BookingProcess, "  API Grand Total: ${data?.grandTotal}")
                        Log.d(DebugTags.BookingProcess, "  Currency: ${response?.currency?.symbol ?: "N/A"}")
                        
                        // Calculate totals from rate array (matches iOS calculateTotalsFromRateArray)
                        if (data != null) {
                            val (calculatedSubTotal, calculatedGrandTotal) = calculateTotalsFromRateArray(data.rateArray, ride.serviceType, ride.noOfVehicles, ride.bookingHour ?: "0", data.minRateInvolved ?: false)
                            Log.d(DebugTags.BookingProcess, "  Calculated Sub Total: $calculatedSubTotal")
                            Log.d(DebugTags.BookingProcess, "  Calculated Grand Total: $calculatedGrandTotal")
                        }
                    } else {
                        Log.e(DebugTags.BookingProcess, "❌ Failed to fetch booking rates: ${bookingRatesResult.exceptionOrNull()?.message}")
                        _bookingRatesData.value = null
                        _bookingRatesCurrency.value = null
                    }
                }
            } catch (e: Exception) {
                Log.e(DebugTags.BookingProcess, "Error fetching booking rates", e)
                _bookingRatesData.value = null
            } finally {
                _bookingRatesLoading.value = false
                currentLoadingServiceType = null // Clear tracking when done
            }
        }
    }
    
    fun createReservation(
        ride: RideData, 
        vehicle: Vehicle,
        extraStops: List<com.example.limouserapp.data.model.booking.ExtraStopRequest> = emptyList(),
        returnExtraStops: List<com.example.limouserapp.data.model.booking.ExtraStopRequest> = emptyList(),
        isRepeatMode: Boolean = false,
        repeatBookingId: Int? = null,
        isReturnFlow: Boolean = false,
        bookingInstructions: String? = null,
        returnBookingInstructions: String? = null,
        meetGreetChoicesName: String? = null,
        returnMeetGreetChoicesName: String? = null,
        cruisePort: String = "",
        cruiseName: String = "",
        cruiseTime: String = ""
    ) {
        viewModelScope.launch {
            _loading.value = true
            Log.d(DebugTags.BookingProcess, "===========================================")
            Log.d(DebugTags.BookingProcess, "🎯 createReservation() CALLED")
            Log.d(DebugTags.BookingProcess, "===========================================")
            Log.d(DebugTags.BookingProcess, "Ride Data: $ride")
            Log.d(DebugTags.BookingProcess, "Vehicle ID: ${vehicle.id}")
            Log.d(DebugTags.BookingProcess, "Vehicle Name: ${vehicle.name}")
            Log.d(DebugTags.BookingProcess, "Service Type: ${ride.serviceType}")
            Log.d(DebugTags.BookingProcess, "Pickup: ${ride.pickupLocation}")
            Log.d(DebugTags.BookingProcess, "Dropoff: ${ride.destinationLocation}")
            Log.d(DebugTags.BookingProcess, "===========================================")
            
            try {
                // Get user profile for passenger information
                val profileResult = dashboardApi.getProfileData()
                val profileData = if (profileResult.success) profileResult.data else null
                
                // Fetch meet & greet choices to map names to IDs
                meetGreetService.fetchMeetGreetChoices()
                val meetGreetChoicesList = meetGreetService.meetGreetChoices.value
                
                // Map meet & greet name to ID
                val meetGreetId = if (meetGreetChoicesName != null && meetGreetChoicesList.isNotEmpty()) {
                    meetGreetChoicesList.find { it.message.equals(meetGreetChoicesName, ignoreCase = true) }?.id ?: 1
                } else {
                    1 // Default to 1 if not found
                }
                
                val returnMeetGreetId = if (returnMeetGreetChoicesName != null && meetGreetChoicesList.isNotEmpty()) {
                    meetGreetChoicesList.find { it.message.equals(returnMeetGreetChoicesName, ignoreCase = true) }?.id ?: 1
                } else {
                    1 // Default to 1 if not found
                }
                
                // Map service type and transfer type
                val serviceType = ReservationRequestBuilder.mapServiceType(ride.serviceType)
                val transferType = ReservationRequestBuilder.mapTransferType(ride.pickupType, ride.dropoffType)
                val returnTransferType = ReservationRequestBuilder.mapReturnTransferType(transferType)
                
                // Calculate distance and journey time using Google Maps Directions API
                val (distance, journeyTime) = if (ride.pickupLat != null && ride.pickupLong != null && 
                    ride.destinationLat != null && ride.destinationLong != null) {
                    directionsService.calculateDistance(
                        ride.pickupLat, ride.pickupLong, 
                        ride.destinationLat, ride.destinationLong
                    )
                } else {
                    Pair(0, 0)
                }
                val (returnDistance, returnJourneyTime) = if (serviceType == "round_trip" && 
                    ride.destinationLat != null && ride.destinationLong != null &&
                    ride.pickupLat != null && ride.pickupLong != null) {
                    directionsService.calculateDistance(
                        ride.destinationLat, ride.destinationLong,
                        ride.pickupLat, ride.pickupLong
                    )
                } else {
                    Pair(0, 0)
                }
                val numberOfHours = ride.bookingHour ?: "0"
                
                // Use stored booking rates data (should be fetched when screen loads)
                val storedRatesData = _bookingRatesData.value
                val bookingRatesData = if (storedRatesData != null) {
                    Log.d(DebugTags.BookingProcess, "✅ Using stored booking rates data (fetched when screen loaded)")
                    storedRatesData
                } else {
                    Log.w(DebugTags.BookingProcess, "⚠️ No stored booking rates found - fetching now as fallback...")
                    // Fallback: fetch now if not already fetched (shouldn't happen in normal flow)
                    val bookingRatesRequest = BookingRatesRequest(
                        vehicleId = vehicle.id,
                        transferType = transferType,
                        serviceType = serviceType,
                        numberOfVehicles = ride.noOfVehicles,
                        distance = distance,
                        returnDistance = returnDistance,
                        noOfHours = numberOfHours,
                        isMasterVehicle = vehicle.isMasterVehicle ?: false,
                        extraStops = emptyList(),
                        returnExtraStops = emptyList(),
                        pickupTime = ReservationRequestBuilder.formatTimeForAPI(ride.pickupTime),
                        returnPickupTime = if (serviceType == "round_trip" && !ride.returnPickupTime.isNullOrEmpty()) {
                            ReservationRequestBuilder.formatTimeForAPI(ride.returnPickupTime)
                        } else {
                            ReservationRequestBuilder.formatTimeForAPI(ride.pickupTime) // Fallback to pickup time if return time not set
                        },
                        returnVehicleId = vehicle.id,
                        returnAffiliateType = "affiliate"
                    )
                    val bookingRatesResult = bookingRatesService.fetchBookingRates(bookingRatesRequest)
                    bookingRatesResult.getOrNull()?.data
                }
                
                val rateArray = bookingRatesData?.rateArray ?: run {
                    Log.w(DebugTags.BookingProcess, "⚠️ Booking rates not available, falling back to vehicle rate breakdown")
                    // Fallback to vehicle rate breakdown if API fails
                    val rateBreakdown = vehicle.getRateBreakdown(ride.serviceType)
                    val grandTotal = (rateBreakdown?.grandTotal ?: rateBreakdown?.total ?: rateBreakdown?.subTotal ?: 0.0) * ride.noOfVehicles
                    ReservationRequestBuilder.constructRateArrayFromVehicle(rateBreakdown, grandTotal)
                }
                
                val returnRateArray = bookingRatesData?.retrunRateArray
                
                // Calculate totals from rate array (matches iOS - uses calculated totals, not API totals)
                val (calculatedSubTotal, calculatedGrandTotal) = if (bookingRatesData != null) {
                    calculateTotalsFromRateArray(bookingRatesData.rateArray, serviceType, ride.noOfVehicles, numberOfHours, bookingRatesData.minRateInvolved ?: false)
                } else {
                    // Fallback calculation
                    val rateBreakdown = vehicle.getRateBreakdown(ride.serviceType)
                    val fallbackSubTotal = (rateBreakdown?.subTotal ?: rateBreakdown?.total ?: 0.0) * ride.noOfVehicles
                    val fallbackGrandTotal = (rateBreakdown?.grandTotal ?: rateBreakdown?.total ?: rateBreakdown?.subTotal ?: 0.0) * ride.noOfVehicles
                    Pair(fallbackSubTotal, fallbackGrandTotal)
                }
                
                val subTotal = calculatedSubTotal
                val grandTotal = calculatedGrandTotal
                val minRateInvolved = bookingRatesData?.minRateInvolved ?: false

                Log.d(DebugTags.BookingProcess, "💰 Using calculated totals for reservation:")
                Log.d(DebugTags.BookingProcess, "  Sub Total: $subTotal")
                Log.d(DebugTags.BookingProcess, "  Rate Array Return: ${returnRateArray.toString()}")
                Log.d(DebugTags.BookingProcess, "  Grand Total: $grandTotal")
                
                // Calculate shares array from rateArray (matches iOS)
                // For round trips, pass the calculated grandTotal as returnGrandTotal (matches iOS line 7136)
                val sharesArray = SharesArrayBuilder.buildSharesArray(
                    rateArray = rateArray,
                    serviceType = serviceType,
                    numberOfHours = numberOfHours,
                    accountType = "individual",
                    returnGrandTotal = if (serviceType == "round_trip") grandTotal else null,
                    minRateInvolved = bookingRatesData?.minRateInvolved ?: false
                )
                
                // Calculate return shares array for round trips (matches iOS)
                val returnSharesArray = if (serviceType == "round_trip" && returnRateArray != null) {
                    SharesArrayBuilder.buildSharesArray(
                        rateArray = returnRateArray,
                        serviceType = serviceType,
                        numberOfHours = numberOfHours,
                        accountType = "individual",
                        returnGrandTotal = null,
                        minRateInvolved = bookingRatesData?.minRateInvolved ?: false
                    )
                } else {
                    null
                }
                
                // Get vehicle details
                val vehicleSeats = vehicle.getCapacity().toString()
                val driverInfo = vehicle.driverInformation
                
                // Build AirportOption objects from selected airports (matches iOS)
                val pickupAirportOption = if (!ride.selectedPickupAirport.isNullOrEmpty()) {
                    airportService.getAirportByDisplayName(ride.selectedPickupAirport)?.let { airport ->
                        AirportOption(
                            id = airport.id,
                            code = airport.code,
                            name = airport.name,
                            city = airport.city,
                            country = airport.country,
                            lat = airport.lat ?: 0.0,
                            long = airport.long ?: 0.0,
                            formattedName = airport.displayName
                        )
                    }
                } else null
                
                val dropoffAirportOption = if (!ride.selectedDestinationAirport.isNullOrEmpty()) {
                    airportService.getAirportByDisplayName(ride.selectedDestinationAirport)?.let { airport ->
                        AirportOption(
                            id = airport.id,
                            code = airport.code,
                            name = airport.name,
                            city = airport.city,
                            country = airport.country,
                            lat = airport.lat ?: 0.0,
                            long = airport.long ?: 0.0,
                            formattedName = airport.displayName
                        )
                    }
                } else null
                
                // Build AirlineOption objects from selected airlines (matches web format)
                val pickupAirlineOption: AirlineOption? = if (!ride.selectedPickupAirline.isNullOrEmpty()) {
                    airlineService.getAirlineByDisplayName(ride.selectedPickupAirline)?.let { airline ->
                        AirlineOption(
                            id = airline.id,
                            code = airline.code,
                            name = airline.name,
                            country = airline.country ?: "",
                            formattedName = airline.fullDisplayName
                        )
                    }
                } else null
                
                val dropoffAirlineOption: AirlineOption? = if (!ride.selectedDestinationAirline.isNullOrEmpty()) {
                    airlineService.getAirlineByDisplayName(ride.selectedDestinationAirline)?.let { airline ->
                        AirlineOption(
                            id = airline.id,
                            code = airline.code,
                            name = airline.name,
                            country = airline.country ?: "",
                            formattedName = airline.fullDisplayName
                        )
                    }
                } else null
                
                // Determine if pickup/dropoff are airports based on transfer type (matches web format)
                val isPickupAirport = transferType.contains("airport", ignoreCase = true) && 
                                     (transferType.startsWith("airport", ignoreCase = true) || 
                                      transferType == "airport_to_airport")
                val isDropoffAirport = transferType.contains("airport", ignoreCase = true) && 
                                       (transferType.endsWith("airport", ignoreCase = true) || 
                                        transferType == "airport_to_airport")
                
                // Get airport IDs and names - use empty strings when not airport (matches web format)
                val pickupAirportId = if (isPickupAirport && pickupAirportOption != null) {
                    pickupAirportOption.id.toString()
                } else {
                    ""
                }
                val pickupAirportName = if (isPickupAirport) (ride.selectedPickupAirport ?: "") else ""
                // Use airport coordinates from AirportOption if available, otherwise fall back to ride coordinates (matches iOS)
                val pickupAirportLat = if (isPickupAirport) {
                    if (pickupAirportOption != null && pickupAirportOption.lat != 0.0) {
                        pickupAirportOption.lat.toString()
                    } else {
                        // Fall back to ride coordinates when airport option is null or has 0.0 coordinates
                        ride.pickupLat?.toString() ?: ""
                    }
                } else {
                    ""
                }
                val pickupAirportLng = if (isPickupAirport) {
                    if (pickupAirportOption != null && pickupAirportOption.long != 0.0) {
                        pickupAirportOption.long.toString()
                    } else {
                        // Fall back to ride coordinates when airport option is null or has 0.0 coordinates
                        ride.pickupLong?.toString() ?: ""
                    }
                } else {
                    ""
                }
                
                val dropoffAirportId = if (isDropoffAirport && dropoffAirportOption != null) {
                    dropoffAirportOption.id.toString()
                } else {
                    ""
                }
                val dropoffAirportName = if (isDropoffAirport) (ride.selectedDestinationAirport ?: "") else ""
                // Use airport coordinates from AirportOption if available, otherwise fall back to ride coordinates (matches iOS)
                val dropoffAirportLat = if (isDropoffAirport) {
                    if (dropoffAirportOption != null && dropoffAirportOption.lat != 0.0) {
                        dropoffAirportOption.lat.toString()
                    } else {
                        // Fall back to ride coordinates when airport option is null or has 0.0 coordinates
                        ride.destinationLat?.toString() ?: ""
                    }
                } else {
                    ""
                }
                val dropoffAirportLng = if (isDropoffAirport) {
                    if (dropoffAirportOption != null && dropoffAirportOption.long != 0.0) {
                        dropoffAirportOption.long.toString()
                    } else {
                        // Fall back to ride coordinates when airport option is null or has 0.0 coordinates
                        ride.destinationLong?.toString() ?: ""
                    }
                } else {
                    ""
                }
                
                // Build return trip fields for round trips (reverse of outbound trip)
                val isRoundTrip = serviceType == "round_trip"
                val isReturnPickupAirport = isRoundTrip && returnTransferType.contains("airport", ignoreCase = true) && 
                                           (returnTransferType.startsWith("airport", ignoreCase = true) || 
                                            returnTransferType == "airport_to_airport")
                val isReturnDropoffAirport = isRoundTrip && returnTransferType.contains("airport", ignoreCase = true) && 
                                             (returnTransferType.endsWith("airport", ignoreCase = true) || 
                                              returnTransferType == "airport_to_airport")
                
                // Return pickup = outbound dropoff (reversed)
                val returnPickupAirportOption = if (isRoundTrip && isReturnPickupAirport) dropoffAirportOption else null
                val returnPickupAirportId = if (isRoundTrip && isReturnPickupAirport && returnPickupAirportOption != null) {
                    returnPickupAirportOption.id.toString()
                } else {
                    ""
                }
                val returnPickupAirportName = if (isRoundTrip && isReturnPickupAirport) dropoffAirportName else ""
                // Use airport coordinates from AirportOption if available, otherwise fall back to ride coordinates (matches iOS)
                val returnPickupAirportLat = if (isRoundTrip && isReturnPickupAirport) {
                    if (returnPickupAirportOption != null && returnPickupAirportOption.lat != 0.0) {
                        returnPickupAirportOption.lat.toString()
                    } else {
                        // Fall back to ride coordinates (return pickup = outbound dropoff)
                        ride.destinationLat?.toString() ?: ""
                    }
                } else {
                    ""
                }
                val returnPickupAirportLng = if (isRoundTrip && isReturnPickupAirport) {
                    if (returnPickupAirportOption != null && returnPickupAirportOption.long != 0.0) {
                        returnPickupAirportOption.long.toString()
                    } else {
                        // Fall back to ride coordinates (return pickup = outbound dropoff)
                        ride.destinationLong?.toString() ?: ""
                    }
                } else {
                    ""
                }
                
                // Return dropoff = outbound pickup (reversed)
                val returnDropoffAirportOption = if (isRoundTrip && isReturnDropoffAirport) pickupAirportOption else null
                val returnDropoffAirportId = if (isRoundTrip && isReturnDropoffAirport && returnDropoffAirportOption != null) {
                    returnDropoffAirportOption.id.toString()
                } else {
                    ""
                }
                val returnDropoffAirportName = if (isRoundTrip && isReturnDropoffAirport) pickupAirportName else ""
                // Use airport coordinates from AirportOption if available, otherwise fall back to ride coordinates (matches iOS)
                val returnDropoffAirportLat = if (isRoundTrip && isReturnDropoffAirport) {
                    if (returnDropoffAirportOption != null && returnDropoffAirportOption.lat != 0.0) {
                        returnDropoffAirportOption.lat.toString()
                    } else {
                        // Fall back to ride coordinates (return dropoff = outbound pickup)
                        ride.pickupLat?.toString() ?: ""
                    }
                } else {
                    ""
                }
                val returnDropoffAirportLng = if (isRoundTrip && isReturnDropoffAirport) {
                    if (returnDropoffAirportOption != null && returnDropoffAirportOption.long != 0.0) {
                        returnDropoffAirportOption.long.toString()
                    } else {
                        // Fall back to ride coordinates (return dropoff = outbound pickup)
                        ride.pickupLong?.toString() ?: ""
                    }
                } else {
                    ""
                }
                
                // Return trip locations (reversed from outbound) - set based on whether they're airports (matches web app)
                val returnPickupLocation = if (isRoundTrip) {
                    if (isReturnPickupAirport) "" else (ride.destinationLocation ?: "")
                } else ""
                val returnPickupLat = if (isRoundTrip) {
                    if (isReturnPickupAirport) "" else (ride.destinationLat?.toString() ?: "")
                } else ""
                val returnPickupLng = if (isRoundTrip) {
                    if (isReturnPickupAirport) "" else (ride.destinationLong?.toString() ?: "")
                } else ""
                val returnDropoffLocation = if (isRoundTrip) {
                    if (isReturnDropoffAirport) "" else (ride.pickupLocation ?: "")
                } else ""
                val returnDropoffLat = if (isRoundTrip) {
                    if (isReturnDropoffAirport) "" else (ride.pickupLat?.toString() ?: "")
                } else ""
                val returnDropoffLng = if (isRoundTrip) {
                    if (isReturnDropoffAirport) "" else (ride.pickupLong?.toString() ?: "")
                } else ""
                
                // Return trip date/time (use pickup date as fallback for round trips)
                val returnPickupDate = if (isRoundTrip) ride.pickupDate else ""
                val returnPickupTime = if (isRoundTrip) "12:00 PM" else ""
                
                // Return airline options (reversed from outbound)
                val returnPickupAirlineOption: AirlineOption? = if (isRoundTrip) dropoffAirlineOption else null
                val returnDropoffAirlineOption: AirlineOption? = if (isRoundTrip) pickupAirlineOption else null
                
                // Build complete request
                val req = CreateReservationRequest(
                    serviceType = serviceType,
                    transferType = transferType,
                    returnTransferType = returnTransferType,
                    numberOfHours = numberOfHours.toIntOrNull() ?: 0,
                    accountType = "individual",
                    changeIndividualData = false,
                    passengerName = if (_uiState.value.passengerName.isNotEmpty()) _uiState.value.passengerName else (profileData?.fullName ?: "Guest"),
                    passengerEmail = if (_uiState.value.passengerEmail.isNotEmpty()) _uiState.value.passengerEmail else (profileData?.email ?: ""),
                    passengerCell = if (_uiState.value.passengerMobile.isNotEmpty()) _uiState.value.passengerMobile else (profileData?.mobile ?: ""),
                    passengerCellIsd = profileData?.mobileIsd ?: "+1",
                    passengerCellCountry = profileData?.mobileCountry ?: "us",
                    totalPassengers = ride.noOfPassenger,
                    luggageCount = ride.noOfLuggage,
                    bookingInstructions = bookingInstructions ?: "1. Driver - Text on location. Text the client a day before to confirm driver name , cell phone and booking details. Text client with ETA when en route",
                    returnBookingInstructions = returnBookingInstructions ?: "1. Driver - Text on location. Text the client a day before to confirm driver name , cell phone and booking details. Text client with ETA when en route",
                    affiliateType = "affiliate",
                    affiliateId = vehicle.affiliateId?.toString() ?: "", // Convert to String, matches iOS: selectedVehicle?.affiliateId?.description ?? ""
                    loseAffiliateName = "",
                    loseAffiliatePhone = "",
                    loseAffiliatePhoneIsd = "+1",
                    loseAffiliatePhoneCountry = "us",
                    loseAffiliateEmail = "",
                    vehicleType = "",
                    vehicleTypeName = "",
                    vehicleId = vehicle.id,
                    vehicleMake = "",
                    vehicleMakeName = "",
                    vehicleModel = "",
                    vehicleModelName = "",
                    vehicleYear = "",
                    vehicleYearName = "",
                    vehicleColor = "",
                    vehicleColorName = "",
                    vehicleLicensePlate = "",
                    vehicleSeats = vehicleSeats,
                    driverId = driverInfo?.id?.toString() ?: "", // Convert to String
                    driverName = driverInfo?.name ?: "1800LIMO Chauffeurs",
                    driverGender = driverInfo?.gender?.lowercase() ?: "male",
                    driverCell = driverInfo?.cellNumber ?: driverInfo?.phone ?: "",
                    driverCellIsd = driverInfo?.cellIsd ?: "+1",
                    driverCellCountry = "us",
                    driverEmail = driverInfo?.email ?: "",
                    driverPhoneType = "",
                    driverImageId = "",
                    vehicleImageId = "",
                    meetGreetChoices = meetGreetId,
                    meetGreetChoicesName = meetGreetChoicesName ?: "Driver - Text/call when on location",
                    numberOfVehicles = ride.noOfVehicles,
                    pickupDate = ride.pickupDate,
                    pickupTime = ReservationRequestBuilder.formatTimeForAPI(ride.pickupTime),
                    extraStops = extraStops,
                    returnExtraStops = returnExtraStops,
                    // Pickup fields - set based on whether it's an airport (matches web app)
                    pickup = if (isPickupAirport) "" else (ride.pickupLocation ?: ""),
                    pickupLatitude = if (isPickupAirport) "" else (ride.pickupLat?.toString() ?: ""),
                    pickupLongitude = if (isPickupAirport) "" else (ride.pickupLong?.toString() ?: ""),
                    pickupAirportOption = pickupAirportOption,
                    pickupAirport = pickupAirportId,
                    pickupAirportName = pickupAirportName,
                    pickupAirportLatitude = pickupAirportLat,
                    pickupAirportLongitude = pickupAirportLng,
                    pickupAirlineOption = pickupAirlineOption,
                    pickupAirline = pickupAirlineOption?.id?.toString() ?: "",
                    pickupAirlineName = pickupAirlineOption?.formattedName ?: "",
                    pickupFlight = ride.pickupFlightNumber ?: "",
                    originAirportCity = (ride.originAirportCity ?: "").also { 
                        Log.d(DebugTags.BookingProcess, "📍 Origin Airport City from RideData: '$it'")
                    },
                    cruisePort = cruisePort,
                    cruiseName = cruiseName,
                    cruiseTime = cruiseTime,
                    // Dropoff fields - set based on whether it's an airport (matches web app)
                    dropoff = if (isDropoffAirport) "" else (ride.destinationLocation ?: ""),
                    dropoffLatitude = if (isDropoffAirport) "" else (ride.destinationLat?.toString() ?: ""),
                    dropoffLongitude = if (isDropoffAirport) "" else (ride.destinationLong?.toString() ?: ""),
                    dropoffAirportOption = dropoffAirportOption,
                    dropoffAirport = dropoffAirportId,
                    dropoffAirportName = dropoffAirportName,
                    dropoffAirportLatitude = dropoffAirportLat,
                    dropoffAirportLongitude = dropoffAirportLng,
                    dropoffAirlineOption = dropoffAirlineOption,
                    dropoffAirline = dropoffAirlineOption?.id?.toString() ?: "",
                    dropoffAirlineName = dropoffAirlineOption?.formattedName ?: "",
                    dropoffFlight = ride.dropoffFlightNumber ?: "",
                    returnMeetGreetChoices = returnMeetGreetId,
                    returnMeetGreetChoicesName = returnMeetGreetChoicesName ?: "Driver - Text/call when on location",
                    returnPickupDate = returnPickupDate,
                    returnPickupTime = if (isRoundTrip) ReservationRequestBuilder.formatTimeForAPI(returnPickupTime) else "",
                    returnPickup = returnPickupLocation,
                    returnPickupLatitude = returnPickupLat,
                    returnPickupLongitude = returnPickupLng,
                    returnPickupAirportOption = returnPickupAirportOption,
                    returnPickupAirport = returnPickupAirportId,
                    returnPickupAirportName = returnPickupAirportName,
                    returnPickupAirportLatitude = returnPickupAirportLat,
                    returnPickupAirportLongitude = returnPickupAirportLng,
                    returnPickupAirlineOption = returnPickupAirlineOption,
                    returnPickupAirline = returnPickupAirlineOption?.id?.toString() ?: "",
                    returnPickupAirlineName = returnPickupAirlineOption?.formattedName ?: "",
                    returnPickupFlight = ride.returnPickupFlightNumber ?: "",
                    returnCruisePort = "",
                    returnCruiseName = "",
                    returnCruiseTime = "",
                    returnDropoff = returnDropoffLocation,
                    returnDropoffLatitude = returnDropoffLat,
                    returnDropoffLongitude = returnDropoffLng,
                    returnDropoffAirportOption = returnDropoffAirportOption,
                    returnDropoffAirport = returnDropoffAirportId,
                    returnDropoffAirportName = returnDropoffAirportName,
                    returnDropoffAirportLatitude = returnDropoffAirportLat,
                    returnDropoffAirportLongitude = returnDropoffAirportLng,
                    returnDropoffAirlineOption = returnDropoffAirlineOption,
                    returnDropoffAirline = returnDropoffAirlineOption?.id?.toString() ?: "",
                    returnDropoffAirlineName = returnDropoffAirlineOption?.formattedName ?: "",
                    returnDropoffFlight = ride.returnDropoffFlightNumber ?: "",
                    driverLanguages = emptyList(),
                    driverDresses = emptyList(),
                    amenities = emptyList(),
                    chargedAmenities = emptyList(),
                    journeyDistance = distance,
                    journeyTime = journeyTime,
                    returnJourneyDistance = if (isRoundTrip) returnDistance.toString() else "",
                    returnJourneyTime = if (isRoundTrip && returnDistance > 0) returnJourneyTime.toString() else "",
                    reservationId = if (isRepeatMode && repeatBookingId != null) repeatBookingId.toString() else "",
                    updateType = if (isRepeatMode) (if (isReturnFlow) "return" else "repeat") else "",
                    departingAirportCity = ride.originAirportCity ?: "",
                    currency = "USD",
                    isMasterVehicle = vehicle.isMasterVehicle ?: false,
                    proceed = true,
                    rateArray = rateArray,
                    returnRateArray = returnRateArray,
                    grandTotal = grandTotal,
                    subTotal = subTotal,
                    returnSubTotal = if (serviceType == "round_trip") returnRateArray?.let { 
                        // Calculate return subtotal from return rate array
                        val returnSubTotal = it.allInclusiveRates.values.sumOf { item -> item.amount } +
                            it.taxes.values.sumOf { tax -> tax.amount } +
                            it.misc.values.sumOf { misc -> misc.amount }
                        returnSubTotal
                    } else null,
                    returnGrandTotal = if (serviceType == "round_trip") returnRateArray?.let {
                        // Calculate return grand total from return rate array
                        val returnGrandTotal = it.allInclusiveRates.values.sumOf { item -> item.amount } +
                            it.taxes.values.sumOf { tax -> tax.amount } +
                            it.misc.values.sumOf { misc -> misc.amount }
                        returnGrandTotal
                    } else null,
                    minRateInvolved = minRateInvolved,
                    sharesArray = sharesArray,
                    returnSharesArray = returnSharesArray,
                    returnAffiliateType = "affiliate",
                    returnDistance = returnDistance,
                    returnVehicleId = if (serviceType == "round_trip") vehicle.id else 0,
                    noOfHours = numberOfHours,
                    fboAddress = "",
                    fboName = "",
                    returnFboAddress = "",
                    returnFboName = "",
                    returnAffiliateId = vehicle.affiliateId?.toString() ?: "", // Use same affiliate ID as outbound
                    createdByRole = null // Optional field, can be set to "admin" if needed
                )
                
                Log.d(DebugTags.BookingProcess, "Built reservation request with sharesArray: baseRate=${sharesArray.baseRate}, grandTotal=${sharesArray.grandTotal}, affiliateShare=${sharesArray.affiliateShare}")
                
                // Log the complete payload as JSON
                // Use serializeNulls() to ensure all fields are included (matches web format)
                val gsonPretty = GsonBuilder()
                    .setPrettyPrinting()
                    .serializeNulls()
                    .create()
                val jsonPayload = gsonPretty.toJson(req)
                Log.d(DebugTags.BookingProcess, "===========================================")
                Log.d(DebugTags.BookingProcess, "COMPLETE RESERVATION REQUEST PAYLOAD:")
                Log.d(DebugTags.BookingProcess, "===========================================")
                Log.d(DebugTags.BookingProcess, jsonPayload)
                Log.d(DebugTags.BookingProcess, "===========================================")
                
                // Create reservation API call

                val result = bookingService.createReservation(req)
                result.onSuccess {
                    Log.d(DebugTags.BookingProcess, "✅ Reservation created successfully: id=${it.bookingId}")
                }.onFailure {
                    Log.e(DebugTags.BookingProcess, "❌ Reservation failed", it)
                }
                _result.value = result


            } catch (e: Exception) {
                Log.e(DebugTags.BookingProcess, "Error building reservation request", e)
                _result.value = Result.failure(e)
            } finally {
                _loading.value = false
            }
        }
    }
    
    /**
     * Reset booking result - clears the result state
     * Should be called when ComprehensiveBookingScreen is shown to prevent old results from triggering success
     */
    fun resetResult() {
        _result.value = null
        Log.d(DebugTags.BookingProcess, "🔄 Booking result reset")
    }
    
    /**
     * Fetch edit reservation data - matches iOS fetchEditReservation
     */
    fun fetchEditReservation(bookingId: Int) {
        viewModelScope.launch {
            _editLoading.value = true
            _editError.value = null
            
            try {
                val result = editReservationService.fetchEditReservation(bookingId)
                result.onSuccess { response ->
                    _editData.value = response.data
                    Log.d(DebugTags.BookingProcess, "✅ Edit reservation data fetched successfully")
                }.onFailure { error ->
                    _editError.value = error.message ?: "Failed to fetch edit reservation data"
                    Log.e(DebugTags.BookingProcess, "❌ Failed to fetch edit reservation data", error)
                }
            } catch (e: Exception) {
                _editError.value = e.message ?: "Unknown error"
                Log.e(DebugTags.BookingProcess, "Error fetching edit reservation data", e)
            } finally {
                _editLoading.value = false
            }
        }
    }
    
    /**
     * Update reservation - matches iOS updateReservation
     */
    fun updateReservation(bookingId: Int, request: EditReservationRequest) {
        viewModelScope.launch {
            _loading.value = true
            
            try {
                val result = editReservationService.updateReservation(request)
                result.onSuccess { response ->
                    Log.d(DebugTags.BookingProcess, "✅ Reservation updated successfully")
                }.onFailure { error ->
                    Log.e(DebugTags.BookingProcess, "❌ Reservation update failed", error)
                }
                _updateResult.value = result
            } catch (e: Exception) {
                Log.e(DebugTags.BookingProcess, "Error updating reservation", e)
                _updateResult.value = Result.failure(e)
            } finally {
                _loading.value = false
            }
        }
    }
    
    /**
     * Update reservation from RideData and Vehicle (matches iOS updateReservation)
     * Builds EditReservationRequest similar to createReservation
     */
    fun updateReservation(
        bookingId: Int, 
        ride: RideData, 
        vehicle: Vehicle,
        extraStops: List<com.example.limouserapp.data.model.booking.ExtraStopRequest> = emptyList(),
        returnExtraStops: List<com.example.limouserapp.data.model.booking.ExtraStopRequest> = emptyList(),
        bookingInstructions: String? = null,
        returnBookingInstructions: String? = null,
        meetGreetChoicesName: String? = null,
        returnMeetGreetChoicesName: String? = null,
        cruisePort: String = "",
        cruiseName: String = "",
        cruiseTime: String = ""
    ) {
        viewModelScope.launch {
            _loading.value = true
            Log.d(DebugTags.BookingProcess, "===========================================")
            Log.d(DebugTags.BookingProcess, "🔄 updateReservation() CALLED")
            Log.d(DebugTags.BookingProcess, "===========================================")
            Log.d(DebugTags.BookingProcess, "Booking ID: $bookingId")
            Log.d(DebugTags.BookingProcess, "Ride Data: $ride")
            Log.d(DebugTags.BookingProcess, "Vehicle ID: ${vehicle.id}")
            Log.d(DebugTags.BookingProcess, "Vehicle Name: ${vehicle.name}")
            Log.d(DebugTags.BookingProcess, "Service Type: ${ride.serviceType}")
            Log.d(DebugTags.BookingProcess, "Pickup: ${ride.pickupLocation}")
            Log.d(DebugTags.BookingProcess, "Dropoff: ${ride.destinationLocation}")
            Log.d(DebugTags.BookingProcess, "===========================================")
            
            try {
                // Get user profile for passenger information
                val profileResult = dashboardApi.getProfileData()
                val profileData = if (profileResult.success) profileResult.data else null
                
                // Fetch meet & greet choices to map names to IDs
                meetGreetService.fetchMeetGreetChoices()
                val meetGreetChoicesList = meetGreetService.meetGreetChoices.value
                
                // Map meet & greet name to ID
                val meetGreetId = if (meetGreetChoicesName != null && meetGreetChoicesList.isNotEmpty()) {
                    meetGreetChoicesList.find { it.message.equals(meetGreetChoicesName, ignoreCase = true) }?.id ?: 1
                } else {
                    1 // Default to 1 if not found
                }
                
                val returnMeetGreetId = if (returnMeetGreetChoicesName != null && meetGreetChoicesList.isNotEmpty()) {
                    meetGreetChoicesList.find { it.message.equals(returnMeetGreetChoicesName, ignoreCase = true) }?.id ?: 1
                } else {
                    1 // Default to 1 if not found
                }
                
                // Map service type and transfer type
                val serviceType = ReservationRequestBuilder.mapServiceType(ride.serviceType)
                val transferType = ReservationRequestBuilder.mapTransferType(ride.pickupType, ride.dropoffType)
                val returnTransferType = ReservationRequestBuilder.mapReturnTransferType(transferType)
                
                // Calculate distance and journey time using Google Maps Directions API
                val (distance, journeyTime) = if (ride.pickupLat != null && ride.pickupLong != null && 
                    ride.destinationLat != null && ride.destinationLong != null) {
                    directionsService.calculateDistance(
                        ride.pickupLat, ride.pickupLong, 
                        ride.destinationLat, ride.destinationLong
                    )
                } else {
                    Pair(0, 0)
                }
                val (returnDistance, returnJourneyTime) = if (serviceType == "round_trip" && 
                    ride.destinationLat != null && ride.destinationLong != null &&
                    ride.pickupLat != null && ride.pickupLong != null) {
                    directionsService.calculateDistance(
                        ride.destinationLat, ride.destinationLong,
                        ride.pickupLat, ride.pickupLong
                    )
                } else {
                    Pair(0, 0)
                }
                val numberOfHours = ride.bookingHour ?: "0"
                
                // Use stored booking rates data (should be fetched when screen loads)
                val storedRatesData = _bookingRatesData.value
                val bookingRatesData = if (storedRatesData != null) {
                    Log.d(DebugTags.BookingProcess, "✅ Using stored booking rates data (fetched when screen loaded)")
                    storedRatesData
                } else {
                    Log.w(DebugTags.BookingProcess, "⚠️ No stored booking rates found - fetching now as fallback...")
                    // Fallback: fetch now if not already fetched
                    val bookingRatesRequest = BookingRatesRequest(
                        vehicleId = vehicle.id,
                        transferType = transferType,
                        serviceType = serviceType,
                        numberOfVehicles = ride.noOfVehicles,
                        distance = distance,
                        returnDistance = returnDistance,
                        noOfHours = numberOfHours,
                        isMasterVehicle = vehicle.isMasterVehicle ?: false,
                        extraStops = emptyList(),
                        returnExtraStops = emptyList(),
                        pickupTime = ReservationRequestBuilder.formatTimeForAPI(ride.pickupTime),
                        returnPickupTime = if (serviceType == "round_trip" && !ride.returnPickupTime.isNullOrEmpty()) {
                            ReservationRequestBuilder.formatTimeForAPI(ride.returnPickupTime)
                        } else {
                            ReservationRequestBuilder.formatTimeForAPI(ride.pickupTime) // Fallback to pickup time if return time not set
                        },
                        returnVehicleId = vehicle.id,
                        returnAffiliateType = "affiliate"
                    )
                    val bookingRatesResult = bookingRatesService.fetchBookingRates(bookingRatesRequest)
                    bookingRatesResult.getOrNull()?.data
                }
                
                val rateArray = bookingRatesData?.rateArray ?: run {
                    Log.w(DebugTags.BookingProcess, "⚠️ Booking rates not available, falling back to vehicle rate breakdown")
                    val rateBreakdown = vehicle.getRateBreakdown(ride.serviceType)
                    val grandTotal = (rateBreakdown?.grandTotal ?: rateBreakdown?.total ?: rateBreakdown?.subTotal ?: 0.0) * ride.noOfVehicles
                    ReservationRequestBuilder.constructRateArrayFromVehicle(rateBreakdown, grandTotal)
                }
                
                val returnRateArray = bookingRatesData?.retrunRateArray
                
                // Get vehicle details
                val vehicleSeats = vehicle.getCapacity().toString()
                val driverInfo = vehicle.driverInformation
                
                // Build AirportOption objects from selected airports
                val pickupAirportOption = if (!ride.selectedPickupAirport.isNullOrEmpty()) {
                    airportService.getAirportByDisplayName(ride.selectedPickupAirport)?.let { airport ->
                        AirportOption(
                            id = airport.id,
                            code = airport.code,
                            name = airport.name,
                            city = airport.city,
                            country = airport.country,
                            lat = airport.lat ?: 0.0,
                            long = airport.long ?: 0.0,
                            formattedName = airport.displayName
                        )
                    }
                } else null
                
                val dropoffAirportOption = if (!ride.selectedDestinationAirport.isNullOrEmpty()) {
                    airportService.getAirportByDisplayName(ride.selectedDestinationAirport)?.let { airport ->
                        AirportOption(
                            id = airport.id,
                            code = airport.code,
                            name = airport.name,
                            city = airport.city,
                            country = airport.country,
                            lat = airport.lat ?: 0.0,
                            long = airport.long ?: 0.0,
                            formattedName = airport.displayName
                        )
                    }
                } else null
                
                // Build AirlineOption objects from selected airlines
                val pickupAirlineOption: AirlineOption? = if (!ride.selectedPickupAirline.isNullOrEmpty()) {
                    airlineService.getAirlineByDisplayName(ride.selectedPickupAirline)?.let { airline ->
                        AirlineOption(
                            id = airline.id,
                            code = airline.code,
                            name = airline.name,
                            country = airline.country ?: "",
                            formattedName = airline.fullDisplayName
                        )
                    }
                } else null
                
                val dropoffAirlineOption: AirlineOption? = if (!ride.selectedDestinationAirline.isNullOrEmpty()) {
                    airlineService.getAirlineByDisplayName(ride.selectedDestinationAirline)?.let { airline ->
                        AirlineOption(
                            id = airline.id,
                            code = airline.code,
                            name = airline.name,
                            country = airline.country ?: "",
                            formattedName = airline.fullDisplayName
                        )
                    }
                } else null
                
                // Determine if pickup/dropoff are airports based on transfer type
                val isPickupAirport = transferType.contains("airport", ignoreCase = true) && 
                                     (transferType.startsWith("airport", ignoreCase = true) || 
                                      transferType == "airport_to_airport")
                val isDropoffAirport = transferType.contains("airport", ignoreCase = true) && 
                                       (transferType.endsWith("airport", ignoreCase = true) || 
                                        transferType == "airport_to_airport")
                
                // Get airport IDs and names - use empty strings when not airport
                val pickupAirportId = if (isPickupAirport && pickupAirportOption != null) {
                    pickupAirportOption.id.toString()
                } else {
                    ""
                }
                val pickupAirportName = if (isPickupAirport) (ride.selectedPickupAirport ?: "") else ""
                // Use airport coordinates from AirportOption if available, otherwise fall back to ride coordinates (matches iOS)
                val pickupAirportLat = if (isPickupAirport) {
                    if (pickupAirportOption != null && pickupAirportOption.lat != 0.0) {
                        pickupAirportOption.lat.toString()
                    } else {
                        // Fall back to ride coordinates when airport option is null or has 0.0 coordinates
                        ride.pickupLat?.toString() ?: ""
                    }
                } else {
                    ""
                }
                val pickupAirportLng = if (isPickupAirport) {
                    if (pickupAirportOption != null && pickupAirportOption.long != 0.0) {
                        pickupAirportOption.long.toString()
                    } else {
                        // Fall back to ride coordinates when airport option is null or has 0.0 coordinates
                        ride.pickupLong?.toString() ?: ""
                    }
                } else {
                    ""
                }
                
                val dropoffAirportId = if (isDropoffAirport && dropoffAirportOption != null) {
                    dropoffAirportOption.id.toString()
                } else {
                    ""
                }
                val dropoffAirportName = if (isDropoffAirport) (ride.selectedDestinationAirport ?: "") else ""
                // Use airport coordinates from AirportOption if available, otherwise fall back to ride coordinates (matches iOS)
                val dropoffAirportLat = if (isDropoffAirport) {
                    if (dropoffAirportOption != null && dropoffAirportOption.lat != 0.0) {
                        dropoffAirportOption.lat.toString()
                    } else {
                        // Fall back to ride coordinates when airport option is null or has 0.0 coordinates
                        ride.destinationLat?.toString() ?: ""
                    }
                } else {
                    ""
                }
                val dropoffAirportLng = if (isDropoffAirport) {
                    if (dropoffAirportOption != null && dropoffAirportOption.long != 0.0) {
                        dropoffAirportOption.long.toString()
                    } else {
                        // Fall back to ride coordinates when airport option is null or has 0.0 coordinates
                        ride.destinationLong?.toString() ?: ""
                    }
                } else {
                    ""
                }
                
                // Build return trip fields for round trips
                val isRoundTrip = serviceType == "round_trip"
                val isReturnPickupAirport = isRoundTrip && returnTransferType.contains("airport", ignoreCase = true) && 
                                           (returnTransferType.startsWith("airport", ignoreCase = true) || 
                                            returnTransferType == "airport_to_airport")
                val isReturnDropoffAirport = isRoundTrip && returnTransferType.contains("airport", ignoreCase = true) && 
                                             (returnTransferType.endsWith("airport", ignoreCase = true) || 
                                              returnTransferType == "airport_to_airport")
                
                val returnPickupAirportOption = if (isRoundTrip && isReturnPickupAirport) dropoffAirportOption else null
                val returnPickupAirportId = if (isRoundTrip && isReturnPickupAirport && returnPickupAirportOption != null) {
                    returnPickupAirportOption.id.toString()
                } else {
                    ""
                }
                val returnPickupAirportName = if (isRoundTrip && isReturnPickupAirport) dropoffAirportName else ""
                // Use airport coordinates from AirportOption if available, otherwise fall back to ride coordinates (matches iOS)
                val returnPickupAirportLat = if (isRoundTrip && isReturnPickupAirport) {
                    if (returnPickupAirportOption != null && returnPickupAirportOption.lat != 0.0) {
                        returnPickupAirportOption.lat.toString()
                    } else {
                        // Fall back to ride coordinates (return pickup = outbound dropoff)
                        ride.destinationLat?.toString() ?: ""
                    }
                } else {
                    ""
                }
                val returnPickupAirportLng = if (isRoundTrip && isReturnPickupAirport) {
                    if (returnPickupAirportOption != null && returnPickupAirportOption.long != 0.0) {
                        returnPickupAirportOption.long.toString()
                    } else {
                        // Fall back to ride coordinates (return pickup = outbound dropoff)
                        ride.destinationLong?.toString() ?: ""
                    }
                } else {
                    ""
                }
                
                val returnDropoffAirportOption = if (isRoundTrip && isReturnDropoffAirport) pickupAirportOption else null
                val returnDropoffAirportId = if (isRoundTrip && isReturnDropoffAirport && returnDropoffAirportOption != null) {
                    returnDropoffAirportOption.id.toString()
                } else {
                    ""
                }
                val returnDropoffAirportName = if (isRoundTrip && isReturnDropoffAirport) pickupAirportName else ""
                // Use airport coordinates from AirportOption if available, otherwise fall back to ride coordinates (matches iOS)
                val returnDropoffAirportLat = if (isRoundTrip && isReturnDropoffAirport) {
                    if (returnDropoffAirportOption != null && returnDropoffAirportOption.lat != 0.0) {
                        returnDropoffAirportOption.lat.toString()
                    } else {
                        // Fall back to ride coordinates (return dropoff = outbound pickup)
                        ride.pickupLat?.toString() ?: ""
                    }
                } else {
                    ""
                }
                val returnDropoffAirportLng = if (isRoundTrip && isReturnDropoffAirport) {
                    if (returnDropoffAirportOption != null && returnDropoffAirportOption.long != 0.0) {
                        returnDropoffAirportOption.long.toString()
                    } else {
                        // Fall back to ride coordinates (return dropoff = outbound pickup)
                        ride.pickupLong?.toString() ?: ""
                    }
                } else {
                    ""
                }
                
                // Return trip locations (reversed from outbound) - set based on whether they're airports (matches web app)
                val returnPickupLocation = if (isRoundTrip) {
                    if (isReturnPickupAirport) "" else (ride.destinationLocation ?: "")
                } else ""
                val returnPickupLat = if (isRoundTrip) {
                    if (isReturnPickupAirport) "" else (ride.destinationLat?.toString() ?: "")
                } else ""
                val returnPickupLng = if (isRoundTrip) {
                    if (isReturnPickupAirport) "" else (ride.destinationLong?.toString() ?: "")
                } else ""
                val returnDropoffLocation = if (isRoundTrip) {
                    if (isReturnDropoffAirport) "" else (ride.pickupLocation ?: "")
                } else ""
                val returnDropoffLat = if (isRoundTrip) {
                    if (isReturnDropoffAirport) "" else (ride.pickupLat?.toString() ?: "")
                } else ""
                val returnDropoffLng = if (isRoundTrip) {
                    if (isReturnDropoffAirport) "" else (ride.pickupLong?.toString() ?: "")
                } else ""
                
                // Return trip date/time
                val returnPickupDate = if (isRoundTrip) ride.pickupDate else ""
                val returnPickupTime = if (isRoundTrip) "12:00 PM" else ""
                
                // Return airline options
                val returnPickupAirlineOption: AirlineOption? = if (isRoundTrip) dropoffAirlineOption else null
                val returnDropoffAirlineOption: AirlineOption? = if (isRoundTrip) pickupAirlineOption else null
                
                // Build EditReservationRequest (similar to CreateReservationRequest but with String types)
                val req = EditReservationRequest(
                    serviceType = serviceType,
                    transferType = transferType,
                    returnTransferType = returnTransferType,
                    numberOfHours = numberOfHours,
                    accountType = "individual",
                    changeIndividualData = "false",
                    passengerName = if (_uiState.value.passengerName.isNotEmpty()) _uiState.value.passengerName else (profileData?.fullName ?: "Guest"),
                    passengerEmail = if (_uiState.value.passengerEmail.isNotEmpty()) _uiState.value.passengerEmail else (profileData?.email ?: ""),
                    passengerCell = if (_uiState.value.passengerMobile.isNotEmpty()) _uiState.value.passengerMobile else (profileData?.mobile ?: ""),
                    passengerCellIsd = profileData?.mobileIsd ?: "+1",
                    passengerCellCountry = profileData?.mobileCountry ?: "us",
                    totalPassengers = ride.noOfPassenger.toString(),
                    luggageCount = ride.noOfLuggage.toString(),
                    bookingInstructions = bookingInstructions ?: "1. Driver - Text on location. Text the client a day before to confirm driver name , cell phone and booking details. Text client with ETA when en route",
                    returnBookingInstructions = returnBookingInstructions ?: "1. Driver - Text on location. Text the client a day before to confirm driver name , cell phone and booking details. Text client with ETA when en route",
                    affiliateType = "affiliate",
                    affiliateId = vehicle.affiliateId?.toString() ?: "",
                    loseAffiliateName = "",
                    loseAffiliatePhone = "",
                    loseAffiliatePhoneIsd = "+1",
                    loseAffiliatePhoneCountry = "us",
                    loseAffiliateEmail = "",
                    vehicleType = "",
                    vehicleTypeName = "",
                    vehicleId = vehicle.id.toString(),
                    vehicleMake = "",
                    vehicleMakeName = "",
                    vehicleModel = "",
                    vehicleModelName = "",
                    vehicleYear = "",
                    vehicleYearName = "",
                    vehicleColor = "",
                    vehicleColorName = "",
                    vehicleLicensePlate = "",
                    vehicleSeats = vehicleSeats,
                    driverId = driverInfo?.id?.toString() ?: "",
                    driverName = driverInfo?.name ?: "1800LIMO Chauffeurs",
                    driverGender = driverInfo?.gender?.lowercase() ?: "male",
                    driverCell = driverInfo?.cellNumber ?: driverInfo?.phone ?: "",
                    driverCellIsd = driverInfo?.cellIsd ?: "+1",
                    driverCellCountry = "us",
                    driverEmail = driverInfo?.email ?: "",
                    driverPhoneType = "",
                    driverImageId = "",
                    vehicleImageId = "",
                    meetGreetChoices = meetGreetId.toString(),
                    meetGreetChoicesName = meetGreetChoicesName ?: "Driver - Text/call when on location",
                    numberOfVehicles = ride.noOfVehicles.toString(),
                    pickupDate = ride.pickupDate,
                    pickupTime = ReservationRequestBuilder.formatTimeForAPI(ride.pickupTime),
                    extraStops = extraStops,
                    returnExtraStops = returnExtraStops,
                    // Pickup fields - set based on whether it's an airport (matches web app)
                    pickup = if (isPickupAirport) "" else (ride.pickupLocation ?: ""),
                    pickupLatitude = if (isPickupAirport) "" else (ride.pickupLat?.toString() ?: ""),
                    pickupLongitude = if (isPickupAirport) "" else (ride.pickupLong?.toString() ?: ""),
                    pickupAirportOption = pickupAirportOption,
                    pickupAirport = pickupAirportId,
                    pickupAirportName = pickupAirportName,
                    pickupAirportLatitude = pickupAirportLat,
                    pickupAirportLongitude = pickupAirportLng,
                    pickupAirlineOption = pickupAirlineOption,
                    pickupAirline = pickupAirlineOption?.id?.toString() ?: "",
                    pickupAirlineName = pickupAirlineOption?.formattedName ?: "",
                    pickupFlight = ride.pickupFlightNumber ?: "",
                    originAirportCity = ride.originAirportCity ?: "",
                    cruisePort = cruisePort,
                    cruiseName = cruiseName,
                    cruiseTime = cruiseTime,
                    // Dropoff fields - set based on whether it's an airport (matches web app)
                    dropoff = if (isDropoffAirport) "" else (ride.destinationLocation ?: ""),
                    dropoffLatitude = if (isDropoffAirport) "" else (ride.destinationLat?.toString() ?: ""),
                    dropoffLongitude = if (isDropoffAirport) "" else (ride.destinationLong?.toString() ?: ""),
                    dropoffAirportOption = dropoffAirportOption,
                    dropoffAirport = dropoffAirportId,
                    dropoffAirportName = dropoffAirportName,
                    dropoffAirportLatitude = dropoffAirportLat,
                    dropoffAirportLongitude = dropoffAirportLng,
                    dropoffAirlineOption = dropoffAirlineOption,
                    dropoffAirline = dropoffAirlineOption?.id?.toString() ?: "",
                    dropoffAirlineName = dropoffAirlineOption?.formattedName ?: "",
                    dropoffFlight = ride.dropoffFlightNumber ?: "",
                    returnMeetGreetChoices = returnMeetGreetId.toString(),
                    returnMeetGreetChoicesName = returnMeetGreetChoicesName ?: "Driver - Text/call when on location",
                    returnPickupDate = returnPickupDate,
                    returnPickupTime = if (isRoundTrip) ReservationRequestBuilder.formatTimeForAPI(returnPickupTime) else "",
                    returnPickup = returnPickupLocation,
                    returnPickupLatitude = returnPickupLat,
                    returnPickupLongitude = returnPickupLng,
                    returnPickupAirportOption = returnPickupAirportOption,
                    returnPickupAirport = returnPickupAirportId,
                    returnPickupAirportName = returnPickupAirportName,
                    returnPickupAirportLatitude = returnPickupAirportLat,
                    returnPickupAirportLongitude = returnPickupAirportLng,
                    returnPickupAirlineOption = returnPickupAirlineOption,
                    returnPickupAirline = returnPickupAirlineOption?.id?.toString() ?: "",
                    returnPickupAirlineName = returnPickupAirlineOption?.formattedName ?: "",
                    returnPickupFlight = ride.returnPickupFlightNumber ?: "",
                    returnCruisePort = "",
                    returnCruiseName = "",
                    returnCruiseTime = "",
                    returnDropoff = returnDropoffLocation,
                    returnDropoffLatitude = returnDropoffLat,
                    returnDropoffLongitude = returnDropoffLng,
                    returnDropoffAirportOption = returnDropoffAirportOption,
                    returnDropoffAirport = returnDropoffAirportId,
                    returnDropoffAirportName = returnDropoffAirportName,
                    returnDropoffAirportLatitude = returnDropoffAirportLat,
                    returnDropoffAirportLongitude = returnDropoffAirportLng,
                    returnDropoffAirlineOption = returnDropoffAirlineOption,
                    returnDropoffAirline = returnDropoffAirlineOption?.id?.toString() ?: "",
                    returnDropoffAirlineName = returnDropoffAirlineOption?.formattedName ?: "",
                    returnDropoffFlight = ride.returnDropoffFlightNumber ?: "",
                    driverLanguages = emptyList(),
                    driverDresses = emptyList(),
                    amenities = emptyList(),
                    chargedAmenities = emptyList(),
                    journeyDistance = distance.toString(),
                    journeyTime = journeyTime.toString(),
                    returnJourneyDistance = if (isRoundTrip) returnDistance.toString() else "",
                    returnJourneyTime = if (isRoundTrip && returnJourneyTime > 0) returnJourneyTime.toString() else "",
                    reservationId = bookingId.toString(),
                    updateType = "",
                    departingAirportCity = ride.originAirportCity ?: "",
                    currency = "USD",
                    isMasterVehicle = (vehicle.isMasterVehicle ?: false).toString(),
                    proceed = "true",
                    rateArray = rateArray,
                    returnRateArray = returnRateArray,
                    grandTotal = if (bookingRatesData != null) {
                        val (_, calculatedGrandTotal) = calculateTotalsFromRateArray(rateArray, serviceType, ride.noOfVehicles, numberOfHours, bookingRatesData.minRateInvolved ?: false)
                        calculatedGrandTotal.toString()
                    } else {
                        val rateBreakdown = vehicle.getRateBreakdown(ride.serviceType)
                        ((rateBreakdown?.grandTotal ?: rateBreakdown?.total ?: rateBreakdown?.subTotal ?: 0.0) * ride.noOfVehicles).toString()
                    },
                    subTotal = if (bookingRatesData != null) {
                        val (calculatedSubTotal, _) = calculateTotalsFromRateArray(rateArray, serviceType, ride.noOfVehicles, numberOfHours, bookingRatesData.minRateInvolved ?: false)
                        calculatedSubTotal.toString()
                    } else {
                        val rateBreakdown = vehicle.getRateBreakdown(ride.serviceType)
                        ((rateBreakdown?.subTotal ?: rateBreakdown?.total ?: 0.0) * ride.noOfVehicles).toString()
                    },
                    returnSubTotal = if (serviceType == "round_trip" && returnRateArray != null) {
                        val (returnSubTotal, _) = calculateTotalsFromRateArray(returnRateArray, serviceType, ride.noOfVehicles, numberOfHours, bookingRatesData?.minRateInvolved ?: false)
                        returnSubTotal.toString()
                    } else null,
                    returnGrandTotal = if (serviceType == "round_trip" && returnRateArray != null) {
                        val (_, returnGrandTotal) = calculateTotalsFromRateArray(returnRateArray, serviceType, ride.noOfVehicles, numberOfHours, bookingRatesData?.minRateInvolved ?: false)
                        returnGrandTotal.toString()
                    } else null,
                    minRateInvolved = (bookingRatesData?.minRateInvolved ?: false).toString(),
                    sharesArray = SharesArrayBuilder.buildSharesArray(
                        rateArray = rateArray,
                        serviceType = serviceType,
                        numberOfHours = numberOfHours,
                        accountType = "individual",
                        returnGrandTotal = if (serviceType == "round_trip") {
                            returnRateArray?.let {
                                val (_, returnGrandTotal) = calculateTotalsFromRateArray(it, serviceType, ride.noOfVehicles, numberOfHours, bookingRatesData?.minRateInvolved ?: false)
                                returnGrandTotal
                            }
                        } else null,
                        minRateInvolved = bookingRatesData?.minRateInvolved ?: false
                    ),
                    returnSharesArray = if (serviceType == "round_trip" && returnRateArray != null) {
                        SharesArrayBuilder.buildSharesArray(
                            rateArray = returnRateArray,
                            serviceType = serviceType,
                            numberOfHours = numberOfHours,
                            accountType = "individual",
                            returnGrandTotal = null,
                            minRateInvolved = bookingRatesData?.minRateInvolved ?: false
                        )
                    } else null,
                    returnAffiliateType = "affiliate",
                    returnAffiliateId = vehicle.affiliateId?.toString() ?: "",
                    returnDistance = if (isRoundTrip) returnDistance.toString() else "",
                    returnVehicleId = if (serviceType == "round_trip") vehicle.id.toString() else "",
                    noOfHours = numberOfHours,
                    fboAddress = "",
                    fboName = "",
                    returnFboAddress = "",
                    returnFboName = "",
                    createdByRole = null
                )
                
                Log.d(DebugTags.BookingProcess, "Built edit reservation request")
                
                // Update reservation API call
                val result = editReservationService.updateReservation(req)
                result.onSuccess { response ->
                    Log.d(DebugTags.BookingProcess, "✅ Reservation updated successfully")
                }.onFailure { error ->
                    Log.e(DebugTags.BookingProcess, "❌ Reservation update failed", error)
                }
                _updateResult.value = result
                
            } catch (e: Exception) {
                Log.e(DebugTags.BookingProcess, "Error building edit reservation request", e)
                _updateResult.value = Result.failure(e)
            } finally {
                _loading.value = false
            }
        }
    }
    
    /**
     * Calculate totals from rate array - matches web buildBookingData() logic exactly
     * Web code: Line 2695-2731 in create-new-booking.component.ts
     */
    private fun calculateTotalsFromRateArray(
        rateArray: BookingRateArray,
        serviceType: String,
        numberOfVehicles: Int,
        bookingHour: String,
        minRateInvolved: Boolean = false
    ): Pair<Double, Double> {

        // ---------- STEP 1: SUM ALL AMOUNT VALUES (WEB LOGIC LINE 2695-2704) ----------
        // Web code sums all 'amount' values from entire rateArray structure first
        var subtotal = 0.0
        
        // Sum all_inclusive_rates amounts
        for (rateItem in rateArray.allInclusiveRates.values) {
            subtotal += rateItem.amount ?: 0.0
        }
        
        // Sum amenities amounts
        for (rateItem in rateArray.amenities.values) {
            subtotal += rateItem.amount ?: 0.0
        }
        
        // Sum taxes amounts
        for (taxItem in rateArray.taxes.values) {
            subtotal += taxItem.amount ?: 0.0
        }
        
        // Sum misc amounts
        for (rateItem in rateArray.misc.values) {
            subtotal += rateItem.amount ?: 0.0
        }

        Log.d(DebugTags.BookingProcess, "📊 Step 1 - Subtotal from all amounts: $subtotal")

        // ---------- STEP 2: CALCULATE BASE RATE FOR ADMIN SHARE (WEB LOGIC LINE 2706-2722) ----------
        // Calculate base_rate from baserates (Base_Rate + ELH_Charges + Stops + Wait + amenities)
        val hoursMultiplier =
            if (serviceType.lowercase() == "charter_tour" && !minRateInvolved) {
                bookingHour.toIntOrNull() ?: 1
            } else {
                1
            }

        // Base Rate (with hours multiplier for charter_tour)
        val baseRateRaw = rateArray.allInclusiveRates["Base_Rate"]?.baserate ?: 0.0
        val baseRateWithHours =
            if (serviceType.lowercase() == "charter_tour" && !minRateInvolved) {
                baseRateRaw * hoursMultiplier
            } else {
                baseRateRaw
            }

        // Add ELH_Charges, Stops, Wait (check for both "ELH_Charges" and "ELH_Charges  " with space)
        val elh = rateArray.allInclusiveRates["ELH_Charges"]?.baserate ?: 
                  rateArray.allInclusiveRates["ELH_Charges  "]?.baserate ?: 0.0
        val stops = rateArray.allInclusiveRates["Stops"]?.baserate ?: 0.0
        val wait = rateArray.allInclusiveRates["Wait"]?.baserate ?: 0.0

        var baseRate = baseRateWithHours + elh + stops + wait

        // Add amenities baserates to base_rate
        for (rateItem in rateArray.amenities.values) {
            baseRate += rateItem.baserate
        }

        Log.d(DebugTags.BookingProcess, "📊 Step 2 - Base rate for admin share calculation: $baseRate")
        Log.d(DebugTags.BookingProcess, "  Base_Rate (with hours): $baseRateWithHours")
        Log.d(DebugTags.BookingProcess, "  ELH_Charges: $elh")
        Log.d(DebugTags.BookingProcess, "  Stops: $stops")
        Log.d(DebugTags.BookingProcess, "  Wait: $wait")
        Log.d(DebugTags.BookingProcess, "  Amenities sum: ${rateArray.amenities.values.sumOf { it.baserate }}")

        // ---------- STEP 3: ADD ADMIN SHARE TO SUBTOTAL (WEB LOGIC LINE 2724-2730) ----------
        // Web code adds admin share based on conditions (25% for individual, 15% for travel_planner, etc.)
        // For individual account type, use 25% (web code line 741-743)
        val adminSharePercent = 25.0
        val adminShare = if (minRateInvolved) {
            0.0
        } else {
            (baseRate * adminSharePercent / 100)
        }

        subtotal += adminShare

        Log.d(DebugTags.BookingProcess, "📊 Step 3 - Admin share (${adminSharePercent}% of base_rate): $adminShare")
        Log.d(DebugTags.BookingProcess, "📊 Final subtotal (amounts + admin share): $subtotal")

        // ---------- STEP 4: CALCULATE GRAND TOTAL (WEB LOGIC - applied in submitForm) ----------
        // Web code applies vehicle multiplier in submitForm (line 2549)
        subtotal = subtotal.roundToTwo()
        val grandTotal = (subtotal * numberOfVehicles).roundToTwo()

        Log.d(DebugTags.BookingProcess, "✅ WEB-MATCHED CALCULATION COMPLETE")
        Log.d(DebugTags.BookingProcess, "  Subtotal: $subtotal")
        Log.d(DebugTags.BookingProcess, "  Number of Vehicles: $numberOfVehicles")
        Log.d(DebugTags.BookingProcess, "  Grand Total: $grandTotal")

        return Pair(subtotal, grandTotal)
    }

    /* ---------- Helper ---------- */
    private fun Double.roundToTwo(): Double =
        kotlin.math.round(this * 100) / 100


    // UI State Management Functions - Initial setters for unidirectional data flow

    /**
     * Validates the current UI state and returns a list of validation error keys
     * Ported from ComprehensiveBookingScreen.validateRequiredFields()
     */
    private fun validateState(): List<String> {
        val errors = mutableListOf<String>()
        val currentState = _uiState.value

        Log.d(DebugTags.BookingProcess, "🔍 Starting form validation...")

        // Service type and transfer type validation
        // Note: ONE_WAY and CITY_TO_CITY are valid default values if explicitly selected by user
        // Only validate if they're truly unset (which shouldn't happen in normal flow)
        // The UI ensures these are always set, so we don't need to validate them here

        if (currentState.pickupDate.isEmpty() || currentState.pickupTime.isEmpty()) {
            errors.add("pickup_datetime")
        }

        // Determine if pickup/dropoff are airports based on transfer type
        val transferType = currentState.transferType.name.lowercase()
        val isPickupAirport = transferType.contains("airport", ignoreCase = true) && 
                             (transferType.startsWith("airport", ignoreCase = true) || 
                              transferType == "airport_to_airport")
        val isDropoffAirport = transferType.contains("airport", ignoreCase = true) && 
                               (transferType.endsWith("airport", ignoreCase = true) || 
                                transferType == "airport_to_airport")
        
        // Location validation - check based on transfer type
        // For non-airport pickups: BOTH location AND coordinates must be present (matches web app validation)
        // This prevents API calls when location is cleared but coordinates remain
        if (!isPickupAirport) {
            if (currentState.pickupLocation.isEmpty() || 
                currentState.rideData.pickupLat == null || 
                currentState.rideData.pickupLong == null) {
                errors.add("pickup_location")
            }
        }
        // For non-airport dropoffs: BOTH location AND coordinates must be present
        if (!isDropoffAirport) {
            if (currentState.dropoffLocation.isEmpty() || 
                currentState.rideData.destinationLat == null || 
                currentState.rideData.destinationLong == null) {
                errors.add("dropoff_location")
            }
        }
        if (isPickupAirport && currentState.rideData.selectedPickupAirport.isEmpty()) {
            errors.add("pickup_airport")
        }
        if (isDropoffAirport && currentState.rideData.selectedDestinationAirport.isEmpty()) {
            errors.add("dropoff_airport")
        }

        // Passenger information validation (matches web app - checks editable fields)
        val passengerName = currentState.passengerName.trim()
        val passengerEmail = currentState.passengerEmail.trim()
        val passengerMobile = currentState.passengerMobile.trim()

        if (passengerName.isEmpty()) {
            errors.add("passenger_name")
        }

        if (passengerEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(passengerEmail).matches()) {
            errors.add("passenger_email")
        }

        if (passengerMobile.isEmpty() || passengerMobile.length < 4) {
            errors.add("passenger_mobile")
        }

        // Transfer type specific validations (matches web app)
        val transferTypeDisplay = currentState.transferType.displayName.lowercase()
        when {
            // Airport pickup validation - airline AND flight are required (matches web app line 2012, 2014)
            transferTypeDisplay.startsWith("airport") -> {
                if (currentState.rideData.selectedPickupAirport.isNullOrEmpty()) {
                    errors.add("pickup_airport")
                }
                // Check if pickup airline is selected (matches web app - pickup_airline_option is required)
                if (currentState.rideData.selectedPickupAirline.isNullOrEmpty()) {
                    errors.add("pickup_airline")
                }
                // Flight number is required for pickup airport (matches web app line 2012)
                if (currentState.rideData.pickupFlightNumber.isNullOrEmpty()) {
                    errors.add("pickup_flight_number")
                }
                if (currentState.rideData.originAirportCity.isNullOrEmpty()) {
                    errors.add("origin_airport_city")
                }
            }
            // Cruise pickup validation - cruise port and cruise ship name are required
            transferTypeDisplay.startsWith("cruise") -> {
                if (currentState.cruisePort.isNullOrEmpty()) {
                    errors.add("cruise_pickup_port")
                }
                if (currentState.cruiseShipName.isNullOrEmpty()) {
                    errors.add("cruise_pickup_ship")
                }
            }
        }

        when {
            // Airport dropoff validation - airline is required, flight is NOT required (matches web app line 1995)
            transferTypeDisplay.endsWith("airport") -> {
                if (currentState.rideData.selectedDestinationAirport.isNullOrEmpty()) {
                    errors.add("dropoff_airport")
                }
                // Check if dropoff airline is selected (matches web app - dropoff_airline_option is required)
                if (currentState.rideData.selectedDestinationAirline.isNullOrEmpty()) {
                    errors.add("dropoff_airline")
                }
                // Flight number is NOT required for dropoff airport (matches web app - line 1993 is commented out)
            }
            // Cruise dropoff validation - cruise port and cruise ship name are required
            // Matches web app: when transfer type includes '_cruise' or 'cruise_', cruise name and port are required
            transferTypeDisplay.contains("cruise", ignoreCase = true) && 
            (transferTypeDisplay.endsWith("cruise", ignoreCase = true) || 
             transferTypeDisplay.endsWith("cruise port", ignoreCase = true)) -> {
                if (currentState.dropoffCruisePort.isNullOrEmpty()) {
                    errors.add("cruise_dropoff_port")
                }
                if (currentState.dropoffCruiseShipName.isNullOrEmpty()) {
                    errors.add("cruise_dropoff_ship")
                }
            }
        }

        // Charter tour validation
        if (currentState.serviceType == ServiceType.CHARTER_TOUR) {
            if (currentState.rideData.bookingHour.isEmpty()) {
                errors.add("charter_hours")
            }
        }

        // Return trip validation (for round trips)
        if (currentState.serviceType == ServiceType.ROUND_TRIP) {
            // Return pickup date and time validation
            if (currentState.returnPickupDate.isEmpty() || currentState.returnPickupTime.isEmpty()) {
                errors.add("return_pickup_datetime")
            }

            // Determine return transfer type for validation
            val returnTransferTypeDisplay = currentState.returnTransferType?.displayName?.lowercase() ?: ""
            val isReturnPickupAirport = returnTransferTypeDisplay.contains("airport", ignoreCase = true) && 
                                       (returnTransferTypeDisplay.startsWith("airport", ignoreCase = true) || 
                                        returnTransferTypeDisplay == "airport_to_airport")
            val isReturnDropoffAirport = returnTransferTypeDisplay.contains("airport", ignoreCase = true) && 
                                        (returnTransferTypeDisplay.endsWith("airport", ignoreCase = true) || 
                                         returnTransferTypeDisplay == "airport_to_airport")
            
            // Return location validation - BOTH location AND coordinates must be present (matches web app validation)
            if (!isReturnPickupAirport) {
                if (currentState.returnPickupLocation.isEmpty() || 
                    currentState.returnPickupLat == null || 
                    currentState.returnPickupLong == null) {
                    errors.add("return_pickup_location")
                }
            }
            if (!isReturnDropoffAirport) {
                if (currentState.returnDropoffLocation.isEmpty() || 
                    currentState.returnDropoffLat == null || 
                    currentState.returnDropoffLong == null) {
                    errors.add("return_dropoff_location")
                }
            }
            
            // Return airport validation
            if (isReturnPickupAirport && currentState.returnPickupAirport.isEmpty()) {
                errors.add("return_pickup_airport")
            }
            if (isReturnDropoffAirport && currentState.returnDropoffAirport.isEmpty()) {
                errors.add("return_dropoff_airport")
            }
            
            // Return transfer type specific validations
            when {
                // Return airport pickup validation
                returnTransferTypeDisplay.startsWith("airport") -> {
                    if (currentState.returnPickupAirport.isEmpty()) {
                        errors.add("return_pickup_airport")
                    }
                    if (currentState.returnPickupAirline.isEmpty()) {
                        errors.add("return_pickup_airline")
                    }
                    if (currentState.returnPickupFlightNumber.isEmpty()) {
                        errors.add("return_pickup_flight_number")
                    }
                    if (currentState.returnOriginAirportCity.isEmpty()) {
                        errors.add("return_origin_airport_city")
                    }
                }
                // Return cruise pickup validation
                returnTransferTypeDisplay.startsWith("cruise") -> {
                    if (currentState.returnCruisePort.isEmpty()) {
                        errors.add("return_cruise_pickup_port")
                    }
                    if (currentState.returnCruiseShipName.isEmpty()) {
                        errors.add("return_cruise_pickup_ship")
                    }
                }
            }
            
            when {
                // Return airport dropoff validation
                returnTransferTypeDisplay.endsWith("airport") -> {
                    if (currentState.returnDropoffAirport.isEmpty()) {
                        errors.add("return_dropoff_airport")
                    }
                    if (currentState.returnDropoffAirline.isEmpty()) {
                        errors.add("return_dropoff_airline")
                    }
                    // Flight number is NOT required for return dropoff airport
                }
                // Return cruise dropoff validation
                returnTransferTypeDisplay.endsWith("cruise") -> {
                    if (currentState.returnCruisePort.isEmpty()) {
                        errors.add("return_cruise_dropoff_port")
                    }
                    if (currentState.returnCruiseShipName.isEmpty()) {
                        errors.add("return_cruise_dropoff_ship")
                    }
                }
            }
        }

        Log.d(DebugTags.BookingProcess, if (errors.isEmpty()) "✅✅✅ All validation checks passed!" else "❌ Validation failed with ${errors.size} errors: $errors")
        return errors
    }

    /**
     * Updates validation errors in the state
     * Public method to allow manual validation trigger before submission (matches web app pattern)
     */
    fun updateValidationErrors() {
        val errors = validateState()
        _uiState.value = _uiState.value.copy(validationErrors = errors)
    }
    
    /**
     * Validate route between pickup and dropoff addresses using Directions API
     * Adds validation errors if route is not possible
     */
    private suspend fun validateAddressRoute(
        pickupLat: Double,
        pickupLong: Double,
        dropoffLat: Double,
        dropoffLong: Double
    ) {
        val (isValid, errorMessage) = directionsService.validateRoute(
            pickupLat = pickupLat,
            pickupLong = pickupLong,
            dropoffLat = dropoffLat,
            dropoffLong = dropoffLong
        )
        
        if (!isValid && errorMessage != null) {
            Log.w(DebugTags.BookingProcess, "❌ Address validation failed: $errorMessage")
            // Add validation errors for both pickup and dropoff locations
            val currentErrors = _uiState.value.addressValidationErrors.toMutableMap()
            currentErrors["pickup_location"] = errorMessage
            currentErrors["dropoff_location"] = errorMessage
            _uiState.value = _uiState.value.copy(addressValidationErrors = currentErrors)
            
            // Also add to validation errors list
            val currentValidationErrors = _uiState.value.validationErrors.toMutableList()
            if (!currentValidationErrors.contains("pickup_location")) {
                currentValidationErrors.add("pickup_location")
            }
            if (!currentValidationErrors.contains("dropoff_location")) {
                currentValidationErrors.add("dropoff_location")
            }
            _uiState.value = _uiState.value.copy(validationErrors = currentValidationErrors)
        } else {
            // Clear validation errors if route is valid
            val currentErrors = _uiState.value.addressValidationErrors.toMutableMap()
            currentErrors.remove("pickup_location")
            currentErrors.remove("dropoff_location")
            _uiState.value = _uiState.value.copy(addressValidationErrors = currentErrors)
            
            // Remove from validation errors list
            val currentValidationErrors = _uiState.value.validationErrors.toMutableList()
            currentValidationErrors.remove("pickup_location")
            currentValidationErrors.remove("dropoff_location")
            _uiState.value = _uiState.value.copy(validationErrors = currentValidationErrors)
        }
    }

    /**
     * Clear specific validation errors when user starts typing in a field
     * This provides immediate feedback that the user is fixing the error
     */
    fun clearValidationErrors(vararg errorKeys: String) {
        val currentErrors = _uiState.value.validationErrors.toMutableList()
        currentErrors.removeAll(errorKeys.toSet())
        _uiState.value = _uiState.value.copy(validationErrors = currentErrors)
    }

    /**
     * Update service type - ensures both flat field and rideData are kept in sync
     */
    fun setServiceType(type: ServiceType) {
        // Clear validation errors immediately when user changes service type
        clearValidationErrors("service_type")
        _uiState.value = _uiState.value.copy(
            serviceType = type,
            rideData = _uiState.value.rideData.copy(serviceType = type.value)
        )
        // Re-validate after service type change (affects validation rules)
        updateValidationErrors()
    }

    /**
     * Update pickup location with address and coordinates - ensures both flat field and rideData are kept in sync
     * If coordinates are not provided, preserves existing coordinates (prevents clearing coordinates during typing)
     */
    fun setPickupLocation(address: String, lat: Double? = null, long: Double? = null) {
        // Clear validation errors immediately when user changes pickup location
        clearValidationErrors("pickup_location", "pickup_coordinates", "locations")
        val currentRideData = _uiState.value.rideData
        // Preserve existing coordinates if new ones are not provided (prevents clearing during typing)
        val finalLat = lat ?: currentRideData.pickupLat
        val finalLong = long ?: currentRideData.pickupLong
        
        _uiState.value = _uiState.value.copy(
            pickupLocation = address,
            rideData = currentRideData.copy(
                pickupLocation = address,
                pickupLat = finalLat,
                pickupLong = finalLong
            ),
            // Clear address validation error when location changes
            addressValidationErrors = _uiState.value.addressValidationErrors.filterKeys { it != "pickup_location" }
        )
        
        // Validate route if both pickup and dropoff coordinates are available
        if (finalLat != null && finalLong != null && 
            currentRideData.destinationLat != null && currentRideData.destinationLong != null) {
            viewModelScope.launch {
                validateAddressRoute(
                    pickupLat = finalLat,
                    pickupLong = finalLong,
                    dropoffLat = currentRideData.destinationLat!!,
                    dropoffLong = currentRideData.destinationLong!!
                )
            }
        }
        
        // Re-validate after coordinate update
        updateValidationErrors()
    }

    /**
     * Update dropoff location with address and coordinates - ensures both flat field and rideData are kept in sync
     * If coordinates are not provided, preserves existing coordinates (prevents clearing coordinates during typing)
     */
    fun setDropoffLocation(address: String, lat: Double? = null, long: Double? = null) {
        // Clear validation errors immediately when user changes dropoff location
        clearValidationErrors("dropoff_location", "dropoff_coordinates", "locations")
        val currentRideData = _uiState.value.rideData
        // Preserve existing coordinates if new ones are not provided (prevents clearing during typing)
        val finalLat = lat ?: currentRideData.destinationLat
        val finalLong = long ?: currentRideData.destinationLong
        
        _uiState.value = _uiState.value.copy(
            dropoffLocation = address,
            rideData = currentRideData.copy(
                destinationLocation = address,
                destinationLat = finalLat,
                destinationLong = finalLong
            ),
            // Clear address validation error when location changes
            addressValidationErrors = _uiState.value.addressValidationErrors.filterKeys { it != "dropoff_location" }
        )
        
        // Validate route if both pickup and dropoff coordinates are available
        if (finalLat != null && finalLong != null && 
            currentRideData.pickupLat != null && currentRideData.pickupLong != null) {
            viewModelScope.launch {
                validateAddressRoute(
                    pickupLat = currentRideData.pickupLat!!,
                    pickupLong = currentRideData.pickupLong!!,
                    dropoffLat = finalLat,
                    dropoffLong = finalLong
                )
            }
        }
        
        // Re-validate after coordinate update
        updateValidationErrors()
    }

    /**
     * Update pickup date - ensures both flat field and rideData are kept in sync
     */
    fun setPickupDate(date: String) {
        // Clear validation errors immediately when user changes pickup date
        clearValidationErrors("pickup_datetime")
        _uiState.value = _uiState.value.copy(
            pickupDate = date,
            rideData = _uiState.value.rideData.copy(pickupDate = date)
        )
        // Re-validate after date change
        updateValidationErrors()
    }

    /**
     * Update passenger count - ensures both flat field and rideData are kept in sync
     */
    fun setPassengerCount(count: Int) {
        val countString = count.toString()
        _uiState.value = _uiState.value.copy(
            passengerCount = countString,
            rideData = _uiState.value.rideData.copy(noOfPassenger = count)
        )
    }

    /**
     * Update transfer type - ensures both flat field and rideData are kept in sync
     */
    fun setTransferType(type: TransferType) {
        // Clear validation errors immediately when user changes transfer type
        clearValidationErrors("transfer_type", "pickup_airport", "dropoff_airport", "pickup_airline", "dropoff_airline", "pickup_flight_number", "origin_airport_city", "cruise_pickup_port", "cruise_pickup_ship", "cruise_dropoff_port", "cruise_dropoff_ship")
        _uiState.value = _uiState.value.copy(
            transferType = type,
            rideData = _uiState.value.rideData.copy(
                pickupType = type.pickupType,
                dropoffType = type.dropoffType
            )
        )
        // Re-validate after transfer type change (affects validation rules)
        updateValidationErrors()
    }

    /**
     * Update booking hours for charter tours - ensures both flat field and rideData are kept in sync
     */
    fun setHours(hours: Int) {
        // Clear validation errors immediately when user changes hours
        clearValidationErrors("charter_hours")
        val hoursString = hours.toString()
        _uiState.value = _uiState.value.copy(
            rideData = _uiState.value.rideData.copy(bookingHour = hoursString)
        )
        // Re-validate after hours change
        updateValidationErrors()
    }

    /**
     * Update number of vehicles - ensures both flat field and rideData are kept in sync
     */
    fun setNumberOfVehicles(count: String) {
        val countInt = count.toIntOrNull() ?: 1
        _uiState.value = _uiState.value.copy(
            rideData = _uiState.value.rideData.copy(noOfVehicles = countInt)
        )
    }

    /**
     * Update luggage count - ensures both flat field and rideData are kept in sync
     */
    fun setLuggageCount(count: Int) {
        val countString = count.toString()
        _uiState.value = _uiState.value.copy(
            luggageCount = countString,
            rideData = _uiState.value.rideData.copy(noOfLuggage = count)
        )
    }

    /**
     * Update pickup time - ensures both flat field and rideData are kept in sync
     */
    fun setPickupTime(time: String) {
        // Clear validation errors immediately when user changes pickup time
        clearValidationErrors("pickup_datetime")
        _uiState.value = _uiState.value.copy(
            pickupTime = time,
            rideData = _uiState.value.rideData.copy(pickupTime = time)
        )
        // Re-validate after time change
        updateValidationErrors()
    }

    /**
     * Update flight information for pickup and dropoff
     */
    fun setFlightInfo(pickupFlightNumber: String? = null, dropoffFlightNumber: String? = null) {
        // Clear validation errors immediately when user changes flight info
        if (pickupFlightNumber != null) {
            clearValidationErrors("pickup_flight_number")
        }
        if (dropoffFlightNumber != null) {
            clearValidationErrors("dropoff_flight_number")
        }
        _uiState.value = _uiState.value.copy(
            rideData = _uiState.value.rideData.copy(
                pickupFlightNumber = pickupFlightNumber,
                dropoffFlightNumber = dropoffFlightNumber
            )
        )
        // Re-validate after flight info change
        updateValidationErrors()
    }

    /**
     * Update airport information for pickup and dropoff fields
     */
    fun setAirportInfo(
        pickupAirport: String? = null,
        dropoffAirport: String? = null,
        originAirportCity: String? = null
    ) {
        // Clear validation errors immediately when user changes airport info
        if (pickupAirport != null) {
            clearValidationErrors("pickup_airport", "pickup_coordinates")
        }
        if (dropoffAirport != null) {
            // Clear both airport and coordinates errors when airport is set
            // This fixes the issue where coordinates error shows even when airport is selected
            clearValidationErrors("dropoff_airport", "dropoff_coordinates")
        }
        if (originAirportCity != null) {
            clearValidationErrors("origin_airport_city")
        }
        _uiState.value = _uiState.value.copy(
            rideData = _uiState.value.rideData.copy(
                selectedPickupAirport = pickupAirport ?: _uiState.value.rideData.selectedPickupAirport,
                selectedDestinationAirport = dropoffAirport ?: _uiState.value.rideData.selectedDestinationAirport,
                originAirportCity = originAirportCity ?: _uiState.value.rideData.originAirportCity
            )
        )
        // Re-validate after airport info change
        updateValidationErrors()
    }

    /**
     * Update airline information for pickup and dropoff
     */
    fun setAirlineInfo(
        pickupAirline: String? = null,
        dropoffAirline: String? = null
    ) {
        // Clear validation errors immediately when user changes airline info
        if (pickupAirline != null) {
            clearValidationErrors("pickup_airline")
        }
        if (dropoffAirline != null) {
            clearValidationErrors("dropoff_airline")
        }
        _uiState.value = _uiState.value.copy(
            rideData = _uiState.value.rideData.copy(
                selectedPickupAirline = pickupAirline ?: _uiState.value.rideData.selectedPickupAirline,
                selectedDestinationAirline = dropoffAirline ?: _uiState.value.rideData.selectedDestinationAirline
            )
        )
        // Re-validate after airline info change
        updateValidationErrors()
    }

    /**
     * Update vehicle selection
     */
    fun setVehicle(vehicle: Vehicle?) {
        _uiState.value = _uiState.value.copy(vehicle = vehicle)
    }

    /**
     * Update profile data
     */
    fun setProfileData(profileData: com.example.limouserapp.data.model.dashboard.ProfileData?) {
        _uiState.value = _uiState.value.copy(profileData = profileData)
        // Re-validate after profile data change
        updateValidationErrors()
    }

    /**
     * Update loading state
     */
    fun setLoading(isLoading: Boolean) {
        _uiState.value = _uiState.value.copy(isLoading = isLoading)
    }

    /**
     * Initialize the UI state from initial ride data and vehicle
     * Called when the screen is first shown to populate the ViewModel state
     */
    fun initialize(rideData: RideData, vehicle: Vehicle? = null, profileData: com.example.limouserapp.data.model.dashboard.ProfileData? = null) {
        val serviceType = ServiceType.fromValue(rideData.serviceType)
        // Normalize pickup/dropoff types - handle "cruise port" -> "cruise"
        val normalizedPickupType = rideData.pickupType.lowercase().replace("cruise port", "cruise").replace("cruise_port", "cruise").trim()
        val normalizedDropoffType = rideData.dropoffType.lowercase().replace("cruise port", "cruise").replace("cruise_port", "cruise").trim()
        val transferType = TransferType.fromTypes(normalizedPickupType, normalizedDropoffType)
        Log.d(DebugTags.BookingProcess, "🎯 Initializing transfer type: pickupType='${rideData.pickupType}' -> normalized='$normalizedPickupType', dropoffType='${rideData.dropoffType}' -> normalized='$normalizedDropoffType', transferType='${transferType.displayName}'")
        
        // Initialize passenger fields from profile data (matches web app)
        val passengerName = profileData?.let { 
            val firstName = it.firstName?.trim() ?: ""
            val lastName = it.lastName?.trim() ?: ""
            val middleName = it.middleName?.trim() ?: ""
            when {
                middleName.isNotEmpty() -> "$firstName $middleName $lastName"
                else -> "$firstName $lastName"
            }.trim()
        } ?: ""
        val passengerEmail = profileData?.email?.trim() ?: ""
        val passengerMobile = profileData?.mobile?.trim() ?: ""

        _uiState.value = BookingUiState(
            rideData = rideData,
            vehicle = vehicle,
            pickupDate = rideData.pickupDate,
            pickupTime = rideData.pickupTime,
            serviceType = serviceType,
            transferType = transferType,
            pickupLocation = rideData.pickupLocation,
            dropoffLocation = rideData.destinationLocation,
            passengerCount = rideData.noOfPassenger.toString(),
            luggageCount = rideData.noOfLuggage.toString(),
            profileData = profileData,
            passengerName = passengerName,
            passengerEmail = passengerEmail,
            passengerMobile = passengerMobile,
            // Initialize return trip fields from rideData if available
            returnPickupDate = rideData.returnPickupDate ?: "",
            returnPickupTime = rideData.returnPickupTime ?: ""
        )

        // Trigger initial validation
        updateValidationErrors()

        Log.d(DebugTags.BookingProcess, "🎯 ViewModel initialized with rideData and vehicle")
    }
    
    /**
     * Update passenger name - ensures validation is triggered
     */
    fun setPassengerName(name: String) {
        // Clear validation errors immediately when user changes passenger name
        clearValidationErrors("passenger_name")
        _uiState.value = _uiState.value.copy(passengerName = name)
        updateValidationErrors()
    }
    
    /**
     * Update passenger email - ensures validation is triggered
     */
    fun setPassengerEmail(email: String) {
        // Clear validation errors immediately when user changes passenger email
        clearValidationErrors("passenger_email")
        _uiState.value = _uiState.value.copy(passengerEmail = email)
        updateValidationErrors()
    }
    
    /**
     * Update passenger mobile - ensures validation is triggered
     */
    fun setPassengerMobile(mobile: String) {
        // Clear validation errors immediately when user changes passenger mobile
        clearValidationErrors("passenger_mobile")
        _uiState.value = _uiState.value.copy(passengerMobile = mobile)
        updateValidationErrors()
    }
    
    /**
     * Update cruise pickup information
     */
    fun setCruisePickupInfo(port: String, shipName: String, arrivalTime: String) {
        // Clear validation errors immediately when user changes cruise fields
        clearValidationErrors("cruise_pickup_port", "cruise_pickup_ship")
        _uiState.value = _uiState.value.copy(
            cruisePort = port,
            cruiseShipName = shipName,
            shipArrivalTime = arrivalTime
        )
        updateValidationErrors()
    }
    
    /**
     * Update cruise dropoff information
     */
    fun setCruiseDropoffInfo(port: String, shipName: String, arrivalTime: String) {
        // Clear validation errors immediately when user changes cruise fields
        clearValidationErrors("cruise_dropoff_port", "cruise_dropoff_ship")
        _uiState.value = _uiState.value.copy(
            dropoffCruisePort = port,
            dropoffCruiseShipName = shipName,
            dropoffShipArrivalTime = arrivalTime
        )
        updateValidationErrors()
    }
    
    /**
     * Update return pickup date
     */
    fun setReturnPickupDate(date: String) {
        // Clear validation error immediately when date is set
        clearValidationErrors("return_pickup_datetime")
        val currentTime = _uiState.value.returnPickupTime
        _uiState.value = _uiState.value.copy(returnPickupDate = date)
        // Only re-validate if both date and time are filled, otherwise the error will be re-added
        if (date.isNotEmpty() && currentTime.isNotEmpty()) {
            // Both are filled, safe to re-validate (won't add error back)
            updateValidationErrors()
        }
        // If only date is filled, don't re-validate yet (time might be empty, which would re-add the error)
    }
    
    /**
     * Update return pickup time
     */
    fun setReturnPickupTime(time: String) {
        // Clear validation error immediately when time is set
        clearValidationErrors("return_pickup_datetime")
        val currentDate = _uiState.value.returnPickupDate
        _uiState.value = _uiState.value.copy(returnPickupTime = time)
        // Only re-validate if both date and time are filled, otherwise the error will be re-added
        if (time.isNotEmpty() && currentDate.isNotEmpty()) {
            // Both are filled, safe to re-validate (won't add error back)
            updateValidationErrors()
        }
        // If only time is filled, don't re-validate yet (date might be empty, which would re-add the error)
    }
    
    /**
     * Update return pickup location
     */
    fun setReturnPickupLocation(address: String, lat: Double? = null, long: Double? = null) {
        clearValidationErrors("return_pickup_location", "return_pickup_coordinates")
        _uiState.value = _uiState.value.copy(
            returnPickupLocation = address,
            returnPickupLat = lat,
            returnPickupLong = long
        )
        updateValidationErrors()
    }
    
    /**
     * Update return dropoff location
     */
    fun setReturnDropoffLocation(address: String, lat: Double? = null, long: Double? = null) {
        clearValidationErrors("return_dropoff_location", "return_dropoff_coordinates")
        _uiState.value = _uiState.value.copy(
            returnDropoffLocation = address,
            returnDropoffLat = lat,
            returnDropoffLong = long
        )
        updateValidationErrors()
    }
    
    /**
     * Update return transfer type
     */
    fun setReturnTransferType(type: TransferType) {
        clearValidationErrors("return_pickup_airport", "return_dropoff_airport", "return_pickup_airline", "return_dropoff_airline", "return_pickup_flight_number", "return_origin_airport_city", "return_cruise_pickup_port", "return_cruise_pickup_ship", "return_cruise_dropoff_port", "return_cruise_dropoff_ship")
        _uiState.value = _uiState.value.copy(returnTransferType = type)
        updateValidationErrors()
    }
    
    /**
     * Update return airport information
     */
    fun setReturnAirportInfo(
        pickupAirport: String? = null,
        dropoffAirport: String? = null
    ) {
        if (pickupAirport != null && pickupAirport.isNotEmpty()) {
            clearValidationErrors("return_pickup_airport")
        }
        if (dropoffAirport != null && dropoffAirport.isNotEmpty()) {
            clearValidationErrors("return_dropoff_airport")
        }
        _uiState.value = _uiState.value.copy(
            returnPickupAirport = pickupAirport ?: _uiState.value.returnPickupAirport,
            returnDropoffAirport = dropoffAirport ?: _uiState.value.returnDropoffAirport
        )
        updateValidationErrors()
    }
    
    /**
     * Update return airline information
     */
    fun setReturnAirlineInfo(
        pickupAirline: String? = null,
        dropoffAirline: String? = null
    ) {
        if (pickupAirline != null && pickupAirline.isNotEmpty()) {
            clearValidationErrors("return_pickup_airline")
        }
        if (dropoffAirline != null && dropoffAirline.isNotEmpty()) {
            clearValidationErrors("return_dropoff_airline")
        }
        _uiState.value = _uiState.value.copy(
            returnPickupAirline = pickupAirline ?: _uiState.value.returnPickupAirline,
            returnDropoffAirline = dropoffAirline ?: _uiState.value.returnDropoffAirline
        )
        updateValidationErrors()
    }
    
    /**
     * Update return flight information
     */
    fun setReturnFlightInfo(
        pickupFlightNumber: String? = null,
        originAirportCity: String? = null
    ) {
        if (pickupFlightNumber != null && pickupFlightNumber.isNotEmpty()) {
            clearValidationErrors("return_pickup_flight_number")
        }
        if (originAirportCity != null && originAirportCity.isNotEmpty()) {
            clearValidationErrors("return_origin_airport_city")
        }
        _uiState.value = _uiState.value.copy(
            returnPickupFlightNumber = pickupFlightNumber ?: _uiState.value.returnPickupFlightNumber,
            returnOriginAirportCity = originAirportCity ?: _uiState.value.returnOriginAirportCity
        )
        updateValidationErrors()
    }
    
    /**
     * Update return cruise information
     */
    fun setReturnCruiseInfo(port: String, shipName: String, arrivalTime: String) {
        clearValidationErrors("return_cruise_pickup_port", "return_cruise_pickup_ship", "return_cruise_dropoff_port", "return_cruise_dropoff_ship")
        _uiState.value = _uiState.value.copy(
            returnCruisePort = port,
            returnCruiseShipName = shipName,
            returnShipArrivalTime = arrivalTime
        )
        updateValidationErrors()
    }
    
    /**
     * Update return cruise pickup information
     */
    fun setReturnCruisePickupInfo(port: String, shipName: String, arrivalTime: String) {
        // Clear validation errors immediately when user changes cruise fields
        clearValidationErrors("return_cruise_pickup_port", "return_cruise_pickup_ship")
        _uiState.value = _uiState.value.copy(
            returnCruisePort = port,
            returnCruiseShipName = shipName,
            returnShipArrivalTime = arrivalTime
        )
        updateValidationErrors()
    }
    
    /**
     * Update return cruise dropoff information
     */
    fun setReturnCruiseDropoffInfo(port: String, shipName: String, arrivalTime: String) {
        // Clear validation errors immediately when user changes cruise fields
        clearValidationErrors("return_cruise_dropoff_port", "return_cruise_dropoff_ship")
        _uiState.value = _uiState.value.copy(
            returnCruisePort = port,
            returnCruiseShipName = shipName,
            returnShipArrivalTime = arrivalTime
        )
        updateValidationErrors()
    }
}



