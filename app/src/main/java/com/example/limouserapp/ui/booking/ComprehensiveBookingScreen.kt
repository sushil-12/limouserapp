package com.example.limouserapp.ui.booking

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.res.painterResource
import com.example.limouserapp.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
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
import com.example.limouserapp.data.model.booking.RateBreakdown
import com.example.limouserapp.data.model.booking.DriverInformation
import com.example.limouserapp.data.model.booking.VehicleDetails
import com.example.limouserapp.ui.theme.LimoBlack
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.LimoWhite
import com.example.limouserapp.ui.theme.GoogleSansFamily
import com.example.limouserapp.ui.viewmodel.ReservationViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import android.util.Log
import androidx.compose.ui.unit.TextUnit
import com.example.limouserapp.ui.utils.DebugTags
import java.text.SimpleDateFormat
import java.util.*
import com.example.limouserapp.data.model.booking.Airline
import com.example.limouserapp.data.model.booking.Airport
import com.example.limouserapp.ui.components.SearchableBottomSheet
import com.example.limouserapp.ui.components.LocationAutocomplete
import com.example.limouserapp.ui.booking.components.DatePickerDialog
import com.example.limouserapp.ui.booking.components.TimePickerDialog
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import coil.compose.rememberAsyncImagePainter
import com.example.limouserapp.data.model.dashboard.ProfileData
import com.example.limouserapp.data.model.booking.ExtraStopRequest
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.text.input.KeyboardType
import org.json.JSONArray
import org.json.JSONObject
import com.example.limouserapp.ui.booking.comprehensivebooking.ExtraStop
import com.example.limouserapp.ui.booking.comprehensivebooking.SectionHeader
import com.example.limouserapp.ui.booking.comprehensivebooking.StyledDropdown
import com.example.limouserapp.ui.booking.comprehensivebooking.StyledInput
import com.example.limouserapp.ui.booking.comprehensivebooking.EditableTextField
import com.example.limouserapp.ui.booking.comprehensivebooking.BookingSection
import com.example.limouserapp.ui.booking.comprehensivebooking.InfoField
import com.example.limouserapp.ui.booking.comprehensivebooking.Tag
import com.example.limouserapp.ui.booking.comprehensivebooking.sections.AccountsInfoSection
import com.example.limouserapp.ui.booking.comprehensivebooking.sections.BookingDetailsSection
import com.example.limouserapp.ui.booking.comprehensivebooking.sections.SpecialInstructionsSection
import com.example.limouserapp.ui.booking.comprehensivebooking.sections.PickupSection
import com.example.limouserapp.ui.booking.comprehensivebooking.sections.DropoffSection
import com.example.limouserapp.ui.booking.comprehensivebooking.sections.ReturnJourneySection
import com.example.limouserapp.ui.booking.comprehensivebooking.sections.ReturnPickupSection
import com.example.limouserapp.ui.booking.comprehensivebooking.sections.ReturnDropoffSection
import com.example.limouserapp.ui.booking.comprehensivebooking.sections.TransportationDetailsSection
import com.example.limouserapp.ui.booking.comprehensivebooking.sections.ExtraStopsSection
import com.example.limouserapp.ui.booking.comprehensivebooking.sections.ReturnExtraStopsSection
import com.example.limouserapp.ui.booking.comprehensivebooking.sections.BookingSummarySection
import com.example.limouserapp.ui.booking.comprehensivebooking.sections.DistanceInformationSection
import com.example.limouserapp.ui.booking.comprehensivebooking.getReversedTransferType
import com.example.limouserapp.ui.booking.comprehensivebooking.prefillExtraStopsFromEditData
import com.example.limouserapp.ui.booking.comprehensivebooking.validateExtraStop
import com.example.limouserapp.ui.booking.comprehensivebooking.hasExtraStops
import com.example.limouserapp.ui.booking.comprehensivebooking.getEffectiveOutboundPickupCoordinate
import com.example.limouserapp.ui.booking.comprehensivebooking.getEffectiveOutboundDropoffCoordinate
import com.example.limouserapp.ui.booking.comprehensivebooking.toExtraStopRequests
import com.example.limouserapp.ui.booking.comprehensivebooking.getMeetAndGreetForTransferType
import com.example.limouserapp.ui.booking.comprehensivebooking.getSpecialInstructionsForTransferType
import com.example.limouserapp.ui.booking.comprehensivebooking.convertTransferTypeToWebFormat

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
    
    // Repeat mode state
    var isLoadingRepeatData by remember { mutableStateOf(false) }
    var hasLoadedExistingRates by remember { mutableStateOf(false) } // Track if rates were loaded from existing booking
    
    // Mutable state for vehicle and rideData (can be updated from edit data)
    var currentVehicle by remember { mutableStateOf(vehicle) }
    var currentRideData by remember { mutableStateOf(rideData) }
    
    // Load edit data when in edit mode
    LaunchedEffect(isEditMode, editBookingId) {
        Log.d(DebugTags.BookingProcess, "===========================================")
        Log.d(DebugTags.BookingProcess, "🔄 LaunchedEffect triggered for edit mode")
        Log.d(DebugTags.BookingProcess, "isEditMode: $isEditMode")
        Log.d(DebugTags.BookingProcess, "editBookingId: $editBookingId")
        Log.d(DebugTags.BookingProcess, "editData: ${if (editData == null) "null" else "not null"}")
        Log.d(DebugTags.BookingProcess, "===========================================")
        
        if (isEditMode && editBookingId != null) {
            // Only fetch if we don't have edit data yet, or if the booking ID changed
            if (editData == null || editData?.reservationId != editBookingId) {
                Log.d(DebugTags.BookingProcess, "🔄 EDIT MODE: Fetching edit data for booking ID: $editBookingId")
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
    
    // Profile data state
    var profileData by remember { mutableStateOf<ProfileData?>(null) }
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
    
    // Editable service type and transfer type (matches iOS selectedServiceType and selectedTransferType)
    var selectedServiceType by remember {
        mutableStateOf(
            when (rideData.serviceType.lowercase()) {
                "one_way" -> "One Way"
                "round_trip" -> "Round Trip"
                "charter_tour", "chartertour" -> "Charter Tour"
                else -> "One Way"
            }
        )
    }
    
    var selectedTransferType by remember {
        mutableStateOf(
            when {
                rideData.pickupType.equals("city", ignoreCase = true) && rideData.dropoffType.equals("city", ignoreCase = true) -> "City to City"
                rideData.pickupType.equals("city", ignoreCase = true) && rideData.dropoffType.equals("airport", ignoreCase = true) -> "City to Airport"
                rideData.pickupType.equals("airport", ignoreCase = true) && rideData.dropoffType.equals("city", ignoreCase = true) -> "Airport to City"
                rideData.pickupType.equals("airport", ignoreCase = true) && rideData.dropoffType.equals("airport", ignoreCase = true) -> "Airport to Airport"
                rideData.pickupType.equals("city", ignoreCase = true) && rideData.dropoffType.equals("cruise", ignoreCase = true) -> "City to Cruise Port"
                rideData.pickupType.equals("airport", ignoreCase = true) && rideData.dropoffType.equals("cruise", ignoreCase = true) -> "Airport to Cruise Port"
                rideData.pickupType.equals("cruise", ignoreCase = true) && rideData.dropoffType.equals("city", ignoreCase = true) -> "Cruise Port to City"
                rideData.pickupType.equals("cruise", ignoreCase = true) && rideData.dropoffType.equals("airport", ignoreCase = true) -> "Cruise Port to Airport"
                else -> "City to City"
            }
        )
    }
    
    // Editable state variables
    var pickupDate by remember { 
        mutableStateOf(rideData.pickupDate) 
    }
    var pickupTime by remember { 
        mutableStateOf(rideData.pickupTime) 
    }
    var pickupLocation by remember { 
        mutableStateOf(rideData.pickupLocation) 
    }
    var dropoffLocation by remember { 
        mutableStateOf(rideData.destinationLocation) 
    }
    
    // Number of vehicles (editable)
    var numberOfVehicles by remember { mutableStateOf(rideData.noOfVehicles) }
    
    // Passenger and luggage count (editable in edit mode)
    var passengerCount by remember { mutableStateOf(rideData.noOfPassenger) }
    var luggageCount by remember { mutableStateOf(rideData.noOfLuggage) }
    
    // Hours for charter tours (editable)
    val hoursOptions = listOf("2 hours minimum", "3 hours", "4 hours", "5 hours", "6 hours", "8 hours", "10 hours", "12 hours")
    var selectedHours by remember {
        mutableStateOf(
            if (rideData.bookingHour != null && rideData.bookingHour != "0") {
                val hourValue = rideData.bookingHour
                if (hourValue == "2") "2 hours minimum" else "$hourValue hours"
            } else {
                "2 hours minimum"
            }
        )
    }
    
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
    
    // Return trip state variables (matches iOS)
    var selectedReturnServiceType by remember { mutableStateOf<String?>(null) }
    var selectedReturnTransferType by remember { mutableStateOf<String?>(null) }
    var selectedReturnMeetAndGreet by remember { mutableStateOf<String?>(null) }
    var returnPickupDate by remember { mutableStateOf("") }
    var returnPickupTime by remember { mutableStateOf("") }
    var returnPickupLocation by remember { mutableStateOf("") }
    var returnDropoffLocation by remember { mutableStateOf("") }
    var returnPickupFlightNumber by remember { mutableStateOf("") }
    var returnDropoffFlightNumber by remember { mutableStateOf("") }
    var returnOriginAirportCity by remember { mutableStateOf("") }
    var returnCruiseShipName by remember { mutableStateOf("") }
    var returnShipArrivalTime by remember { mutableStateOf("") }
    var returnCruisePort by remember { mutableStateOf("") }
    var returnSpecialInstructions by remember { mutableStateOf("") }
    var returnNumberOfVehicles by remember { mutableStateOf(1) }
    var selectedReturnHours by remember { mutableStateOf("2 hours minimum") }
    
    // Extra stops state (matches iOS extraStops and returnExtraStops)
    var extraStops by remember { mutableStateOf<List<ExtraStop>>(emptyList()) }
    var returnExtraStops by remember { mutableStateOf<List<ExtraStop>>(emptyList()) }
    var showInvalidLocationDialog by remember { mutableStateOf(false) }
    var invalidLocationMessage by remember { mutableStateOf("") }
    var invalidLocationDismissJob by remember { mutableStateOf<Job?>(null) }
    
    // Calculate distances using Directions API (moved earlier so it's available in LaunchedEffect blocks)
    var outboundDistance by remember { mutableStateOf<Pair<String, String>?>(null) } // (distance, duration)
    var returnDistance by remember { mutableStateOf<Pair<String, String>?>(null) } // (distance, duration)
    var distancesLoading by remember { mutableStateOf(false) }
    
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
    
    /**
     * Validate all required fields based on service type and transfer type (matches web app validation)
     * Returns true if all required fields are filled, false otherwise
     */
    fun validateRequiredFields(): Boolean {
        Log.d(DebugTags.BookingProcess, "🔍 Starting form validation...")
        
        // Always required fields
        if (selectedServiceType.isEmpty() || selectedTransferType.isEmpty()) {
            Log.d(DebugTags.BookingProcess, "❌ Validation failed: ServiceType='$selectedServiceType', TransferType='$selectedTransferType'")
            return false
        }
        Log.d(DebugTags.BookingProcess, "✅ ServiceType='$selectedServiceType', TransferType='$selectedTransferType'")
        
        if (pickupDate.isEmpty() || pickupTime.isEmpty()) {
            Log.d(DebugTags.BookingProcess, "❌ Validation failed: PickupDate='$pickupDate', PickupTime='$pickupTime'")
            return false
        }
        Log.d(DebugTags.BookingProcess, "✅ PickupDate='$pickupDate', PickupTime='$pickupTime'")
        
        if (pickupLocation.isEmpty() || dropoffLocation.isEmpty()) {
            Log.d(DebugTags.BookingProcess, "❌ Validation failed: PickupLocation='$pickupLocation', DropoffLocation='$dropoffLocation'")
            return false
        }
        Log.d(DebugTags.BookingProcess, "✅ PickupLocation='$pickupLocation', DropoffLocation='$dropoffLocation'")
        
        if (currentRideData.pickupLat == null || currentRideData.pickupLong == null) {
            Log.d(DebugTags.BookingProcess, "❌ Validation failed: Pickup coordinates missing - lat=${currentRideData.pickupLat}, long=${currentRideData.pickupLong}")
            return false
        }
        Log.d(DebugTags.BookingProcess, "✅ Pickup coordinates: lat=${currentRideData.pickupLat}, long=${currentRideData.pickupLong}")
        
        if (currentRideData.destinationLat == null || currentRideData.destinationLong == null) {
            Log.d(DebugTags.BookingProcess, "❌ Validation failed: Dropoff coordinates missing - lat=${currentRideData.destinationLat}, long=${currentRideData.destinationLong}")
            return false
        }
        Log.d(DebugTags.BookingProcess, "✅ Dropoff coordinates: lat=${currentRideData.destinationLat}, long=${currentRideData.destinationLong}")
        
        // Passenger information is required (matches web app: passenger_name, passenger_email, passenger_cell)
        if (profileData == null) {
            Log.d(DebugTags.BookingProcess, "❌ Validation failed: ProfileData is null")
            return false
        }
        val firstName = profileData?.firstName?.trim() ?: ""
        val lastName = profileData?.lastName?.trim() ?: ""
        val email = profileData?.email?.trim() ?: ""
        val mobile = profileData?.mobile?.trim() ?: ""
        
        if (firstName.isEmpty() && lastName.isEmpty()) {
            Log.d(DebugTags.BookingProcess, "❌ Validation failed: Both firstName and lastName are empty")
            return false
        }
        Log.d(DebugTags.BookingProcess, "✅ Name: firstName='$firstName', lastName='$lastName'")
        
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Log.d(DebugTags.BookingProcess, "❌ Validation failed: Invalid email='$email'")
            return false
        }
        Log.d(DebugTags.BookingProcess, "✅ Email='$email'")
        
        if (mobile.isEmpty() || mobile.length < 4) {
            Log.d(DebugTags.BookingProcess, "❌ Validation failed: Invalid mobile='$mobile' (length=${mobile.length})")
            return false
        }
        Log.d(DebugTags.BookingProcess, "✅ Mobile='$mobile'")
        
        // Validate pickup fields based on transfer type
        // Web app logic: airport_ prefix means pickup is at airport, cruise_ prefix means pickup is at cruise port
        val transferTypeLower = selectedTransferType.lowercase()
        when {
            // Airport pickup - transfer type starts with "Airport" (e.g., "Airport To City")
            transferTypeLower.startsWith("airport") -> {
                if (selectedPickupAirport == null || selectedPickupAirline == null) {
                    Log.d(DebugTags.BookingProcess, "❌ Validation failed: Airport pickup - airport=${selectedPickupAirport != null}, airline=${selectedPickupAirline != null}")
                    return false
                }
                if (originAirportCity.isEmpty() || pickupFlightNumber.isEmpty()) {
                    Log.d(DebugTags.BookingProcess, "❌ Validation failed: Airport pickup - originCity='$originAirportCity', flightNumber='$pickupFlightNumber'")
                    return false
                }
                Log.d(DebugTags.BookingProcess, "✅ Airport pickup validated")
            }
            // Cruise pickup - transfer type starts with "Cruise Port" (e.g., "Cruise Port To City")
            transferTypeLower.startsWith("cruise port") || transferTypeLower.startsWith("cruise") -> {
                if (cruisePort.isEmpty() || cruiseShipName.isEmpty()) {
                    Log.d(DebugTags.BookingProcess, "❌ Validation failed: Cruise pickup - cruisePort='$cruisePort', cruiseShipName='$cruiseShipName'")
                    return false
                }
                Log.d(DebugTags.BookingProcess, "✅ Cruise pickup validated")
            }
        }
        
        // Validate dropoff fields based on transfer type
        // Web app logic: _airport suffix means dropoff is at airport, _cruise suffix means dropoff is at cruise port
        when {
            // Airport dropoff - transfer type ends with "Airport" (e.g., "City To Airport")
            transferTypeLower.endsWith("airport") -> {
                if (selectedDropoffAirport == null || selectedDropoffAirline == null) {
                    Log.d(DebugTags.BookingProcess, "❌ Validation failed: Airport dropoff - airport=${selectedDropoffAirport != null}, airline=${selectedDropoffAirline != null}")
                    return false
                }
                Log.d(DebugTags.BookingProcess, "✅ Airport dropoff validated")
            }
            // Cruise dropoff - transfer type ends with "Cruise Port" (e.g., "City To Cruise Port")
            transferTypeLower.endsWith("cruise port") || transferTypeLower.endsWith("cruise") -> {
                if (cruisePort.isEmpty() || cruiseShipName.isEmpty()) {
                    Log.d(DebugTags.BookingProcess, "❌ Validation failed: Cruise dropoff - cruisePort='$cruisePort', cruiseShipName='$cruiseShipName'")
                    return false
                }
                Log.d(DebugTags.BookingProcess, "✅ Cruise dropoff validated")
            }
        }
        
        // For round trips, validate return fields
        if (selectedServiceType == "Round Trip") {
            Log.d(DebugTags.BookingProcess, "🔍 Validating Round Trip return fields...")
            // Store in local variable to avoid smart cast issues with delegated property
            val returnTransferType = selectedReturnTransferType
            if (returnTransferType == null || returnTransferType.isEmpty()) {
                Log.d(DebugTags.BookingProcess, "❌ Validation failed: ReturnTransferType='$returnTransferType'")
                return false
            }
            Log.d(DebugTags.BookingProcess, "✅ ReturnTransferType='$returnTransferType'")
            
            if (returnPickupDate.isEmpty() || returnPickupTime.isEmpty()) {
                Log.d(DebugTags.BookingProcess, "❌ Validation failed: ReturnPickupDate='$returnPickupDate', ReturnPickupTime='$returnPickupTime'")
                return false
            }
            Log.d(DebugTags.BookingProcess, "✅ ReturnPickupDate='$returnPickupDate', ReturnPickupTime='$returnPickupTime'")
            
            if (returnPickupLocation.isEmpty() || returnDropoffLocation.isEmpty()) {
                Log.d(DebugTags.BookingProcess, "❌ Validation failed: ReturnPickupLocation='$returnPickupLocation', ReturnDropoffLocation='$returnDropoffLocation'")
                return false
            }
            Log.d(DebugTags.BookingProcess, "✅ ReturnPickupLocation='$returnPickupLocation', ReturnDropoffLocation='$returnDropoffLocation'")
            
            // Validate return pickup fields based on return transfer type
            val returnTransferTypeLower = returnTransferType.lowercase()
            when {
                // Airport return pickup - return transfer type starts with "Airport" (e.g., "Airport To City")
                returnTransferTypeLower.startsWith("airport") -> {
                    if (selectedReturnPickupAirport == null || selectedReturnPickupAirline == null) {
                        Log.d(DebugTags.BookingProcess, "❌ Validation failed: Return Airport pickup - airport=${selectedReturnPickupAirport != null}, airline=${selectedReturnPickupAirline != null}")
                        return false
                    }
                    if (returnOriginAirportCity.isEmpty() || returnPickupFlightNumber.isEmpty()) {
                        Log.d(DebugTags.BookingProcess, "❌ Validation failed: Return Airport pickup - originCity='$returnOriginAirportCity', flightNumber='$returnPickupFlightNumber'")
                        return false
                    }
                    Log.d(DebugTags.BookingProcess, "✅ Return Airport pickup validated")
                }
                // Cruise return pickup - return transfer type starts with "Cruise Port" (e.g., "Cruise Port To City")
                returnTransferTypeLower.startsWith("cruise port") || returnTransferTypeLower.startsWith("cruise") -> {
                    if (returnCruisePort.isEmpty() || returnCruiseShipName.isEmpty()) {
                        Log.d(DebugTags.BookingProcess, "❌ Validation failed: Return Cruise pickup - cruisePort='$returnCruisePort', cruiseShipName='$returnCruiseShipName'")
                        return false
                    }
                    Log.d(DebugTags.BookingProcess, "✅ Return Cruise pickup validated")
                }
            }
            
            // Validate return dropoff fields based on return transfer type
            when {
                // Airport return dropoff - return transfer type ends with "Airport" (e.g., "City To Airport")
                returnTransferTypeLower.endsWith("airport") -> {
                    if (selectedReturnDropoffAirport == null || selectedReturnDropoffAirline == null) {
                        Log.d(DebugTags.BookingProcess, "❌ Validation failed: Return Airport dropoff - airport=${selectedReturnDropoffAirport != null}, airline=${selectedReturnDropoffAirline != null}")
                        return false
                    }
                    Log.d(DebugTags.BookingProcess, "✅ Return Airport dropoff validated")
                }
                // Cruise return dropoff - return transfer type ends with "Cruise Port" (e.g., "City To Cruise Port")
                returnTransferTypeLower.endsWith("cruise port") || returnTransferTypeLower.endsWith("cruise") -> {
                    if (returnCruisePort.isEmpty() || returnCruiseShipName.isEmpty()) {
                        Log.d(DebugTags.BookingProcess, "❌ Validation failed: Return Cruise dropoff - cruisePort='$returnCruisePort', cruiseShipName='$returnCruiseShipName'")
                        return false
                    }
                    Log.d(DebugTags.BookingProcess, "✅ Return Cruise dropoff validated")
                }
            }
        }
        
        // For charter tours, validate hours
        if (selectedServiceType == "Charter Tour") {
            if (selectedHours.isEmpty()) {
                Log.d(DebugTags.BookingProcess, "❌ Validation failed: Charter Tour - selectedHours='$selectedHours'")
                return false
            }
            Log.d(DebugTags.BookingProcess, "✅ Charter Tour hours='$selectedHours'")
        }
        
        Log.d(DebugTags.BookingProcess, "✅✅✅ All validation checks passed! Form is valid.")
        return true
    }
    
    // Computed validation state for button enable/disable
    val isFormValid = remember(
        selectedServiceType,
        selectedTransferType,
        selectedReturnTransferType,
        pickupDate,
        pickupTime,
        pickupLocation,
        dropoffLocation,
        currentRideData.pickupLat,
        currentRideData.pickupLong,
        currentRideData.destinationLat,
        currentRideData.destinationLong,
        selectedPickupAirport,
        selectedPickupAirline,
        selectedDropoffAirport,
        selectedDropoffAirline,
        originAirportCity,
        pickupFlightNumber,
        cruisePort,
        cruiseShipName,
        returnPickupDate,
        returnPickupTime,
        returnPickupLocation,
        returnDropoffLocation,
        selectedReturnPickupAirport,
        selectedReturnPickupAirline,
        selectedReturnDropoffAirport,
        selectedReturnDropoffAirline,
        returnOriginAirportCity,
        returnPickupFlightNumber,
        returnCruisePort,
        returnCruiseShipName,
        selectedHours,
        profileData
    ) {
        validateRequiredFields()
    }
    var lastReturnDropoffCoord by remember { mutableStateOf<Pair<Double?, Double?>?>(null) }
    
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
                pickupDate = data.pickupDate,
                pickupTime = data.pickupTime,
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
                selectedPickupAirline = data.pickupAirline,
                selectedDestinationAirline = data.dropoffAirline,
                pickupFlightNumber = data.pickupFlight,
                dropoffFlightNumber = data.dropoffFlight,
                returnPickupFlightNumber = data.returnPickupFlight,
                returnDropoffFlightNumber = data.returnDropoffFlight,
                originAirportCity = data.originAirportCity
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
            pickupDate = data.pickupDate
            pickupTime = data.pickupTime
            returnPickupDate = data.returnPickupDate ?: ""
            returnPickupTime = data.returnPickupTime ?: ""
            
            // Prefill locations
            pickupLocation = data.pickup ?: ""
            dropoffLocation = data.dropoff ?: ""
            returnPickupLocation = data.returnPickup ?: ""
            returnDropoffLocation = data.returnDropoff ?: ""
            
            // Prefill passenger and luggage counts
            passengerCount = data.totalPassengers
            luggageCount = data.luggageCount
            numberOfVehicles = data.numberOfVehicles ?: 1
            
            // Prefill flight numbers
            pickupFlightNumber = data.pickupFlight ?: ""
            dropoffFlightNumber = data.dropoffFlight ?: ""
            returnPickupFlightNumber = data.returnPickupFlight ?: ""
            returnDropoffFlightNumber = data.returnDropoffFlight ?: ""
            
            // Prefill origin airport city
            originAirportCity = data.originAirportCity ?: ""
            Log.d(DebugTags.BookingProcess, "✅ Set origin airport city from edit data: '${data.originAirportCity}'")
            
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
            
            // Fetch booking rates after prefilling data (matches iOS loadEditData)
            // Use coroutineScope since we're already in a LaunchedEffect coroutine context
            coroutineScope {
                launch {
                    Log.d(DebugTags.BookingProcess, "🔄 Fetching booking rates after edit data loaded")
                    vm.fetchBookingRates(
                        ride = currentRideData,
                        vehicle = currentVehicle,
                        isEditMode = true,
                        editBookingId = editBookingId,
                        hasExtraStops = hasExtraStops(editData),
                        extraStops = extraStops.toExtraStopRequests(),
                        returnExtraStops = returnExtraStops.toExtraStopRequests()
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
                    pickupDate = data.pickupDate,
                    pickupTime = data.pickupTime,
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
                    selectedPickupAirline = data.pickupAirline,
                    selectedDestinationAirline = data.dropoffAirline,
                    pickupFlightNumber = data.pickupFlight,
                    dropoffFlightNumber = data.dropoffFlight,
                    returnPickupFlightNumber = data.returnPickupFlight,
                    returnDropoffFlightNumber = data.returnDropoffFlight,
                    originAirportCity = data.originAirportCity
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
                passengerCount = data.totalPassengers
                luggageCount = data.luggageCount
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
                        vm.fetchBookingRates(
                            ride = currentRideData,
                            vehicle = currentVehicle,
                            isEditMode = false, // Not edit mode, it's repeat mode
                            editBookingId = null,
                            hasExtraStops = hasExtraStops(editData),
                            extraStops = extraStops.toExtraStopRequests(),
                            returnExtraStops = returnExtraStops.toExtraStopRequests()
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
        
        // Fetch user profile data
        profileLoading = true
        try {
            val response = dashboardApi.getProfileData()
            if (response.success) {
                profileData = response.data
                Log.d(DebugTags.BookingProcess, "✅ Profile data loaded: ${response.data.fullName}")
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
            
            // Initialize return date/time (default to same as outbound)
            if (returnPickupDate.isEmpty()) {
                returnPickupDate = pickupDate
            }
            if (returnPickupTime.isEmpty()) {
                returnPickupTime = pickupTime
            }
            
            // Pre-fill return locations based on reversed transfer type
            val reversedType = selectedReturnTransferType ?: getReversedTransferType(selectedTransferType)
            if (returnPickupLocation.isEmpty() && reversedType.contains("City")) {
                // Return pickup is city, use dropoff location from outbound
                returnPickupLocation = dropoffLocation
            } else if (returnPickupLocation.isEmpty() && reversedType.contains("Airport")) {
                // Return pickup is airport, use dropoff airport from outbound
                returnPickupLocation = selectedDropoffAirport?.displayName ?: ""
                if (selectedReturnPickupAirport == null && selectedDropoffAirport != null) {
                    selectedReturnPickupAirport = selectedDropoffAirport
                }
            }
            
            if (returnDropoffLocation.isEmpty() && reversedType.endsWith("City")) {
                // Return dropoff is city, use pickup location from outbound
                returnDropoffLocation = pickupLocation
            } else if (returnDropoffLocation.isEmpty() && reversedType.endsWith("Airport")) {
                // Return dropoff is airport, use pickup airport from outbound
                returnDropoffLocation = selectedPickupAirport?.displayName ?: ""
                if (selectedReturnDropoffAirport == null && selectedPickupAirport != null) {
                    selectedReturnDropoffAirport = selectedPickupAirport
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
    LaunchedEffect(currentRideData, currentVehicle, selectedServiceType, selectedTransferType, isEditMode, editData, isRepeatMode, hasLoadedExistingRates) {
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
        
        // Update rideData with current service/transfer type for API call
        val updatedRideData = currentRideData.copy(
            serviceType = when (selectedServiceType) {
                "One Way" -> "one_way"
                "Round Trip" -> "round_trip"
                "Charter Tour" -> "charter_tour"
                else -> currentRideData.serviceType
            },
            pickupType = when {
                selectedTransferType.startsWith("City") -> "city"
                selectedTransferType.startsWith("Airport") -> "airport"
                selectedTransferType.startsWith("Cruise") -> "cruise"
                else -> currentRideData.pickupType
            },
            dropoffType = when {
                selectedTransferType.endsWith("City") -> "city"
                selectedTransferType.endsWith("Airport") -> "airport"
                selectedTransferType.endsWith("Cruise Port") -> "cruise"
                else -> currentRideData.dropoffType
            },
            noOfVehicles = numberOfVehicles,
            bookingHour = if (selectedServiceType == "Charter Tour") {
                selectedHours.replace(" hours minimum", "").replace(" hours", "").trim()
            } else {
                currentRideData.bookingHour
            }
        )
        vm.fetchBookingRates(
            ride = updatedRideData,
            vehicle = currentVehicle,
            isEditMode = isEditMode,
            editBookingId = editBookingId,
            hasExtraStops = hasExtraStops(editData),
            extraStops = extraStops.toExtraStopRequests(),
            returnExtraStops = returnExtraStops.toExtraStopRequests()
        )
    }
    
    // Trigger API when hours change (Charter Tour) - matches iOS
    LaunchedEffect(selectedHours) {
        if (selectedServiceType == "Charter Tour") {
            Log.d(DebugTags.BookingProcess, "🕐 Hours changed to: $selectedHours - Triggering booking rates API")
            val updatedRideData = currentRideData.copy(
                bookingHour = selectedHours.replace(" hours minimum", "").replace(" hours", "").trim()
            )
            vm.fetchBookingRates(
                ride = updatedRideData,
                vehicle = currentVehicle,
                isEditMode = isEditMode,
                editBookingId = editBookingId,
                hasExtraStops = hasExtraStops(editData),
                extraStops = extraStops.toExtraStopRequests(),
                returnExtraStops = returnExtraStops.toExtraStopRequests()
            )
        }
    }
    
    // Trigger API when number of vehicles changes - matches iOS
    LaunchedEffect(numberOfVehicles) {
        Log.d(DebugTags.BookingProcess, "🚗 Number of vehicles changed to: $numberOfVehicles - Triggering booking rates API")
        val updatedRideData = currentRideData.copy(noOfVehicles = numberOfVehicles)
        vm.fetchBookingRates(
            ride = updatedRideData,
            vehicle = currentVehicle,
            isEditMode = isEditMode,
            editBookingId = editBookingId,
            hasExtraStops = hasExtraStops(editData),
            extraStops = extraStops.toExtraStopRequests(),
            returnExtraStops = returnExtraStops.toExtraStopRequests()
        )
    }
    
    // Trigger API when pickup airport changes - matches iOS
    LaunchedEffect(selectedPickupAirport) {
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
                vm.fetchBookingRates(
                    ride = currentRideData,
                    vehicle = currentVehicle,
                    isEditMode = isEditMode,
                    editBookingId = editBookingId,
                    hasExtraStops = extraStops.any { it.isLocationSelected && it.latitude != null },
                    extraStops = extraStops.toExtraStopRequests(),
                    returnExtraStops = returnExtraStops.toExtraStopRequests()
                )
            }
        }
    }
    
    // Trigger API when dropoff airport changes - matches iOS
    LaunchedEffect(selectedDropoffAirport) {
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
                vm.fetchBookingRates(
                    ride = currentRideData,
                    vehicle = currentVehicle,
                    isEditMode = isEditMode,
                    editBookingId = editBookingId,
                    hasExtraStops = extraStops.any { it.isLocationSelected && it.latitude != null },
                    extraStops = extraStops.toExtraStopRequests(),
                    returnExtraStops = returnExtraStops.toExtraStopRequests()
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
    LaunchedEffect(pickupTime) {
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
                vm.fetchBookingRates(
                    ride = currentRideData.copy(pickupTime = pickupTime),
                    vehicle = currentVehicle,
                    isEditMode = isEditMode,
                    editBookingId = editBookingId,
                    hasExtraStops = extraStops.any { it.isLocationSelected && it.latitude != null },
                    extraStops = extraStops.toExtraStopRequests(),
                    returnExtraStops = returnExtraStops.toExtraStopRequests()
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
            vm.fetchBookingRates(
                ride = currentRideData,
                vehicle = currentVehicle,
                isEditMode = isEditMode,
                editBookingId = editBookingId,
                hasExtraStops = hasStopsWithLocations,
                extraStops = extraStops.toExtraStopRequests(),
                returnExtraStops = returnExtraStops.toExtraStopRequests()
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
    val ratesData: RatesData = remember(bookingRatesData, currentRideData.serviceType, currentRideData.bookingHour, currentRideData.noOfVehicles, selectedServiceType) {
        val bookingData = bookingRatesData // Local variable to avoid smart cast issue
        if (bookingData != null) {
            // Calculate from rate array (matches iOS calculateTotalsFromRateArray)
            val rateArray = bookingData.rateArray
            var totalBaserate = 0.0
            var allInclusiveBaserate = 0.0
            
            // Get hours multiplier for charter tours (use currentRideData, not rideData)
            // If min_rate_involved is true, don't multiply by hours (use base rate as-is)
            val minRateInvolved = bookingData.minRateInvolved ?: false
            val hoursMultiplier = if (currentRideData.serviceType.lowercase() == "charter_tour" && !minRateInvolved) {
                (currentRideData.bookingHour ?: "0").toIntOrNull() ?: 1
            } else {
                1
            }
            
            // Sum all baserate values from all_inclusive_rates
            for ((key, rateItem) in rateArray.allInclusiveRates) {
                // If min_rate_involved is true, don't multiply by hours
                val adjustedBaserate = if (currentRideData.serviceType.lowercase() == "charter_tour" && key == "Base_Rate" && !minRateInvolved) {
                    rateItem.baserate * hoursMultiplier
                } else {
                    rateItem.baserate
                }
                totalBaserate += adjustedBaserate
                allInclusiveBaserate += adjustedBaserate
            }
            
            // Sum amenities, taxes, misc
            for (rateItem in rateArray.amenities.values) {
                totalBaserate += rateItem.baserate
            }
            for (taxItem in rateArray.taxes.values) {
                // Use amount instead of baserate for taxes (amount is the actual tax value)
                totalBaserate += taxItem.amount
            }
            for (rateItem in rateArray.misc.values) {
                totalBaserate += rateItem.baserate
            }
            
            // Calculate subtotal: total baserate + 25% of all_inclusive_rates baserate
            val twentyFivePercentOfAllInclusive = allInclusiveBaserate * 0.25
            val calculatedSubtotal = totalBaserate + twentyFivePercentOfAllInclusive
            
            // Grand total = subtotal × number of vehicles (use currentRideData, not rideData)
            val calculatedGrandTotal = calculatedSubtotal * currentRideData.noOfVehicles
            
            // Calculate return trip totals if round trip (matches iOS - check selectedServiceType == "Round Trip")
            val (calculatedReturnSubtotal, calculatedReturnGrandTotal) = if (selectedServiceType == "Round Trip" && bookingData.retrunRateArray != null) {
                val returnRateArray = bookingData.retrunRateArray
                var returnTotalBaserate = 0.0
                var returnAllInclusiveBaserate = 0.0
                
                Log.d(DebugTags.BookingProcess, "📊 PROCESSING RETURN ALL_INCLUSIVE_RATES - Total items: ${returnRateArray.allInclusiveRates.size}")
                
                // Get hours multiplier for return trip (only for Charter Tour, not Round Trip - matches iOS)
                // If min_rate_involved is true, don't multiply by hours
                val returnHoursMultiplier = if (selectedServiceType == "Charter Tour" && !minRateInvolved) {
                    hoursMultiplier
                } else {
                    1
                }
                
                // Sum all baserate values from return rate array (matches iOS calculateReturnTotalsFromReturnRateArray)
                for ((key, rateItem) in returnRateArray.allInclusiveRates) {
                    // If min_rate_involved is true, don't multiply by hours
                    val adjustedBaserate = if (selectedServiceType == "Charter Tour" && key == "Base_Rate" && !minRateInvolved) {
                        rateItem.baserate * returnHoursMultiplier
                    } else {
                        rateItem.baserate
                    }
                    Log.d(DebugTags.BookingProcess, "  ✓ Return $key: baserate=${rateItem.baserate}, adjusted=$adjustedBaserate")
                    returnTotalBaserate += adjustedBaserate
                    returnAllInclusiveBaserate += adjustedBaserate
                }
                Log.d(DebugTags.BookingProcess, "  → Return All Inclusive Baserate Sum: $returnAllInclusiveBaserate")
                
                Log.d(DebugTags.BookingProcess, "📦 PROCESSING RETURN AMENITIES - Total items: ${returnRateArray.amenities.size}")
                // Sum amenities, taxes, misc for return trip
                for ((key, rateItem) in returnRateArray.amenities) {
                    Log.d(DebugTags.BookingProcess, "  ✓ Return $key: ${rateItem.baserate}")
                    returnTotalBaserate += rateItem.baserate
                }
                
                Log.d(DebugTags.BookingProcess, "💰 PROCESSING RETURN TAXES - Total items: ${returnRateArray.taxes.size}")
                for ((key, taxItem) in returnRateArray.taxes) {
                    // Use amount instead of baserate for taxes (amount is the actual tax value)
                    Log.d(DebugTags.BookingProcess, "  ✓ Return $key: baserate=${taxItem.baserate}, amount=${taxItem.amount}, using amount=${taxItem.amount}")
                    returnTotalBaserate += taxItem.amount
                }
                
                Log.d(DebugTags.BookingProcess, "📋 PROCESSING RETURN MISC - Total items: ${returnRateArray.misc.size}")
                for ((key, rateItem) in returnRateArray.misc) {
                    Log.d(DebugTags.BookingProcess, "  ✓ Return $key: ${rateItem.baserate}")
                    returnTotalBaserate += rateItem.baserate
                }
                
                // Calculate return subtotal
                val returnTwentyFivePercent = returnAllInclusiveBaserate * 0.25
                val calculatedReturnSubtotal = returnTotalBaserate + returnTwentyFivePercent
                
                // Return grand total = return subtotal × number of vehicles (use currentRideData, not rideData)
                val calculatedReturnGrandTotal = calculatedReturnSubtotal * currentRideData.noOfVehicles
                
                Log.d(DebugTags.BookingProcess, "📊 RETURN RATE CALCULATION BREAKDOWN:")
                Log.d(DebugTags.BookingProcess, "Service Type: $selectedServiceType")
                Log.d(DebugTags.BookingProcess, "Hours Multiplier: $returnHoursMultiplier")
                Log.d(DebugTags.BookingProcess, "Return All Inclusive Baserate (adjusted): $returnAllInclusiveBaserate")
                Log.d(DebugTags.BookingProcess, "Return Total Baserate (all categories, adjusted): $returnTotalBaserate")
                Log.d(DebugTags.BookingProcess, "Return 25% of All Inclusive: $returnTwentyFivePercent")
                Log.d(DebugTags.BookingProcess, "Calculated Return Subtotal: $calculatedReturnSubtotal")
                Log.d(DebugTags.BookingProcess, "Number of Vehicles: ${currentRideData.noOfVehicles}")
                Log.d(DebugTags.BookingProcess, "Calculated Return Grand Total: $calculatedReturnGrandTotal (Return Subtotal × ${currentRideData.noOfVehicles})")
                Log.d(DebugTags.BookingProcess, "✅ UI Updated with return booking rates: ReturnSubtotal=$calculatedReturnSubtotal, ReturnGrandTotal=$calculatedReturnGrandTotal")
                
                Pair(calculatedReturnSubtotal, calculatedReturnGrandTotal)
            } else {
                if (selectedServiceType == "Round Trip" && bookingData.retrunRateArray == null) {
                    Log.d(DebugTags.BookingProcess, "❌ No return rate array found in response for round trip")
                }
                Pair(0.0, 0.0)
            }
            
            Log.d(DebugTags.BookingProcess, "✅ UI Updated with booking rates: Subtotal=$calculatedSubtotal, GrandTotal=$calculatedGrandTotal")
            
            RatesData(calculatedSubtotal, calculatedGrandTotal, calculatedReturnSubtotal, calculatedReturnGrandTotal)
        } else {
            // Fallback to vehicle rate breakdown (use currentRideData, not rideData)
            val rateBreakdown = currentVehicle.getRateBreakdown(currentRideData.serviceType)
            val fallbackSubtotal = rateBreakdown?.subTotal ?: rateBreakdown?.total ?: 0.0
            val fallbackGrandTotal = (rateBreakdown?.grandTotal ?: rateBreakdown?.total ?: 0.0) * currentRideData.noOfVehicles
            Log.d(DebugTags.BookingProcess, "⚠️ Using fallback vehicle rates: Subtotal=$fallbackSubtotal, GrandTotal=$fallbackGrandTotal")
            RatesData(fallbackSubtotal, fallbackGrandTotal, 0.0, 0.0)
        }
    }
    
    val subtotal = ratesData.subtotal
    val grandTotal = ratesData.grandTotal
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
    // Only recalculate if coordinates have changed
    LaunchedEffect(
        currentRideData.pickupLat, currentRideData.pickupLong, 
        currentRideData.destinationLat, currentRideData.destinationLong,
        selectedTransferType, selectedPickupAirport, selectedDropoffAirport,
        selectedServiceType, isEditMode, editData, extraStops
    ) {
        // In edit mode, only calculate if we don't have distance/duration from editData
        val currentEditData = editData
        if (isEditMode && currentEditData != null && !currentEditData.distance.isNullOrEmpty() && !currentEditData.duration.isNullOrEmpty()) {
            Log.d(DebugTags.BookingProcess, "⏭️ Skipping distance calculation - using editData values")
            return@LaunchedEffect
        }
        
        // Get effective coordinates based on transfer type
        val effectivePickupCoord = getEffectiveOutboundPickupCoordinate(
            selectedTransferType,
            currentRideData.pickupLat,
            currentRideData.pickupLong,
            selectedPickupAirport
        )
        val effectiveDropoffCoord = getEffectiveOutboundDropoffCoordinate(
            selectedTransferType,
            currentRideData.destinationLat,
            currentRideData.destinationLong,
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
        
        if (pickupCoordChanged || dropoffCoordChanged || extraStopsChanged || cachedTravelInfo == null) {
            if (effectivePickupCoord.first != null && effectivePickupCoord.second != null &&
                effectiveDropoffCoord.first != null && effectiveDropoffCoord.second != null) {
                
                // Update cache keys
                lastOutboundPickupCoord = effectivePickupCoord
                lastOutboundDropoffCoord = effectiveDropoffCoord
                lastOutboundExtraStops = waypoints
                
                isCalculatingTravel = true
                distancesLoading = true
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
                    
                    Log.d(DebugTags.BookingProcess, "📍 Distance cached: $cachedTravelInfo")
                } catch (e: Exception) {
                    Log.e(DebugTags.BookingProcess, "Error calculating distances", e)
                } finally {
                    isCalculatingTravel = false
                    distancesLoading = false
                }
            }
        } else {
            Log.d(DebugTags.BookingProcess, "⏭️ Using cached travel info: $cachedTravelInfo")
        }
        
        // Calculate return distance for round trips
        if (selectedServiceType == "Round Trip") {
            val returnTransferType = getReversedTransferType(selectedTransferType)
            val effectiveReturnPickupCoord = getEffectiveOutboundPickupCoordinate(
                returnTransferType,
                currentRideData.destinationLat, // Return pickup = outbound dropoff
                currentRideData.destinationLong,
                selectedDropoffAirport // Return pickup airport = outbound dropoff airport
            )
            val effectiveReturnDropoffCoord = getEffectiveOutboundDropoffCoordinate(
                returnTransferType,
                currentRideData.pickupLat, // Return dropoff = outbound pickup
                currentRideData.pickupLong,
                selectedPickupAirport // Return dropoff airport = outbound pickup airport
            )
            
            val returnPickupCoordChanged = effectiveReturnPickupCoord != lastReturnPickupCoord
            val returnDropoffCoordChanged = effectiveReturnDropoffCoord != lastReturnDropoffCoord
            
            if (returnPickupCoordChanged || returnDropoffCoordChanged || cachedReturnTravelInfo == null) {
                if (effectiveReturnPickupCoord.first != null && effectiveReturnPickupCoord.second != null &&
                    effectiveReturnDropoffCoord.first != null && effectiveReturnDropoffCoord.second != null) {
                    
                    lastReturnPickupCoord = effectiveReturnPickupCoord
                    lastReturnDropoffCoord = effectiveReturnDropoffCoord
                    
                    isCalculatingReturnTravel = true
                    try {
                        // Extract waypoints from return extra stops
                        val returnWaypoints = returnExtraStops
                            .filter { it.isLocationSelected && it.latitude != null && it.longitude != null }
                            .map { Pair(it.latitude!!, it.longitude!!) }
                            .takeIf { it.isNotEmpty() }
                        
                        val (returnDistanceMeters, returnDurationSeconds) = directionsService.calculateDistance(
                            effectiveReturnPickupCoord.first!!, effectiveReturnPickupCoord.second!!,
                            effectiveReturnDropoffCoord.first!!, effectiveReturnDropoffCoord.second!!,
                            returnWaypoints
                        )
                        val (returnDistanceText, _) = directionsService.formatDistance(returnDistanceMeters)
                        val (returnDurationText, _) = directionsService.formatDuration(returnDurationSeconds)
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
                        
                        Log.d(DebugTags.BookingProcess, "📍 Return distance cached: $cachedReturnTravelInfo")
                    } catch (e: Exception) {
                        Log.e(DebugTags.BookingProcess, "Error calculating return distances", e)
                    } finally {
                        isCalculatingReturnTravel = false
                    }
                }
            }
        } else {
            returnDistance = null
            cachedReturnTravelInfo = null
            lastReturnPickupCoord = null
            lastReturnDropoffCoord = null
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
            timeFormatter.parse(pickupTime)
        } catch (e: Exception) {
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
        val parsedDate = try {
            dateFormatter.parse(pickupDate) ?: Date()
        } catch (e: Exception) {
            Date()
        }
        
        val parsedTime = try {
            timeFormatter.parse(pickupTime)
        } catch (e: Exception) {
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

        // Scrollable form content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .imePadding()
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Accounts Information Section
            AccountsInfoSection(profileData = profileData)

            Spacer(modifier = Modifier.height(24.dp))

            // Booking Details Section
            BookingDetailsSection(
                rideData = rideData,
                vehicle = vehicle,
                pickupDate = pickupDate,
                pickupTime = pickupTime,
                selectedServiceType = selectedServiceType,
                selectedTransferType = selectedTransferType,
                selectedHours = selectedHours,
                numberOfVehicles = numberOfVehicles,
                serviceTypes = serviceTypes,
                transferTypes = transferTypes,
                hoursOptions = hoursOptions,
                showServiceTypeDropdown = showServiceTypeDropdown,
                showTransferTypeDropdown = showTransferTypeDropdown,
                showHoursDropdown = showHoursDropdown,
                onServiceTypeSelected = { selectedServiceType = it },
                onTransferTypeSelected = { selectedTransferType = it },
                onHoursSelected = { selectedHours = it },
                onNumberOfVehiclesChange = { numberOfVehicles = it },
                onServiceTypeDropdownChange = { showServiceTypeDropdown = it },
                onTransferTypeDropdownChange = { showTransferTypeDropdown = it },
                onHoursDropdownChange = { showHoursDropdown = it },
                onDateClick = { showDatePicker = true },
                onTimeClick = { showTimePicker = true },
                isEditMode = isEditMode,
                passengerCount = passengerCount,
                luggageCount = luggageCount,
                onPassengerCountChange = { passengerCount = it },
                onLuggageCountChange = { luggageCount = it },
                // Meet & Greet fields (matches web app)
                selectedMeetAndGreet = selectedMeetAndGreet,
                meetAndGreetOptions = meetAndGreetOptions,
                showMeetAndGreetDropdown = showMeetAndGreetDropdown,
                onMeetAndGreetChange = { selectedMeetAndGreet = it },
                onMeetAndGreetDropdownChange = { showMeetAndGreetDropdown = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Pick-up Section
            PickupSection(
                selectedTransferType = selectedTransferType,
                pickupLocation = pickupLocation,
                pickupDate = pickupDate,
                pickupTime = pickupTime,
                pickupFlightNumber = pickupFlightNumber,
                originAirportCity = originAirportCity,
                cruiseShipName = cruiseShipName,
                shipArrivalTime = shipArrivalTime,
                cruisePort = cruisePort,
                selectedPickupAirport = selectedPickupAirport,
                selectedPickupAirline = selectedPickupAirline,
                onLocationSelected = { fullAddress, city, state, zipCode, locationDisplay, latitude, longitude, country ->
                    pickupLocation = locationDisplay
                    // Update coordinates in currentRideData
                    currentRideData = currentRideData.copy(
                        pickupLocation = locationDisplay,
                        pickupLat = latitude,
                        pickupLong = longitude
                    )
                    Log.d(DebugTags.BookingProcess, "📍 Pickup location selected: $locationDisplay")
                    
                    // Recalculate distance and time when location changes (matches iOS calculateAndCacheTravelInfo)
                    coroutineScope.launch {
                        if (currentRideData.pickupLat != null && currentRideData.pickupLong != null &&
                            currentRideData.destinationLat != null && currentRideData.destinationLong != null) {
                            try {
                                distancesLoading = true
                                val (distanceMeters, durationSeconds) = directionsService.calculateDistance(
                                    currentRideData.pickupLat!!, currentRideData.pickupLong!!,
                                    currentRideData.destinationLat!!, currentRideData.destinationLong!!
                                )
                                val (distanceText, _) = directionsService.formatDistance(distanceMeters)
                                val (durationText, _) = directionsService.formatDuration(durationSeconds)
                                outboundDistance = Pair(distanceText, durationText)
                                Log.d(DebugTags.BookingProcess, "📍 Updated distance: $distanceText, duration: $durationText")
                            } catch (e: Exception) {
                                Log.e(DebugTags.BookingProcess, "Error recalculating distance", e)
                            } finally {
                                distancesLoading = false
                            }
                        }
                        
                        // Automatically call booking rates API when pickup location is selected - matches iOS
                        vm.fetchBookingRates(
                            ride = currentRideData,
                            vehicle = currentVehicle,
                            isEditMode = isEditMode,
                            editBookingId = editBookingId,
                            hasExtraStops = extraStops.any { it.isLocationSelected && it.latitude != null },
                            extraStops = extraStops.toExtraStopRequests(),
                            returnExtraStops = returnExtraStops.toExtraStopRequests()
                        )
                    }
                },
                onLocationChange = { pickupLocation = it },
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
                    pickupFlightNumber = it
                    // Auto-populate return dropoff flight number if empty (matches web app)
                    if (selectedServiceType == "Round Trip" && returnDropoffFlightNumber.isEmpty() && it.isNotEmpty()) {
                        returnDropoffFlightNumber = it
                        Log.d(DebugTags.BookingProcess, "🔄 Auto-populated return dropoff flight number: $it")
                    }
                },
                onOriginCityChange = {
                    originAirportCity = it
                    // Also update currentRideData to keep them in sync
                    currentRideData = currentRideData.copy(originAirportCity = it)
                    Log.d(DebugTags.BookingProcess, "📍 Origin Airport City updated by user: '$it'")
                },
                onCruiseShipChange = { cruiseShipName = it },
                onShipArrivalChange = { shipArrivalTime = it },
                onCruisePortChange = { cruisePort = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Drop-off Section
            DropoffSection(
                selectedTransferType = selectedTransferType,
                dropoffLocation = dropoffLocation,
                dropoffFlightNumber = dropoffFlightNumber,
                cruiseShipName = cruiseShipName,
                shipArrivalTime = shipArrivalTime,
                cruisePort = cruisePort,
                selectedDropoffAirport = selectedDropoffAirport,
                selectedDropoffAirline = selectedDropoffAirline,
                onLocationSelected = { fullAddress, city, state, zipCode, locationDisplay, latitude, longitude, country ->
                    dropoffLocation = locationDisplay
                    // Update coordinates in currentRideData
                    currentRideData = currentRideData.copy(
                        destinationLocation = locationDisplay,
                        destinationLat = latitude,
                        destinationLong = longitude
                    )
                    Log.d(DebugTags.BookingProcess, "📍 Dropoff location selected: $locationDisplay")
                    
                    // Recalculate distance and time when location changes (matches iOS calculateAndCacheTravelInfo)
                    coroutineScope.launch {
                        if (currentRideData.pickupLat != null && currentRideData.pickupLong != null &&
                            currentRideData.destinationLat != null && currentRideData.destinationLong != null) {
                            try {
                                distancesLoading = true
                                val (distanceMeters, durationSeconds) = directionsService.calculateDistance(
                                    currentRideData.pickupLat!!, currentRideData.pickupLong!!,
                                    currentRideData.destinationLat!!, currentRideData.destinationLong!!
                                )
                                val (distanceText, _) = directionsService.formatDistance(distanceMeters)
                                val (durationText, _) = directionsService.formatDuration(durationSeconds)
                                outboundDistance = Pair(distanceText, durationText)
                                Log.d(DebugTags.BookingProcess, "📍 Updated distance: $distanceText, duration: $durationText")
                            } catch (e: Exception) {
                                Log.e(DebugTags.BookingProcess, "Error recalculating distance", e)
                            } finally {
                                distancesLoading = false
                            }
                        }
                        
                        // Automatically call booking rates API when dropoff location is selected - matches iOS
                        vm.fetchBookingRates(
                            ride = currentRideData,
                            vehicle = currentVehicle,
                            isEditMode = isEditMode,
                            editBookingId = editBookingId,
                            hasExtraStops = extraStops.any { it.isLocationSelected && it.latitude != null },
                            extraStops = extraStops.toExtraStopRequests(),
                            returnExtraStops = returnExtraStops.toExtraStopRequests()
                        )
                    }
                },
                onLocationChange = { dropoffLocation = it },
                onAirportClick = {
                    currentAirportType = "dropoff"
                    showAirportBottomSheet = true
                },
                onAirlineClick = {
                    currentAirlineType = "dropoff"
                    showAirlineBottomSheet = true
                },
                onFlightNumberChange = { 
                    dropoffFlightNumber = it
                    // Auto-populate return pickup flight number if empty (matches web app)
                    if (selectedServiceType == "Round Trip" && returnPickupFlightNumber.isEmpty() && it.isNotEmpty()) {
                        returnPickupFlightNumber = it
                        Log.d(DebugTags.BookingProcess, "🔄 Auto-populated return pickup flight number: $it")
                    }
                },
                onCruiseShipChange = { cruiseShipName = it },
                onShipArrivalChange = { shipArrivalTime = it },
                onCruisePortChange = { cruisePort = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Extra Stops Section (positioned right after Dropoff Section)
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
                        vm.fetchBookingRates(
                            ride = currentRideData,
                            vehicle = currentVehicle,
                            isEditMode = isEditMode,
                            editBookingId = editBookingId,
                            hasExtraStops = updatedStops.any { it.isLocationSelected && it.latitude != null },
                            extraStops = updatedStops.toExtraStopRequests(),
                            returnExtraStops = returnExtraStops.toExtraStopRequests()
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
                        extraStops = extraStops.map {
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
                        // Automatically call booking rates API when extra stop location is selected
                        coroutineScope.launch {
                            vm.fetchBookingRates(
                                ride = currentRideData,
                                vehicle = currentVehicle,
                                isEditMode = isEditMode,
                                editBookingId = editBookingId,
                                hasExtraStops = true,
                                extraStops = extraStops.toExtraStopRequests(),
                                returnExtraStops = returnExtraStops.toExtraStopRequests()
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
                },
                showInvalidLocationDialog = showInvalidLocationDialog,
                invalidLocationMessage = invalidLocationMessage
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
                    onReturnTransferTypeSelected = { selectedReturnTransferType = it },
                    onReturnMeetAndGreetSelected = { selectedReturnMeetAndGreet = it },
                    onReturnDateClick = { showReturnDatePicker = true },
                    onReturnTimeClick = { showReturnTimePicker = true },
                    onReturnPickupLocationSelected = { fullAddress, city, state, zipCode, locationDisplay, latitude, longitude, country ->
                        returnPickupLocation = locationDisplay
                        Log.d(
                            DebugTags.BookingProcess,
                            "📍 Return pickup location selected: $locationDisplay"
                        )
                    },
                    onReturnPickupLocationChange = { returnPickupLocation = it },
                    onReturnDropoffLocationSelected = { fullAddress, city, state, zipCode, locationDisplay, latitude, longitude, country ->
                        returnDropoffLocation = locationDisplay
                        Log.d(
                            DebugTags.BookingProcess,
                            "📍 Return dropoff location selected: $locationDisplay"
                        )
                        // Automatically call booking rates API when return dropoff location is selected - matches iOS
                        coroutineScope.launch {
                            vm.fetchBookingRates(
                                ride = currentRideData,
                                vehicle = currentVehicle,
                                isEditMode = isEditMode,
                                editBookingId = editBookingId,
                                hasExtraStops = hasExtraStops(editData),
                                extraStops = extraStops.toExtraStopRequests(),
                                returnExtraStops = returnExtraStops.toExtraStopRequests()
                            )
                        }
                    },
                    onReturnDropoffLocationChange = { returnDropoffLocation = it },
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
                    onReturnFlightNumberChange = { returnPickupFlightNumber = it },
                    onReturnDropoffFlightNumberChange = { returnDropoffFlightNumber = it },
                    onReturnOriginCityChange = { returnOriginAirportCity = it },
                    onReturnCruiseShipChange = { returnCruiseShipName = it },
                    onReturnShipArrivalChange = { returnShipArrivalTime = it },
                    onReturnSpecialInstructionsChange = { returnSpecialInstructions = it },
                    onReturnServiceTypeDropdownChange = { showReturnServiceTypeDropdown = it },
                    onReturnTransferTypeDropdownChange = { showReturnTransferTypeDropdown = it },
                    onReturnMeetAndGreetDropdownChange = { showReturnMeetAndGreetDropdown = it },
                    // Return Extra Stops parameters
                    returnExtraStops = returnExtraStops,
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
                                hasExtraStops = updatedStops.any { it.isLocationSelected && it.latitude != null }
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
                            pickupLat = null, // TODO: Get return pickup coordinates
                            pickupLong = null,
                            dropoffLat = null, // TODO: Get return dropoff coordinates
                            dropoffLong = null,
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
                            returnExtraStops = returnExtraStops.map {
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
                            // Automatically call booking rates API
                            coroutineScope.launch {
                                vm.fetchBookingRates(
                                    ride = currentRideData,
                                    vehicle = currentVehicle,
                                    isEditMode = isEditMode,
                                    editBookingId = editBookingId,
                                    hasExtraStops = true
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
                    showReturnInvalidLocationDialog = showInvalidLocationDialog,
                    returnInvalidLocationMessage = invalidLocationMessage,
                    // Return Distance parameters
                    returnDistance = returnDistance,
                    returnDistanceLoading = distancesLoading
                )
            }

            // Distance Information Section (positioned after Extra Stops to align with distance calculation)
            val currentOutboundDistance = outboundDistance
            if (currentOutboundDistance != null) {
                Spacer(modifier = Modifier.height(24.dp))
                DistanceInformationSection(
                    outboundDistance = currentOutboundDistance,
                    serviceType = selectedServiceType,
                    isLoading = distancesLoading
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Special Instructions Section (positioned after Distance Information)
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
                        Log.d(
                            DebugTags.BookingProcess,
                            "==========================================="
                        )
                        Log.d(
                            DebugTags.BookingProcess,
                            "📱 BOOK NOW BUTTON CLICKED (ComprehensiveBookingScreen)"
                        )
                        Log.d(
                            DebugTags.BookingProcess,
                            "==========================================="
                        )
                        Log.d(
                            DebugTags.BookingProcess,
                            "Original Ride Data: serviceType=${rideData.serviceType}, pickup=${rideData.pickupLocation}, dropoff=${rideData.destinationLocation}"
                        )
                        Log.d(DebugTags.BookingProcess, "Updated Pickup: $pickupLocation")
                        Log.d(DebugTags.BookingProcess, "Updated Dropoff: $dropoffLocation")
                        Log.d(
                            DebugTags.BookingProcess,
                            "Vehicle: id=${vehicle.id}, name=${vehicle.name}"
                        )
                        Log.d(DebugTags.BookingProcess, "Calling vm.createReservation()...")
                        Log.d(
                            DebugTags.BookingProcess,
                            "==========================================="
                        )

                        // CRITICAL FIX: Mark that booking was initiated by user
                        bookingInitiated = true

                        // Update rideData with edited values including airline and flight data
                        Log.d(
                            DebugTags.BookingProcess,
                            "📍 Origin Airport City from screen state: '$originAirportCity'"
                        )
                        val updatedRideData = currentRideData.copy(
                            pickupDate = pickupDate,
                            pickupTime = pickupTime,
                            pickupLocation = pickupLocation,
                            destinationLocation = dropoffLocation,
                            selectedPickupAirline = selectedPickupAirline?.displayName,
                            selectedDestinationAirline = selectedDropoffAirline?.displayName,
                            pickupFlightNumber = pickupFlightNumber,
                            dropoffFlightNumber = dropoffFlightNumber,
                            returnPickupFlightNumber = returnPickupFlightNumber,
                            returnDropoffFlightNumber = returnDropoffFlightNumber,
                            originAirportCity = originAirportCity,
                            noOfPassenger = passengerCount,
                            noOfLuggage = luggageCount,
                            noOfVehicles = numberOfVehicles,
                            bookingHour = if (selectedServiceType == "Charter Tour") {
                                selectedHours.replace(" hours minimum", "").replace(" hours", "")
                                    .trim()
                            } else {
                                currentRideData.bookingHour
                            },
                            serviceType = when (selectedServiceType) {
                                "One Way" -> "one_way"
                                "Round Trip" -> "round_trip"
                                "Charter Tour" -> "charter_tour"
                                else -> currentRideData.serviceType
                            },
                            pickupType = when {
                                selectedTransferType.startsWith("City") -> "city"
                                selectedTransferType.startsWith("Airport") -> "airport"
                                selectedTransferType.startsWith("Cruise") -> "cruise"
                                else -> currentRideData.pickupType
                            },
                            dropoffType = when {
                                selectedTransferType.endsWith("City") -> "city"
                                selectedTransferType.endsWith("Airport") -> "airport"
                                selectedTransferType.endsWith("Cruise Port") -> "cruise"
                                else -> currentRideData.dropoffType
                            },
                            selectedPickupAirport = selectedPickupAirport?.displayName ?: "",
                            selectedDestinationAirport = selectedDropoffAirport?.displayName ?: ""
                        )
                        Log.d(
                            DebugTags.BookingProcess,
                            "📍 Origin Airport City in updatedRideData: '${updatedRideData.originAirportCity}'"
                        )

                        if (isEditMode && editBookingId != null) {
                            // Update existing reservation (matches iOS updateReservation)
                            Log.d(
                                DebugTags.BookingProcess,
                                "🔄 Updating reservation with ID: $editBookingId"
                            )
                            // CRITICAL FIX: Mark that booking update was initiated by user
                            bookingInitiated = true
                            vm.updateReservation(
                                editBookingId,
                                updatedRideData,
                                currentVehicle,
                                extraStops = extraStops.toExtraStopRequests(),
                                returnExtraStops = returnExtraStops.toExtraStopRequests(),
                                bookingInstructions = specialInstructions,
                                returnBookingInstructions = returnSpecialInstructions,
                                meetGreetChoicesName = selectedMeetAndGreet,
                                returnMeetGreetChoicesName = selectedReturnMeetAndGreet,
                                cruisePort = cruisePort,
                                cruiseName = cruiseShipName,
                                cruiseTime = shipArrivalTime
                            )
                        } else {
                            // Create new reservation or duplicate (repeat mode)
                            vm.createReservation(
                                updatedRideData,
                                currentVehicle,
                                extraStops = extraStops.toExtraStopRequests(),
                                returnExtraStops = returnExtraStops.toExtraStopRequests(),
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
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 20.dp,
                            vertical = 16.dp
                        ) // UI FIX: Specific padding for sticky look
                        .height(50.dp),
                    enabled = !loading && isFormValid,
                    colors = ButtonDefaults.buttonColors(containerColor = LimoOrange),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            if (isEditMode) "Update Reservation" else "Create Booking",
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
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
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
                returnPickupDate = dateFormatter.format(date)
                showReturnDatePicker = false
            },
            onDismiss = { showReturnDatePicker = false }
        )
    }
    
    // Return Time Picker Dialog
    val returnSelectedTime = remember(returnPickupTime) {
        if (returnPickupTime.isNotEmpty()) {
            try {
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val parsedTime = timeFormat.parse(returnPickupTime)
                if (parsedTime != null) {
                    Calendar.getInstance().apply { time = parsedTime }
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
    
    if (showReturnTimePicker) {
        TimePickerDialog(
            selectedTime = returnSelectedTime.time,
            onTimeSelected = { time ->
                returnPickupTime = timeFormatter.format(time)
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
                    // Auto-populate return dropoff airline if empty (matches web app)
                    if (selectedServiceType == "Round Trip" && selectedReturnDropoffAirline == null) {
                        selectedReturnDropoffAirline = airline
                        Log.d(DebugTags.BookingProcess, "🔄 Auto-populated return dropoff airline: ${airline.displayName}")
                    }
                }
                "dropoff" -> {
                    selectedDropoffAirline = airline
                    // Auto-populate return pickup airline if empty (matches web app)
                    if (selectedServiceType == "Round Trip" && selectedReturnPickupAirline == null) {
                        selectedReturnPickupAirline = airline
                        Log.d(DebugTags.BookingProcess, "🔄 Auto-populated return pickup airline: ${airline.displayName}")
                    }
                }
                "returnPickup" -> selectedReturnPickupAirline = airline
                "returnDropoff" -> selectedReturnDropoffAirline = airline
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
        onDismiss = { showAirportBottomSheet = false },
        onItemSelected = { airport ->
            when (currentAirportType) {
                "pickup" -> {
                    selectedPickupAirport = airport
                    pickupLocation = airport.displayName
                }
                "dropoff" -> {
                    selectedDropoffAirport = airport
                    dropoffLocation = airport.displayName
                }
                "returnPickup" -> {
                    selectedReturnPickupAirport = airport
                    returnPickupLocation = airport.displayName
                }
                "returnDropoff" -> {
                    selectedReturnDropoffAirport = airport
                    returnDropoffLocation = airport.displayName
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

// ==================== EXTRACTED CODE ====================
// All helper functions and section composables have been extracted to separate files:
// - BookingUtils.kt: Helper functions (prefillExtraStopsFromEditData, validateExtraStop, coordinate functions, etc.)
// - BookingFormComponents.kt: UI components (EditableField, StyledDropdown, etc.)
// - sections/BookingSections.kt: BookingDetailsSection, SpecialInstructionsSection
// - sections/LocationSections.kt: PickupSection, DropoffSection (to be created)
// - sections/ReturnSections.kt: ReturnJourneySection, ReturnPickupSection, ReturnDropoffSection (to be created)
// - sections/ExtraStopsSections.kt: ExtraStopsSection, ReturnExtraStopsSection, ExtraStopRow (to be created)
// - sections/SummarySections.kt: BookingSummarySection, DistanceInformationSection (to be created)
// - sections/TransportationSections.kt: TransportationDetailsSection (to be created)
//
// The main screen now only contains the core booking logic and state management.
// All extracted code has been removed to reduce file size from ~4800 lines to ~2700 lines.
//
// NOTE: The section composables (PickupSection, DropoffSection, etc.) are still defined
// as private functions in this file because they are tightly coupled with the main screen's
// state management. They can be extracted to separate files in a future refactoring if needed.
//
// All helper functions below have been moved to BookingUtils.kt and should be imported from there.
// The functions are kept here temporarily for backward compatibility but will be removed.

// Helper functions removed - now imported from BookingUtils.kt
// The following functions have been moved:
// - prefillExtraStopsFromEditData
// - coordinatesApproximatelyEqual
// - normalizeLocationText
// - extractCountryFromAddress
// - normalizeCountry
// - checkCountryMismatch
// - getEffectiveOutboundPickupCoordinate
// - getEffectiveOutboundDropoffCoordinate
// - getOutboundPickupLatitude/Longitude
// - getOutboundDropoffLatitude/Longitude
// - getReturnPickupLatitude/Longitude
// - getReturnDropoffLatitude/Longitude
// - validateExtraStop
// - calculateDistance
// - List<ExtraStop>.toExtraStopRequests()
// - hasExtraStops
// - Vehicle.getRateBreakdown()

// ==================== SECTION COMPOSABLES ====================
// Section composables are still defined below as they are tightly coupled with the main screen.
// They can be extracted to separate files in a future refactoring.

// BookingDetailsSection has been moved to comprehensivebooking/sections/BookingSections.kt

// PickupSection and DropoffSection have been moved to comprehensivebooking/sections/LocationSections.kt

// All duplicate helper functions have been removed - now imported from BookingUtils.kt
// All duplicate section composables have been removed - they are defined below or in separate files

// ==================== SECTION COMPOSABLES ====================

// AccountsInfoCard and InfoField have been moved to comprehensivebooking/sections/AccountsInfoSection.kt and BookingFormComponents.kt



// BookingDetailsSection has been moved to comprehensivebooking/sections/BookingSections.kt
// PickupSection and DropoffSection have been moved to comprehensivebooking/sections/LocationSections.kt

// ReturnJourneySection, ReturnPickupSection, and ReturnDropoffSection have been moved to comprehensivebooking/sections/ReturnSections.kt

// SpecialInstructionsSection has been moved to comprehensivebooking/sections/BookingSections.kt

// TransportationDetailsSection has been moved to comprehensivebooking/sections/TransportationDetailsSection.kt

// ExtraStopsSection, ReturnExtraStopsSection, and ExtraStopRow have been moved to comprehensivebooking/sections/ExtraStopsSections.kt
// BookingSummarySection and DistanceInformationSection have been moved to comprehensivebooking/sections/SummarySections.kt
// EditableField has been moved to comprehensivebooking/BookingFormComponents.kt
// DropdownField is not used - StyledDropdown is used instead

// EditableTextField, StyledDropdown, StyledInput, SectionHeader, and helper functions 
// have been moved to comprehensivebooking/BookingFormComponents.kt and BookingUtils.kt