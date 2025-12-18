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
    fun fetchBookingRates(
        ride: RideData, 
        vehicle: Vehicle,
        isEditMode: Boolean = false,
        editBookingId: Int? = null,
        hasExtraStops: Boolean = false,
        extraStops: List<com.example.limouserapp.data.model.booking.ExtraStopRequest> = emptyList(),
        returnExtraStops: List<com.example.limouserapp.data.model.booking.ExtraStopRequest> = emptyList()
    ) {
        viewModelScope.launch {
            _bookingRatesLoading.value = true
            Log.d(DebugTags.BookingProcess, "===========================================")
            Log.d(DebugTags.BookingProcess, "🔄 FETCHING BOOKING RATES")
            Log.d(DebugTags.BookingProcess, "===========================================")
            Log.d(DebugTags.BookingProcess, "Edit Mode: $isEditMode")
            Log.d(DebugTags.BookingProcess, "Ride Data: serviceType=${ride.serviceType}, pickup=${ride.pickupLocation}")
            Log.d(DebugTags.BookingProcess, "Vehicle ID: ${vehicle.id}, Name: ${vehicle.name}")
            
            try {
                // Map service type and transfer type
                val serviceType = ReservationRequestBuilder.mapServiceType(ride.serviceType)
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
                    
                    // Extract waypoints from extra stops (matches iOS calculateDistanceWithExtraStops)
                    val waypoints = extraStops
                        .filter { it.latitude != null && it.longitude != null }
                        .map { Pair(it.latitude!!, it.longitude!!) }
                        .takeIf { it.isNotEmpty() }
                    
                    val (distance, duration) = if (ride.pickupLat != null && ride.pickupLong != null && 
                        ride.destinationLat != null && ride.destinationLong != null) {
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
                        Pair(0, 0)
                    }
                    
                    // CRITICAL FIX: For round trips, ALSO recalculate return distance (matches iOS line 2402-2406)
                    // Extract waypoints from return extra stops
                    val returnWaypoints = returnExtraStops
                        .filter { it.latitude != null && it.longitude != null }
                        .map { Pair(it.latitude!!, it.longitude!!) }
                        .takeIf { it.isNotEmpty() }
                    
                    val (returnDistance, returnDuration) = if (serviceType == "round_trip" && 
                        ride.destinationLat != null && ride.destinationLong != null &&
                        ride.pickupLat != null && ride.pickupLong != null) {
                        Log.d(DebugTags.BookingProcess, "🔄 RECALCULATING RETURN DISTANCE AS WELL")
                        val result = directionsService.calculateDistance(
                            ride.destinationLat, ride.destinationLong,
                            ride.pickupLat, ride.pickupLong,
                            returnWaypoints
                        )
                        Log.d(DebugTags.BookingProcess, "📏 RECALCULATED RETURN DISTANCE: ${result.first} meters (with ${returnWaypoints?.size ?: 0} waypoints)")
                        Log.d(DebugTags.BookingProcess, "⏱️ RECALCULATED RETURN DURATION: ${result.second} seconds")
                        result
                    } else {
                        Pair(0, 0)
                    }
                    
                    // Validate distance before API call
                    if (distance <= 0) {
                        Log.w(DebugTags.BookingProcess, "⚠️ WARNING: Distance is 0 or invalid. Rates may be incorrect.")
                    }
                    
                    // Build booking rates request
                    val numberOfHours = ride.bookingHour ?: "0"
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
                        pickupTime = ReservationRequestBuilder.formatTimeForAPI(ride.pickupTime),
                        returnPickupTime = ReservationRequestBuilder.formatTimeForAPI(ride.pickupTime),
                        returnVehicleId = vehicle.id,
                        returnAffiliateType = "affiliate"
                    )
                    
                    Log.d(DebugTags.BookingProcess, "📍 Extra Stops: ${extraStops.size}")
                    Log.d(DebugTags.BookingProcess, "📍 Return Extra Stops: ${returnExtraStops.size}")
                    
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
                        returnPickupTime = ReservationRequestBuilder.formatTimeForAPI(ride.pickupTime),
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
                val pickupAirportLat = if (isPickupAirport && pickupAirportOption != null) {
                    pickupAirportOption.lat.toString()
                } else {
                    ""
                }
                val pickupAirportLng = if (isPickupAirport && pickupAirportOption != null) {
                    pickupAirportOption.long.toString()
                } else {
                    ""
                }
                
                val dropoffAirportId = if (isDropoffAirport && dropoffAirportOption != null) {
                    dropoffAirportOption.id.toString()
                } else {
                    ""
                }
                val dropoffAirportName = if (isDropoffAirport) (ride.selectedDestinationAirport ?: "") else ""
                val dropoffAirportLat = if (isDropoffAirport && dropoffAirportOption != null) {
                    dropoffAirportOption.lat.toString()
                } else {
                    ""
                }
                val dropoffAirportLng = if (isDropoffAirport && dropoffAirportOption != null) {
                    dropoffAirportOption.long.toString()
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
                val returnPickupAirportLat = if (isRoundTrip && isReturnPickupAirport && returnPickupAirportOption != null) {
                    returnPickupAirportOption.lat.toString()
                } else {
                    ""
                }
                val returnPickupAirportLng = if (isRoundTrip && isReturnPickupAirport && returnPickupAirportOption != null) {
                    returnPickupAirportOption.long.toString()
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
                val returnDropoffAirportLat = if (isRoundTrip && isReturnDropoffAirport && returnDropoffAirportOption != null) {
                    returnDropoffAirportOption.lat.toString()
                } else {
                    ""
                }
                val returnDropoffAirportLng = if (isRoundTrip && isReturnDropoffAirport && returnDropoffAirportOption != null) {
                    returnDropoffAirportOption.long.toString()
                } else {
                    ""
                }
                
                // Return trip locations (reversed from outbound)
                val returnPickupLocation = if (isRoundTrip) ride.destinationLocation else ""
                val returnPickupLat = if (isRoundTrip) (ride.destinationLat?.toString() ?: "") else ""
                val returnPickupLng = if (isRoundTrip) (ride.destinationLong?.toString() ?: "") else ""
                val returnDropoffLocation = if (isRoundTrip) ride.pickupLocation else ""
                val returnDropoffLat = if (isRoundTrip) (ride.pickupLat?.toString() ?: "") else ""
                val returnDropoffLng = if (isRoundTrip) (ride.pickupLong?.toString() ?: "") else ""
                
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
                    passengerName = profileData?.fullName ?: "Guest",
                    passengerEmail = profileData?.email ?: "",
                    passengerCell = profileData?.mobile ?: "",
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
                    pickup = ride.pickupLocation,
                    pickupLatitude = ride.pickupLat?.toString() ?: "",
                    pickupLongitude = ride.pickupLong?.toString() ?: "",
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
                    dropoff = ride.destinationLocation,
                    dropoffLatitude = ride.destinationLat?.toString() ?: "",
                    dropoffLongitude = ride.destinationLong?.toString() ?: "",
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
                        returnPickupTime = ReservationRequestBuilder.formatTimeForAPI(ride.pickupTime),
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
                val pickupAirportLat = if (isPickupAirport && pickupAirportOption != null) {
                    pickupAirportOption.lat.toString()
                } else {
                    ""
                }
                val pickupAirportLng = if (isPickupAirport && pickupAirportOption != null) {
                    pickupAirportOption.long.toString()
                } else {
                    ""
                }
                
                val dropoffAirportId = if (isDropoffAirport && dropoffAirportOption != null) {
                    dropoffAirportOption.id.toString()
                } else {
                    ""
                }
                val dropoffAirportName = if (isDropoffAirport) (ride.selectedDestinationAirport ?: "") else ""
                val dropoffAirportLat = if (isDropoffAirport && dropoffAirportOption != null) {
                    dropoffAirportOption.lat.toString()
                } else {
                    ""
                }
                val dropoffAirportLng = if (isDropoffAirport && dropoffAirportOption != null) {
                    dropoffAirportOption.long.toString()
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
                val returnPickupAirportLat = if (isRoundTrip && isReturnPickupAirport && returnPickupAirportOption != null) {
                    returnPickupAirportOption.lat.toString()
                } else {
                    ""
                }
                val returnPickupAirportLng = if (isRoundTrip && isReturnPickupAirport && returnPickupAirportOption != null) {
                    returnPickupAirportOption.long.toString()
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
                val returnDropoffAirportLat = if (isRoundTrip && isReturnDropoffAirport && returnDropoffAirportOption != null) {
                    returnDropoffAirportOption.lat.toString()
                } else {
                    ""
                }
                val returnDropoffAirportLng = if (isRoundTrip && isReturnDropoffAirport && returnDropoffAirportOption != null) {
                    returnDropoffAirportOption.long.toString()
                } else {
                    ""
                }
                
                // Return trip locations
                val returnPickupLocation = if (isRoundTrip) ride.destinationLocation else ""
                val returnPickupLat = if (isRoundTrip) (ride.destinationLat?.toString() ?: "") else ""
                val returnPickupLng = if (isRoundTrip) (ride.destinationLong?.toString() ?: "") else ""
                val returnDropoffLocation = if (isRoundTrip) ride.pickupLocation else ""
                val returnDropoffLat = if (isRoundTrip) (ride.pickupLat?.toString() ?: "") else ""
                val returnDropoffLng = if (isRoundTrip) (ride.pickupLong?.toString() ?: "") else ""
                
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
                    passengerName = profileData?.fullName ?: "Guest",
                    passengerEmail = profileData?.email ?: "",
                    passengerCell = profileData?.mobile ?: "",
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
                    pickup = ride.pickupLocation,
                    pickupLatitude = ride.pickupLat?.toString() ?: "",
                    pickupLongitude = ride.pickupLong?.toString() ?: "",
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
                    dropoff = ride.destinationLocation,
                    dropoffLatitude = ride.destinationLat?.toString() ?: "",
                    dropoffLongitude = ride.destinationLong?.toString() ?: "",
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
     * Calculate totals from rate array - matches iOS calculateTotalsFromRateArray()
     */
    private fun calculateTotalsFromRateArray(
        rateArray: BookingRateArray,
        serviceType: String,
        numberOfVehicles: Int,
        bookingHour: String,
        minRateInvolved: Boolean = false
    ): Pair<Double, Double> {
        var totalBaserate = 0.0
        var allInclusiveBaserate = 0.0
        
        // Get hours multiplier for charter tours
        // If min_rate_involved is true, don't multiply by hours (use base rate as-is)
        // iOS checks for "Charter Tour" (with space) but serviceType passed is "charter_tour" (mapped)
        // Check for both to handle the mapped value
        val hoursMultiplier = if ((serviceType == "Charter Tour" || serviceType.lowercase() == "charter_tour") && !minRateInvolved) {
            bookingHour.toIntOrNull() ?: 1
        } else {
            1
        }
        
        Log.d(DebugTags.BookingProcess, "📊 PROCESSING ALL_INCLUSIVE_RATES - Total items: ${rateArray.allInclusiveRates.size}")
        Log.d(DebugTags.BookingProcess, "Min Rate Involved: $minRateInvolved, Hours Multiplier: $hoursMultiplier")
        
        // Sum all baserate values from ALL items in all_inclusive_rates
        for ((key, rateItem) in rateArray.allInclusiveRates) {
            // iOS checks for "Charter Tour" (with space) but serviceType passed is "charter_tour" (mapped)
            // Check for both to handle the mapped value
            // If min_rate_involved is true, don't multiply by hours
            val adjustedBaserate = if ((serviceType == "Charter Tour" || serviceType.lowercase() == "charter_tour") && key == "Base_Rate" && !minRateInvolved) {
                rateItem.baserate * hoursMultiplier
            } else {
                rateItem.baserate
            }
            totalBaserate += adjustedBaserate
            allInclusiveBaserate += adjustedBaserate
            Log.d(DebugTags.BookingProcess, "  ✓ $key: baserate=${rateItem.baserate}, adjusted=$adjustedBaserate")
        }
        
        Log.d(DebugTags.BookingProcess, "📦 PROCESSING AMENITIES - Total items: ${rateArray.amenities.size}")
        for ((key, rateItem) in rateArray.amenities) {
            totalBaserate += rateItem.baserate
            Log.d(DebugTags.BookingProcess, "  ✓ $key: ${rateItem.baserate}")
        }
        
        Log.d(DebugTags.BookingProcess, "💰 PROCESSING TAXES - Total items: ${rateArray.taxes.size}")
        for ((key, taxItem) in rateArray.taxes) {
            // Use amount instead of baserate for taxes (amount is the actual tax value)
            totalBaserate += taxItem.amount
            Log.d(DebugTags.BookingProcess, "  ✓ $key: baserate=${taxItem.baserate}, amount=${taxItem.amount}, using amount=${taxItem.amount}")
        }
        
        Log.d(DebugTags.BookingProcess, "📋 PROCESSING MISC - Total items: ${rateArray.misc.size}")
        for ((key, rateItem) in rateArray.misc) {
            totalBaserate += rateItem.baserate
            Log.d(DebugTags.BookingProcess, "  ✓ $key: ${rateItem.baserate}")
        }
        
        // Calculate subtotal: total baserate + 25% of all_inclusive_rates baserate
        val twentyFivePercentOfAllInclusive = allInclusiveBaserate * 0.25
        val calculatedSubtotal = totalBaserate + twentyFivePercentOfAllInclusive
        
        // Grand total = subtotal × number of vehicles
        val calculatedGrandTotal = calculatedSubtotal * numberOfVehicles
        
        Log.d(DebugTags.BookingProcess, "📊 RATE CALCULATION BREAKDOWN:")
        Log.d(DebugTags.BookingProcess, "Service Type: $serviceType")
        Log.d(DebugTags.BookingProcess, "Hours Multiplier: $hoursMultiplier")
        Log.d(DebugTags.BookingProcess, "All Inclusive Baserate (adjusted): $allInclusiveBaserate")
        Log.d(DebugTags.BookingProcess, "Total Baserate (all categories, adjusted): $totalBaserate")
        Log.d(DebugTags.BookingProcess, "25% of All Inclusive: $twentyFivePercentOfAllInclusive")
        Log.d(DebugTags.BookingProcess, "Calculated Subtotal: $calculatedSubtotal")
        Log.d(DebugTags.BookingProcess, "Number of Vehicles: $numberOfVehicles")
        Log.d(DebugTags.BookingProcess, "Calculated Grand Total: $calculatedGrandTotal (Subtotal × $numberOfVehicles)")
        
        return Pair(calculatedSubtotal, calculatedGrandTotal)
    }
}



