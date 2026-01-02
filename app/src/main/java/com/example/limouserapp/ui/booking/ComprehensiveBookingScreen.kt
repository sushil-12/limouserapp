package com.example.limouserapp.ui.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.ScrollState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.limouserapp.data.model.booking.RideData
import com.example.limouserapp.data.model.booking.Vehicle
import com.example.limouserapp.data.model.booking.DriverInformation
import com.example.limouserapp.data.model.booking.VehicleDetails
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.LimoWhite
import com.example.limouserapp.ui.theme.GoogleSansFamily
import com.example.limouserapp.ui.viewmodel.ReservationViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import android.util.Log
import com.example.limouserapp.ui.utils.DebugTags
import java.text.SimpleDateFormat
import java.util.*
import com.example.limouserapp.data.model.booking.Airline
import com.example.limouserapp.data.model.booking.Airport
import com.example.limouserapp.ui.components.SearchableBottomSheet
import com.example.limouserapp.ui.booking.components.DatePickerDialog
import com.example.limouserapp.ui.booking.components.TimePickerDialog
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.limouserapp.data.model.dashboard.ProfileData
import com.example.limouserapp.ui.booking.comprehensivebooking.ExtraStop
import com.example.limouserapp.ui.booking.comprehensivebooking.sections.AccountsInfoSection
import com.example.limouserapp.ui.booking.comprehensivebooking.sections.BookingDetailsSection
import com.example.limouserapp.ui.booking.comprehensivebooking.sections.SpecialInstructionsSection
import com.example.limouserapp.ui.booking.comprehensivebooking.sections.PickupSection
import com.example.limouserapp.ui.booking.comprehensivebooking.sections.DropoffSection
import com.example.limouserapp.ui.booking.comprehensivebooking.sections.ReturnJourneySection
import com.example.limouserapp.ui.booking.comprehensivebooking.sections.TransportationDetailsSection
import com.example.limouserapp.ui.booking.comprehensivebooking.sections.ExtraStopsSection
import com.example.limouserapp.ui.booking.comprehensivebooking.sections.BookingSummarySection
import com.example.limouserapp.ui.booking.comprehensivebooking.sections.DistanceInformationSection
import com.example.limouserapp.ui.booking.comprehensivebooking.getReversedTransferType
import com.example.limouserapp.ui.booking.comprehensivebooking.prefillExtraStopsFromEditData
import com.example.limouserapp.ui.booking.comprehensivebooking.validateExtraStop
import com.example.limouserapp.ui.booking.comprehensivebooking.hasExtraStops
import com.example.limouserapp.ui.booking.comprehensivebooking.getEffectiveOutboundPickupCoordinate
import com.example.limouserapp.ui.booking.comprehensivebooking.getEffectiveOutboundDropoffCoordinate
import com.example.limouserapp.ui.booking.comprehensivebooking.toExtraStopRequests
import com.example.limouserapp.ui.booking.comprehensivebooking.toExtraStopRequestsWithTownComparison
import com.example.limouserapp.ui.booking.comprehensivebooking.getMeetAndGreetForTransferType
import com.example.limouserapp.ui.booking.comprehensivebooking.getSpecialInstructionsForTransferType
import com.example.limouserapp.ui.booking.comprehensivebooking.getTransferTypeDisplayName
import com.example.limouserapp.ui.booking.comprehensivebooking.getServiceTypeDisplayName
import com.example.limouserapp.ui.viewmodel.ServiceType
import com.example.limouserapp.ui.viewmodel.TransferType
import com.example.limouserapp.ui.components.ShimmerBox
import androidx.compose.foundation.shape.CircleShape

/**
 * Comprehensive Booking Screen
 * Matches iOS ComprehensiveBookingView - full booking form with all fields, editable with bottom sheets
 */
@Composable
fun ComprehensiveBookingScreen(
    rideData: RideData,
    vehicle: Vehicle,
    onDismiss: () -> Unit,
    onSuccess: (com.example.limouserapp.data.model.booking.ReservationData?) -> Unit,
    isEditMode: Boolean = false,
    editBookingId: Int? = null,
    isRepeatMode: Boolean = false,
    repeatBookingId: Int? = null,
    isReturnFlow: Boolean = false
) {
    val vm: ReservationViewModel = hiltViewModel()
    val loading by vm.loading.collectAsState()
    val result by vm.result.collectAsState()
    val bookingRatesLoading by vm.bookingRatesLoading.collectAsState()
    val bookingRatesData by vm.bookingRatesData.collectAsState()
    val bookingRatesCurrency by vm.bookingRatesCurrency.collectAsState()

    // Edit mode state
    val editData by vm.editData.collectAsState()
    val editLoading by vm.editLoading.collectAsState()
    val editError by vm.editError.collectAsState()
    val updateResult by vm.updateResult.collectAsState()

    // UI State from ViewModel (MVI/MVVM)
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    // Repeat mode state
    var isLoadingRepeatData by remember { mutableStateOf(false) }
    var hasLoadedExistingRates by remember { mutableStateOf(false) } // Track if rates were loaded from existing booking


    // Load edit data when in edit mode
    LaunchedEffect(isEditMode, editBookingId) {
        if (isEditMode && editBookingId != null) {
            // Only fetch if we don't have edit data yet, or if the booking ID changed
            if (editData == null || editData?.reservationId != editBookingId) {
                vm.fetchEditReservation(editBookingId)
            } else {
                Log.d(DebugTags.BookingProcess, "⏭️ EDIT MODE: Edit data already loaded for booking ID: $editBookingId")
            }
        } else {
            Log.d(DebugTags.BookingProcess, "⏭️ EDIT MODE: Conditions not met - isEditMode=$isEditMode, editBookingId=$editBookingId")
        }
    }

    // CRITICAL FIX: Track if booking was initiated to prevent old results from triggering success
    var bookingInitiated by remember { mutableStateOf(false) }

    // Reset result when screen is shown to prevent old booking results from triggering success
    LaunchedEffect(Unit) {
        Log.d(DebugTags.BookingProcess, "🔄 Resetting booking result when screen is shown")
        vm.resetResult()
        bookingInitiated = false
    }

    // Service type and transfer type (local mutable state for edit mode compatibility)
    // Initialize from rideData first (before ViewModel state is set)
    var selectedServiceType by remember { 
        mutableStateOf(getServiceTypeDisplayName(uiState.rideData.serviceType))
    }
    var selectedTransferType by remember { 
        mutableStateOf(getTransferTypeDisplayName(uiState.rideData))
    }

    // Sync with ViewModel state changes AFTER initialization
    LaunchedEffect(uiState.serviceType) { 
        val calculatedServiceType = getServiceTypeDisplayName(uiState.rideData.serviceType)
        if (selectedServiceType != uiState.serviceType.displayName && uiState.serviceType.displayName.isNotEmpty()) {
            selectedServiceType = uiState.serviceType.displayName
        } else if (selectedServiceType != calculatedServiceType && calculatedServiceType.isNotEmpty()) {
            selectedServiceType = calculatedServiceType
        }
    }
    LaunchedEffect(uiState.transferType) { 
        val calculatedTransferType = getTransferTypeDisplayName(uiState.rideData)
        // Only sync if ViewModel state is different and matches rideData, or if ViewModel was explicitly set
        if (uiState.transferType.displayName != calculatedTransferType && uiState.transferType.displayName.isNotEmpty()) {
            // ViewModel was explicitly set, use it
            selectedTransferType = uiState.transferType.displayName
            Log.d(DebugTags.BookingProcess, "🔄 Transfer type synced from ViewModel: ${uiState.transferType.displayName}")
        } else if (selectedTransferType != calculatedTransferType && calculatedTransferType.isNotEmpty()) {
            // Use calculated value from rideData
            selectedTransferType = calculatedTransferType
            Log.d(DebugTags.BookingProcess, "🔄 Transfer type synced from rideData: $calculatedTransferType")
        }
    }
    
    // Also sync when rideData changes (in case transfer type needs to be recalculated)
    LaunchedEffect(uiState.rideData.pickupType, uiState.rideData.dropoffType) {
        val calculatedTransferType = getTransferTypeDisplayName(uiState.rideData)
        if (selectedTransferType != calculatedTransferType && calculatedTransferType.isNotEmpty()) {
            Log.d(DebugTags.BookingProcess, "🔄 Updating transfer type from rideData: $selectedTransferType -> $calculatedTransferType")
            selectedTransferType = calculatedTransferType
            vm.setTransferType(TransferType.fromDisplayName(calculatedTransferType))
        }
    }

    // Editable state variables (local mutable state that syncs with ViewModel)
    var pickupDate by remember { mutableStateOf(uiState.pickupDate) }
    var pickupTime by remember { mutableStateOf(uiState.pickupTime) }
    var pickupLocation by remember { mutableStateOf(uiState.pickupLocation) }
    // Initialize dropoffLocation from rideData first, then sync with ViewModel
    var dropoffLocation by remember { mutableStateOf(rideData.destinationLocation.ifEmpty { uiState.dropoffLocation }) }
    var numberOfVehicles by remember { mutableStateOf(uiState.rideData.noOfVehicles) }
    var passengerCount by remember { mutableStateOf(uiState.passengerCount) }
    var luggageCount by remember { mutableStateOf(uiState.luggageCount) }
    var selectedHours by remember { mutableStateOf(uiState.rideData.bookingHour ?: "2 hours minimum") }

    // Sync with ViewModel state changes
    LaunchedEffect(uiState.pickupDate) { pickupDate = uiState.pickupDate }
    LaunchedEffect(uiState.pickupTime) { pickupTime = uiState.pickupTime }
    LaunchedEffect(uiState.pickupLocation) { pickupLocation = uiState.pickupLocation }
    LaunchedEffect(uiState.dropoffLocation) { dropoffLocation = uiState.dropoffLocation }
    LaunchedEffect(uiState.rideData.noOfVehicles) { numberOfVehicles = uiState.rideData.noOfVehicles }
    LaunchedEffect(uiState.passengerCount) { passengerCount = uiState.passengerCount }
    LaunchedEffect(uiState.luggageCount) { luggageCount = uiState.luggageCount }
    LaunchedEffect(uiState.rideData.bookingHour) { selectedHours = uiState.rideData.bookingHour ?: "2 hours minimum" }

    // Current ride data and vehicle (local mutable state for edit mode handling)
    var currentRideData by remember { mutableStateOf(uiState.rideData) }
    var currentVehicle by remember { mutableStateOf(uiState.vehicle ?: vehicle) }

    // Profile data state (needed for ViewModel initialization)
    var profileData by remember { mutableStateOf<ProfileData?>(null) }

    // Track if ViewModel has been initialized
    var isViewModelInitialized by remember { mutableStateOf(false) }
    
    // Return trip state variables (matches iOS) - Declared early so they can be used in initialization
    var selectedReturnServiceType by remember { mutableStateOf<String?>(null) }
    var selectedReturnTransferType by remember { mutableStateOf<String?>(null) }
    var selectedReturnMeetAndGreet by remember { mutableStateOf<String?>(null) }
    var returnPickupDate by remember { mutableStateOf("") }
    var returnPickupTime by remember { mutableStateOf("") }
    var returnPickupLocation by remember { mutableStateOf("") }
    var returnDropoffLocation by remember { mutableStateOf("") }
    var returnPickupLat by remember { mutableStateOf<Double?>(null) }
    var returnPickupLong by remember { mutableStateOf<Double?>(null) }
    var returnDropoffLat by remember { mutableStateOf<Double?>(null) }
    var returnDropoffLong by remember { mutableStateOf<Double?>(null) }
    var returnPickupFlightNumber by remember { mutableStateOf("") }
    var returnDropoffFlightNumber by remember { mutableStateOf("") }
    var returnOriginAirportCity by remember { mutableStateOf("") }
    var returnCruiseShipName by remember { mutableStateOf("") }
    var returnShipArrivalTime by remember { mutableStateOf("") }
    var returnCruisePort by remember { mutableStateOf("") }
    var returnSpecialInstructions by remember { mutableStateOf("") }
    var returnNumberOfVehicles by remember { mutableStateOf(1) }
    var selectedReturnHours by remember { mutableStateOf("2 hours minimum") }

    // Initialize ViewModel state with initial data
    LaunchedEffect(Unit) {
        Log.d(DebugTags.BookingProcess, "🎯 Initializing ViewModel state")
        // Calculate correct transfer type from rideData before initializing
        val calculatedTransferType = getTransferTypeDisplayName(rideData)
        Log.d(DebugTags.BookingProcess, "🎯 Calculated transfer type from rideData: $calculatedTransferType (pickupType=${rideData.pickupType}, dropoffType=${rideData.dropoffType})")
        
        // Update selectedTransferType immediately from rideData
        selectedTransferType = calculatedTransferType
        
        // Prefill return trip data from rideData if available (from previous screens)
        if (rideData.serviceType == "round_trip" || selectedServiceType == "Round Trip") {
            if (rideData.returnPickupDate != null && rideData.returnPickupDate.isNotEmpty()) {
                returnPickupDate = rideData.returnPickupDate
                Log.d(DebugTags.BookingProcess, "✅ Prefilled return pickup date from rideData: ${rideData.returnPickupDate}")
            }
            if (rideData.returnPickupTime != null && rideData.returnPickupTime.isNotEmpty()) {
                returnPickupTime = rideData.returnPickupTime
                Log.d(DebugTags.BookingProcess, "✅ Prefilled return pickup time from rideData: ${rideData.returnPickupTime}")
            }
            // Note: Return locations and coordinates would need to be passed via rideData if available
            // For now, they'll be auto-filled based on reversed transfer type below
        }
        
        vm.initialize(rideData, vehicle, profileData)
        
        // Prefill dropoff location from rideData if available and sync to ViewModel
        if (rideData.destinationLocation.isNotEmpty() && dropoffLocation.isEmpty()) {
            dropoffLocation = rideData.destinationLocation
            // Sync to ViewModel with coordinates if available
            vm.setDropoffLocation(
                rideData.destinationLocation,
                rideData.destinationLat,
                rideData.destinationLong
            )
            Log.d(DebugTags.BookingProcess, "✅ Prefilled dropoff location from rideData: ${rideData.destinationLocation}")
        }
        
        // Sync return trip data to ViewModel if prefilled
        if (rideData.serviceType == "round_trip" || selectedServiceType == "Round Trip") {
            if (returnPickupDate.isNotEmpty()) {
                vm.setReturnPickupDate(returnPickupDate)
            }
            if (returnPickupTime.isNotEmpty()) {
                vm.setReturnPickupTime(returnPickupTime)
            }
        }
        
        isViewModelInitialized = true
    }

    // Load repeat data when in repeat mode
    LaunchedEffect(isRepeatMode, repeatBookingId) {
        if (isRepeatMode && repeatBookingId != null) {
            Log.d(DebugTags.BookingProcess, "===========================================")
            Log.d(DebugTags.BookingProcess, "🔄 REPEAT MODE: Loading repeat data for booking ID: $repeatBookingId")
            Log.d(DebugTags.BookingProcess, "isReturnFlow: $isReturnFlow")
            Log.d(DebugTags.BookingProcess, "===========================================")
            isLoadingRepeatData = true
            // Fetch booking data using the same API as edit mode
            vm.fetchEditReservation(repeatBookingId)
        }
    }

    // Services accessed via ViewModel (matches ScheduleRideBottomSheet pattern)
    val airlineService = vm.airlineService
    val airportService = vm.airportService
    val dashboardApi = vm.dashboardApi
    val directionsService = vm.directionsService
    val meetGreetService = vm.meetGreetService
    
    // PlacesService for town comparison in extra stops (matches web app checkExtraStopInTown)
    val context = LocalContext.current
    val placesService = remember { com.example.limouserapp.data.PlacesService(context) }

    var profileLoading by remember { mutableStateOf(false) }

    // Collect airline, airport, and meet & greet data
    val airlines by airlineService.airlines.collectAsState()
    val airports by airportService.airports.collectAsState()
    val airlinesLoading by airlineService.isLoading.collectAsState()
    val airportsLoading by airportService.isLoading.collectAsState()
    val meetGreetChoices by meetGreetService.meetGreetChoices.collectAsState()
    val meetGreetLoading by meetGreetService.isLoading.collectAsState()

    // Service type and transfer type options (matches iOS)
    val serviceTypes = listOf("One Way", "Round Trip", "Charter Tour")
    val transferTypes = listOf(
        "City to City",
        "City to Airport",
        "Airport to City",
        "Airport to Airport",
        "City to Cruise Port",
        "Airport to Cruise Port",
        "Cruise Port to City",
        "Cruise Port to Airport"
    )

    // Hours for charter tours (editable)
    val hoursOptions = listOf("2 hours minimum", "3 hours", "4 hours", "5 hours", "6 hours", "8 hours", "10 hours", "12 hours")

    // Form state variables
    var pickupFlightNumber by remember { mutableStateOf(rideData.pickupFlightNumber ?: "") }
    var dropoffFlightNumber by remember { mutableStateOf(rideData.dropoffFlightNumber ?: "") }
    var originAirportCity by remember { mutableStateOf(rideData.originAirportCity ?: "") }
    var cruiseShipName by remember { mutableStateOf("") }
    var shipArrivalTime by remember { mutableStateOf("") }
    var cruisePort by remember { mutableStateOf("") }

    // Initialize meet & greet and special instructions based on initial transfer type (matches web app)
    var specialInstructions by remember {
        mutableStateOf(getSpecialInstructionsForTransferType(selectedTransferType))
    }
    var selectedMeetAndGreet by remember {
        mutableStateOf(getMeetAndGreetForTransferType(selectedTransferType))
    }

    // Airline selection state
    var selectedPickupAirline by remember { mutableStateOf<Airline?>(null) }
    var selectedDropoffAirline by remember { mutableStateOf<Airline?>(null) }
    var selectedReturnPickupAirline by remember { mutableStateOf<Airline?>(null) }
    var selectedReturnDropoffAirline by remember { mutableStateOf<Airline?>(null) }

    // Airport selection state
    var selectedPickupAirport by remember { mutableStateOf<Airport?>(null) }
    var selectedDropoffAirport by remember { mutableStateOf<Airport?>(null) }
    var selectedReturnPickupAirport by remember { mutableStateOf<Airport?>(null) }
    var selectedReturnDropoffAirport by remember { mutableStateOf<Airport?>(null) }

    // Return trip state variables are now declared earlier (before initialization LaunchedEffect)

    // Extra stops state (matches iOS extraStops and returnExtraStops)
    var extraStops by remember { mutableStateOf<List<ExtraStop>>(emptyList()) }
    var returnExtraStops by remember { mutableStateOf<List<ExtraStop>>(emptyList()) }
    var showInvalidLocationDialog by remember { mutableStateOf(false) }
    var invalidLocationMessage by remember { mutableStateOf("") }
    var invalidLocationDismissJob by remember { mutableStateOf<Job?>(null) }

    // Calculate distances using Directions API (moved earlier so it's available in LaunchedEffect blocks)
    // Initialize from rideData if available (optimization - use distance from previous screens)
    var outboundDistance by remember { 
        mutableStateOf<Pair<String, String>?>(null)
    }
    var returnDistance by remember { mutableStateOf<Pair<String, String>?>(null) } // (distance, duration)
    var distancesLoading by remember { mutableStateOf(false) }
    
    // Initialize distance from rideData if available (optimization - use distance from previous screens)
    LaunchedEffect(rideData.distanceMeters, rideData.durationSeconds, directionsService) {
        if (rideData.distanceMeters != null && rideData.durationSeconds != null && outboundDistance == null) {
            // Use distance from previous screen (TimeSelectionScreen or ScheduleRideBottomSheet)
            val (distanceText, _) = directionsService.formatDistance(rideData.distanceMeters.toInt())
            val (durationText, _) = directionsService.formatDuration(rideData.durationSeconds)
            outboundDistance = Pair(distanceText, durationText)
            Log.d(DebugTags.BookingProcess, "✅ Initialized outbound distance from rideData: $distanceText, duration: $durationText")
        }
    }

    // Travel info caching (matches iOS cachedTravelInfo and cachedReturnTravelInfo)
    var cachedTravelInfo by remember { mutableStateOf<String?>(null) }
    var isCalculatingTravel by remember { mutableStateOf(false) }
    var cachedReturnTravelInfo by remember { mutableStateOf<String?>(null) }
    var isCalculatingReturnTravel by remember { mutableStateOf(false) }

    // Cache keys to track when to recalculate
    var lastOutboundPickupCoord by remember { mutableStateOf<Pair<Double?, Double?>?>(null) }
    var lastOutboundDropoffCoord by remember { mutableStateOf<Pair<Double?, Double?>?>(null) }
    var lastOutboundExtraStops by remember { mutableStateOf<List<Pair<Double, Double>>?>(null) }
    var lastReturnPickupCoord by remember { mutableStateOf<Pair<Double?, Double?>?>(null) }
    var lastReturnDropoffCoord by remember { mutableStateOf<Pair<Double?, Double?>?>(null) }
    var lastReturnExtraStops by remember { mutableStateOf<List<Pair<Double, Double>>?>(null) }

    /**
     * Validate all required fields based on service type and transfer type (matches web app validation)
     * Returns true if all required fields are filled, false otherwise
     */

    // Validation errors from ViewModel - used to show red indicators on fields
    val validationErrors = uiState.validationErrors

    // Helper function to map validation error keys to field-specific error states
    fun hasError(errorKey: String): Boolean = validationErrors.contains(errorKey)
    
    // Helper function to get error message for a validation key
    fun getErrorMessage(errorKey: String): String? {
        return if (hasError(errorKey)) {
            when (errorKey) {
                "service_type" -> "Service type is required"
                "transfer_type" -> "Transfer type is required"
                "pickup_datetime" -> "Pickup date and time are required"
                "pickup_location" -> "Pickup location is required"
                "dropoff_location" -> "Dropoff location is required"
                "pickup_coordinates" -> "Please select a valid pickup location with coordinates"
                "dropoff_coordinates" -> "Please select a valid dropoff location with coordinates"
                "pickup_airport" -> "Pickup airport is required"
                "dropoff_airport" -> "Dropoff airport is required"
                "pickup_flight_number" -> "Pickup flight number is required"
                "origin_airport_city" -> "Origin airport city is required"
                "passenger_name" -> "Passenger name is required"
                "passenger_email" -> "Valid passenger email is required"
                "passenger_mobile" -> "Valid passenger mobile number is required"
                "profile_data" -> "Please complete your profile information in Account Settings"
                "charter_hours" -> "Charter hours are required"
                // Return trip validation errors
                "return_pickup_datetime" -> "Return pickup date and time are required"
                "return_pickup_location" -> "Return pickup location is required"
                "return_dropoff_location" -> "Return dropoff location is required"
                "return_pickup_coordinates" -> "Please select a valid return pickup location with coordinates"
                "return_dropoff_coordinates" -> "Please select a valid return dropoff location with coordinates"
                "return_pickup_airport" -> "Return pickup airport is required"
                "return_dropoff_airport" -> "Return dropoff airport is required"
                "return_pickup_airline" -> "Return pickup airline is required"
                "return_dropoff_airline" -> "Return dropoff airline is required"
                "return_pickup_flight_number" -> "Return pickup flight number is required"
                "return_origin_airport_city" -> "Return origin airport city is required"
                "return_cruise_pickup_port" -> "Return cruise port is required"
                "return_cruise_pickup_ship" -> "Return cruise ship name is required"
                "return_cruise_dropoff_port" -> "Return cruise port is required"
                "return_cruise_dropoff_ship" -> "Return cruise ship name is required"
                else -> null
            }
        } else null
    }

    // Prefill all fields from edit data when it's loaded (matches iOS prefillDataFromEditResponse)
    LaunchedEffect(editData, isEditMode, airports, airlines) {
        if (isEditMode && editData != null) {
            val data = editData!!
            Log.d(DebugTags.BookingProcess, "🔄 PREFILLING DATA FROM EDIT RESPONSE")

            // Create vehicle from edit data (matches iOS createVehicleFromEditData)
            val createdVehicle = Vehicle(
                id = data.vehicleId ?: 0,
                name = data.vehicleTypeName ?: "Vehicle",
                image = if (data.vehicleImages != null && data.vehicleImages.isNotEmpty()) data.vehicleImages.first() else null,
                vehicleImages = data.vehicleImages ?: emptyList(),
                capacity = data.vehicleSeatsName ?: data.vehicleSeats ?: 4,
                passenger = data.vehicleSeatsName ?: data.vehicleSeats ?: 4,
                luggage = data.luggageCount,
                price = null, // Will be fetched from booking rates
                affiliateId = data.affiliateId,
                rateBreakdownOneWay = null,
                rateBreakdownRoundTrip = null,
                rateBreakdownCharterTour = null,
                driverInformation = DriverInformation(
                    id = data.driverId,
                    name = data.driverName ?: "Driver",
                    gender = data.driverGender ?: "Male",
                    email = data.driverEmail,
                    phone = data.driverCell ?: "",
                    cellIsd = data.driverCellIsd ?: "+1",
                    cellNumber = data.driverCell ?: "",
                    starRating = "4.5", // Default
                    background = "Background Checked", // Default
                    dress = "Business Casual", // Default
                    languages = "English", // Default
                    experience = "5+ Years", // Default
                    imageUrl = data.driverImage ?: "",
                    insuranceLimit = "1M" // Default
                ),
                amenities = null,
                vehicleDetails = VehicleDetails(
                    make = data.vehicleMakeName ?: "Unknown",
                    model = data.vehicleModelName ?: "Unknown",
                    year = data.vehicleYearName ?: "Unknown"
                ),
                isMasterVehicle = data.affiliateType == "unassigned"
            )
            currentVehicle = createdVehicle
            Log.d(DebugTags.BookingProcess, "✅ Created vehicle from edit data: ${createdVehicle.name}, ID: ${createdVehicle.id}")

            // Log edit data received from API
            Log.d(DebugTags.BookingProcess, "📥 EDIT DATA RECEIVED FROM API:")
            Log.d(DebugTags.BookingProcess, "  pickupDate: '${data.pickupDate}'")
            Log.d(DebugTags.BookingProcess, "  pickupTime: '${data.pickupTime}'")
            Log.d(DebugTags.BookingProcess, "  pickupFlight: '${data.pickupFlight}'")
            Log.d(DebugTags.BookingProcess, "  dropoffFlight: '${data.dropoffFlight}'")
            Log.d(DebugTags.BookingProcess, "  originAirportCity: '${data.originAirportCity}'")
            Log.d(DebugTags.BookingProcess, "  passengerName: '${data.passengerName}'")
            Log.d(DebugTags.BookingProcess, "  passengerEmail: '${data.passengerEmail}'")
            Log.d(DebugTags.BookingProcess, "  passengerCell: '${data.passengerCell}'")
            Log.d(DebugTags.BookingProcess, "  passengerCellIsd: '${data.passengerCellIsd}'")
            Log.d(DebugTags.BookingProcess, "  passengerCellCountry: '${data.passengerCellCountry}'")
            
            // Update rideData with coordinates from edit data
            currentRideData = currentRideData.copy(
                serviceType = data.serviceType,
                pickupType = when {
                    data.transferType.contains("city", ignoreCase = true) && data.transferType.startsWith("city", ignoreCase = true) -> "city"
                    data.transferType.contains("airport", ignoreCase = true) && data.transferType.startsWith("airport", ignoreCase = true) -> "airport"
                    data.transferType.contains("cruise", ignoreCase = true) && data.transferType.startsWith("cruise", ignoreCase = true) -> "cruise"
                    else -> currentRideData.pickupType
                },
                dropoffType = when {
                    data.transferType.contains("city", ignoreCase = true) && data.transferType.endsWith("city", ignoreCase = true) -> "city"
                    data.transferType.contains("airport", ignoreCase = true) && data.transferType.endsWith("airport", ignoreCase = true) -> "airport"
                    data.transferType.contains("cruise", ignoreCase = true) && data.transferType.endsWith("cruise", ignoreCase = true) -> "cruise"
                    else -> currentRideData.dropoffType
                },
                pickupDate = data.pickupDate ?: "",
                pickupTime = data.pickupTime ?: "",
                pickupLocation = data.pickup ?: "",
                destinationLocation = data.dropoff ?: "",
                pickupLat = data.pickupLatitude,
                pickupLong = data.pickupLongitude,
                destinationLat = data.dropoffLatitude,
                destinationLong = data.dropoffLongitude,
                noOfPassenger = data.totalPassengers,
                noOfLuggage = data.luggageCount,
                noOfVehicles = data.numberOfVehicles ?: 1,
                bookingHour = data.numberOfHours?.toString() ?: "0",
                selectedPickupAirport = data.pickupAirport ?: "",
                selectedDestinationAirport = data.dropoffAirport ?: "",
                selectedPickupAirline = data.pickupAirline ?: "",
                selectedDestinationAirline = data.dropoffAirline ?: "",
                pickupFlightNumber = data.pickupFlight ?: "",
                dropoffFlightNumber = data.dropoffFlight ?: "",
                returnPickupFlightNumber = data.returnPickupFlight ?: "",
                returnDropoffFlightNumber = data.returnDropoffFlight ?: "",
                originAirportCity = data.originAirportCity ?: ""
            )

            // Prefill service type and transfer type
            selectedServiceType = when (data.serviceType.lowercase()) {
                "one_way", "oneway" -> "One Way"
                "round_trip" -> "Round Trip"
                "charter_tour", "chartertour" -> "Charter Tour"
                else -> selectedServiceType
            }

            selectedTransferType = when (data.transferType.lowercase()) {
                "city_to_city" -> "City to City"
                "city_to_airport" -> "City to Airport"
                "airport_to_city" -> "Airport to City"
                "airport_to_airport" -> "Airport to Airport"
                "city_to_cruise" -> "City to Cruise Port"
                "airport_to_cruise" -> "Airport to Cruise Port"
                "cruise_to_city" -> "Cruise Port to City"
                "cruise_to_airport" -> "Cruise Port to Airport"
                else -> selectedTransferType
            }

            // Prefill dates and times
            pickupDate = data.pickupDate ?: ""
            pickupTime = data.pickupTime ?: ""
            returnPickupDate = data.returnPickupDate ?: ""
            returnPickupTime = data.returnPickupTime ?: ""
            Log.d(DebugTags.BookingProcess, "✅ Prefilled pickup date from edit data: '${data.pickupDate}'")
            Log.d(DebugTags.BookingProcess, "✅ Prefilled pickup time from edit data: '${data.pickupTime}'")

            // Prefill locations
            pickupLocation = data.pickup ?: ""
            dropoffLocation = data.dropoff ?: ""
            returnPickupLocation = data.returnPickup ?: ""
            returnDropoffLocation = data.returnDropoff ?: ""

            // Prefill passenger and luggage counts
            passengerCount = data.totalPassengers.toString()
            luggageCount = data.luggageCount.toString()
            numberOfVehicles = data.numberOfVehicles ?: 1

            // Prefill flight numbers
            pickupFlightNumber = data.pickupFlight ?: ""
            dropoffFlightNumber = data.dropoffFlight ?: ""
            returnPickupFlightNumber = data.returnPickupFlight ?: ""
            returnDropoffFlightNumber = data.returnDropoffFlight ?: ""
            Log.d(DebugTags.BookingProcess, "✅ Prefilled pickup flight from edit data: '${data.pickupFlight}'")
            Log.d(DebugTags.BookingProcess, "✅ Prefilled dropoff flight from edit data: '${data.dropoffFlight}'")

            // Prefill origin airport city
            originAirportCity = data.originAirportCity ?: ""
            Log.d(DebugTags.BookingProcess, "✅ Prefilled origin airport city from edit data: '${data.originAirportCity}'")

            // Prefill cruise details
            cruiseShipName = data.cruiseName ?: ""
            shipArrivalTime = data.cruiseTime ?: ""
            returnCruiseShipName = data.returnCruiseName ?: ""
            returnShipArrivalTime = data.returnCruiseTime ?: ""

            // Prefill airports from edit data (matches iOS updateAirportSelectionsFromEditData)
            if (airports.isNotEmpty()) {
                // Pickup airport
                if (!data.pickupAirport.isNullOrEmpty()) {
                    val pickupAirportId = data.pickupAirport.toIntOrNull()
                    if (pickupAirportId != null) {
                        val airport = airports.find { it.id == pickupAirportId }
                        if (airport != null) {
                            selectedPickupAirport = airport
                            Log.d(DebugTags.BookingProcess, "✅ Set pickup airport: ${airport.displayName}")
                        }
                    }
                }

                // Dropoff airport
                if (!data.dropoffAirport.isNullOrEmpty()) {
                    val dropoffAirportId = data.dropoffAirport.toIntOrNull()
                    if (dropoffAirportId != null) {
                        val airport = airports.find { it.id == dropoffAirportId }
                        if (airport != null) {
                            selectedDropoffAirport = airport
                            Log.d(DebugTags.BookingProcess, "✅ Set dropoff airport: ${airport.displayName}")
                        }
                    }
                }

                // Return pickup airport
                if (!data.returnPickupAirport.isNullOrEmpty()) {
                    val returnPickupAirportId = data.returnPickupAirport.toIntOrNull()
                    if (returnPickupAirportId != null) {
                        val airport = airports.find { it.id == returnPickupAirportId }
                        if (airport != null) {
                            selectedReturnPickupAirport = airport
                            Log.d(DebugTags.BookingProcess, "✅ Set return pickup airport: ${airport.displayName}")
                        }
                    }
                }

                // Return dropoff airport
                if (!data.returnDropoffAirport.isNullOrEmpty()) {
                    val returnDropoffAirportId = data.returnDropoffAirport.toIntOrNull()
                    if (returnDropoffAirportId != null) {
                        val airport = airports.find { it.id == returnDropoffAirportId }
                        if (airport != null) {
                            selectedReturnDropoffAirport = airport
                            Log.d(DebugTags.BookingProcess, "✅ Set return dropoff airport: ${airport.displayName}")
                        }
                    }
                }
            }

            // Prefill airlines from edit data (matches iOS updateAirlineSelectionsFromEditData)
            if (airlines.isNotEmpty()) {
                // Pickup airline
                if (!data.pickupAirline.isNullOrEmpty()) {
                    val pickupAirlineId = data.pickupAirline.toIntOrNull()
                    if (pickupAirlineId != null) {
                        val airline = airlines.find { it.id == pickupAirlineId }
                        if (airline != null) {
                            selectedPickupAirline = airline
                            Log.d(DebugTags.BookingProcess, "✅ Set pickup airline: ${airline.displayName}")
                        }
                    }
                }

                // Dropoff airline
                if (!data.dropoffAirline.isNullOrEmpty()) {
                    val dropoffAirlineId = data.dropoffAirline.toIntOrNull()
                    if (dropoffAirlineId != null) {
                        val airline = airlines.find { it.id == dropoffAirlineId }
                        if (airline != null) {
                            selectedDropoffAirline = airline
                            Log.d(DebugTags.BookingProcess, "✅ Set dropoff airline: ${airline.displayName}")
                        }
                    }
                }

                // Return pickup airline
                if (!data.returnPickupAirline.isNullOrEmpty()) {
                    val returnPickupAirlineId = data.returnPickupAirline.toIntOrNull()
                    if (returnPickupAirlineId != null) {
                        val airline = airlines.find { it.id == returnPickupAirlineId }
                        if (airline != null) {
                            selectedReturnPickupAirline = airline
                            Log.d(DebugTags.BookingProcess, "✅ Set return pickup airline: ${airline.displayName}")
                        }
                    }
                }

                // Return dropoff airline
                if (!data.returnDropoffAirline.isNullOrEmpty()) {
                    val returnDropoffAirlineId = data.returnDropoffAirline.toIntOrNull()
                    if (returnDropoffAirlineId != null) {
                        val airline = airlines.find { it.id == returnDropoffAirlineId }
                        if (airline != null) {
                            selectedReturnDropoffAirline = airline
                            Log.d(DebugTags.BookingProcess, "✅ Set return dropoff airline: ${airline.displayName}")
                        }
                    }
                }
            }

            // Prefill special instructions
            specialInstructions = data.bookingInstructions ?: specialInstructions
            returnSpecialInstructions = "" // TODO: Add return booking instructions if available

            // Prefill extra stops from edit data (matches iOS prefillExtraStopsFromEditData)
            val (prefilledOutboundStops, prefilledReturnStops) = prefillExtraStopsFromEditData(data, selectedServiceType)
            extraStops = prefilledOutboundStops
            returnExtraStops = prefilledReturnStops

            // Prefill number of hours
            if (data.numberOfHours != null) {
                val hours = data.numberOfHours
                selectedHours = if (hours == 2) "2 hours minimum" else "$hours hours"
            }

            // Prefill meet and greet - find matching choice from API or use default
            val meetGreetName = data.meetGreetChoiceName ?: "Driver - Text/call when on location"
            selectedMeetAndGreet = if (meetGreetChoices.isNotEmpty()) {
                // Try to find exact match first
                meetGreetChoices.find { it.message.equals(meetGreetName, ignoreCase = true) }?.message
                    ?: meetGreetChoices.firstOrNull()?.message
                    ?: meetGreetName
            } else {
                meetGreetName
            }

            Log.d(DebugTags.BookingProcess, "✅ Prefilled all fields from edit data")
            
            // CRITICAL: Sync all prefilled fields to ViewModel so validation works correctly
            Log.d(DebugTags.BookingProcess, "🔄 Syncing prefilled data to ViewModel...")
            
            // Sync service type and transfer type
            vm.setServiceType(ServiceType.fromDisplayName(selectedServiceType))
            vm.setTransferType(TransferType.fromDisplayName(selectedTransferType))
            
            // Sync dates and times
            vm.setPickupDate(pickupDate)
            vm.setPickupTime(pickupTime)
            
            // Sync airport selections FIRST (before locations) so validation knows if dropoff is an airport
            vm.setAirportInfo(
                pickupAirport = selectedPickupAirport?.displayName,
                dropoffAirport = selectedDropoffAirport?.displayName,
                originAirportCity = originAirportCity
            )
            
            // Sync airline selections
            vm.setAirlineInfo(
                pickupAirline = selectedPickupAirline?.displayName,
                dropoffAirline = selectedDropoffAirline?.displayName
            )
            
            // Sync locations with coordinates
            // For airport dropoffs, use airport coordinates if location coordinates are null/0.0
            val dropoffAirport = selectedDropoffAirport
            val dropoffLat = if (dropoffAirport != null && 
                                 (data.dropoffLatitude == null || data.dropoffLatitude == 0.0)) {
                dropoffAirport.lat
            } else {
                data.dropoffLatitude
            }
            val dropoffLong = if (dropoffAirport != null && 
                                  (data.dropoffLongitude == null || data.dropoffLongitude == 0.0)) {
                dropoffAirport.long
            } else {
                data.dropoffLongitude
            }
            
            // For airport pickups, use airport coordinates if location coordinates are null/0.0
            val pickupAirport = selectedPickupAirport
            val pickupLat = if (pickupAirport != null && 
                                (data.pickupLatitude == null || data.pickupLatitude == 0.0)) {
                pickupAirport.lat
            } else {
                data.pickupLatitude
            }
            val pickupLong = if (pickupAirport != null && 
                                 (data.pickupLongitude == null || data.pickupLongitude == 0.0)) {
                pickupAirport.long
            } else {
                data.pickupLongitude
            }
            
            vm.setPickupLocation(pickupLocation, pickupLat, pickupLong)
            vm.setDropoffLocation(dropoffLocation, dropoffLat, dropoffLong)
            
            // Sync passenger and luggage counts
            vm.setPassengerCount(passengerCount.toIntOrNull() ?: 1)
            vm.setLuggageCount(luggageCount.toIntOrNull() ?: 0)
            vm.setNumberOfVehicles(numberOfVehicles.toString())
            
            // Sync hours for charter tours
            if (selectedServiceType == "Charter Tour") {
                val hours = selectedHours.replace(" hours minimum", "").replace(" hours", "").trim().toIntOrNull() ?: 2
                vm.setHours(hours)
            }
            
            // Sync flight info - CRITICAL: Ensure this happens AFTER all data is prefilled
            vm.setFlightInfo(
                pickupFlightNumber = pickupFlightNumber,
                dropoffFlightNumber = dropoffFlightNumber
            )
            Log.d(DebugTags.BookingProcess, "✅ Synced flight info to ViewModel - pickup: '$pickupFlightNumber', dropoff: '$dropoffFlightNumber'")
            
            // CRITICAL: Ensure date and time are synced AFTER prefilling (they're synced above, but ensure they're correct)
            // Double-check that ViewModel has the correct values
            if (pickupDate != uiState.pickupDate) {
                vm.setPickupDate(pickupDate)
                Log.d(DebugTags.BookingProcess, "🔄 Re-synced pickupDate to ViewModel: '$pickupDate'")
            }
            if (pickupTime != uiState.pickupTime) {
                vm.setPickupTime(pickupTime)
                Log.d(DebugTags.BookingProcess, "🔄 Re-synced pickupTime to ViewModel: '$pickupTime'")
            }
            
            // Ensure origin airport city is synced to ViewModel (it's already synced in setAirportInfo above, but verify)
            if (originAirportCity != uiState.rideData.originAirportCity) {
                vm.setAirportInfo(originAirportCity = originAirportCity)
                Log.d(DebugTags.BookingProcess, "🔄 Re-synced originAirportCity to ViewModel: '$originAirportCity'")
            }
            
            Log.d(DebugTags.BookingProcess, "✅ Final ViewModel state after prefilling:")
            Log.d(DebugTags.BookingProcess, "  uiState.pickupDate: '${uiState.pickupDate}'")
            Log.d(DebugTags.BookingProcess, "  uiState.pickupTime: '${uiState.pickupTime}'")
            Log.d(DebugTags.BookingProcess, "  uiState.rideData.pickupFlightNumber: '${uiState.rideData.pickupFlightNumber}'")
            Log.d(DebugTags.BookingProcess, "  uiState.rideData.dropoffFlightNumber: '${uiState.rideData.dropoffFlightNumber}'")
            Log.d(DebugTags.BookingProcess, "  uiState.rideData.originAirportCity: '${uiState.rideData.originAirportCity}'")
            
            // Sync passenger fields from edit data (if available)
            if (!data.passengerName.isNullOrEmpty()) {
                vm.setPassengerName(data.passengerName)
                Log.d(DebugTags.BookingProcess, "✅ Synced passenger name from edit data: ${data.passengerName}")
            }
            if (!data.passengerEmail.isNullOrEmpty()) {
                vm.setPassengerEmail(data.passengerEmail)
                Log.d(DebugTags.BookingProcess, "✅ Synced passenger email from edit data: ${data.passengerEmail}")
            }
            // Handle passenger cell - check if it's not null, not empty, and not "0"
            val passengerCellValue = data.passengerCell?.trim()
            if (!passengerCellValue.isNullOrEmpty() && passengerCellValue != "0") {
                vm.setPassengerMobile(passengerCellValue)
                Log.d(DebugTags.BookingProcess, "✅ Synced passenger mobile from edit data: '$passengerCellValue'")
            } else {
                Log.d(DebugTags.BookingProcess, "⚠️ Passenger cell from edit data is null, empty, or '0': '$passengerCellValue'")
                // Try to get from passengerCellIsd + passengerCell if available
                val fullCellNumber = if (!data.passengerCellIsd.isNullOrEmpty() && !passengerCellValue.isNullOrEmpty() && passengerCellValue != "0") {
                    "${data.passengerCellIsd}$passengerCellValue"
                } else {
                    null
                }
                if (fullCellNumber != null) {
                    vm.setPassengerMobile(fullCellNumber)
                    Log.d(DebugTags.BookingProcess, "✅ Synced passenger mobile (with ISD) from edit data: '$fullCellNumber'")
                }
            }
            
            // Sync vehicle
            vm.setVehicle(currentVehicle)
            
            Log.d(DebugTags.BookingProcess, "✅ Synced all prefilled data to ViewModel")

            // Fetch booking rates after prefilling data (matches iOS loadEditData)
            // Use coroutineScope since we're already in a LaunchedEffect coroutine context
            coroutineScope {
                launch {
                    Log.d(DebugTags.BookingProcess, "🔄 Fetching booking rates after edit data loaded")
                    // Use uiState.rideData to get latest coordinates (now synced from prefill)
                    val latestRideData = uiState.rideData
                    val updatedRideData = latestRideData.copy(
                        // All data is now synced to ViewModel, so use it directly
                        serviceType = when (selectedServiceType) {
                            "One Way" -> "one_way"
                            "Round Trip" -> "round_trip"
                            "Charter Tour" -> "charter_tour"
                            else -> latestRideData.serviceType
                        },
                        pickupType = when {
                            selectedTransferType.startsWith("City") -> "city"
                            selectedTransferType.startsWith("Airport") -> "airport"
                            selectedTransferType.startsWith("Cruise") -> "cruise"
                            else -> latestRideData.pickupType
                        },
                        dropoffType = when {
                            selectedTransferType.endsWith("City") -> "city"
                            selectedTransferType.endsWith("Airport") -> "airport"
                            selectedTransferType.endsWith("Cruise Port") -> "cruise"
                            else -> latestRideData.dropoffType
                        },
                        bookingHour = if (selectedServiceType == "Charter Tour") {
                            selectedHours.replace(" hours minimum", "").replace(" hours", "").trim()
                        } else {
                            latestRideData.bookingHour
                        },
                        noOfVehicles = numberOfVehicles,
                        selectedPickupAirport = selectedPickupAirport?.displayName ?: "",
                        selectedDestinationAirport = selectedDropoffAirport?.displayName ?: "",
                        selectedPickupAirline = selectedPickupAirline?.displayName,
                        selectedDestinationAirline = selectedDropoffAirline?.displayName,
                        pickupFlightNumber = pickupFlightNumber,
                        dropoffFlightNumber = dropoffFlightNumber,
                        originAirportCity = originAirportCity,
                        // Preserve pickup date and time (use local state if available, otherwise ViewModel state)
                        // CRITICAL: Always prefer local state for pickupTime as it's the source of truth
                        pickupDate = if (pickupDate.isNotEmpty()) pickupDate else latestRideData.pickupDate,
                        pickupTime = if (pickupTime.isNotEmpty()) pickupTime else latestRideData.pickupTime,
                        returnPickupTime = if (returnPickupTime.isNotEmpty()) returnPickupTime else (latestRideData.returnPickupTime ?: "")
                    )
                    vm.fetchBookingRates(
                        ride = updatedRideData,
                        vehicle = currentVehicle,
                        isEditMode = true,
                        editBookingId = editBookingId,
                        hasExtraStops = hasExtraStops(editData),
                        extraStops = extraStops.toExtraStopRequestsWithTownComparison(placesService, pickupLocation),
                        returnExtraStops = returnExtraStops.toExtraStopRequestsWithTownComparison(placesService, returnPickupLocation)
                    )
                }
            }
        }
    }

    // Prefill data from repeat mode (similar to edit mode but handles isReturnFlow)
    LaunchedEffect(editData, isRepeatMode, isReturnFlow, airports, airlines, meetGreetChoices) {
        if (isRepeatMode && editData != null && isLoadingRepeatData) {
            val data = editData!!
            Log.d(DebugTags.BookingProcess, "🔄 PREFILLING DATA FROM REPEAT MODE")
            Log.d(DebugTags.BookingProcess, "isReturnFlow: $isReturnFlow")

            // Similar to edit mode prefill, but handle isReturnFlow flag
            // If isReturnFlow is true, we only prefill return trip fields
            // If isReturnFlow is false, we prefill outbound trip fields (like repeat)

            if (isReturnFlow) {
                // Return flow: Prefill return trip fields only
                returnPickupDate = data.returnPickupDate ?: ""
                returnPickupTime = data.returnPickupTime ?: ""
                returnPickupLocation = data.returnPickup ?: ""
                returnDropoffLocation = data.returnDropoff ?: ""
                returnPickupFlightNumber = data.returnPickupFlight ?: ""
                returnDropoffFlightNumber = data.returnDropoffFlight ?: ""
                returnOriginAirportCity = data.returnOriginAirportCity ?: ""
                returnCruiseShipName = data.returnCruiseName ?: ""
                returnShipArrivalTime = data.returnCruiseTime ?: ""

                // Prefill return airports
                if (airports.isNotEmpty()) {
                    if (!data.returnPickupAirport.isNullOrEmpty()) {
                        val airportId = data.returnPickupAirport.toIntOrNull()
                        if (airportId != null) {
                            airports.find { it.id == airportId }?.let { selectedReturnPickupAirport = it }
                        }
                    }
                    if (!data.returnDropoffAirport.isNullOrEmpty()) {
                        val airportId = data.returnDropoffAirport.toIntOrNull()
                        if (airportId != null) {
                            airports.find { it.id == airportId }?.let { selectedReturnDropoffAirport = it }
                        }
                    }
                }

                // Prefill return airlines
                if (airlines.isNotEmpty()) {
                    if (!data.returnPickupAirline.isNullOrEmpty()) {
                        val airlineId = data.returnPickupAirline.toIntOrNull()
                        if (airlineId != null) {
                            airlines.find { it.id == airlineId }?.let { selectedReturnPickupAirline = it }
                        }
                    }
                    if (!data.returnDropoffAirline.isNullOrEmpty()) {
                        val airlineId = data.returnDropoffAirline.toIntOrNull()
                        if (airlineId != null) {
                            airlines.find { it.id == airlineId }?.let { selectedReturnDropoffAirline = it }
                        }
                    }
                }
            } else {
                // Repeat flow: Prefill outbound trip fields (same as edit mode)
                // Create vehicle from repeat data
                val createdVehicle = Vehicle(
                    id = data.vehicleId ?: 0,
                    name = data.vehicleTypeName ?: "Vehicle",
                    image = if (data.vehicleImages != null && data.vehicleImages.isNotEmpty()) data.vehicleImages.first() else null,
                    vehicleImages = data.vehicleImages ?: emptyList(),
                    capacity = data.vehicleSeatsName ?: data.vehicleSeats ?: 4,
                    passenger = data.vehicleSeatsName ?: data.vehicleSeats ?: 4,
                    luggage = data.luggageCount,
                    price = null,
                    affiliateId = data.affiliateId,
                    rateBreakdownOneWay = null,
                    rateBreakdownRoundTrip = null,
                    rateBreakdownCharterTour = null,
                    driverInformation = DriverInformation(
                        id = data.driverId,
                        name = data.driverName ?: "Driver",
                        gender = data.driverGender ?: "Male",
                        email = data.driverEmail,
                        phone = data.driverCell ?: "",
                        cellIsd = data.driverCellIsd ?: "+1",
                        cellNumber = data.driverCell ?: "",
                        starRating = "4.5",
                        background = "Background Checked",
                        dress = "Business Casual",
                        languages = "English",
                        experience = "5+ Years",
                        imageUrl = data.driverImage ?: "",
                        insuranceLimit = "1M"
                    ),
                    amenities = null,
                    vehicleDetails = VehicleDetails(
                        make = data.vehicleMakeName ?: "Unknown",
                        model = data.vehicleModelName ?: "Unknown",
                        year = data.vehicleYearName ?: "Unknown"
                    ),
                    isMasterVehicle = data.affiliateType == "unassigned"
                )
                currentVehicle = createdVehicle

                // Log repeat data received from API
                Log.d(DebugTags.BookingProcess, "📥 REPEAT DATA RECEIVED FROM API:")
                Log.d(DebugTags.BookingProcess, "  pickupDate: '${data.pickupDate}'")
                Log.d(DebugTags.BookingProcess, "  pickupTime: '${data.pickupTime}'")
                Log.d(DebugTags.BookingProcess, "  pickupFlight: '${data.pickupFlight}'")
                Log.d(DebugTags.BookingProcess, "  dropoffFlight: '${data.dropoffFlight}'")
                Log.d(DebugTags.BookingProcess, "  originAirportCity: '${data.originAirportCity}'")
                
                // Update rideData
                currentRideData = currentRideData.copy(
                    serviceType = data.serviceType,
                    pickupType = when {
                        data.transferType.contains("city", ignoreCase = true) && data.transferType.startsWith("city", ignoreCase = true) -> "city"
                        data.transferType.contains("airport", ignoreCase = true) && data.transferType.startsWith("airport", ignoreCase = true) -> "airport"
                        data.transferType.contains("cruise", ignoreCase = true) && data.transferType.startsWith("cruise", ignoreCase = true) -> "cruise"
                        else -> currentRideData.pickupType
                    },
                    dropoffType = when {
                        data.transferType.contains("city", ignoreCase = true) && data.transferType.endsWith("city", ignoreCase = true) -> "city"
                        data.transferType.contains("airport", ignoreCase = true) && data.transferType.endsWith("airport", ignoreCase = true) -> "airport"
                        data.transferType.contains("cruise", ignoreCase = true) && data.transferType.endsWith("cruise", ignoreCase = true) -> "cruise"
                        else -> currentRideData.dropoffType
                    },
                    pickupDate = data.pickupDate ?: "",
                    pickupTime = data.pickupTime ?: "",
                    pickupLocation = data.pickup ?: "",
                    destinationLocation = data.dropoff ?: "",
                    pickupLat = data.pickupLatitude,
                    pickupLong = data.pickupLongitude,
                    destinationLat = data.dropoffLatitude,
                    destinationLong = data.dropoffLongitude,
                    noOfPassenger = data.totalPassengers,
                    noOfLuggage = data.luggageCount,
                    noOfVehicles = data.numberOfVehicles ?: 1,
                    bookingHour = data.numberOfHours?.toString() ?: "0",
                    selectedPickupAirport = data.pickupAirport ?: "",
                    selectedDestinationAirport = data.dropoffAirport ?: "",
                    selectedPickupAirline = data.pickupAirline ?: "",
                    selectedDestinationAirline = data.dropoffAirline ?: "",
                    pickupFlightNumber = data.pickupFlight ?: "",
                    dropoffFlightNumber = data.dropoffFlight ?: "",
                    returnPickupFlightNumber = data.returnPickupFlight ?: "",
                    returnDropoffFlightNumber = data.returnDropoffFlight ?: "",
                    originAirportCity = data.originAirportCity ?: ""
                )

                // Prefill service type and transfer type
                selectedServiceType = when (data.serviceType.lowercase()) {
                    "one_way", "oneway" -> "One Way"
                    "round_trip" -> "Round Trip"
                    "charter_tour", "chartertour" -> "Charter Tour"
                    else -> selectedServiceType
                }

                selectedTransferType = when (data.transferType.lowercase()) {
                    "city_to_city" -> "City to City"
                    "city_to_airport" -> "City to Airport"
                    "airport_to_city" -> "Airport to City"
                    "airport_to_airport" -> "Airport to Airport"
                    "city_to_cruise" -> "City to Cruise Port"
                    "airport_to_cruise" -> "Airport to Cruise Port"
                    "cruise_to_city" -> "Cruise Port to City"
                    "cruise_to_airport" -> "Cruise Port to Airport"
                    else -> selectedTransferType
                }

                // Prefill dates, times, locations
                pickupDate = data.pickupDate
                pickupTime = data.pickupTime
                pickupLocation = data.pickup ?: ""
                dropoffLocation = data.dropoff ?: ""
                passengerCount = data.totalPassengers.toString()
                luggageCount = data.luggageCount.toString()
                numberOfVehicles = data.numberOfVehicles ?: 1
                pickupFlightNumber = data.pickupFlight ?: ""
                dropoffFlightNumber = data.dropoffFlight ?: ""
                originAirportCity = data.originAirportCity ?: ""
                cruiseShipName = data.cruiseName ?: ""
                shipArrivalTime = data.cruiseTime ?: ""
                specialInstructions = data.bookingInstructions ?: specialInstructions

                // Prefill airports
                if (airports.isNotEmpty()) {
                    if (!data.pickupAirport.isNullOrEmpty()) {
                        val airportId = data.pickupAirport.toIntOrNull()
                        if (airportId != null) {
                            airports.find { it.id == airportId }?.let { selectedPickupAirport = it }
                        }
                    }
                    if (!data.dropoffAirport.isNullOrEmpty()) {
                        val airportId = data.dropoffAirport.toIntOrNull()
                        if (airportId != null) {
                            airports.find { it.id == airportId }?.let { selectedDropoffAirport = it }
                        }
                    }
                }

                // Prefill airlines
                if (airlines.isNotEmpty()) {
                    if (!data.pickupAirline.isNullOrEmpty()) {
                        val airlineId = data.pickupAirline.toIntOrNull()
                        if (airlineId != null) {
                            airlines.find { it.id == airlineId }?.let { selectedPickupAirline = it }
                        }
                    }
                    if (!data.dropoffAirline.isNullOrEmpty()) {
                        val airlineId = data.dropoffAirline.toIntOrNull()
                        if (airlineId != null) {
                            airlines.find { it.id == airlineId }?.let { selectedDropoffAirline = it }
                        }
                    }
                }

                // Prefill hours and meet & greet
                if (data.numberOfHours != null) {
                    val hours = data.numberOfHours
                    selectedHours = if (hours == 2) "2 hours minimum" else "$hours hours"
                }

                val meetGreetName = data.meetGreetChoiceName ?: "Driver - Text/call when on location"
                selectedMeetAndGreet = if (meetGreetChoices.isNotEmpty()) {
                    meetGreetChoices.find { it.message.equals(meetGreetName, ignoreCase = true) }?.message
                        ?: meetGreetChoices.firstOrNull()?.message
                        ?: meetGreetName
                } else {
                    meetGreetName
                }

                // Prefill extra stops
                val (prefilledOutboundStops, prefilledReturnStops) = prefillExtraStopsFromEditData(data, selectedServiceType)
                extraStops = prefilledOutboundStops
                returnExtraStops = prefilledReturnStops
            }

            isLoadingRepeatData = false
            Log.d(DebugTags.BookingProcess, "✅ Prefilled all fields from repeat data")

            // In repeat mode, check if we should skip rate API call
            // If rates were already loaded from the existing booking, we can skip
            // Only fetch if user has made changes that would affect rates
            if (!hasLoadedExistingRates) {
                hasLoadedExistingRates = true
                // Fetch booking rates after prefilling data
                coroutineScope {
                    launch {
                        // Use uiState.rideData to get latest coordinates
                        val latestRideData = uiState.rideData
                        val updatedRideData = currentRideData.copy(
                            // CRITICAL: Always set serviceType from selectedServiceType to avoid stale values
                            serviceType = when (selectedServiceType) {
                                "One Way" -> "one_way"
                                "Round Trip" -> "round_trip"
                                "Charter Tour" -> "charter_tour"
                                else -> currentRideData.serviceType
                            },
                            // Preserve coordinates from ViewModel state
                            pickupLat = latestRideData.pickupLat,
                            pickupLong = latestRideData.pickupLong,
                            destinationLat = latestRideData.destinationLat,
                            destinationLong = latestRideData.destinationLong,
                            pickupLocation = latestRideData.pickupLocation,
                            destinationLocation = latestRideData.destinationLocation,
                            // Preserve pickup date and time (use local state if available, otherwise ViewModel state)
                            pickupDate = pickupDate.ifEmpty { latestRideData.pickupDate },
                            pickupTime = pickupTime.ifEmpty { latestRideData.pickupTime },
                            returnPickupTime = returnPickupTime.ifEmpty { latestRideData.returnPickupTime }
                        )
                        vm.fetchBookingRates(
                            ride = updatedRideData,
                            vehicle = currentVehicle,
                            isEditMode = false, // Not edit mode, it's repeat mode
                            editBookingId = null,
                            hasExtraStops = hasExtraStops(editData),
                            extraStops = extraStops.toExtraStopRequestsWithTownComparison(placesService, pickupLocation),
                            returnExtraStops = returnExtraStops.toExtraStopRequestsWithTownComparison(placesService, returnPickupLocation)
                        )
                    }
                }
            } else {
                Log.d(DebugTags.BookingProcess, "⏭️ Skipping rate API call in repeat mode - rates already loaded")
            }
        }
    }

    // Return trip dropdown states
    var showReturnServiceTypeDropdown by remember { mutableStateOf(false) }
    var showReturnTransferTypeDropdown by remember { mutableStateOf(false) }
    var showReturnMeetAndGreetDropdown by remember { mutableStateOf(false) }
    var showReturnDatePicker by remember { mutableStateOf(false) }
    var showReturnTimePicker by remember { mutableStateOf(false) }

    // Bottom sheet states
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showShipArrivalTimePicker by remember { mutableStateOf(false) }
    var showDropoffShipArrivalTimePicker by remember { mutableStateOf(false) }
    // Location picker is now handled by LocationAutocomplete component - no need for separate state
    var showAirlineBottomSheet by remember { mutableStateOf(false) }
    var showAirportBottomSheet by remember { mutableStateOf(false) }
    var currentAirlineType by remember { mutableStateOf<String?>(null) } // "pickup", "dropoff", "returnPickup", "returnDropoff"
    var currentAirportType by remember { mutableStateOf<String?>(null) } // "pickup", "dropoff", "returnPickup", "returnDropoff"

    // Dropdown states
    var showServiceTypeDropdown by remember { mutableStateOf(false) }
    var showTransferTypeDropdown by remember { mutableStateOf(false) }
    var showHoursDropdown by remember { mutableStateOf(false) }
    var showMeetAndGreetDropdown by remember { mutableStateOf(false) }

    // Meet and greet options - fetched from API
    val meetAndGreetOptions = remember(meetGreetChoices) {
        if (meetGreetChoices.isNotEmpty()) {
            meetGreetChoices.map { it.message }
        } else {
            listOf("Driver - Text/call when on location") // Fallback
        }
    }

    // Coroutine scope for search operations
    val coroutineScope = rememberCoroutineScope()

    /**
     * Show validation error with auto-dismiss after 5 seconds (matches iOS showValidationError)
     */
    fun showValidationError(message: String) {
        invalidLocationMessage = message
        showInvalidLocationDialog = true

        // Cancel previous dismiss job if exists
        invalidLocationDismissJob?.cancel()

        // Auto-dismiss after 5 seconds
        invalidLocationDismissJob = coroutineScope.launch {
            delay(5000)
            showInvalidLocationDialog = false
        }
    }

    // Helper function to find airport by display name (matches iOS)
    fun findAirportByDisplayName(displayName: String): Airport? {
        return airports.find { it.displayName == displayName }
    }

    // Fetch airlines, airports, and meet & greet choices on screen load
    LaunchedEffect(Unit) {
        airlineService.fetchAirlines()
        airportService.fetchInitialAirports()
        meetGreetService.fetchMeetGreetChoices()

        // Fetch user profile data and pre-populate passenger fields in ViewModel
        profileLoading = true
        try {
            val response = dashboardApi.getProfileData()
            if (response.success) {
                profileData = response.data
                val data = response.data
                
                // Pre-populate passenger fields from profile data (matches web app)
                val passengerName = if (data.middleName != null && data.middleName.isNotEmpty()) {
                    "${data.firstName} ${data.middleName} ${data.lastName}".trim()
                } else {
                    "${data.firstName} ${data.lastName}".trim()
                }
                val passengerEmail = data.email ?: ""
                val passengerMobile = data.mobile ?: ""
                
                // Update ViewModel with passenger data from profile
                // Only update if fields are empty AND not in edit mode (don't overwrite edit data)
                // In edit mode, passenger fields come from edit data, not profile
                if (!isEditMode) {
                    if (uiState.passengerName.isEmpty()) {
                        vm.setPassengerName(passengerName)
                    }
                    if (uiState.passengerEmail.isEmpty()) {
                        vm.setPassengerEmail(passengerEmail)
                    }
                    if (uiState.passengerMobile.isEmpty()) {
                        vm.setPassengerMobile(passengerMobile)
                    }
                    Log.d(DebugTags.BookingProcess, "✅ Profile data loaded: ${data.fullName}")
                    Log.d(DebugTags.BookingProcess, "✅ Passenger fields pre-populated: name=$passengerName, email=$passengerEmail, mobile=$passengerMobile")
                } else {
                    Log.d(DebugTags.BookingProcess, "✅ Profile data loaded: ${data.fullName}")
                    Log.d(DebugTags.BookingProcess, "⏭️ Skipping profile passenger fields in edit mode - using edit data instead")
                }
            }
        } catch (e: Exception) {
            Log.e(DebugTags.BookingProcess, "Error loading profile data", e)
        } finally {
            profileLoading = false
        }
    }

    // Prefill airports from rideData (matches iOS initialization logic)
    LaunchedEffect(airports, rideData) {
        if (airports.isNotEmpty()) {
            // Prefill pickup airport if rideData has airport info
            if (rideData.pickupType.equals("airport", ignoreCase = true) &&
                rideData.selectedPickupAirport.isNotEmpty()) {
                val airport = findAirportByDisplayName(rideData.selectedPickupAirport)
                if (airport != null) {
                    selectedPickupAirport = airport
                    pickupLocation = airport.displayName
                    Log.d(DebugTags.BookingProcess, "✅ Prefilled pickup airport: ${airport.displayName}")
                }
            }

            // Prefill dropoff airport if rideData has airport info
            if (rideData.dropoffType.equals("airport", ignoreCase = true) &&
                rideData.selectedDestinationAirport.isNotEmpty()) {
                val airport = findAirportByDisplayName(rideData.selectedDestinationAirport)
                if (airport != null) {
                    selectedDropoffAirport = airport
                    dropoffLocation = airport.displayName
                    Log.d(DebugTags.BookingProcess, "✅ Prefilled dropoff airport: ${airport.displayName}")
                }
            }
        }
    }

    // Auto-update meet & greet and special instructions when transfer type changes (matches web app)
    LaunchedEffect(selectedTransferType) {
        if (selectedTransferType.isNotEmpty()) {
            // Update meet & greet based on transfer type
            val newMeetAndGreet = getMeetAndGreetForTransferType(selectedTransferType)
            if (selectedMeetAndGreet != newMeetAndGreet) {
                selectedMeetAndGreet = newMeetAndGreet
                Log.d(DebugTags.BookingProcess, "🔄 Auto-updated meet & greet: $newMeetAndGreet")
            }

            // Update special instructions based on transfer type
            val newSpecialInstructions = getSpecialInstructionsForTransferType(selectedTransferType)
            if (specialInstructions != newSpecialInstructions) {
                specialInstructions = newSpecialInstructions
                Log.d(DebugTags.BookingProcess, "🔄 Auto-updated special instructions for transfer type: $selectedTransferType")
            }
        }
    }

    // Auto-update return meet & greet and special instructions when return transfer type changes (matches web app)
    LaunchedEffect(selectedReturnTransferType) {
        val returnTransferType = selectedReturnTransferType // Local variable to avoid smart cast issues
        if (selectedServiceType == "Round Trip" && returnTransferType != null && returnTransferType.isNotEmpty()) {
            // Update return meet & greet based on return transfer type
            val newReturnMeetAndGreet = getMeetAndGreetForTransferType(returnTransferType)
            if (selectedReturnMeetAndGreet != newReturnMeetAndGreet) {
                selectedReturnMeetAndGreet = newReturnMeetAndGreet
                Log.d(DebugTags.BookingProcess, "🔄 Auto-updated return meet & greet: $newReturnMeetAndGreet")
            }

            // Update return special instructions based on return transfer type
            val newReturnSpecialInstructions = getSpecialInstructionsForTransferType(returnTransferType)
            if (returnSpecialInstructions != newReturnSpecialInstructions) {
                returnSpecialInstructions = newReturnSpecialInstructions
                Log.d(DebugTags.BookingProcess, "🔄 Auto-updated return special instructions for transfer type: $returnTransferType")
            }
        }
    }

    // Initialize return trip fields when service type changes to Round Trip (matches iOS)
    LaunchedEffect(selectedServiceType, selectedTransferType, selectedMeetAndGreet, selectedHours, numberOfVehicles, specialInstructions) {
        if (selectedServiceType == "Round Trip") {
            // Auto-reverse transfer type for return trip
            if (selectedReturnTransferType == null) {
                selectedReturnTransferType = getReversedTransferType(selectedTransferType)
            }

            // Copy values from outbound trip
            if (selectedReturnServiceType == null) {
                selectedReturnServiceType = selectedServiceType
            }
            if (selectedReturnMeetAndGreet == null) {
                selectedReturnMeetAndGreet = selectedMeetAndGreet
            }
            if (selectedReturnHours == "2 hours minimum" && selectedServiceType == "Charter Tour") {
                selectedReturnHours = selectedHours
            }
            if (returnNumberOfVehicles == 1) {
                returnNumberOfVehicles = numberOfVehicles
            }
            if (returnSpecialInstructions.isEmpty()) {
                returnSpecialInstructions = specialInstructions
            }

            // Initialize return date/time (default to same as outbound, or use from rideData if available)
            if (returnPickupDate.isEmpty()) {
                returnPickupDate = rideData.returnPickupDate ?: pickupDate
                // Sync to ViewModel if set
                if (returnPickupDate.isNotEmpty()) {
                    vm.setReturnPickupDate(returnPickupDate)
                }
            }
            if (returnPickupTime.isEmpty()) {
                returnPickupTime = rideData.returnPickupTime ?: pickupTime
                // Sync to ViewModel if set
                if (returnPickupTime.isNotEmpty()) {
                    vm.setReturnPickupTime(returnPickupTime)
                }
            }

            // Pre-fill return locations based on reversed transfer type
            val reversedType = selectedReturnTransferType ?: getReversedTransferType(selectedTransferType)
            if (returnPickupLocation.isEmpty() && reversedType.contains("City")) {
                // Return pickup is city, use dropoff location from outbound
                returnPickupLocation = dropoffLocation
                // Also set coordinates if available
                if (uiState.rideData.destinationLat != null && uiState.rideData.destinationLong != null) {
                    returnPickupLat = uiState.rideData.destinationLat
                    returnPickupLong = uiState.rideData.destinationLong
                    // Sync to ViewModel
                    vm.setReturnPickupLocation(returnPickupLocation, returnPickupLat, returnPickupLong)
                    Log.d(DebugTags.BookingProcess, "✅ Auto-filled return pickup location: $returnPickupLocation (Lat: $returnPickupLat, Long: $returnPickupLong)")
                }
            } else if (returnPickupLocation.isEmpty() && reversedType.contains("Airport")) {
                // Return pickup is airport, use dropoff airport from outbound
                returnPickupLocation = selectedDropoffAirport?.displayName ?: ""
                val dropoffAirport = selectedDropoffAirport // Local variable for smart cast
                if (selectedReturnPickupAirport == null && dropoffAirport != null) {
                    selectedReturnPickupAirport = dropoffAirport
                    // Set coordinates from airport
                    returnPickupLat = dropoffAirport.lat
                    returnPickupLong = dropoffAirport.long
                    // Sync to ViewModel
                    vm.setReturnPickupLocation(returnPickupLocation, returnPickupLat, returnPickupLong)
                    Log.d(DebugTags.BookingProcess, "✅ Auto-filled return pickup airport: $returnPickupLocation (Lat: $returnPickupLat, Long: $returnPickupLong)")
                }
            }

            if (returnDropoffLocation.isEmpty() && reversedType.endsWith("City")) {
                // Return dropoff is city, use pickup location from outbound
                returnDropoffLocation = pickupLocation
                // Also set coordinates if available
                if (uiState.rideData.pickupLat != null && uiState.rideData.pickupLong != null) {
                    returnDropoffLat = uiState.rideData.pickupLat
                    returnDropoffLong = uiState.rideData.pickupLong
                    // Sync to ViewModel
                    vm.setReturnDropoffLocation(returnDropoffLocation, returnDropoffLat, returnDropoffLong)
                    Log.d(DebugTags.BookingProcess, "✅ Auto-filled return dropoff location: $returnDropoffLocation (Lat: $returnDropoffLat, Long: $returnDropoffLong)")
                }
            } else if (returnDropoffLocation.isEmpty() && reversedType.endsWith("Airport")) {
                // Return dropoff is airport, use pickup airport from outbound
                val pickupAirport = selectedPickupAirport // Local variable for smart cast
                returnDropoffLocation = pickupAirport?.displayName ?: ""
                if (selectedReturnDropoffAirport == null && pickupAirport != null) {
                    selectedReturnDropoffAirport = pickupAirport
                    // Set coordinates from airport
                    returnDropoffLat = pickupAirport.lat
                    returnDropoffLong = pickupAirport.long
                    // Sync to ViewModel
                    vm.setReturnDropoffLocation(returnDropoffLocation, returnDropoffLat, returnDropoffLong)
                    Log.d(DebugTags.BookingProcess, "✅ Auto-filled return dropoff airport: $returnDropoffLocation (Lat: $returnDropoffLat, Long: $returnDropoffLong)")
                }
            }
            
            // Sync return pickup location coordinates if auto-filled
            if (returnPickupLocation.isNotEmpty() && (returnPickupLat == null || returnPickupLong == null)) {
                // Try to get coordinates from dropoff location (return pickup = outbound dropoff)
                if (uiState.rideData.destinationLat != null && uiState.rideData.destinationLong != null) {
                    returnPickupLat = uiState.rideData.destinationLat
                    returnPickupLong = uiState.rideData.destinationLong
                    // Sync to ViewModel
                    vm.setReturnPickupLocation(returnPickupLocation, returnPickupLat, returnPickupLong)
                    Log.d(DebugTags.BookingProcess, "✅ Auto-filled return pickup location coordinates: (Lat: $returnPickupLat, Long: $returnPickupLong)")
                }
            }
        }
    }

    // Bidirectional updates: When outbound airports change, update return airports (for round trips)
    LaunchedEffect(selectedPickupAirport, selectedServiceType, selectedReturnTransferType) {
        val pickupAirport = selectedPickupAirport // Local variable to avoid smart cast issue
        if (selectedServiceType == "Round Trip" && pickupAirport != null) {
            val reversedType = selectedReturnTransferType ?: getReversedTransferType(selectedTransferType)
            // Return dropoff = outbound pickup (reversed)
            if (reversedType.endsWith("Airport")) {
                selectedReturnDropoffAirport = pickupAirport
                returnDropoffLocation = pickupAirport.displayName
            }
        }
    }

    LaunchedEffect(selectedDropoffAirport, selectedServiceType, selectedReturnTransferType) {
        val dropoffAirport = selectedDropoffAirport // Local variable to avoid smart cast issue
        if (selectedServiceType == "Round Trip" && dropoffAirport != null) {
            val reversedType = selectedReturnTransferType ?: getReversedTransferType(selectedTransferType)
            // Return pickup = outbound dropoff (reversed)
            if (reversedType.contains("Airport")) {
                selectedReturnPickupAirport = dropoffAirport
                returnPickupLocation = dropoffAirport.displayName
            }
        }
    }

    // Bidirectional updates: When return airports change, update outbound airports (for round trips)
    LaunchedEffect(selectedReturnPickupAirport, selectedServiceType, selectedReturnTransferType) {
        val returnPickupAirport = selectedReturnPickupAirport // Local variable to avoid smart cast issue
        if (selectedServiceType == "Round Trip" && returnPickupAirport != null) {
            val reversedType = selectedReturnTransferType ?: getReversedTransferType(selectedTransferType)
            // Outbound dropoff = return pickup (reversed)
            if (reversedType.contains("Airport")) {
                selectedDropoffAirport = returnPickupAirport
                dropoffLocation = returnPickupAirport.displayName
            }
        }
    }

    LaunchedEffect(selectedReturnDropoffAirport, selectedServiceType, selectedReturnTransferType) {
        val returnDropoffAirport = selectedReturnDropoffAirport // Local variable to avoid smart cast issue
        if (selectedServiceType == "Round Trip" && returnDropoffAirport != null) {
            val reversedType = selectedReturnTransferType ?: getReversedTransferType(selectedTransferType)
            // Outbound pickup = return dropoff (reversed)
            if (reversedType.endsWith("Airport")) {
                selectedPickupAirport = returnDropoffAirport
                pickupLocation = returnDropoffAirport.displayName
            }
        }
    }

    // Fetch booking rates when screen loads or when service/transfer type changes (matches iOS)
    // Skip in edit mode until edit data is loaded (edit data LaunchedEffect will fetch rates)
    // Skip in repeat mode if rates were already loaded from existing booking
    // Include coordinates in dependencies so API is called when locations are selected
    LaunchedEffect(
        currentRideData, 
        currentVehicle, 
        selectedServiceType, 
        selectedTransferType, 
        isEditMode, 
        editData, 
        isRepeatMode, 
        hasLoadedExistingRates,
        uiState.rideData.pickupLat,
        uiState.rideData.pickupLong,
        uiState.rideData.destinationLat,
        uiState.rideData.destinationLong,
        isViewModelInitialized
    ) {
        // Wait for ViewModel to be initialized before making API calls
        if (!isViewModelInitialized) {
            Log.d(DebugTags.BookingProcess, "⏳ Waiting for ViewModel initialization before fetching rates")
            return@LaunchedEffect
        }
        
        // In edit mode, wait for edit data to be loaded before fetching rates
        if (isEditMode && editData == null) {
            Log.d(DebugTags.BookingProcess, "⏳ Edit mode: Waiting for edit data to load before fetching rates")
            return@LaunchedEffect
        }

        // In repeat mode, skip if rates were already loaded (user hasn't made changes yet)
        if (isRepeatMode && hasLoadedExistingRates && !isLoadingRepeatData) {
            Log.d(DebugTags.BookingProcess, "⏭️ Repeat mode: Skipping rate API call - rates already loaded from existing booking")
            return@LaunchedEffect
        }

        Log.d(DebugTags.BookingProcess, "===========================================")
        Log.d(DebugTags.BookingProcess, "📱 ComprehensiveBookingScreen - LaunchedEffect triggered")
        Log.d(DebugTags.BookingProcess, "Service Type: $selectedServiceType, Transfer Type: $selectedTransferType")
        Log.d(DebugTags.BookingProcess, "Calling vm.fetchBookingRates()...")
        Log.d(DebugTags.BookingProcess, "===========================================")

        // Use uiState.rideData to get latest coordinates (setPickupLocation updates ViewModel state)
        val latestRideData = uiState.rideData
        
        // CRITICAL: Sync pickupTime and pickupDate to ViewModel if they're set locally but not in ViewModel
        // This ensures rideData.pickupTime is always up-to-date before building the request
        if (pickupTime.isNotEmpty() && latestRideData.pickupTime != pickupTime) {
            vm.setPickupTime(pickupTime)
            Log.d(DebugTags.BookingProcess, "🔄 Synced pickupTime to ViewModel: '$pickupTime'")
        }
        if (pickupDate.isNotEmpty() && latestRideData.pickupDate != pickupDate) {
            vm.setPickupDate(pickupDate)
            Log.d(DebugTags.BookingProcess, "🔄 Synced pickupDate to ViewModel: '$pickupDate'")
        }
        
        // CRITICAL: For returnPickupTime, sync if set locally
        if (returnPickupTime.isNotEmpty() && latestRideData.returnPickupTime != returnPickupTime) {
            // Note: There's no setReturnPickupTime method, so we'll include it directly in updatedRideData
            Log.d(DebugTags.BookingProcess, "🔄 Return pickup time set locally: '$returnPickupTime'")
        }
        
        // Use latestRideData (StateFlow updates are async, so we'll use local values directly in updatedRideData)
        val syncedRideData = latestRideData
        
        // Update rideData with current service/transfer type for API call, preserving coordinates from ViewModel
        val updatedRideData = syncedRideData.copy(
            serviceType = when (selectedServiceType) {
                "One Way" -> "one_way"
                "Round Trip" -> "round_trip"
                "Charter Tour" -> "charter_tour"
                else -> syncedRideData.serviceType
            },
            pickupType = when {
                selectedTransferType.startsWith("City") -> "city"
                selectedTransferType.startsWith("Airport") -> "airport"
                selectedTransferType.startsWith("Cruise") -> "cruise"
                else -> {
                    // Fallback: normalize from rideData
                    val normalized = syncedRideData.pickupType.lowercase().replace("cruise port", "cruise").replace("cruise_port", "cruise").trim()
                    normalized
                }
            },
            dropoffType = when {
                selectedTransferType.endsWith("City") -> "city"
                selectedTransferType.endsWith("Airport") -> "airport"
                selectedTransferType.endsWith("Cruise Port") -> "cruise"
                selectedTransferType == "Cruise Port to Cruise Port" -> "cruise" // Handle cruise to cruise case
                else -> {
                    // Fallback: normalize from rideData
                    val normalized = syncedRideData.dropoffType.lowercase().replace("cruise port", "cruise").replace("cruise_port", "cruise").trim()
                    normalized
                }
            },
            noOfVehicles = numberOfVehicles,
            bookingHour = if (selectedServiceType == "Charter Tour") {
                selectedHours.replace(" hours minimum", "").replace(" hours", "").trim()
            } else {
                syncedRideData.bookingHour
            },
            // Preserve coordinates from ViewModel state (they're updated when location is selected)
            pickupLat = syncedRideData.pickupLat,
            pickupLong = syncedRideData.pickupLong,
            destinationLat = syncedRideData.destinationLat,
            destinationLong = syncedRideData.destinationLong,
            pickupLocation = syncedRideData.pickupLocation,
            destinationLocation = syncedRideData.destinationLocation,
            // CRITICAL: Always use local state for pickupTime/pickupDate as they're the source of truth
            // Use local value directly - it's the most up-to-date
            // If local is empty, use ViewModel state, but prefer local state
            pickupDate = if (pickupDate.isNotEmpty()) pickupDate else syncedRideData.pickupDate,
            pickupTime = if (pickupTime.isNotEmpty()) pickupTime else syncedRideData.pickupTime,
            returnPickupTime = if (returnPickupTime.isNotEmpty()) returnPickupTime else (syncedRideData.returnPickupTime ?: "")
        )
        
        Log.d(DebugTags.BookingProcess, "📍 fetchBookingRates - Pickup: ${updatedRideData.pickupLocation}, Lat: ${updatedRideData.pickupLat}, Long: ${updatedRideData.pickupLong}")
        Log.d(DebugTags.BookingProcess, "🕐 fetchBookingRates - Pickup Time: local='$pickupTime', syncedRideData='${syncedRideData.pickupTime}', final='${updatedRideData.pickupTime}'")
        Log.d(DebugTags.BookingProcess, "🕐 fetchBookingRates - Pickup Date: local='$pickupDate', syncedRideData='${syncedRideData.pickupDate}', final='${updatedRideData.pickupDate}'")
        Log.d(DebugTags.BookingProcess, "🔍 fetchBookingRates - Service Type: selectedServiceType='$selectedServiceType', updatedRideData.serviceType='${updatedRideData.serviceType}'")
        Log.d(DebugTags.BookingProcess, "🔍 fetchBookingRates - Booking Hour: selectedHours='$selectedHours', updatedRideData.bookingHour='${updatedRideData.bookingHour}'")
        
        // CRITICAL: If pickupTime is still empty after all checks, log a warning
        if (updatedRideData.pickupTime.isEmpty()) {
            Log.w(DebugTags.BookingProcess, "⚠️ WARNING: pickupTime is empty in updatedRideData! local='$pickupTime', syncedRideData='${syncedRideData.pickupTime}'")
        }
        
        // CRITICAL: Verify service type is correctly set
        if (selectedServiceType == "Charter Tour" && updatedRideData.serviceType != "charter_tour") {
            Log.e(DebugTags.BookingProcess, "❌ ERROR: Service type mismatch! selectedServiceType='$selectedServiceType' but updatedRideData.serviceType='${updatedRideData.serviceType}'")
        }
        Log.d(DebugTags.BookingProcess, "📍 fetchBookingRates - Dropoff: ${updatedRideData.destinationLocation}, Lat: ${updatedRideData.destinationLat}, Long: ${updatedRideData.destinationLong}")
        Log.d(DebugTags.BookingProcess, "📍 fetchBookingRates - Return Pickup: Lat: $returnPickupLat, Long: $returnPickupLong")
        Log.d(DebugTags.BookingProcess, "📍 fetchBookingRates - Return Dropoff: Lat: $returnDropoffLat, Long: $returnDropoffLong")
        
        // ==========================================
        // ENHANCED LOGGING: Trace what we're passing to ViewModel
        // ==========================================
        Log.d(DebugTags.BookingProcess, "===========================================")
        Log.d(DebugTags.BookingProcess, "📤 CALLING vm.fetchBookingRates() FROM COMPOSABLE")
        Log.d(DebugTags.BookingProcess, "===========================================")
        Log.d(DebugTags.BookingProcess, "📤 updatedRideData.serviceType='${updatedRideData.serviceType}'")
        Log.d(DebugTags.BookingProcess, "📤 updatedRideData.bookingHour='${updatedRideData.bookingHour}'")
        Log.d(DebugTags.BookingProcess, "📤 selectedServiceType='$selectedServiceType'")
        Log.d(DebugTags.BookingProcess, "📤 selectedHours='$selectedHours'")
        Log.d(DebugTags.BookingProcess, "===========================================")
        
        // CRITICAL: Verify serviceType is correct before passing to ViewModel
        if (selectedServiceType == "Charter Tour" && updatedRideData.serviceType != "charter_tour") {
            Log.e(DebugTags.BookingProcess, "===========================================")
            Log.e(DebugTags.BookingProcess, "❌ CRITICAL ERROR BEFORE CALLING ViewModel!")
            Log.e(DebugTags.BookingProcess, "❌ selectedServiceType='$selectedServiceType'")
            Log.e(DebugTags.BookingProcess, "❌ updatedRideData.serviceType='${updatedRideData.serviceType}'")
            Log.e(DebugTags.BookingProcess, "❌ Expected: 'charter_tour', Got: '${updatedRideData.serviceType}'")
            Log.e(DebugTags.BookingProcess, "===========================================")
        }
        
        vm.fetchBookingRates(
            ride = updatedRideData,
            vehicle = currentVehicle,
            isEditMode = isEditMode,
            editBookingId = editBookingId,
            hasExtraStops = hasExtraStops(editData),
            extraStops = extraStops.toExtraStopRequestsWithTownComparison(placesService, pickupLocation),
            returnExtraStops = returnExtraStops.toExtraStopRequestsWithTownComparison(placesService, returnPickupLocation),
            returnPickupLat = returnPickupLat,
            returnPickupLong = returnPickupLong,
            returnDropoffLat = returnDropoffLat,
            returnDropoffLong = returnDropoffLong
        )
    }

    // Trigger API when hours change (Charter Tour) - matches iOS
    LaunchedEffect(selectedHours, isViewModelInitialized) {
        if (!isViewModelInitialized) return@LaunchedEffect
        
        if (selectedServiceType == "Charter Tour") {
            Log.d(DebugTags.BookingProcess, "🕐 Hours changed to: $selectedHours - Triggering booking rates API")
            
            // CRITICAL: Update currentRideData.bookingHour so the ratesData calculation uses the correct hours
            val hoursValue = selectedHours.replace(" hours minimum", "").replace(" hours", "").trim()
            currentRideData = currentRideData.copy(bookingHour = hoursValue)
            Log.d(DebugTags.BookingProcess, "✅ Updated currentRideData.bookingHour to: $hoursValue")
            
            // Use uiState.rideData to get latest coordinates
            val latestRideData = uiState.rideData
            val updatedRideData = latestRideData.copy(
                // CRITICAL: Always set serviceType from selectedServiceType to avoid stale values
                serviceType = when (selectedServiceType) {
                    "One Way" -> "one_way"
                    "Round Trip" -> "round_trip"
                    "Charter Tour" -> "charter_tour"
                    else -> latestRideData.serviceType
                },
                bookingHour = hoursValue,
                // Preserve coordinates from ViewModel state
                pickupLat = latestRideData.pickupLat,
                pickupLong = latestRideData.pickupLong,
                destinationLat = latestRideData.destinationLat,
                destinationLong = latestRideData.destinationLong,
                // Preserve pickup date and time (use local state if available, otherwise ViewModel state)
                pickupDate = pickupDate.ifEmpty { latestRideData.pickupDate },
                pickupTime = pickupTime.ifEmpty { latestRideData.pickupTime },
                returnPickupTime = returnPickupTime.ifEmpty { latestRideData.returnPickupTime }
            )
            vm.fetchBookingRates(
                ride = updatedRideData,
                vehicle = currentVehicle,
                isEditMode = isEditMode,
                editBookingId = editBookingId,
                hasExtraStops = hasExtraStops(editData),
                extraStops = extraStops.toExtraStopRequestsWithTownComparison(placesService, pickupLocation),
                returnExtraStops = returnExtraStops.toExtraStopRequestsWithTownComparison(placesService, returnPickupLocation)
            )
        }
    }

    // Track previous return coordinates to detect changes (avoid duplicate calls)
    var previousReturnPickupLat by remember { mutableStateOf<Double?>(null) }
    var previousReturnPickupLong by remember { mutableStateOf<Double?>(null) }
    var previousReturnDropoffLat by remember { mutableStateOf<Double?>(null) }
    var previousReturnDropoffLong by remember { mutableStateOf<Double?>(null) }
    
    // Trigger API when return trip coordinates change (for round trips) - matches iOS
    LaunchedEffect(returnPickupLat, returnPickupLong, returnDropoffLat, returnDropoffLong, isViewModelInitialized) {
        if (!isViewModelInitialized) return@LaunchedEffect
        if (selectedServiceType != "Round Trip") return@LaunchedEffect
        
        // Only trigger if both return pickup and dropoff coordinates are available
        // AND at least one coordinate has changed
        val coordinatesChanged = returnPickupLat != previousReturnPickupLat ||
                                 returnPickupLong != previousReturnPickupLong ||
                                 returnDropoffLat != previousReturnDropoffLat ||
                                 returnDropoffLong != previousReturnDropoffLong
        
        if (returnPickupLat != null && returnPickupLong != null && 
            returnDropoffLat != null && returnDropoffLong != null &&
            coordinatesChanged) {
            Log.d(DebugTags.BookingProcess, "📍 Return trip coordinates changed - Triggering booking rates API")
            Log.d(DebugTags.BookingProcess, "📍 Return Pickup: Lat=$returnPickupLat, Long=$returnPickupLong")
            Log.d(DebugTags.BookingProcess, "📍 Return Dropoff: Lat=$returnDropoffLat, Long=$returnDropoffLong")
            
            // CRITICAL: Use uiState.rideData to get latest coordinates
            val latestRideData = uiState.rideData
            val updatedRideData = latestRideData.copy(
                // CRITICAL: Always set serviceType from selectedServiceType to avoid stale values
                serviceType = when (selectedServiceType) {
                    "One Way" -> "one_way"
                    "Round Trip" -> "round_trip"
                    "Charter Tour" -> "charter_tour"
                    else -> latestRideData.serviceType
                },
                // Preserve pickup date and time (use local state if available, otherwise ViewModel state)
                pickupDate = pickupDate.ifEmpty { latestRideData.pickupDate },
                pickupTime = pickupTime.ifEmpty { latestRideData.pickupTime },
                returnPickupTime = returnPickupTime.ifEmpty { latestRideData.returnPickupTime }
            )
            vm.fetchBookingRates(
                ride = updatedRideData,
                vehicle = currentVehicle,
                isEditMode = isEditMode,
                editBookingId = editBookingId,
                hasExtraStops = hasExtraStops(editData),
                extraStops = extraStops.toExtraStopRequestsWithTownComparison(placesService, pickupLocation),
                returnExtraStops = returnExtraStops.toExtraStopRequestsWithTownComparison(placesService, returnPickupLocation),
                returnPickupLat = returnPickupLat,
                returnPickupLong = returnPickupLong,
                returnDropoffLat = returnDropoffLat,
                returnDropoffLong = returnDropoffLong
            )
            
            // Update previous values
            previousReturnPickupLat = returnPickupLat
            previousReturnPickupLong = returnPickupLong
            previousReturnDropoffLat = returnDropoffLat
            previousReturnDropoffLong = returnDropoffLong
        }
    }

    // Update currentRideData when number of vehicles changes - recalculates rates locally (no API call)
    // This matches iOS behavior where numberOfVehicles change only recalculates grandTotal = subtotal × numberOfVehicles
    LaunchedEffect(numberOfVehicles, isViewModelInitialized) {
        if (!isViewModelInitialized) return@LaunchedEffect
        
        Log.d(DebugTags.BookingProcess, "🚗 Number of vehicles changed to: $numberOfVehicles - Recalculating rates locally (no API call)")
        // Update currentRideData with new numberOfVehicles
        // The ratesData will automatically recalculate because it watches currentRideData.noOfVehicles
        currentRideData = currentRideData.copy(noOfVehicles = numberOfVehicles)
        // Also update ViewModel's rideData to keep it in sync
        vm.setNumberOfVehicles(numberOfVehicles.toString())
        Log.d(DebugTags.BookingProcess, "✅ Updated currentRideData.noOfVehicles to $numberOfVehicles - rates will recalculate automatically")
    }

    // Trigger API when pickup airport changes - matches iOS
    LaunchedEffect(selectedPickupAirport, isViewModelInitialized) {
        if (!isViewModelInitialized) return@LaunchedEffect
        
        val airport = selectedPickupAirport
        if (airport != null) {
            Log.d(DebugTags.BookingProcess, "✈️ Pickup airport changed to: ${airport.displayName} - Triggering booking rates API")
            // Update pickup location to airport name
            pickupLocation = airport.displayName
            // Update coordinates from airport (matches iOS handleAirportSelection)
            val airportLat = airport.lat ?: 0.0
            val airportLong = airport.long ?: 0.0
            currentRideData = currentRideData.copy(
                pickupLocation = airport.displayName,
                pickupLat = airportLat,
                pickupLong = airportLong,
                selectedPickupAirport = airport.displayName
            )

            // Recalculate distance and time when airport changes (matches iOS)
            coroutineScope.launch {
                if (airportLat != 0.0 && airportLong != 0.0 &&
                    currentRideData.destinationLat != null && currentRideData.destinationLong != null) {
                    try {
                        distancesLoading = true
                        val (distanceMeters, durationSeconds) = directionsService.calculateDistance(
                            airportLat, airportLong,
                            currentRideData.destinationLat!!, currentRideData.destinationLong!!
                        )
                        val (distanceText, _) = directionsService.formatDistance(distanceMeters)
                        val (durationText, _) = directionsService.formatDuration(durationSeconds)
                        outboundDistance = Pair(distanceText, durationText)
                        Log.d(DebugTags.BookingProcess, "📍 Updated distance from airport: $distanceText, duration: $durationText")
                    } catch (e: Exception) {
                        Log.e(DebugTags.BookingProcess, "Error recalculating distance from airport", e)
                    } finally {
                        distancesLoading = false
                    }
                }

                // Automatically call booking rates API when pickup airport is selected - matches iOS
                // CRITICAL: Use uiState.rideData to get latest coordinates
                val latestRideData = uiState.rideData
                val updatedRideData = latestRideData.copy(
                    // CRITICAL: Always set serviceType from selectedServiceType to avoid stale values
                    serviceType = when (selectedServiceType) {
                        "One Way" -> "one_way"
                        "Round Trip" -> "round_trip"
                        "Charter Tour" -> "charter_tour"
                        else -> latestRideData.serviceType
                    },
                    selectedPickupAirport = airport.displayName
                )
                vm.fetchBookingRates(
                    ride = updatedRideData,
                    vehicle = currentVehicle,
                    isEditMode = isEditMode,
                    editBookingId = editBookingId,
                    hasExtraStops = extraStops.any { it.isLocationSelected && it.latitude != null },
                    extraStops = extraStops.toExtraStopRequestsWithTownComparison(placesService, pickupLocation),
                    returnExtraStops = returnExtraStops.toExtraStopRequestsWithTownComparison(placesService, returnPickupLocation),
                    returnPickupLat = returnPickupLat,
                    returnPickupLong = returnPickupLong,
                    returnDropoffLat = returnDropoffLat,
                    returnDropoffLong = returnDropoffLong
                )
            }
        }
    }

    // Trigger API when dropoff airport changes - matches iOS
    LaunchedEffect(selectedDropoffAirport, isViewModelInitialized) {
        if (!isViewModelInitialized) return@LaunchedEffect
        val airport = selectedDropoffAirport
        if (airport != null) {
            Log.d(DebugTags.BookingProcess, "✈️ Dropoff airport changed to: ${airport.displayName} - Triggering booking rates API")
            // Update dropoff location to airport name
            dropoffLocation = airport.displayName
            // Update coordinates from airport (matches iOS handleAirportSelection)
            val airportLat = airport.lat ?: 0.0
            val airportLong = airport.long ?: 0.0
            currentRideData = currentRideData.copy(
                destinationLocation = airport.displayName,
                destinationLat = airportLat,
                destinationLong = airportLong,
                selectedDestinationAirport = airport.displayName
            )

            // Recalculate distance and time when airport changes (matches iOS)
            coroutineScope.launch {
                if (currentRideData.pickupLat != null && currentRideData.pickupLong != null &&
                    airportLat != 0.0 && airportLong != 0.0) {
                    try {
                        distancesLoading = true
                        val (distanceMeters, durationSeconds) = directionsService.calculateDistance(
                            currentRideData.pickupLat!!, currentRideData.pickupLong!!,
                            airportLat, airportLong
                        )
                        val (distanceText, _) = directionsService.formatDistance(distanceMeters)
                        val (durationText, _) = directionsService.formatDuration(durationSeconds)
                        outboundDistance = Pair(distanceText, durationText)
                        Log.d(DebugTags.BookingProcess, "📍 Updated distance from airport: $distanceText, duration: $durationText")
                    } catch (e: Exception) {
                        Log.e(DebugTags.BookingProcess, "Error recalculating distance from airport", e)
                    } finally {
                        distancesLoading = false
                    }
                }

                // Automatically call booking rates API when dropoff airport is selected - matches iOS
                // CRITICAL: Use uiState.rideData to get latest coordinates (setPickupLocation/setDropoffLocation update ViewModel state)
                val latestRideData = uiState.rideData
                val updatedRideData = latestRideData.copy(
                    // CRITICAL: Always set serviceType from selectedServiceType to avoid stale values
                    serviceType = when (selectedServiceType) {
                        "One Way" -> "one_way"
                        "Round Trip" -> "round_trip"
                        "Charter Tour" -> "charter_tour"
                        else -> latestRideData.serviceType
                    },
                    selectedDestinationAirport = airport.displayName
                )
                vm.fetchBookingRates(
                    ride = updatedRideData,
                    vehicle = currentVehicle,
                    isEditMode = isEditMode,
                    editBookingId = editBookingId,
                    hasExtraStops = extraStops.any { it.isLocationSelected && it.latitude != null },
                    extraStops = extraStops.toExtraStopRequestsWithTownComparison(placesService, pickupLocation),
                    returnExtraStops = returnExtraStops.toExtraStopRequestsWithTownComparison(placesService, returnPickupLocation),
                    returnPickupLat = returnPickupLat,
                    returnPickupLong = returnPickupLong,
                    returnDropoffLat = returnDropoffLat,
                    returnDropoffLong = returnDropoffLong
                )
            }
        }
    }

    // Helper function to check if time is in early morning hours (12 AM - 5 AM) - matches iOS
    fun isEarlyMorningHours(timeString: String): Boolean {
        return try {
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val time = timeFormat.parse(timeString)
            if (time != null) {
                val calendar = Calendar.getInstance().apply { this.time = time }
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                hour >= 0 && hour <= 5
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    // Track previous pickup time to detect changes
    var previousPickupTime by remember { mutableStateOf(pickupTime) }
    var isInitialTimeLoad by remember { mutableStateOf(true) }

    // Trigger API when pickup time changes - matches iOS refreshRates() behavior
    LaunchedEffect(pickupTime, isViewModelInitialized) {
        if (!isViewModelInitialized) return@LaunchedEffect
        
        // Skip on initial load
        if (isInitialTimeLoad) {
            isInitialTimeLoad = false
            previousPickupTime = pickupTime
            return@LaunchedEffect
        }

        // Trigger API for ANY pickup time change (matches iOS behavior)
        if (pickupTime != previousPickupTime) {
            Log.d(DebugTags.BookingProcess, "🕐 Pickup time changed: $previousPickupTime -> $pickupTime")
            Log.d(DebugTags.BookingProcess, "  Triggering booking rates API")

            coroutineScope.launch {
                // CRITICAL: Use uiState.rideData to get latest coordinates
                val latestRideData = uiState.rideData
                val updatedRideData = latestRideData.copy(
                    // CRITICAL: Always set serviceType from selectedServiceType to avoid stale values
                    serviceType = when (selectedServiceType) {
                        "One Way" -> "one_way"
                        "Round Trip" -> "round_trip"
                        "Charter Tour" -> "charter_tour"
                        else -> latestRideData.serviceType
                    },
                    pickupTime = if (pickupTime.isNotEmpty()) pickupTime else latestRideData.pickupTime,
                    pickupDate = if (pickupDate.isNotEmpty()) pickupDate else latestRideData.pickupDate,
                    returnPickupTime = if (returnPickupTime.isNotEmpty()) returnPickupTime else (latestRideData.returnPickupTime ?: "")
                )
                Log.d(DebugTags.BookingProcess, "🕐 Pickup time changed - local='$pickupTime', rideData='${latestRideData.pickupTime}', final='${updatedRideData.pickupTime}'")
                vm.fetchBookingRates(
                    ride = updatedRideData,
                    vehicle = currentVehicle,
                    isEditMode = isEditMode,
                    editBookingId = editBookingId,
                    hasExtraStops = extraStops.any { it.isLocationSelected && it.latitude != null },
                    extraStops = extraStops.toExtraStopRequestsWithTownComparison(placesService, pickupLocation),
                    returnExtraStops = returnExtraStops.toExtraStopRequestsWithTownComparison(placesService, returnPickupLocation),
                    returnPickupLat = returnPickupLat,
                    returnPickupLong = returnPickupLong,
                    returnDropoffLat = returnDropoffLat,
                    returnDropoffLong = returnDropoffLong
                )
            }
        }

        previousPickupTime = pickupTime
    }

    // Track previous extra stops size to detect add/remove operations (not location updates)
    var previousExtraStopsSize by remember { mutableStateOf(extraStops.size) }

    // Trigger API when extra stops list changes (add/remove operations) - matches iOS
    // Note: Location selection already triggers API, so this only handles add/remove
    LaunchedEffect(extraStops.size) {
        // Only trigger if size actually changed (add/remove operation)
        if (extraStops.size != previousExtraStopsSize) {
            val hasStopsWithLocations = extraStops.any { it.isLocationSelected && it.latitude != null }
            Log.d(DebugTags.BookingProcess, "📍 Extra stops list changed (size: ${previousExtraStopsSize} -> ${extraStops.size}) - Triggering booking rates API")
            // CRITICAL: Use uiState.rideData to get latest coordinates
            val latestRideData = uiState.rideData
            vm.fetchBookingRates(
                ride = latestRideData,
                vehicle = currentVehicle,
                isEditMode = isEditMode,
                editBookingId = editBookingId,
                hasExtraStops = hasStopsWithLocations,
                extraStops = extraStops.toExtraStopRequestsWithTownComparison(placesService, pickupLocation),
                returnExtraStops = returnExtraStops.toExtraStopRequestsWithTownComparison(placesService, returnPickupLocation),
                returnPickupLat = returnPickupLat,
                returnPickupLong = returnPickupLong,
                returnDropoffLat = returnDropoffLat,
                returnDropoffLong = returnDropoffLong
            )
            previousExtraStopsSize = extraStops.size
        }
    }

    // Helper data class for rates (defined before use)
    data class RatesData(
        val subtotal: Double,
        val grandTotal: Double,
        val returnSubtotal: Double,
        val returnGrandTotal: Double
    )

    // Calculate rates from booking rates API response (matches iOS)
    // Use booking rates data if available, otherwise fallback to vehicle rate breakdown
    // CRITICAL FIX: Watch currentRideData and selectedServiceType so rates update when service type/vehicles change
    val ratesData: RatesData = remember(
        bookingRatesData,
        currentRideData.serviceType,
        currentRideData.bookingHour,
        currentRideData.noOfVehicles,
        selectedServiceType
    ) {
        val bookingData = bookingRatesData

        if (bookingData != null) {
            val rateArray = bookingData.rateArray

            var totalBaserate = 0.0
            var allInclusiveBaserate = 0.0

            val minRateInvolved = bookingData.minRateInvolved ?: false

            val hoursMultiplier =
                if (currentRideData.serviceType.lowercase() == "charter_tour" && !minRateInvolved) {
                    // Parse bookingHour - it should be stored as "2", "3", etc. (not "2 hours minimum")
                    val hoursString = currentRideData.bookingHour ?: "0"
                    // Handle case where it might still have " hours" suffix
                    val cleanedHours = hoursString.replace(" hours minimum", "").replace(" hours", "").trim()
                    val multiplier = cleanedHours.toIntOrNull() ?: 1
                    Log.d(DebugTags.BookingProcess, "📊 hoursMultiplier calculation - bookingHour='$hoursString', cleaned='$cleanedHours', multiplier=$multiplier")
                    multiplier
                } else {
                    1
                }

            Log.d(DebugTags.BookingProcess, "===========================================")
            Log.d(DebugTags.BookingProcess, "📊 UI COMPOSABLE RATES CALCULATION ${currentRideData.toString()}")
            Log.d(DebugTags.BookingProcess, "📊 UI COMPOSABLE RATES CALCULATION ${currentRideData.toString()}")

            Log.d(DebugTags.BookingProcess, "📊 UI COMPOSABLE RATES CALCULATION")

            Log.d(DebugTags.BookingProcess, "===========================================")

            /* ---------------- ALL INCLUSIVE RATES ---------------- */

            for ((key, rateItem) in rateArray.allInclusiveRates) {
                val adjustedBaserate =
                    if (
                        key == "Base_Rate" &&
                        currentRideData.serviceType.lowercase() == "charter_tour" &&
                        !minRateInvolved
                    ) {
                        rateItem.baserate * hoursMultiplier
                    } else {
                        rateItem.baserate
                    }

                totalBaserate += adjustedBaserate
                allInclusiveBaserate += adjustedBaserate
            }

            /* ---------------- AMENITIES ---------------- */

            for (rateItem in rateArray.amenities.values) {
                totalBaserate += rateItem.baserate
            }

            /* ---------------- TAXES ---------------- */

            for (taxItem in rateArray.taxes.values) {
                totalBaserate += taxItem.amount
            }

            /* ---------------- MISC ---------------- */

            for (rateItem in rateArray.misc.values) {
                totalBaserate += rateItem.baserate
            }

            /* ---------------- 25% OF ALL INCLUSIVE (iOS LOGIC) ---------------- */

            val twentyFivePercentOfAllInclusive = allInclusiveBaserate * 0.25
            val calculatedSubtotal = totalBaserate + twentyFivePercentOfAllInclusive
            val calculatedGrandTotal = calculatedSubtotal * currentRideData.noOfVehicles
            
            // ENHANCED LOGGING: Trace UI calculation
            Log.d(DebugTags.BookingProcess, "===========================================")
            Log.d(DebugTags.BookingProcess, "📊 UI COMPOSABLE RATES CALCULATION")
            Log.d(DebugTags.BookingProcess, "===========================================")
            Log.d(DebugTags.BookingProcess, "📊 totalBaserate: $totalBaserate")
            Log.d(DebugTags.BookingProcess, "📊 allInclusiveBaserate: $allInclusiveBaserate")
            Log.d(DebugTags.BookingProcess, "📊 twentyFivePercentOfAllInclusive: $twentyFivePercentOfAllInclusive")
            Log.d(DebugTags.BookingProcess, "📊 calculatedSubtotal: $calculatedSubtotal")
            Log.d(DebugTags.BookingProcess, "📊 currentRideData.noOfVehicles: ${currentRideData.noOfVehicles}")
            Log.d(DebugTags.BookingProcess, "📊 calculatedGrandTotal: $calculatedGrandTotal")
            Log.d(DebugTags.BookingProcess, "===========================================")

            /* ================= RETURN TRIP ================= */

            val (calculatedReturnSubtotal, calculatedReturnGrandTotal) =
                if (selectedServiceType == "Round Trip" && bookingData.retrunRateArray != null) {

                    val returnRateArray = bookingData.retrunRateArray
                    var returnTotalBaserate = 0.0
                    var returnAllInclusiveBaserate = 0.0

                    for ((key, rateItem) in returnRateArray.allInclusiveRates) {
                        val adjustedBaserate =
                            if (
                                key == "Base_Rate" &&
                                currentRideData.serviceType.lowercase() == "charter_tour" &&
                                !minRateInvolved
                            ) {
                                rateItem.baserate * hoursMultiplier
                            } else {
                                rateItem.baserate
                            }

                        returnTotalBaserate += adjustedBaserate
                        returnAllInclusiveBaserate += adjustedBaserate
                    }

                    for (rateItem in returnRateArray.amenities.values) {
                        returnTotalBaserate += rateItem.baserate
                    }

                    for (taxItem in returnRateArray.taxes.values) {
                        returnTotalBaserate += taxItem.amount
                    }

                    for (rateItem in returnRateArray.misc.values) {
                        returnTotalBaserate += rateItem.baserate
                    }

                    val returnTwentyFivePercent = returnAllInclusiveBaserate * 0.25
                    val returnSubtotal = returnTotalBaserate + returnTwentyFivePercent
                    val returnGrandTotal = returnSubtotal * currentRideData.noOfVehicles

                    Pair(returnSubtotal, returnGrandTotal)
                } else {
                    Pair(0.0, 0.0)
                }

            val finalRatesData = RatesData(
                calculatedSubtotal,
                calculatedGrandTotal,
                calculatedReturnSubtotal,
                calculatedReturnGrandTotal
            )
            
            // ENHANCED LOGGING: Final UI rates data
            Log.d(DebugTags.BookingProcess, "===========================================")
            Log.d(DebugTags.BookingProcess, "✅ UI COMPOSABLE FINAL RATES DATA")
            Log.d(DebugTags.BookingProcess, "===========================================")
            Log.d(DebugTags.BookingProcess, "✅ finalRatesData.subtotal: ${finalRatesData.subtotal}")
            Log.d(DebugTags.BookingProcess, "✅ finalRatesData.grandTotal: ${finalRatesData.grandTotal}")
            Log.d(DebugTags.BookingProcess, "===========================================")
            
            finalRatesData
        } else {
            val rateBreakdown = currentVehicle.getRateBreakdown(currentRideData.serviceType)
            val fallbackSubtotal = rateBreakdown?.subTotal ?: rateBreakdown?.total ?: 0.0
            val fallbackGrandTotal =
                (rateBreakdown?.grandTotal ?: rateBreakdown?.total ?: 0.0) *
                        currentRideData.noOfVehicles

            Log.d(DebugTags.BookingProcess, "⚠️ Using fallback rates - subtotal: $fallbackSubtotal, grandTotal: $fallbackGrandTotal")
            RatesData(fallbackSubtotal, fallbackGrandTotal, 0.0, 0.0)
        }
    }


    val subtotal = ratesData.subtotal
    val grandTotal = ratesData.grandTotal
    
    // ENHANCED LOGGING: Values being passed to UI
    LaunchedEffect(subtotal, grandTotal) {
        Log.d(DebugTags.BookingProcess, "===========================================")
        Log.d(DebugTags.BookingProcess, "🎨 UI DISPLAY VALUES")
        Log.d(DebugTags.BookingProcess, "===========================================")
        Log.d(DebugTags.BookingProcess, "🎨 subtotal (from ratesData): $subtotal")
        Log.d(DebugTags.BookingProcess, "🎨 grandTotal (from ratesData): $grandTotal")
        Log.d(DebugTags.BookingProcess, "===========================================")
    }
    val returnSubtotal = ratesData.returnSubtotal
    val returnGrandTotal = ratesData.returnGrandTotal
    // Get currency symbol from API response (matches iOS bookingRatesService.currency?.symbol ?? "$")
    val currencySymbol = bookingRatesCurrency?.symbol ?: "$"

    // Helper function to parse distance/duration from editData strings
    fun parseDistanceFromEditData(distanceStr: String?): String {
        if (distanceStr.isNullOrEmpty()) return "0"
        // Distance format from API: "X km" or "X m" or just a number
        return distanceStr.trim()
    }

    fun parseDurationFromEditData(durationStr: String?): String {
        if (durationStr.isNullOrEmpty()) return "0 mins"
        // Duration format from API: "X hours Y mins" or "X mins" or just a number
        return durationStr.trim()
    }

    // Load distance/duration from editData when in edit mode (priority over calculated values)
    LaunchedEffect(editData, isEditMode) {
        if (isEditMode && editData != null) {
            val data = editData!!
            Log.d(DebugTags.BookingProcess, "🔄 Loading distance/duration from editData")
            Log.d(DebugTags.BookingProcess, "  Distance: ${data.distance}")
            Log.d(DebugTags.BookingProcess, "  Duration: ${data.duration}")

            // Use editData distance/duration if available
            if (!data.distance.isNullOrEmpty() && !data.duration.isNullOrEmpty()) {
                outboundDistance = Pair(
                    parseDistanceFromEditData(data.distance),
                    parseDurationFromEditData(data.duration)
                )
                Log.d(DebugTags.BookingProcess, "✅ Set outbound distance from editData: ${outboundDistance?.first}, ${outboundDistance?.second}")

                // For round trips, return distance/duration would be in return fields if available
                // For now, we'll calculate return distance if needed
                if (selectedServiceType == "Round Trip") {
                    // Return distance might not be in editData, so we'll calculate it if coordinates are available
                    // But prefer editData if it exists
                    returnDistance = null // Will be calculated below if coordinates are available
                }
            }
        }
    }

    // Calculate and cache travel info (matches iOS calculateAndCacheTravelInfo)
    // Recalculate immediately when coordinates, transfer type, airports, or extra stops change
    LaunchedEffect(
        uiState.rideData.pickupLat, uiState.rideData.pickupLong,
        uiState.rideData.destinationLat, uiState.rideData.destinationLong,
        selectedTransferType, selectedPickupAirport, selectedDropoffAirport,
        selectedServiceType, isEditMode, editData, extraStops,
        isViewModelInitialized, pickupLocation, dropoffLocation
    ) {
        // Wait for ViewModel initialization
        if (!isViewModelInitialized) return@LaunchedEffect
        
        // In edit mode, only calculate if we don't have distance/duration from editData
        val currentEditData = editData
        if (isEditMode && currentEditData != null && !currentEditData.distance.isNullOrEmpty() && !currentEditData.duration.isNullOrEmpty()) {
            Log.d(DebugTags.BookingProcess, "⏭️ Skipping distance calculation - using editData values")
            return@LaunchedEffect
        }

        // Get effective coordinates based on transfer type (use uiState.rideData for latest coordinates)
        val effectivePickupCoord = getEffectiveOutboundPickupCoordinate(
            selectedTransferType,
            uiState.rideData.pickupLat,
            uiState.rideData.pickupLong,
            selectedPickupAirport
        )
        val effectiveDropoffCoord = getEffectiveOutboundDropoffCoordinate(
            selectedTransferType,
            uiState.rideData.destinationLat,
            uiState.rideData.destinationLong,
            selectedDropoffAirport
        )

        // Check if coordinates have changed (cache key comparison)
        val pickupCoordChanged = effectivePickupCoord != lastOutboundPickupCoord
        val dropoffCoordChanged = effectiveDropoffCoord != lastOutboundDropoffCoord

        // Get waypoints from extra stops
        val waypoints = extraStops
            .filter { it.isLocationSelected && it.latitude != null && it.longitude != null }
            .map { Pair(it.latitude!!, it.longitude!!) }

        // Check if extra stops have changed
        val extraStopsChanged = waypoints != lastOutboundExtraStops

        // Only recalculate if coordinates changed, extra stops changed, or we don't have distance yet
        if (pickupCoordChanged || dropoffCoordChanged || extraStopsChanged || outboundDistance == null) {
            if (effectivePickupCoord.first != null && effectivePickupCoord.second != null &&
                effectiveDropoffCoord.first != null && effectiveDropoffCoord.second != null) {

                // Update cache keys
                lastOutboundPickupCoord = effectivePickupCoord
                lastOutboundDropoffCoord = effectiveDropoffCoord
                lastOutboundExtraStops = waypoints

                isCalculatingTravel = true
                distancesLoading = true
                Log.d(DebugTags.BookingProcess, "📍 Calculating outbound distance - Pickup: (${effectivePickupCoord.first}, ${effectivePickupCoord.second}), Dropoff: (${effectiveDropoffCoord.first}, ${effectiveDropoffCoord.second}), Extra stops: ${waypoints.size}")
                
                coroutineScope.launch {
                    try {
                        // Calculate outbound distance with waypoints
                        val waypointPairs = waypoints.map { Pair(it.first, it.second) }
                        val (distanceMeters, durationSeconds) = directionsService.calculateDistance(
                            effectivePickupCoord.first!!, effectivePickupCoord.second!!,
                            effectiveDropoffCoord.first!!, effectiveDropoffCoord.second!!,
                            waypointPairs.takeIf { it.isNotEmpty() }
                        )

                        val (distanceText, _) = directionsService.formatDistance(distanceMeters)
                        val (durationText, _) = directionsService.formatDuration(durationSeconds)
                        outboundDistance = Pair(distanceText, durationText)

                        // Format cached travel info (matches iOS format)
                        val distanceMiles = distanceMeters / 1609.34
                        val totalMinutes = (durationSeconds / 60).toInt()
                        val hours = totalMinutes / 60
                        val minutes = totalMinutes % 60
                        val distanceTextFormatted = String.format("%.1f", distanceMiles)
                        cachedTravelInfo = if (hours > 0) {
                            "$hours hours, $minutes minutes / $distanceTextFormatted miles"
                        } else {
                            "$minutes minutes / $distanceTextFormatted miles"
                        }

                        Log.d(DebugTags.BookingProcess, "✅ Distance calculated and cached: $cachedTravelInfo")
                    } catch (e: Exception) {
                        Log.e(DebugTags.BookingProcess, "❌ Error calculating distances", e)
                        e.printStackTrace()
                    } finally {
                        isCalculatingTravel = false
                        distancesLoading = false
                    }
                }
            } else {
                Log.d(DebugTags.BookingProcess, "⏭️ Skipping distance calculation - coordinates not available")
                distancesLoading = false
            }
        } else {
            Log.d(DebugTags.BookingProcess, "⏭️ Using cached travel info: $cachedTravelInfo")
        }

        // Calculate return distance for round trips - separate LaunchedEffect for better performance
        // This ensures return distance recalculates independently when return trip changes
    }
    
    // Separate LaunchedEffect for return trip distance calculation
    LaunchedEffect(
        returnPickupLat, returnPickupLong, returnDropoffLat, returnDropoffLong,
        selectedReturnTransferType, selectedReturnPickupAirport, selectedReturnDropoffAirport,
        selectedServiceType, returnExtraStops, isViewModelInitialized,
        returnPickupLocation, returnDropoffLocation
    ) {
        if (!isViewModelInitialized) return@LaunchedEffect
        if (selectedServiceType != "Round Trip") {
            returnDistance = null
            cachedReturnTravelInfo = null
            lastReturnPickupCoord = null
            lastReturnDropoffCoord = null
            lastReturnExtraStops = null
            return@LaunchedEffect
        }
        
        // Use return trip coordinates if available, otherwise use reversed outbound coordinates
        val returnPickupLatValue = returnPickupLat ?: uiState.rideData.destinationLat
        val returnPickupLongValue = returnPickupLong ?: uiState.rideData.destinationLong
        val returnDropoffLatValue = returnDropoffLat ?: uiState.rideData.pickupLat
        val returnDropoffLongValue = returnDropoffLong ?: uiState.rideData.pickupLong
        
        val returnTransferType = selectedReturnTransferType ?: getReversedTransferType(selectedTransferType)
        val effectiveReturnPickupCoord = getEffectiveOutboundPickupCoordinate(
            returnTransferType,
            returnPickupLatValue,
            returnPickupLongValue,
            selectedReturnPickupAirport ?: selectedDropoffAirport
        )
        val effectiveReturnDropoffCoord = getEffectiveOutboundDropoffCoordinate(
            returnTransferType,
            returnDropoffLatValue,
            returnDropoffLongValue,
            selectedReturnDropoffAirport ?: selectedPickupAirport
        )

        val returnPickupCoordChanged = effectiveReturnPickupCoord != lastReturnPickupCoord
        val returnDropoffCoordChanged = effectiveReturnDropoffCoord != lastReturnDropoffCoord

        // Get waypoints from return extra stops
        val returnWaypoints = returnExtraStops
            .filter { it.isLocationSelected && it.latitude != null && it.longitude != null }
            .map { Pair(it.latitude!!, it.longitude!!) }
        
        val returnExtraStopsChanged = returnWaypoints != lastReturnExtraStops

        if (returnPickupCoordChanged || returnDropoffCoordChanged || returnExtraStopsChanged || returnDistance == null) {
            if (effectiveReturnPickupCoord.first != null && effectiveReturnPickupCoord.second != null &&
                effectiveReturnDropoffCoord.first != null && effectiveReturnDropoffCoord.second != null) {

                lastReturnPickupCoord = effectiveReturnPickupCoord
                lastReturnDropoffCoord = effectiveReturnDropoffCoord
                lastReturnExtraStops = returnWaypoints

                isCalculatingReturnTravel = true
                Log.d(DebugTags.BookingProcess, "📍 Calculating return distance - Pickup: (${effectiveReturnPickupCoord.first}, ${effectiveReturnPickupCoord.second}), Dropoff: (${effectiveReturnDropoffCoord.first}, ${effectiveReturnDropoffCoord.second}), Extra stops: ${returnWaypoints.size}")
                
                coroutineScope.launch {
                    try {
                        // Extract waypoints from return extra stops
                        val waypointPairs = returnWaypoints.map { Pair(it.first, it.second) }
                            .takeIf { it.isNotEmpty() }

                        val (returnDistanceMeters, returnDurationSeconds) = directionsService.calculateDistance(
                            effectiveReturnPickupCoord.first!!, effectiveReturnPickupCoord.second!!,
                            effectiveReturnDropoffCoord.first!!, effectiveReturnDropoffCoord.second!!,
                            waypointPairs
                        )
                        val (returnDistanceText, _) = directionsService.formatDistance(returnDistanceMeters)
                        val (returnDurationText, _) = directionsService.formatDuration(returnDurationSeconds)
                        val previousReturnDistance = returnDistance
                        returnDistance = Pair(returnDistanceText, returnDurationText)

                        // Format cached return travel info
                        val returnDistanceMiles = returnDistanceMeters / 1609.34
                        val returnTotalMinutes = (returnDurationSeconds / 60).toInt()
                        val returnHours = returnTotalMinutes / 60
                        val returnMinutes = returnTotalMinutes % 60
                        val returnDistanceTextFormatted = String.format("%.1f", returnDistanceMiles)
                        cachedReturnTravelInfo = if (returnHours > 0) {
                            "$returnHours hours, $returnMinutes minutes / $returnDistanceTextFormatted miles"
                        } else {
                            "$returnMinutes minutes / $returnDistanceTextFormatted miles"
                        }

                        Log.d(DebugTags.BookingProcess, "✅ Return distance calculated and cached: $cachedReturnTravelInfo")
                        
                        // Trigger booking rates API when return distance changes (if both return coordinates are available)
                        if (previousReturnDistance != returnDistance && 
                            returnPickupLat != null && returnPickupLong != null &&
                            returnDropoffLat != null && returnDropoffLong != null) {
                            Log.d(DebugTags.BookingProcess, "🔄 Return distance changed - Triggering booking rates API")
                            vm.fetchBookingRates(
                                ride = uiState.rideData,
                                vehicle = currentVehicle,
                                isEditMode = isEditMode,
                                editBookingId = editBookingId,
                                hasExtraStops = hasExtraStops(editData),
                                extraStops = extraStops.toExtraStopRequests(),
                                returnExtraStops = returnExtraStops.toExtraStopRequests(),
                                returnPickupLat = returnPickupLat,
                                returnPickupLong = returnPickupLong,
                                returnDropoffLat = returnDropoffLat,
                                returnDropoffLong = returnDropoffLong
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(DebugTags.BookingProcess, "❌ Error calculating return distances", e)
                        e.printStackTrace()
                    } finally {
                        isCalculatingReturnTravel = false
                    }
                }
            } else {
                Log.d(DebugTags.BookingProcess, "⏭️ Skipping return distance calculation - coordinates not available")
            }
        } else {
            Log.d(DebugTags.BookingProcess, "⏭️ Using cached return travel info: $cachedReturnTravelInfo")
        }
    }

    // Date/Time formatters
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayDateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val displayTimeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    // Parse initial date and time - CRITICAL FIX: Combine date and time properly (matches iOS)
    var selectedDate by remember {
        val date = try {
            dateFormatter.parse(pickupDate) ?: Date()
        } catch (e: Exception) {
            Date()
        }
        mutableStateOf(date)
    }

    var selectedTime by remember {
        // Parse date and time separately, then combine them (matches iOS lines 902-924)
        val parsedDate = try {
            dateFormatter.parse(pickupDate) ?: Date()
        } catch (e: Exception) {
            Date()
        }

        val parsedTime = try {
            // Try multiple time formats to handle different API response formats
            val formats = listOf("HH:mm:ss", "HH:mm", "h:mm a", "h:mm:ss a")
            var parsed: Date? = null
            for (format in formats) {
                try {
                    val formatter = SimpleDateFormat(format, Locale.getDefault())
                    parsed = formatter.parse(pickupTime)
                    if (parsed != null) {
                        Log.d(DebugTags.BookingProcess, "✅ Parsed initial pickup time '$pickupTime' using format '$format'")
                        break
                    }
                } catch (e: Exception) {
                    // Try next format
                }
            }
            if (parsed == null && pickupTime.isNotEmpty()) {
                Log.w(DebugTags.BookingProcess, "⚠️ Could not parse initial pickup time: '$pickupTime'")
            }
            parsed
        } catch (e: Exception) {
            Log.e(DebugTags.BookingProcess, "❌ Error parsing initial pickup time: '$pickupTime'", e)
            null
        }

        // Combine date and time components properly
        val calendar = Calendar.getInstance()
        calendar.time = parsedDate

        if (parsedTime != null) {
            val timeCalendar = Calendar.getInstance()
            timeCalendar.time = parsedTime
            // Set time components from parsed time to the date calendar
            calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
            calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
            calendar.set(Calendar.SECOND, timeCalendar.get(Calendar.SECOND))
            calendar.set(Calendar.MILLISECOND, 0)
        } else {
            // Fallback to 12:00 PM if time parsing fails
            calendar.set(Calendar.HOUR_OF_DAY, 12)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }

        mutableStateOf(calendar.time)
    }

    // CRITICAL FIX: Update selectedTime when pickupTime or pickupDate changes (e.g., from edit data)
    LaunchedEffect(pickupTime, pickupDate) {
        // Combine date and time properly when they change
        Log.d(DebugTags.BookingProcess, "🔄 LaunchedEffect triggered - pickupDate: '$pickupDate', pickupTime: '$pickupTime'")
        val parsedDate = try {
            dateFormatter.parse(pickupDate) ?: Date()
        } catch (e: Exception) {
            Log.e(DebugTags.BookingProcess, "❌ Error parsing pickup date: '$pickupDate'", e)
            Date()
        }

        val parsedTime = try {
            // Try multiple time formats to handle different API response formats
            val formats = listOf("HH:mm:ss", "HH:mm", "h:mm a", "h:mm:ss a")
            var parsed: Date? = null
            for (format in formats) {
                try {
                    val formatter = SimpleDateFormat(format, Locale.getDefault())
                    parsed = formatter.parse(pickupTime)
                    if (parsed != null) {
                        Log.d(DebugTags.BookingProcess, "✅ Parsed pickup time '$pickupTime' using format '$format'")
                        break
                    }
                } catch (e: Exception) {
                    // Try next format
                }
            }
            if (parsed == null && pickupTime.isNotEmpty()) {
                Log.w(DebugTags.BookingProcess, "⚠️ Could not parse pickup time: '$pickupTime'")
            }
            parsed
        } catch (e: Exception) {
            Log.e(DebugTags.BookingProcess, "❌ Error parsing pickup time: '$pickupTime'", e)
            null
        }

        // Combine date and time components properly
        val calendar = Calendar.getInstance()
        calendar.time = parsedDate

        if (parsedTime != null) {
            val timeCalendar = Calendar.getInstance()
            timeCalendar.time = parsedTime
            // Set time components from parsed time to the date calendar
            calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
            calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
            calendar.set(Calendar.SECOND, timeCalendar.get(Calendar.SECOND))
            calendar.set(Calendar.MILLISECOND, 0)
        } else {
            // Fallback to 12:00 PM if time parsing fails
            calendar.set(Calendar.HOUR_OF_DAY, 12)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
        }

        selectedTime = calendar.time
        selectedDate = parsedDate
    }

    LaunchedEffect(result) {
        // CRITICAL FIX: Only process result if booking was actually initiated by user
        // This prevents old results from previous bookings from triggering success screen
        if (!bookingInitiated) {
            Log.d(DebugTags.BookingProcess, "⏭️ Ignoring result - booking not initiated yet")
            return@LaunchedEffect
        }

        result?.onSuccess { res ->
            if (res.success) {
                // Show success screen if API call was successful (matches iOS behavior)
                // Pass the reservation data to the success callback
                val reservationData = res.data
                Log.d(DebugTags.BookingProcess, "✅ Booking created successfully: success=${res.success}, reservationId=${reservationData?.reservationId}, orderId=${reservationData?.orderId}, returnReservationId=${reservationData?.returnReservationId}, message=${res.message}")
                bookingInitiated = false // Reset flag after processing
                onSuccess(reservationData)
            }
        }
        result?.onFailure { error ->
            Log.e(DebugTags.BookingProcess, "❌ Booking creation failed", error)
            bookingInitiated = false // Reset flag on failure
        }
    }

    // Handle booking update success (edit mode)
    LaunchedEffect(updateResult) {
        // CRITICAL FIX: Only process updateResult if booking update was actually initiated by user
        // This prevents old results from previous updates from triggering success screen
        if (!bookingInitiated) {
            Log.d(DebugTags.BookingProcess, "⏭️ Ignoring updateResult - booking update not initiated yet")
            return@LaunchedEffect
        }

        updateResult?.onSuccess { res ->
            if (res.success == true) {
                // Show success screen if API call was successful (matches iOS behavior)
                // Convert EditReservationUpdateResponse to ReservationData format
                val reservationId = res.data?.reservationId?.toIntOrNull()
                val reservationData = com.example.limouserapp.data.model.booking.ReservationData(
                    reservationId = reservationId,
                    orderId = null, // Update response doesn't include orderId
                    returnReservationId = null // Update response doesn't include returnReservationId
                )
                Log.d(DebugTags.BookingProcess, "✅ Booking updated successfully: success=${res.success}, reservationId=${reservationId}, message=${res.message}")
                bookingInitiated = false // Reset flag after processing
                onSuccess(reservationData)
            }
        }
        updateResult?.onFailure { error ->
            Log.e(DebugTags.BookingProcess, "❌ Booking update failed", error)
            bookingInitiated = false // Reset flag on failure
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9F9)) // Light grey background
            .imePadding()
    ) {
        // Header - Matching My Cards header style
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                // CRITICAL FIX: Adds padding matching the system status bar height
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                }

                Text(
                    text = when (selectedServiceType) {
                        "One Way" -> "Create One way booking"
                        "Round Trip" -> "Create Round trip booking"
                        "Charter Tour" -> "Create Charter booking"
                        else -> "Create a booking"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = GoogleSansFamily,
                    color = Color.Black
                )

                // Dummy spacer to center the title perfectly
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        // Scrollable form content - use rememberScrollState so we can scroll to errors
        val scrollState = rememberScrollState()
        val density = LocalDensity.current
        
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .imePadding()
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Accounts Information Section (read-only display)
            AccountsInfoSection(profileData = profileData)

            Spacer(modifier = Modifier.height(24.dp))

            // Booking Details Section
            BookingDetailsSection(
                rideData = uiState.rideData,
                vehicle = uiState.vehicle ?: vehicle,
                pickupDate = uiState.pickupDate,
                pickupTime = uiState.pickupTime,
                selectedServiceType = uiState.serviceType.displayName,
                selectedTransferType = uiState.transferType.displayName,
                selectedHours = uiState.rideData.bookingHour,
                numberOfVehicles = uiState.rideData.noOfVehicles,
                serviceTypes = serviceTypes,
                transferTypes = transferTypes,
                hoursOptions = hoursOptions,
                showServiceTypeDropdown = showServiceTypeDropdown,
                showTransferTypeDropdown = showTransferTypeDropdown,
                showHoursDropdown = showHoursDropdown,
                onServiceTypeSelected = { vm.setServiceType(ServiceType.fromDisplayName(it)) },
                onTransferTypeSelected = { vm.setTransferType(TransferType.fromDisplayName(it)) },
                onHoursSelected = { vm.setHours(it.toIntOrNull() ?: 2) },
                onNumberOfVehiclesChange = { vm.setNumberOfVehicles(it.toString()) },
                onServiceTypeDropdownChange = { showServiceTypeDropdown = it },
                onTransferTypeDropdownChange = { showTransferTypeDropdown = it },
                onHoursDropdownChange = { showHoursDropdown = it },
                onDateClick = { showDatePicker = true },
                onTimeClick = { showTimePicker = true },
                isEditMode = isEditMode,
                passengerCount = uiState.passengerCount,
                luggageCount = uiState.luggageCount,
                onPassengerCountChange = { vm.setPassengerCount(it) },
                onLuggageCountChange = { vm.setLuggageCount(it) },
                // Meet & Greet fields (matches web app)
                selectedMeetAndGreet = selectedMeetAndGreet,
                meetAndGreetOptions = meetAndGreetOptions,
                showMeetAndGreetDropdown = showMeetAndGreetDropdown,
                onMeetAndGreetChange = { selectedMeetAndGreet = it },
                onMeetAndGreetDropdownChange = { showMeetAndGreetDropdown = it },
                // Error states - map validation errors to field errors
                serviceTypeError = hasError("service_type"),
                transferTypeError = hasError("transfer_type"),
                pickupDateTimeError = hasError("pickup_datetime"),
                charterHoursError = hasError("charter_hours"),
                // Error messages - show user-friendly messages below fields
                serviceTypeErrorMessage = if (hasError("service_type")) "Service type is required" else null,
                transferTypeErrorMessage = if (hasError("transfer_type")) "Transfer type is required" else null,
                pickupDateTimeErrorMessage = if (hasError("pickup_datetime")) "Pickup date and time are required" else null,
                charterHoursErrorMessage = if (hasError("charter_hours")) "Charter hours are required" else null
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Pick-up Section
            PickupSection(
                selectedTransferType = uiState.transferType.displayName,
                pickupLocation = uiState.pickupLocation,
                pickupDate = uiState.pickupDate,
                pickupTime = uiState.pickupTime,
                pickupFlightNumber = uiState.rideData.pickupFlightNumber ?: "",
                originAirportCity = uiState.rideData.originAirportCity ?: "",
                cruiseShipName = cruiseShipName,
                shipArrivalTime = shipArrivalTime,
                cruisePort = cruisePort,
                selectedPickupAirport = selectedPickupAirport,
                selectedPickupAirline = selectedPickupAirline,
                onLocationSelected = { fullAddress, city, state, zipCode, locationDisplay, latitude, longitude, country ->
                    vm.setPickupLocation(locationDisplay, latitude, longitude)
                    Log.d(DebugTags.BookingProcess, "📍 Pickup location selected: $locationDisplay")
                    // Trigger rate update when pickup location changes
                    coroutineScope.launch {
                        // Use uiState.rideData to get latest coordinates after update
                        kotlinx.coroutines.delay(100) // Small delay to ensure ViewModel state is updated
                        val latestRideData = uiState.rideData
                        vm.fetchBookingRates(
                            ride = latestRideData,
                            vehicle = currentVehicle,
                            isEditMode = isEditMode,
                            editBookingId = editBookingId,
                            hasExtraStops = extraStops.any { it.isLocationSelected && it.latitude != null },
                            extraStops = extraStops.toExtraStopRequestsWithTownComparison(placesService, pickupLocation),
                            returnExtraStops = returnExtraStops.toExtraStopRequestsWithTownComparison(placesService, returnPickupLocation),
                            returnPickupLat = returnPickupLat,
                            returnPickupLong = returnPickupLong,
                            returnDropoffLat = returnDropoffLat,
                            returnDropoffLong = returnDropoffLong
                        )
                    }
                },
                onLocationChange = { vm.setPickupLocation(it) },
                onAirportClick = {
                    currentAirportType = "pickup"
                    showAirportBottomSheet = true
                },
                onAirlineClick = {
                    currentAirlineType = "pickup"
                    showAirlineBottomSheet = true
                },
                onDateClick = { showDatePicker = true },
                onTimeClick = { showTimePicker = true },
                onFlightNumberChange = { 
                    pickupFlightNumber = it // Update local state for payload
                    vm.setFlightInfo(pickupFlightNumber = it) // Update ViewModel
                },
                onOriginCityChange = { 
                    originAirportCity = it // Update local state for payload
                    vm.setAirportInfo(originAirportCity = it) // Update ViewModel
                },
                onCruiseShipChange = { 
                    cruiseShipName = it
                    vm.setCruisePickupInfo(cruisePort, it, shipArrivalTime)
                },
                onShipArrivalChange = { shipArrivalTime = it },
                onShipArrivalClick = { showShipArrivalTimePicker = true },
                onCruisePortChange = { 
                    cruisePort = it
                    vm.setCruisePickupInfo(it, cruiseShipName, shipArrivalTime)
                },
                // Error states - map validation errors to field errors
                pickupLocationError = hasError("locations") || hasError("pickup_coordinates"),
                pickupCoordinatesError = hasError("pickup_coordinates"),
                pickupAddressValidationError = uiState.addressValidationErrors["pickup_location"], // Address validation error from Directions API
                airportPickupError = hasError("pickup_airport") || hasError("pickup_airline") || hasError("pickup_flight_number") || hasError("origin_airport_city"),
                cruisePickupError = hasError("cruise_pickup_port") || hasError("cruise_pickup_ship"),
                cruisePickupPortError = hasError("cruise_pickup_port"),
                cruisePickupShipError = hasError("cruise_pickup_ship"),
                pickupDateTimeError = hasError("pickup_datetime"),
                // Specific airport field errors
                pickupAirportError = hasError("pickup_airport"),
                pickupAirlineError = hasError("pickup_airline"),
                pickupFlightError = hasError("pickup_flight_number"),
                originCityError = hasError("origin_airport_city")
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Drop-off Section
            DropoffSection(
                selectedTransferType = uiState.transferType.displayName,
                dropoffLocation = uiState.dropoffLocation,
                dropoffFlightNumber = uiState.rideData.dropoffFlightNumber ?: "",
                cruiseShipName = cruiseShipName,
                shipArrivalTime = shipArrivalTime,
                cruisePort = cruisePort,
                selectedDropoffAirport = selectedDropoffAirport,
                selectedDropoffAirline = selectedDropoffAirline,
                onLocationSelected = { fullAddress, city, state, zipCode, locationDisplay, latitude, longitude, country ->
                    vm.setDropoffLocation(locationDisplay, latitude, longitude)
                    Log.d(DebugTags.BookingProcess, "📍 Dropoff location selected: $locationDisplay")
                    // Trigger rate update when dropoff location changes
                    coroutineScope.launch {
                        // Use uiState.rideData to get latest coordinates after update
                        kotlinx.coroutines.delay(100) // Small delay to ensure ViewModel state is updated
                        val latestRideData = uiState.rideData
                        vm.fetchBookingRates(
                            ride = latestRideData,
                            vehicle = currentVehicle,
                            isEditMode = isEditMode,
                            editBookingId = editBookingId,
                            hasExtraStops = extraStops.any { it.isLocationSelected && it.latitude != null },
                            extraStops = extraStops.toExtraStopRequestsWithTownComparison(placesService, pickupLocation),
                            returnExtraStops = returnExtraStops.toExtraStopRequestsWithTownComparison(placesService, returnPickupLocation),
                            returnPickupLat = returnPickupLat,
                            returnPickupLong = returnPickupLong,
                            returnDropoffLat = returnDropoffLat,
                            returnDropoffLong = returnDropoffLong
                        )
                    }
                },
                onLocationChange = { vm.setDropoffLocation(it) },
                onAirportClick = {
                    currentAirportType = "dropoff"
                    showAirportBottomSheet = true
                },
                onAirlineClick = {
                    currentAirlineType = "dropoff"
                    showAirlineBottomSheet = true
                },
                onFlightNumberChange = { 
                    dropoffFlightNumber = it // Update local state for payload
                    vm.setFlightInfo(dropoffFlightNumber = it) // Update ViewModel
                },
                onCruiseShipChange = { 
                    cruiseShipName = it
                    vm.setCruiseDropoffInfo(cruisePort, it, shipArrivalTime)
                },
                onShipArrivalChange = { shipArrivalTime = it },
                onShipArrivalClick = { showDropoffShipArrivalTimePicker = true },
                onCruisePortChange = { 
                    cruisePort = it
                    vm.setCruiseDropoffInfo(it, cruiseShipName, shipArrivalTime)
                },
                // Error states - map validation errors to field errors
                dropoffLocationError = hasError("dropoff_location") || hasError("locations") || hasError("dropoff_coordinates"),
                dropoffCoordinatesError = hasError("dropoff_coordinates"),
                dropoffAddressValidationError = uiState.addressValidationErrors["dropoff_location"], // Address validation error from Directions API
                airportDropoffError = hasError("dropoff_airport") || hasError("dropoff_airline"),
                cruiseDropoffError = hasError("cruise_dropoff_port") || hasError("cruise_dropoff_ship"),
                cruiseDropoffPortError = hasError("cruise_dropoff_port"),
                cruiseDropoffShipError = hasError("cruise_dropoff_ship"),
                // Specific airport field errors
                dropoffAirportError = hasError("dropoff_airport"),
                dropoffAirlineError = hasError("dropoff_airline")
                // Note: dropoff flight is NOT required (matches web app)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Outbound Distance Information Section (positioned right after Drop-off Section)
            // For one-way: Shows "TOTAL TRIP" distance/time
            // For round trip: Shows "OUTBOUND TRIP" distance/time
            DistanceInformationSection(
                outboundDistance = outboundDistance ?: Pair("", ""),
                serviceType = selectedServiceType,
                isLoading = distancesLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Extra Stops Section (positioned right after Distance Information)
            ExtraStopsSection(
                extraStops = extraStops,
                onAddStop = {
                    extraStops = extraStops + ExtraStop()
                },
                onRemoveStop = { stop ->
                    val updatedStops = extraStops.filter { it.id != stop.id }
                    extraStops = updatedStops
                    // Refresh booking rates when stop is removed
                    coroutineScope.launch {
                        // CRITICAL: Use uiState.rideData to get latest coordinates
                        val latestRideData = uiState.rideData
                            vm.fetchBookingRates(
                                ride = latestRideData,
                                vehicle = currentVehicle,
                                isEditMode = isEditMode,
                                editBookingId = editBookingId,
                                hasExtraStops = updatedStops.any { it.isLocationSelected && it.latitude != null },
                                extraStops = updatedStops.toExtraStopRequestsWithTownComparison(placesService, pickupLocation),
                                returnExtraStops = returnExtraStops.toExtraStopRequestsWithTownComparison(placesService, returnPickupLocation),
                                returnPickupLat = returnPickupLat,
                                returnPickupLong = returnPickupLong,
                                returnDropoffLat = returnDropoffLat,
                                returnDropoffLong = returnDropoffLong
                            )
                    }
                },
                onLocationSelected = { stop, fullAddress, city, state, zipCode, locationDisplay, latitude, longitude, country ->
                    // Validate the stop
                    val errorMessage = validateExtraStop(
                        stop = stop.copy(
                            address = locationDisplay,
                            latitude = latitude,
                            longitude = longitude,
                            isLocationSelected = true
                        ),
                        pickupLocation = pickupLocation,
                        dropoffLocation = dropoffLocation,
                        pickupLat = currentRideData.pickupLat,
                        pickupLong = currentRideData.pickupLong,
                        dropoffLat = currentRideData.destinationLat,
                        dropoffLong = currentRideData.destinationLong
                    )

                    if (errorMessage != null) {
                        // Validation failed - show error and don't update stop
                        showValidationError(errorMessage)
                        // Clear the error after 3 seconds
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(3000)
                            showInvalidLocationDialog = false
                        }
                    } else {
                        // Validation passed - update stop
                        val updatedStops = extraStops.map {
                            if (it.id == stop.id) {
                                it.copy(
                                    address = locationDisplay,
                                    latitude = latitude,
                                    longitude = longitude,
                                    isLocationSelected = true
                                )
                            } else {
                                it
                            }
                        }
                        extraStops = updatedStops
                        // Automatically call booking rates API when extra stop location is selected
                        coroutineScope.launch {
                            // CRITICAL: Use uiState.rideData to get latest coordinates
                            // Use updatedStops list (after update) instead of extraStops (before update)
                            val latestRideData = uiState.rideData
                            vm.fetchBookingRates(
                                ride = latestRideData,
                                vehicle = currentVehicle,
                                isEditMode = isEditMode,
                                editBookingId = editBookingId,
                                hasExtraStops = updatedStops.any { it.isLocationSelected && it.latitude != null },
                                extraStops = updatedStops.toExtraStopRequestsWithTownComparison(placesService, pickupLocation),
                                returnExtraStops = returnExtraStops.toExtraStopRequestsWithTownComparison(placesService, returnPickupLocation),
                                returnPickupLat = returnPickupLat,
                                returnPickupLong = returnPickupLong,
                                returnDropoffLat = returnDropoffLat,
                                returnDropoffLong = returnDropoffLong
                            )
                        }
                    }
                },
                onLocationChange = { stop, newValue ->
                    extraStops = extraStops.map {
                        if (it.id == stop.id) {
                            it.copy(address = newValue)
                        } else {
                            it
                        }
                    }
                },
                onInstructionsChange = { stop, newInstructions ->
                    extraStops = extraStops.map {
                        if (it.id == stop.id) {
                            it.copy(bookingInstructions = newInstructions)
                        } else {
                            it
                        }
                    }
                }
            )

            // Return Trip Section (only for Round Trip - matches iOS)
            if (selectedServiceType == "Round Trip") {
                Spacer(modifier = Modifier.height(24.dp))

                ReturnJourneySection(
                    selectedReturnServiceType = selectedReturnServiceType,
                    selectedReturnTransferType = selectedReturnTransferType,
                    selectedReturnMeetAndGreet = selectedReturnMeetAndGreet,
                    returnPickupDate = returnPickupDate,
                    returnPickupTime = returnPickupTime,
                    returnPickupLocation = returnPickupLocation,
                    returnDropoffLocation = returnDropoffLocation,
                    returnPickupFlightNumber = returnPickupFlightNumber,
                    returnDropoffFlightNumber = returnDropoffFlightNumber,
                    returnOriginAirportCity = returnOriginAirportCity,
                    returnCruisePort = returnCruisePort,
                    returnCruiseShipName = returnCruiseShipName,
                    returnShipArrivalTime = returnShipArrivalTime,
                    returnSpecialInstructions = returnSpecialInstructions,
                    returnNumberOfVehicles = returnNumberOfVehicles,
                    selectedReturnHours = selectedReturnHours,
                    selectedReturnPickupAirport = selectedReturnPickupAirport,
                    selectedReturnDropoffAirport = selectedReturnDropoffAirport,
                    selectedReturnPickupAirline = selectedReturnPickupAirline,
                    selectedReturnDropoffAirline = selectedReturnDropoffAirline,
                    serviceTypes = serviceTypes,
                    transferTypes = transferTypes,
                    hoursOptions = hoursOptions,
                    meetAndGreetOptions = meetAndGreetOptions,
                    showReturnServiceTypeDropdown = showReturnServiceTypeDropdown,
                    showReturnTransferTypeDropdown = showReturnTransferTypeDropdown,
                    showReturnMeetAndGreetDropdown = showReturnMeetAndGreetDropdown,
                    showReturnDatePicker = showReturnDatePicker,
                    showReturnTimePicker = showReturnTimePicker,
                    onReturnServiceTypeSelected = { selectedReturnServiceType = it },
                    onReturnTransferTypeSelected = { 
                        selectedReturnTransferType = it
                        // Sync return transfer type to ViewModel
                        it?.let { transferType ->
                            vm.setReturnTransferType(TransferType.fromDisplayName(transferType))
                        }
                    },
                    onReturnMeetAndGreetSelected = { selectedReturnMeetAndGreet = it },
                    onReturnDateClick = { showReturnDatePicker = true },
                    onReturnTimeClick = { showReturnTimePicker = true },
                    onReturnPickupLocationSelected = { fullAddress, city, state, zipCode, locationDisplay, latitude, longitude, country ->
                        returnPickupLocation = locationDisplay
                        returnPickupLat = latitude
                        returnPickupLong = longitude
                        // Sync to ViewModel
                        vm.setReturnPickupLocation(locationDisplay, latitude, longitude)
                        Log.d(
                            DebugTags.BookingProcess,
                            "📍 Return pickup location selected: $locationDisplay (Lat: $latitude, Long: $longitude)"
                        )
                        // Automatically call booking rates API when return pickup location is selected (if dropoff is already set)
                        if (returnDropoffLat != null && returnDropoffLong != null) {
                            coroutineScope.launch {
                                vm.fetchBookingRates(
                                    ride = uiState.rideData, // Use ViewModel state for latest data
                                    vehicle = currentVehicle,
                                    isEditMode = isEditMode,
                                    editBookingId = editBookingId,
                                    hasExtraStops = hasExtraStops(editData),
                                    extraStops = extraStops.toExtraStopRequestsWithTownComparison(placesService, pickupLocation),
                                    returnExtraStops = returnExtraStops.toExtraStopRequestsWithTownComparison(placesService, returnPickupLocation),
                                    returnPickupLat = latitude, // Use the newly selected coordinates
                                    returnPickupLong = longitude,
                                    returnDropoffLat = returnDropoffLat,
                                    returnDropoffLong = returnDropoffLong
                                )
                            }
                        }
                    },
                    onReturnPickupLocationChange = { 
                        returnPickupLocation = it
                        vm.setReturnPickupLocation(it)
                    },
                    onReturnDropoffLocationSelected = { fullAddress, city, state, zipCode, locationDisplay, latitude, longitude, country ->
                        returnDropoffLocation = locationDisplay
                        returnDropoffLat = latitude
                        returnDropoffLong = longitude
                        // Sync to ViewModel
                        vm.setReturnDropoffLocation(locationDisplay, latitude, longitude)
                        Log.d(
                            DebugTags.BookingProcess,
                            "📍 Return dropoff location selected: $locationDisplay (Lat: $latitude, Long: $longitude)"
                        )
                        // Automatically call booking rates API when return dropoff location is selected - matches iOS
                        // Also trigger if pickup is already set
                        if (returnPickupLat != null && returnPickupLong != null) {
                            coroutineScope.launch {
                                vm.fetchBookingRates(
                                    ride = uiState.rideData, // Use ViewModel state for latest data
                                    vehicle = currentVehicle,
                                    isEditMode = isEditMode,
                                    editBookingId = editBookingId,
                                    hasExtraStops = hasExtraStops(editData),
                                    extraStops = extraStops.toExtraStopRequestsWithTownComparison(placesService, pickupLocation),
                                    returnExtraStops = returnExtraStops.toExtraStopRequestsWithTownComparison(placesService, returnPickupLocation),
                                    returnPickupLat = returnPickupLat, // Use the stored coordinates
                                    returnPickupLong = returnPickupLong,
                                    returnDropoffLat = latitude, // Use the newly selected coordinates
                                    returnDropoffLong = longitude
                                )
                            }
                        }
                    },
                    onReturnDropoffLocationChange = { 
                        returnDropoffLocation = it
                        vm.setReturnDropoffLocation(it)
                    },
                    onReturnPickupAirportClick = {
                        currentAirportType = "returnPickup"
                        showAirportBottomSheet = true
                    },
                    onReturnDropoffAirportClick = {
                        currentAirportType = "returnDropoff"
                        showAirportBottomSheet = true
                    },
                    onReturnPickupAirlineClick = {
                        currentAirlineType = "returnPickup"
                        showAirlineBottomSheet = true
                    },
                    onReturnDropoffAirlineClick = {
                        currentAirlineType = "returnDropoff"
                        showAirlineBottomSheet = true
                    },
                    onReturnFlightNumberChange = { 
                        returnPickupFlightNumber = it
                        vm.setReturnFlightInfo(pickupFlightNumber = it)
                    },
                    onReturnDropoffFlightNumberChange = { returnDropoffFlightNumber = it },
                    onReturnOriginCityChange = { 
                        returnOriginAirportCity = it
                        vm.setReturnFlightInfo(originAirportCity = it)
                    },
                    onReturnCruisePortChange = { 
                        returnCruisePort = it
                        // For cruise port to cruise port, update both pickup and dropoff cruise info
                        vm.setReturnCruisePickupInfo(it, returnCruiseShipName, returnShipArrivalTime)
                        vm.setReturnCruiseDropoffInfo(it, returnCruiseShipName, returnShipArrivalTime)
                    },
                    onReturnCruiseShipChange = { 
                        returnCruiseShipName = it
                        // For cruise port to cruise port, update both pickup and dropoff cruise info
                        vm.setReturnCruisePickupInfo(returnCruisePort, it, returnShipArrivalTime)
                        vm.setReturnCruiseDropoffInfo(returnCruisePort, it, returnShipArrivalTime)
                    },
                    onReturnShipArrivalChange = { 
                        returnShipArrivalTime = it
                        // For cruise port to cruise port, update both pickup and dropoff cruise info
                        vm.setReturnCruisePickupInfo(returnCruisePort, returnCruiseShipName, it)
                        vm.setReturnCruiseDropoffInfo(returnCruisePort, returnCruiseShipName, it)
                    },
                    onReturnSpecialInstructionsChange = { returnSpecialInstructions = it },
                    onReturnServiceTypeDropdownChange = { showReturnServiceTypeDropdown = it },
                    onReturnTransferTypeDropdownChange = { showReturnTransferTypeDropdown = it },
                    onReturnMeetAndGreetDropdownChange = { showReturnMeetAndGreetDropdown = it },
                    // Return Extra Stops parameters
                    returnExtraStops = returnExtraStops,
                    // Error states - map validation errors to return trip field errors
                    returnPickupDateTimeError = hasError("return_pickup_datetime"),
                    returnPickupLocationError = hasError("return_pickup_location"),
                    returnPickupCoordinatesError = hasError("return_pickup_coordinates"),
                    returnPickupAirportError = hasError("return_pickup_airport"),
                    returnPickupAirlineError = hasError("return_pickup_airline"),
                    returnPickupFlightError = hasError("return_pickup_flight_number"),
                    returnOriginCityError = hasError("return_origin_airport_city"),
                    returnDropoffLocationError = hasError("return_dropoff_location"),
                    returnDropoffCoordinatesError = hasError("return_dropoff_coordinates"),
                    returnDropoffAirportError = hasError("return_dropoff_airport"),
                    returnDropoffAirlineError = hasError("return_dropoff_airline"),
                    onReturnExtraStopsAdd = {
                        returnExtraStops = returnExtraStops + ExtraStop()
                    },
                    onReturnExtraStopsRemove = { stop ->
                        val updatedStops = returnExtraStops.filter { it.id != stop.id }
                        returnExtraStops = updatedStops
                        // Refresh booking rates when return stop is removed
                        coroutineScope.launch {
                            vm.fetchBookingRates(
                                ride = currentRideData,
                                vehicle = currentVehicle,
                                isEditMode = isEditMode,
                                editBookingId = editBookingId,
                                hasExtraStops = updatedStops.any { it.isLocationSelected && it.latitude != null },
                                returnExtraStops = updatedStops.toExtraStopRequestsWithTownComparison(placesService, returnPickupLocation),
                                returnPickupLat = returnPickupLat,
                                returnPickupLong = returnPickupLong,
                                returnDropoffLat = returnDropoffLat,
                                returnDropoffLong = returnDropoffLong
                            )
                        }
                    },
                    onReturnExtraStopsLocationSelected = { stop, fullAddress, city, state, zipCode, locationDisplay, latitude, longitude, country ->
                        // Validate the return stop
                        val errorMessage = validateExtraStop(
                            stop = stop.copy(
                                address = locationDisplay,
                                latitude = latitude,
                                longitude = longitude,
                                isLocationSelected = true
                            ),
                            pickupLocation = returnPickupLocation,
                            dropoffLocation = returnDropoffLocation,
                            pickupLat = returnPickupLat,
                            pickupLong = returnPickupLong,
                            dropoffLat = returnDropoffLat,
                            dropoffLong = returnDropoffLong,
                            isReturnTrip = true
                        )

                        if (errorMessage != null) {
                            // Validation failed
                            showInvalidLocationDialog = true
                            invalidLocationMessage = errorMessage
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(3000)
                                showInvalidLocationDialog = false
                            }
                        } else {
                            // Validation passed - update stop
                            val updatedReturnStops = returnExtraStops.map {
                                if (it.id == stop.id) {
                                    it.copy(
                                        address = locationDisplay,
                                        latitude = latitude,
                                        longitude = longitude,
                                        isLocationSelected = true
                                    )
                                } else {
                                    it
                                }
                            }
                            returnExtraStops = updatedReturnStops
                            // Automatically call booking rates API
                            coroutineScope.launch {
                                // Use updatedReturnStops list (after update) instead of returnExtraStops (before update)
                                val latestRideData = uiState.rideData
                                vm.fetchBookingRates(
                                    ride = latestRideData,
                                    vehicle = currentVehicle,
                                    isEditMode = isEditMode,
                                    editBookingId = editBookingId,
                                    hasExtraStops = extraStops.any { it.isLocationSelected && it.latitude != null },
                                    extraStops = extraStops.toExtraStopRequestsWithTownComparison(placesService, pickupLocation),
                                    returnExtraStops = updatedReturnStops.toExtraStopRequestsWithTownComparison(placesService, returnPickupLocation),
                                    returnPickupLat = returnPickupLat,
                                    returnPickupLong = returnPickupLong,
                                    returnDropoffLat = returnDropoffLat,
                                    returnDropoffLong = returnDropoffLong
                                )
                            }
                        }
                    },
                    onReturnExtraStopsLocationChange = { stop, newValue ->
                        // Update return stop location
                        returnExtraStops = returnExtraStops.map {
                            if (it.id == stop.id) {
                                it.copy(address = newValue, isLocationSelected = false)
                            } else {
                                it
                            }
                        }
                    },
                    onReturnExtraStopsInstructionsChange = { stop, newInstructions ->
                        // Update booking instructions for return stop
                        returnExtraStops = returnExtraStops.map {
                            if (it.id == stop.id) {
                                it.copy(bookingInstructions = newInstructions)
                            } else {
                                it
                            }
                        }
                    },
                    // Return Distance parameters
                    returnDistance = returnDistance,
                    returnDistanceLoading = distancesLoading
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SpecialInstructionsSection(
                specialInstructions = specialInstructions,
                onInstructionsChange = { specialInstructions = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Transportation Details Section
            TransportationDetailsSection(
                selectedMeetAndGreet = selectedMeetAndGreet,
                onMeetAndGreetChange = { selectedMeetAndGreet = it },
                isEditMode = isEditMode,
                editData = editData,
                vehicle = currentVehicle, // Pass currentVehicle (updated from editData in edit mode)
                meetAndGreetOptions = meetAndGreetOptions,
                showMeetAndGreetDropdown = showMeetAndGreetDropdown,
                onMeetAndGreetDropdownChange = { showMeetAndGreetDropdown = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Booking Summary Section (Rates)
            BookingSummarySection(
                selectedServiceType = selectedServiceType,
                selectedReturnServiceType = selectedReturnServiceType,
                subtotal = subtotal,
                grandTotal = grandTotal,
                numberOfVehicles = numberOfVehicles,
                currencySymbol = currencySymbol,
                returnSubtotal = returnSubtotal,
                returnGrandTotal = returnGrandTotal
            )

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Submit Button
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = LimoWhite,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
            ) {
                Button(
                    onClick = {
                        bookingInitiated = true

                        // Sync latest values
                        vm.setPickupDate(pickupDate)
                        vm.setPickupTime(pickupTime)

                        if (pickupLocation != uiState.pickupLocation) {
                            vm.setPickupLocation(pickupLocation)
                        }
                        if (dropoffLocation != uiState.dropoffLocation) {
                            vm.setDropoffLocation(dropoffLocation)
                        }

                        vm.setAirlineInfo(
                            pickupAirline = selectedPickupAirline?.displayName,
                            dropoffAirline = selectedDropoffAirline?.displayName
                        )

                        vm.setAirportInfo(
                            pickupAirport = selectedPickupAirport?.displayName,
                            dropoffAirport = selectedDropoffAirport?.displayName,
                            originAirportCity = originAirportCity
                        )

                        vm.setFlightInfo(
                            pickupFlightNumber = pickupFlightNumber,
                            dropoffFlightNumber = dropoffFlightNumber
                        )

                        vm.setServiceType(ServiceType.fromDisplayName(selectedServiceType))
                        vm.setTransferType(TransferType.fromDisplayName(selectedTransferType))
                        vm.setHours(
                            selectedHours
                                .replace(" hours minimum", "")
                                .replace(" hours", "")
                                .trim()
                                .toIntOrNull() ?: 0
                        )
                        vm.setNumberOfVehicles(numberOfVehicles.toString())
                        vm.setPassengerCount(passengerCount.toIntOrNull() ?: 1)
                        vm.setLuggageCount(luggageCount.toIntOrNull() ?: 0)

                        val latestRideData = uiState.rideData

                        val updatedRideData = latestRideData.copy(
                            pickupDate = pickupDate,
                            pickupTime = pickupTime,
                            pickupLocation = pickupLocation,
                            destinationLocation = dropoffLocation,
                            pickupLat = latestRideData.pickupLat,
                            pickupLong = latestRideData.pickupLong,
                            destinationLat = latestRideData.destinationLat,
                            destinationLong = latestRideData.destinationLong,
                            selectedPickupAirline = selectedPickupAirline?.displayName,
                            selectedDestinationAirline = selectedDropoffAirline?.displayName,
                            pickupFlightNumber = pickupFlightNumber,
                            dropoffFlightNumber = dropoffFlightNumber,
                            originAirportCity = originAirportCity,
                            noOfPassenger = passengerCount.toIntOrNull() ?: 1,
                            noOfLuggage = luggageCount.toIntOrNull() ?: 0,
                            noOfVehicles = numberOfVehicles,
                            bookingHour = if (selectedServiceType == "Charter Tour") {
                                selectedHours.replace(" hours minimum", "").replace(" hours", "")
                                    .trim()
                            } else {
                                latestRideData.bookingHour
                            },
                            serviceType = when (selectedServiceType) {
                                "One Way" -> "one_way"
                                "Round Trip" -> "round_trip"
                                "Charter Tour" -> "charter_tour"
                                else -> latestRideData.serviceType
                            }
                        )

                        vm.updateValidationErrors()
                        val errors = uiState.validationErrors
                        if (errors.isNotEmpty()) {
                            showValidationError("Please fix validation errors")
                            return@Button
                        }

                        coroutineScope.launch {
                            if (isEditMode && editBookingId != null) {
                                // Mark that booking update was initiated
                                bookingInitiated = true
                                Log.d(DebugTags.BookingProcess, "🔄 Initiating booking update for booking ID: $editBookingId")
                                vm.updateReservation(
                                    bookingId = editBookingId,
                                    ride = updatedRideData,
                                    vehicle = currentVehicle,
                                    extraStops = extraStops.toExtraStopRequestsWithTownComparison(
                                        placesService,
                                        pickupLocation
                                    ),
                                    returnExtraStops = returnExtraStops.toExtraStopRequestsWithTownComparison(
                                        placesService,
                                        returnPickupLocation
                                    ),
                                    bookingInstructions = specialInstructions,
                                    returnBookingInstructions = returnSpecialInstructions,
                                    meetGreetChoicesName = selectedMeetAndGreet,
                                    returnMeetGreetChoicesName = selectedReturnMeetAndGreet,
                                    cruisePort = cruisePort,
                                    cruiseName = cruiseShipName,
                                    cruiseTime = shipArrivalTime
                                )
                            } else {
                                // Mark that booking creation was initiated
                                bookingInitiated = true
                                Log.d(DebugTags.BookingProcess, "🔄 Initiating booking creation")
                                vm.createReservation(
                                    ride = updatedRideData,
                                    vehicle = currentVehicle,
                                    extraStops = extraStops.toExtraStopRequestsWithTownComparison(
                                        placesService,
                                        pickupLocation
                                    ),
                                    returnExtraStops = returnExtraStops.toExtraStopRequestsWithTownComparison(
                                        placesService,
                                        returnPickupLocation
                                    ),
                                    isRepeatMode = isRepeatMode,
                                    repeatBookingId = repeatBookingId,
                                    isReturnFlow = isReturnFlow,
                                    bookingInstructions = specialInstructions,
                                    returnBookingInstructions = returnSpecialInstructions,
                                    meetGreetChoicesName = selectedMeetAndGreet,
                                    returnMeetGreetChoicesName = selectedReturnMeetAndGreet,
                                    cruisePort = cruisePort,
                                    cruiseName = cruiseShipName,
                                    cruiseTime = shipArrivalTime
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .height(50.dp),
                    enabled = !loading,
                    colors = ButtonDefaults.buttonColors(containerColor = LimoOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (loading) {
                        ShimmerBox(
                            modifier = Modifier.size(20.dp),
                            shape = CircleShape
                        )
                    } else {
                        Text(
                            text = if (isEditMode) "Update Reservation" else "Create Booking",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
            }
    }

    // Date Picker Dialog - using the one from TimeSelectionScreen
    if (showDatePicker) {
        DatePickerDialog(
            selectedDate = selectedDate,
            onDateSelected = { date ->
                // CRITICAL FIX: Preserve the time component when updating date (matches iOS)
                val calendar = Calendar.getInstance()
                calendar.time = date // Use the new date
                val timeCalendar = Calendar.getInstance()
                timeCalendar.time = selectedTime // Get time from current selectedTime
                // Set time components from selectedTime to the new date calendar
                calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                calendar.set(Calendar.SECOND, timeCalendar.get(Calendar.SECOND))
                calendar.set(Calendar.MILLISECOND, 0)

                selectedDate = date
                selectedTime = calendar.time // Update selectedTime with combined date+time
                pickupDate = dateFormatter.format(date)
                // Sync to ViewModel so validation works correctly
                vm.setPickupDate(pickupDate)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    // Time Picker Dialog - using the one from TimeSelectionScreen
    if (showTimePicker) {
        TimePickerDialog(
            selectedTime = selectedTime,
            onTimeSelected = { time ->
                // CRITICAL FIX: Preserve the date component when updating time (matches iOS)
                val calendar = Calendar.getInstance()
                calendar.time = selectedDate // Use the selected date
                val timeCalendar = Calendar.getInstance()
                timeCalendar.time = time
                // Set time components from selected time to the date calendar
                calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
                calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
                calendar.set(Calendar.SECOND, timeCalendar.get(Calendar.SECOND))
                calendar.set(Calendar.MILLISECOND, 0)

                selectedTime = calendar.time
                pickupTime = timeFormatter.format(calendar.time)
                // Sync to ViewModel so validation works correctly
                vm.setPickupTime(pickupTime)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    // Ship Arrival Time Picker Dialog (for pickup cruise)
    val shipArrivalSelectedTime = remember(shipArrivalTime) {
        if (shipArrivalTime.isNotEmpty()) {
            try {
                timeFormatter.parse(shipArrivalTime)
            } catch (e: Exception) {
                Calendar.getInstance().time
            }
        } else {
            Calendar.getInstance().time
        }
    }
    
    if (showShipArrivalTimePicker) {
        TimePickerDialog(
            selectedTime = shipArrivalSelectedTime,
            onTimeSelected = { time ->
                shipArrivalTime = timeFormatter.format(time)
                vm.setCruisePickupInfo(cruisePort, cruiseShipName, shipArrivalTime)
                showShipArrivalTimePicker = false
            },
            onDismiss = { showShipArrivalTimePicker = false }
        )
    }

    // Dropoff Ship Arrival Time Picker Dialog (for dropoff cruise)
    // Note: Uses same shipArrivalTime variable as pickup (shared state)
    val dropoffShipArrivalSelectedTime = remember(shipArrivalTime) {
        if (shipArrivalTime.isNotEmpty()) {
            try {
                timeFormatter.parse(shipArrivalTime)
            } catch (e: Exception) {
                Calendar.getInstance().time
            }
        } else {
            Calendar.getInstance().time
        }
    }
    
    if (showDropoffShipArrivalTimePicker) {
        TimePickerDialog(
            selectedTime = dropoffShipArrivalSelectedTime,
            onTimeSelected = { time ->
                val timeString = timeFormatter.format(time)
                shipArrivalTime = timeString
                // Update ViewModel with dropoff cruise info (using same variables as they're shared)
                vm.setCruiseDropoffInfo(cruisePort, cruiseShipName, timeString)
                showDropoffShipArrivalTimePicker = false
            },
            onDismiss = { showDropoffShipArrivalTimePicker = false }
        )
    }

    // Return Date Picker Dialog
    val returnSelectedDate = remember(returnPickupDate) {
        if (returnPickupDate.isNotEmpty()) {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = dateFormat.parse(returnPickupDate)
                if (date != null) {
                    Calendar.getInstance().apply { time = date }
                } else {
                    Calendar.getInstance()
                }
            } catch (e: Exception) {
                Calendar.getInstance()
            }
        } else {
            Calendar.getInstance()
        }
    }

    if (showReturnDatePicker) {
        DatePickerDialog(
            selectedDate = returnSelectedDate.time,
            onDateSelected = { date ->
                val dateString = dateFormatter.format(date)
                returnPickupDate = dateString
                // Sync to ViewModel with the NEW value
                vm.setReturnPickupDate(dateString)
                showReturnDatePicker = false
            },
            onDismiss = { showReturnDatePicker = false }
        )
    }

    // Return Time Picker Dialog
    val returnSelectedTime = remember(returnPickupTime) {
        if (returnPickupTime.isNotEmpty()) {
            try {
                timeFormatter.parse(returnPickupTime)
            } catch (e: Exception) {
                Calendar.getInstance().time
            }
        } else {
            Calendar.getInstance().time
        }
    }

    if (showReturnTimePicker) {
        TimePickerDialog(
            selectedTime = returnSelectedTime,
            onTimeSelected = { time ->
                val timeString = timeFormatter.format(time)
                returnPickupTime = timeString
                // Sync to ViewModel with the NEW value
                vm.setReturnPickupTime(timeString)
                showReturnTimePicker = false
            },
            onDismiss = { showReturnTimePicker = false }
        )
    }

    // Location Picker is now handled by LocationAutocomplete component - no need for separate dialog

    // Airline Selection Bottom Sheet
    SearchableBottomSheet(
        title = "Select Airline",
        items = airlines,
        selectedItemId = when (currentAirlineType) {
            "pickup" -> selectedPickupAirline?.id
            "dropoff" -> selectedDropoffAirline?.id
            "returnPickup" -> selectedReturnPickupAirline?.id
            "returnDropoff" -> selectedReturnDropoffAirline?.id
            else -> null
        },
        isVisible = showAirlineBottomSheet,
        onDismiss = { showAirlineBottomSheet = false },
        onItemSelected = { airline ->
            when (currentAirlineType) {
                "pickup" -> {
                    selectedPickupAirline = airline
                    // Immediately update ViewModel to clear validation errors
                    vm.setAirlineInfo(pickupAirline = airline.displayName)
                    // Auto-populate return dropoff airline if empty (matches web app)
                    if (selectedServiceType == "Round Trip" && selectedReturnDropoffAirline == null) {
                        selectedReturnDropoffAirline = airline
                        Log.d(DebugTags.BookingProcess, "🔄 Auto-populated return dropoff airline: ${airline.displayName}")
                    }
                }
                "dropoff" -> {
                    selectedDropoffAirline = airline
                    // Immediately update ViewModel to clear validation errors
                    vm.setAirlineInfo(dropoffAirline = airline.displayName)
                    // Auto-populate return pickup airline if empty (matches web app)
                    if (selectedServiceType == "Round Trip" && selectedReturnPickupAirline == null) {
                        selectedReturnPickupAirline = airline
                        Log.d(DebugTags.BookingProcess, "🔄 Auto-populated return pickup airline: ${airline.displayName}")
                    }
                }
                "returnPickup" -> {
                    selectedReturnPickupAirline = airline
                    // Immediately update ViewModel to clear validation errors
                    vm.setReturnAirlineInfo(pickupAirline = airline.displayName)
                }
                "returnDropoff" -> {
                    selectedDropoffAirline = airline
                    // Immediately update ViewModel to clear validation errors
                    vm.setReturnAirlineInfo(dropoffAirline = airline.displayName)
                }
            }
            showAirlineBottomSheet = false
        },
        onSearchChanged = { query ->
            // Search airlines via service
            coroutineScope.launch {
                airlineService.searchAirlines(query)
            }
        },
        getItemId = { it.id },
        getDisplayText = { it.displayName },
        getSubtitle = { it.country }
    )

    // Airport Selection Bottom Sheet
    // Fetch initial airports when bottom sheet opens
    LaunchedEffect(showAirportBottomSheet) {
        if (showAirportBottomSheet) {
            Log.d(DebugTags.BookingProcess, "🔄 Airport bottom sheet opened - fetching initial airports")
            // Fetch initial airports to show options when bottom sheet opens
            coroutineScope.launch {
                try {
                    airportService.fetchInitialAirports()
                    Log.d(DebugTags.BookingProcess, "✅ Initial airports fetched for bottom sheet")
                } catch (e: Exception) {
                    Log.e(DebugTags.BookingProcess, "❌ Error fetching initial airports for bottom sheet", e)
                }
            }
        }
    }
    
    SearchableBottomSheet(
        title = "Select Airport",
        items = airports,
        selectedItemId = when (currentAirportType) {
            "pickup" -> selectedPickupAirport?.id
            "dropoff" -> selectedDropoffAirport?.id
            "returnPickup" -> selectedReturnPickupAirport?.id
            "returnDropoff" -> selectedReturnDropoffAirport?.id
            else -> null
        },
        isVisible = showAirportBottomSheet,
        onDismiss = { 
            showAirportBottomSheet = false
            // Reset airport service to clear search state and suggestions
            coroutineScope.launch {
                airportService.reset()
            }
        },
        onItemSelected = { airport ->
            when (currentAirportType) {
                "pickup" -> {
                    selectedPickupAirport = airport
                    pickupLocation = airport.displayName
                    // Immediately update ViewModel to clear validation errors
                    vm.setAirportInfo(pickupAirport = airport.displayName)
                    // Update coordinates and trigger rate fetch
                    vm.setPickupLocation(airport.displayName, airport.lat, airport.long)
                    coroutineScope.launch {
                        kotlinx.coroutines.delay(100) // Small delay to ensure ViewModel state is updated
                        val latestRideData = uiState.rideData
                        vm.fetchBookingRates(
                            ride = latestRideData,
                            vehicle = currentVehicle,
                            isEditMode = isEditMode,
                            editBookingId = editBookingId,
                            hasExtraStops = extraStops.any { it.isLocationSelected && it.latitude != null },
                            extraStops = extraStops.toExtraStopRequestsWithTownComparison(placesService, pickupLocation),
                            returnExtraStops = returnExtraStops.toExtraStopRequestsWithTownComparison(placesService, returnPickupLocation),
                            returnPickupLat = returnPickupLat,
                            returnPickupLong = returnPickupLong,
                            returnDropoffLat = returnDropoffLat,
                            returnDropoffLong = returnDropoffLong
                        )
                    }
                }
                "dropoff" -> {
                    selectedDropoffAirport = airport
                    dropoffLocation = airport.displayName
                    // Immediately update ViewModel to clear validation errors
                    vm.setAirportInfo(dropoffAirport = airport.displayName)
                    // Update coordinates and trigger rate fetch
                    vm.setDropoffLocation(airport.displayName, airport.lat, airport.long)
                    coroutineScope.launch {
                        kotlinx.coroutines.delay(100) // Small delay to ensure ViewModel state is updated
                        val latestRideData = uiState.rideData
                        vm.fetchBookingRates(
                            ride = latestRideData,
                            vehicle = currentVehicle,
                            isEditMode = isEditMode,
                            editBookingId = editBookingId,
                            hasExtraStops = extraStops.any { it.isLocationSelected && it.latitude != null },
                            extraStops = extraStops.toExtraStopRequestsWithTownComparison(placesService, pickupLocation),
                            returnExtraStops = returnExtraStops.toExtraStopRequestsWithTownComparison(placesService, returnPickupLocation),
                            returnPickupLat = returnPickupLat,
                            returnPickupLong = returnPickupLong,
                            returnDropoffLat = returnDropoffLat,
                            returnDropoffLong = returnDropoffLong
                        )
                    }
                }
                "returnPickup" -> {
                    selectedReturnPickupAirport = airport
                    returnPickupLocation = airport.displayName
                    // Update coordinates from airport
                    returnPickupLat = airport.lat
                    returnPickupLong = airport.long
                    // Immediately update ViewModel to clear validation errors
                    vm.setReturnAirportInfo(pickupAirport = airport.displayName)
                    vm.setReturnPickupLocation(airport.displayName, airport.lat, airport.long)
                }
                "returnDropoff" -> {
                    selectedReturnDropoffAirport = airport
                    returnDropoffLocation = airport.displayName
                    // Update coordinates from airport
                    returnDropoffLat = airport.lat
                    returnDropoffLong = airport.long
                    // Immediately update ViewModel to clear validation errors
                    vm.setReturnAirportInfo(dropoffAirport = airport.displayName)
                    vm.setReturnDropoffLocation(airport.displayName, airport.lat, airport.long)
                }
            }
            showAirportBottomSheet = false
        },
        onSearchChanged = { query ->
            // Search airports via service
            coroutineScope.launch {
                airportService.searchAirports(query)
            }
        },
        getItemId = { it.id },
        getDisplayText = { it.displayName },
        getSubtitle = { "${it.city}, ${it.country}" }
    )
}

