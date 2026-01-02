package com.example.limouserapp.ui.booking

import android.content.Context
import android.location.Geocoder
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.example.limouserapp.R
import com.example.limouserapp.data.PlacePrediction
import com.example.limouserapp.data.PlacesService
import com.example.limouserapp.data.model.booking.*
import com.example.limouserapp.ui.booking.components.*
import com.example.limouserapp.ui.theme.*
import com.example.limouserapp.ui.viewmodel.ScheduleRideViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// ==========================================
// Events
// ==========================================
data class ScheduleRideEvents(
    val onRideTypeSelected: (RideType) -> Unit,
    val onBookingTypeSelected: (BookingType) -> Unit,
    val onDestinationTypeSelected: (BookingType) -> Unit,
    val onPickupValueChange: (String) -> Unit,
    val onDestinationValueChange: (String) -> Unit,
    val onPickupFocusChanged: (Boolean) -> Unit,
    val onDestinationFocusChanged: (Boolean) -> Unit,
    val onPickupClear: () -> Unit,
    val onDestinationClear: () -> Unit,
    val onPickupSuggestionSelected: (PlacePrediction) -> Unit,
    val onDestinationSuggestionSelected: (PlacePrediction) -> Unit,
    val onPickupAirportSelected: (String) -> Unit,
    val onDestinationAirportSelected: (String) -> Unit,
    val onNext: () -> Unit,
    val onDismiss: () -> Unit,
    val onPickupMapClick: () -> Unit,
    val onDestinationMapClick: () -> Unit
)

// ==========================================
// Main Screen Controller
// ==========================================
@Composable
fun ScheduleRideScreen(
    onDismiss: () -> Unit,
    onNavigateToTimeSelection: (RideData) -> Unit,
    navController: NavHostController,
    initialLocation: LocationCoordinate? = null,
    initialAddress: String? = null,
    initialRideData: RideData? = null
) {
    val context = LocalContext.current
    val placesService = remember { PlacesService(context) }
    val viewModel: ScheduleRideViewModel = hiltViewModel()
    val airportService = viewModel.airportService
    val recentLocationService = viewModel.recentLocationService
    val locationManager = viewModel.locationManager
    val coroutineScope = rememberCoroutineScope()

    // State Variables
    // hasAutoNavigated is now managed in ViewModel - consume as one-time guard
    val hasAutoNavigated by viewModel.hasAutoNavigated.collectAsStateWithLifecycle()

    var selectedRideType by rememberSaveable {
        mutableStateOf(initialRideData?.let { RideType.fromServiceType(it.serviceType) } ?: RideType.ONE_WAY)
    }
    var selectedBookingType by rememberSaveable {
        mutableStateOf(initialRideData?.let { BookingType.fromPickupType(it.pickupType) } ?: BookingType.CITY_FBO)
    }
    var selectedDestinationType by rememberSaveable {
        mutableStateOf(initialRideData?.let { BookingType.fromPickupType(it.dropoffType) } ?: BookingType.AIRPORT)
    }
    var selectedHours by rememberSaveable {
        mutableStateOf(initialRideData?.bookingHour?.let { "$it hours minimum" } ?: "2 hours minimum")
    }

    // Text Fields
    var pickupLocation by rememberSaveable { mutableStateOf(initialRideData?.pickupLocation ?: initialAddress ?: "") }
    var destinationLocation by rememberSaveable { mutableStateOf(initialRideData?.destinationLocation ?: "") }
    var selectedPickupAirport by rememberSaveable { mutableStateOf(initialRideData?.selectedPickupAirport ?: "") }
    var selectedDestinationAirport by rememberSaveable { mutableStateOf(initialRideData?.selectedDestinationAirport ?: "") }

    // Coordinates - Save as separate values to persist across navigation
    var pickupLat by rememberSaveable { mutableStateOf(initialRideData?.pickupLat ?: initialLocation?.latitude ?: 0.0) }
    var pickupLong by rememberSaveable { mutableStateOf(initialRideData?.pickupLong ?: initialLocation?.longitude ?: 0.0) }
    var pickupCountryCode by rememberSaveable { mutableStateOf(initialRideData?.pickupCountryCode ?: initialLocation?.countryCode) }
    var pickupPostalCode by rememberSaveable { mutableStateOf(initialRideData?.pickupPostalCode ?: initialLocation?.postalCode) }
    
    var destLat by rememberSaveable { mutableStateOf(initialRideData?.destinationLat ?: 0.0) }
    var destLong by rememberSaveable { mutableStateOf(initialRideData?.destinationLong ?: 0.0) }
    var destCountryCode by rememberSaveable { mutableStateOf(initialRideData?.destinationCountryCode) }
    var destPostalCode by rememberSaveable { mutableStateOf(initialRideData?.destinationPostalCode) }
    
    // Reconstruct LocationCoordinate from saved values
    var pickupCoordinate by remember(pickupLat, pickupLong, pickupCountryCode, pickupPostalCode) {
        mutableStateOf<LocationCoordinate?>(
            if (pickupLat != 0.0 && pickupLong != 0.0) {
                LocationCoordinate(pickupLat, pickupLong, pickupCountryCode, pickupPostalCode)
            } else null
        )
    }
    var destinationCoordinate by remember(destLat, destLong, destCountryCode, destPostalCode) {
        mutableStateOf<LocationCoordinate?>(
            if (destLat != 0.0 && destLong != 0.0) {
                LocationCoordinate(destLat, destLong, destCountryCode, destPostalCode)
            } else null
        )
    }

    // Search/UI States
    var showPickupSuggestions by rememberSaveable { mutableStateOf(false) }
    var showDestinationSuggestions by rememberSaveable { mutableStateOf(false) }
    var pickupAirportSearch by rememberSaveable { mutableStateOf("") }
    var destinationAirportSearch by rememberSaveable { mutableStateOf("") }
    var focusedField by rememberSaveable { mutableStateOf<String?>(null) }
    var isValidating by remember { mutableStateOf(false) } // Shows loading spinner
    var isNavigatingToTimeSelection by remember { mutableStateOf(false) } // Prevents multiple navigations
    var invalidLocationMessage by remember { mutableStateOf("") }
    var showInvalidLocationDialog by remember { mutableStateOf(false) }

    // Data Lists
    var pickupPredictions by remember { mutableStateOf<List<PlacePrediction>>(emptyList()) }
    var destinationPredictions by remember { mutableStateOf<List<PlacePrediction>>(emptyList()) }
    val airportSuggestions by airportService.suggestions.collectAsStateWithLifecycle()
    val pickupRecentLocations by recentLocationService.pickupLocations.collectAsStateWithLifecycle()
    val dropoffRecentLocations by recentLocationService.dropoffLocations.collectAsStateWithLifecycle()
    val isLoadingRecent by recentLocationService.isLoading.collectAsStateWithLifecycle()
    val isLoadingAirports by airportService.isLoading.collectAsStateWithLifecycle()

    // Search Jobs
    var searchJob by remember { mutableStateOf<Job?>(null) }

    // Keyboard instances
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current


    // --- LOGIC: MAP NAVIGATION HELPER ---
    suspend fun navigateToMap(isPickup: Boolean) {
        val coord = if (isPickup) pickupCoordinate else destinationCoordinate
        val addr = if (isPickup) pickupLocation else destinationLocation
        val key = if (isPickup) "pickup" else "destination"

        // Default to current location if nothing selected
        val (lat, lng, address) = if (coord != null) {
            Triple(coord.latitude, coord.longitude, addr)
        } else {
            val current = locationManager.getCurrentLocation().getOrNull()
            if (current != null) Triple(current.latitude, current.longitude, current.address)
            else Triple(0.0, 0.0, "")
        }

        navController.currentBackStackEntry?.savedStateHandle?.set("map_picker_result_key", key)
        navController.navigate("mapLocationPicker?initialLat=$lat&initialLong=$lng&initialAddress=$address")
    }

    suspend fun executeValidationAndNext(
        pCoord: LocationCoordinate?,
        dCoord: LocationCoordinate?,
        pText: String,
        dText: String
    ) {
        if (isNavigatingToTimeSelection) return

        if (pCoord == null || dCoord == null) return
        if (showPickupSuggestions || showDestinationSuggestions) return

        val latDiff = kotlin.math.abs(pCoord.latitude - dCoord.latitude)
        val longDiff = kotlin.math.abs(pCoord.longitude - dCoord.longitude)
        if (latDiff < 0.0001 && longDiff < 0.0001) {
            invalidLocationMessage = "Pickup and destination cannot be the same location"
            showInvalidLocationDialog = true
            return
        }

        isNavigatingToTimeSelection = true
        isValidating = true

        val error = getLocationValidationError(context, pCoord, dCoord)

        isValidating = false

        if (error != null) {
            invalidLocationMessage = error
            showInvalidLocationDialog = true
            isNavigatingToTimeSelection = false
            return
        }

        val now = Date()
        val rideData = RideData(
            serviceType = selectedRideType.toServiceType(),
            bookingHour = selectedHours.replace(" hours minimum", "").trim(),
            pickupType = selectedBookingType.toPickupType(),
            dropoffType = selectedDestinationType.toPickupType(),
            pickupDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now),
            pickupTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now),
            pickupLocation = pText,
            destinationLocation = dText,
            selectedPickupAirport = if (selectedBookingType == BookingType.AIRPORT) pText else "",
            selectedDestinationAirport = if (selectedDestinationType == BookingType.AIRPORT) dText else "",
            noOfPassenger = 1,
            noOfLuggage = 1,
            noOfVehicles = 1,
            pickupLat = pCoord.latitude,
            pickupLong = pCoord.longitude,
            destinationLat = dCoord.latitude,
            destinationLong = dCoord.longitude,
            pickupCountryCode = pCoord.countryCode,
            destinationCountryCode = dCoord.countryCode,
            pickupPostalCode = pCoord.postalCode,
            destinationPostalCode = dCoord.postalCode
        )

        delay(80) // let keyboard + UI settle
        onNavigateToTimeSelection(rideData)
    }


    // --- LOGIC: CHECK IF PLACE IS AIRPORT ---
    fun isAirportPlace(placeTypes: List<String>?): Boolean {
        if (placeTypes == null) return false
        return placeTypes.any { type ->
            type.contains("AIRPORT", ignoreCase = true) ||
            type.contains("AERODROME", ignoreCase = true)
        }
    }

    // --- LOGIC: HANDLE ANY SELECTION (Map, List, Recent) ---
    fun handleLocationSelection(
        isPickup: Boolean,
        text: String,
        coordinate: LocationCoordinate?,
        placeTypes: List<String>? = null
    ) {
        // Reset suggestions and focused field to show the Continue button
        showPickupSuggestions = false
        showDestinationSuggestions = false
        focusedField = null

        // Reset auto-navigation guard when location selection changes
        if (hasAutoNavigated) {
            viewModel.resetAutoNavigationGuard()
        }

        // Auto-detect airport and set booking type
        val isAirport = isAirportPlace(placeTypes)
        if (isAirport) {
            if (isPickup) {
                selectedBookingType = BookingType.AIRPORT
                selectedPickupAirport = text
            } else {
                selectedDestinationType = BookingType.AIRPORT
                selectedDestinationAirport = text
            }
        }

        // Update location state
        if (isPickup) {
            pickupLocation = text
            if (coordinate != null) {
                pickupLat = coordinate.latitude
                pickupLong = coordinate.longitude
                pickupCountryCode = coordinate.countryCode
                pickupPostalCode = coordinate.postalCode
            }
            pickupCoordinate = coordinate
        } else {
            destinationLocation = text
            if (coordinate != null) {
                destLat = coordinate.latitude
                destLong = coordinate.longitude
                destCountryCode = coordinate.countryCode
                destPostalCode = coordinate.postalCode
            }
            destinationCoordinate = coordinate
        }
    }
    
    // --- AUTO-NAVIGATION: Automatically proceed when both locations are selected ---
    LaunchedEffect(
        pickupCoordinate,
        destinationCoordinate,
        hasAutoNavigated
    ) {
        // One-time navigation guard - check ViewModel state
        if (hasAutoNavigated) return@LaunchedEffect
        if (showPickupSuggestions || showDestinationSuggestions) return@LaunchedEffect

        val p = pickupCoordinate
        val d = destinationCoordinate

        if (p == null || d == null) return@LaunchedEffect
        if (p.latitude == 0.0 || p.longitude == 0.0) return@LaunchedEffect
        if (d.latitude == 0.0 || d.longitude == 0.0) return@LaunchedEffect

        val sameLocation =
            kotlin.math.abs(p.latitude - d.latitude) < 0.0001 &&
                    kotlin.math.abs(p.longitude - d.longitude) < 0.0001

        if (sameLocation) return@LaunchedEffect

        // ✅ Mark as auto-navigated in ViewModel (one-time guard)
        viewModel.markAutoNavigated()

        // 🔽 CLOSE KEYBOARD FIRST (smooth UX)
        keyboardController?.hide()
        focusManager.clearFocus(force = true)

        // (Optional but recommended)
        delay(80) // allows keyboard animation to start

        // 🚀 Navigate
        executeValidationAndNext(p, d, pickupLocation, destinationLocation)
    }


    // --- EFFECT: MAP RESULT LISTENER ---
    val currentBackStackEntry = navController.currentBackStackEntry
    LaunchedEffect(currentBackStackEntry) {
        currentBackStackEntry?.savedStateHandle?.let { handle ->
            handle.getStateFlow<String?>("map_picker_result_key", null).collect { resultKey ->
                if (resultKey != null) {
                    val loc = handle.get<LocationCoordinate>("selected_location")
                    val addr = handle.get<String>("selected_address") ?: ""

                    if (loc != null) {
                        handle.remove<String>("map_picker_result_key")
                        handle.remove<LocationCoordinate>("selected_location")
                        handle.remove<String>("selected_address")

                        // For map selection, we don't have place types, so pass null
                        // User can manually change booking type if needed
                        handleLocationSelection(
                            isPickup = (resultKey == "pickup"),
                            text = addr,
                            coordinate = loc,
                            placeTypes = null
                        )
                    }
                }
            }
        }
    }

    // --- UI LAYOUT ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LimoWhite)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { ScheduleRideHeader(onDismiss) }

            // FIXED: Using named arguments
            item {
                RideTypeSelection(
                    selectedRideType = selectedRideType,
                    onRideTypeSelected = { selectedRideType = it }
                )
            }

            if (selectedRideType == RideType.HOURLY) {
                item {
                    Spacer(Modifier.height(16.dp))
                    // FIXED: Using named arguments
                    HoursDropdown(
                        selectedHours = selectedHours,
                        onHoursSelected = { selectedHours = it },
                        modifier = Modifier.padding(horizontal = 0.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            // FIXED: Using named arguments
            item {
                BookingTypeSelection(
                    selectedBookingType = selectedBookingType,
                    selectedDestinationType = selectedDestinationType,
                    onBookingTypeSelected = { selectedBookingType = it },
                    onDestinationTypeSelected = { selectedDestinationType = it }
                )
            }

            // --- INPUT CARD + BUTTON ---
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        LocationInputCard(
                            pickupValue = pickupLocation,
                            destinationValue = destinationLocation,
                            onPickupValueChange = {
                                pickupLocation = it
                                // Reset auto-navigation guard when pickup input changes
                                if (hasAutoNavigated) {
                                    viewModel.resetAutoNavigationGuard()
                                }
                                if (selectedBookingType == BookingType.AIRPORT) {
                                    searchJob?.cancel()
                                    searchJob = coroutineScope.launch {
                                        delay(300) // Debounce airport search
                                        if (it.length >= 2) {
                                            showPickupSuggestions = true
                                            airportService.searchAirports(it)
                                        } else if (it.isEmpty()) {
                                            showPickupSuggestions = false
                                            airportService.clearSuggestions()
                                        }
                                    }
                                } else {
                                    searchJob?.cancel()
                                    searchJob = coroutineScope.launch {
                                        delay(300)
                                        if (it.length >= 2) {
                                            pickupPredictions = placesService.getPlacePredictions(it)
                                            showPickupSuggestions = true
                                        }
                                    }
                                }
                            },
                            onDestinationValueChange = {
                                destinationLocation = it
                                // Reset auto-navigation guard when destination input changes
                                if (hasAutoNavigated) {
                                    viewModel.resetAutoNavigationGuard()
                                }
                                if (selectedDestinationType == BookingType.AIRPORT) {
                                    searchJob?.cancel()
                                    searchJob = coroutineScope.launch {
                                        delay(300) // Debounce airport search
                                        if (it.length >= 2) {
                                            showDestinationSuggestions = true
                                            airportService.searchAirports(it)
                                        } else if (it.isEmpty()) {
                                            showDestinationSuggestions = false
                                            airportService.clearSuggestions()
                                        }
                                    }
                                } else {
                                    searchJob?.cancel()
                                    searchJob = coroutineScope.launch {
                                        delay(300)
                                        if (it.length >= 2) {
                                            destinationPredictions = placesService.getPlacePredictions(it)
                                            showDestinationSuggestions = true
                                        }
                                    }
                                }
                            },
                            onPickupFocusChanged = { if(it) { focusedField = "pickup"; showDestinationSuggestions = false } },
                            onDestinationFocusChanged = { if(it) { focusedField = "destination"; showPickupSuggestions = false } },
                            showPickupClear = pickupLocation.isNotEmpty(),
                            showDestinationClear = destinationLocation.isNotEmpty(),
                            onPickupClear = { 
                                pickupLocation = ""
                                pickupLat = 0.0
                                pickupLong = 0.0
                                pickupCountryCode = null
                                pickupPostalCode = null
                                pickupCoordinate = null
                                // Reset auto-navigation guard when pickup is cleared
                                if (hasAutoNavigated) {
                                    viewModel.resetAutoNavigationGuard()
                                }
                            },
                            onDestinationClear = { 
                                destinationLocation = ""
                                destLat = 0.0
                                destLong = 0.0
                                destCountryCode = null
                                destPostalCode = null
                                destinationCoordinate = null
                                // Reset auto-navigation guard when destination is cleared
                                if (hasAutoNavigated) {
                                    viewModel.resetAutoNavigationGuard()
                                }
                            },
                            onPickupMapClick = { coroutineScope.launch { navigateToMap(true) } },
                            onDestinationMapClick = { coroutineScope.launch { navigateToMap(false) } }
                        )

                        // --- CONTINUE BUTTON (Stable, Right Aligned) ---
                        val bothLocationsFilled = (pickupLocation.isNotEmpty() || selectedPickupAirport.isNotEmpty()) &&
                                                 (destinationLocation.isNotEmpty() || selectedDestinationAirport.isNotEmpty())
                        val noSuggestionsShowing = !showPickupSuggestions && !showDestinationSuggestions

                        // Capture current coordinate values to avoid smart cast issues with delegated properties
                        val currentPickupCoord = pickupCoordinate
                        val currentDestCoord = destinationCoordinate

                        val hasValidCoordinates = currentPickupCoord != null && currentDestCoord != null &&
                                                 currentPickupCoord.latitude != 0.0 && currentPickupCoord.longitude != 0.0 &&
                                                 currentDestCoord.latitude != 0.0 && currentDestCoord.longitude != 0.0

                        // Check if locations are the same (within ~11 meters)
                        val locationsAreSame = if (currentPickupCoord != null && currentDestCoord != null) {
                            val latDiff = kotlin.math.abs(currentPickupCoord.latitude - currentDestCoord.latitude)
                            val longDiff = kotlin.math.abs(currentPickupCoord.longitude - currentDestCoord.longitude)
                            latDiff < 0.0001 && longDiff < 0.0001
                        } else false

                        // Log button visibility state for debugging
                        val shouldShowButton = bothLocationsFilled && noSuggestionsShowing && hasValidCoordinates && !locationsAreSame
                        LaunchedEffect(shouldShowButton, bothLocationsFilled, noSuggestionsShowing, hasValidCoordinates, locationsAreSame) {
                            Log.d("ScheduleRide", "🔘 Next button visibility check:")
                            Log.d("ScheduleRide", "🔘   bothLocationsFilled: $bothLocationsFilled")
                            Log.d("ScheduleRide", "🔘   noSuggestionsShowing: $noSuggestionsShowing")
                            Log.d("ScheduleRide", "🔘   hasValidCoordinates: $hasValidCoordinates")
                            Log.d("ScheduleRide", "🔘   locationsAreSame: $locationsAreSame")
                            Log.d("ScheduleRide", "🔘   shouldShowButton: $shouldShowButton")
                            if (hasValidCoordinates) {
                                Log.d("ScheduleRide", "🔘   Pickup coord: Lat=${currentPickupCoord?.latitude}, Long=${currentPickupCoord?.longitude}")
                                Log.d("ScheduleRide", "🔘   Dest coord: Lat=${currentDestCoord?.latitude}, Long=${currentDestCoord?.longitude}")
                            }
                        }

                        AnimatedVisibility(
                            visible = shouldShowButton,
                            enter = fadeIn(animationSpec = tween(durationMillis = 200)),
                            exit = fadeOut(animationSpec = tween(durationMillis = 200))
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    Surface(
                                        onClick = {
                                            coroutineScope.launch {
                                                executeValidationAndNext(
                                                    pickupCoordinate,
                                                    destinationCoordinate,
                                                    pickupLocation,
                                                    destinationLocation
                                                )
                                            }
                                        },
                                        shape = CircleShape,
                                        color = LimoOrange,
                                        shadowElevation = 4.dp,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (isValidating) {
                                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                            } else {
                                                Icon(Icons.Default.ArrowForward, "Continue", tint = Color.White, modifier = Modifier.size(24.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (showInvalidLocationDialog && invalidLocationMessage.isNotEmpty()) {
                item { UberStyleErrorBanner(invalidLocationMessage) { showInvalidLocationDialog = false } }
            }

            if (focusedField == "pickup" && selectedBookingType != BookingType.AIRPORT) {
                item { SetOnMapItem { coroutineScope.launch { navigateToMap(true) } } }
            } else if (focusedField == "destination" && selectedDestinationType != BookingType.AIRPORT) {
                item { SetOnMapItem { coroutineScope.launch { navigateToMap(false) } } }
            }

            // --- LIST SUGGESTIONS ---
            if (focusedField == "pickup") {
                if (showPickupSuggestions) {
                    if (selectedBookingType == BookingType.AIRPORT) {
                        itemsIndexed(airportSuggestions) { _, name ->
                            RideAirportItem(name) { 
                                selectedPickupAirport = name
                                handleLocationSelection(
                                    true, 
                                    name, 
                                    airportService.selectAirportSuggestion(name)?.let { LocationCoordinate(it.lat?:0.0, it.long?:0.0) },
                                    listOf("AIRPORT") // Known airport
                                ) 
                            }
                        }
                    } else {
                        itemsIndexed(pickupPredictions) { _, pred ->
                            RideAddressItem(pred) {
                                coroutineScope.launch {
                                    val details = placesService.getPlaceDetails(pred.placeId)
                                    val coord = details?.let { LocationCoordinate(it.latitude?:0.0, it.longitude?:0.0, it.country, it.postalCode) }
                                    // Pass place types for airport detection
                                    handleLocationSelection(true, details?.address ?: pred.primaryText, coord, details?.types)
                                }
                            }
                        }
                    }
                } else if (pickupLocation.isEmpty()) {
                    if (isLoadingRecent) items(3) { UberShimmerItem() }
                    else items(pickupRecentLocations) { loc ->
                        UberRecentLocationItem(loc) { 
                            // If it's a known airport from recent locations, auto-set booking type
                            if (loc.isAirport) {
                                selectedBookingType = BookingType.AIRPORT
                                selectedPickupAirport = loc.airportName ?: loc.address
                            }
                            handleLocationSelection(
                                true, 
                                if(loc.isAirport) loc.airportName?:loc.address else loc.address, 
                                loc.toLocationCoordinate(),
                                // Pass airport type if known
                                if (loc.isAirport) listOf("AIRPORT") else null
                            ) 
                        }
                    }
                }
            }
            else if (focusedField == "destination") {
                if (showDestinationSuggestions) {
                    if (selectedDestinationType == BookingType.AIRPORT) {
                        itemsIndexed(airportSuggestions) { _, name ->
                            RideAirportItem(name) { 
                                selectedDestinationAirport = name
                                handleLocationSelection(
                                    false, 
                                    name, 
                                    airportService.selectAirportSuggestion(name)?.let { LocationCoordinate(it.lat?:0.0, it.long?:0.0) },
                                    listOf("AIRPORT") // Known airport
                                ) 
                            }
                        }
                    } else {
                        itemsIndexed(destinationPredictions) { _, pred ->
                            RideAddressItem(pred) {
                                coroutineScope.launch {
                                    val details = placesService.getPlaceDetails(pred.placeId)
                                    val coord = details?.let { LocationCoordinate(it.latitude?:0.0, it.longitude?:0.0, it.country, it.postalCode) }
                                    // Pass place types for airport detection
                                    handleLocationSelection(false, details?.address ?: pred.primaryText, coord, details?.types)
                                }
                            }
                        }
                    }
                } else if (destinationLocation.isEmpty()) {
                    if (isLoadingRecent) items(3) { UberShimmerItem() }
                    else items(dropoffRecentLocations) { loc ->
                        UberRecentLocationItem(loc) { 
                            // If it's a known airport from recent locations, auto-set booking type
                            if (loc.isAirport) {
                                selectedDestinationType = BookingType.AIRPORT
                                selectedDestinationAirport = loc.airportName ?: loc.address
                            }
                            handleLocationSelection(
                                false, 
                                if(loc.isAirport) loc.airportName?:loc.address else loc.address, 
                                loc.toLocationCoordinate(),
                                // Pass airport type if known
                                if (loc.isAirport) listOf("AIRPORT") else null
                            ) 
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// Components & Helpers
// ==========================================

@Composable
fun UberStyleErrorBanner(message: String, onDismiss: () -> Unit) {
    Surface(
        onClick = onDismiss,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFEF2F2),
        border = BorderStroke(1.dp, Color(0xFFA6342E)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, "Error", tint = Color(0xFFA6342E), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Location Error", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFA6342E)))
                Text(message, style = TextStyle(fontSize = 13.sp, color = Color(0xFFA6342E)))
            }
        }
    }
}

@Composable
fun SetOnMapItem(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(36.dp).background(LimoOrange.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_location_pin), null, tint = LimoOrange, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text("Set location on map", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = LimoBlack))
    }
    Divider(color = LimoBlack.copy(0.05f), thickness = 1.dp)
}

@Composable
fun UberRecentLocationItem(location: RecentLocation, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(36.dp).background(Color(0xFFEEEEEE), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Schedule, null, tint = Color.Black, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(if (location.isAirport) location.airportName ?: location.address else location.address.substringBefore(","), style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold))
            Text(location.address, style = TextStyle(fontSize = 14.sp, color = Color.Gray), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun RideAddressItem(prediction: PlacePrediction, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(36.dp).background(Color(0xFFEEEEEE), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.LocationOn, null, tint = Color.Black, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(prediction.primaryText, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium))
            Text(prediction.fullText ?: prediction.secondaryText ?: "", style = TextStyle(fontSize = 14.sp, color = Color.Gray), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun RideAirportItem(airportName: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 16.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(36.dp).background(Color(0xFFEEEEEE), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Flight, null, tint = Color.Black, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(airportName, style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium))
    }
}

@Composable
fun UberShimmerItem() {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(Color.LightGray.copy(0.3f)))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Box(Modifier.width(100.dp).height(14.dp).background(Color.LightGray.copy(0.3f)))
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth(0.8f).height(12.dp).background(Color.LightGray.copy(0.3f)))
        }
    }
}

private fun calculateDistance(c1: LocationCoordinate, c2: LocationCoordinate): Double {
    val R = 6371.0; val dLat = Math.toRadians(c2.latitude - c1.latitude); val dLon = Math.toRadians(c2.longitude - c1.longitude)
    val a = sin(dLat / 2) * sin(dLat / 2) + cos(Math.toRadians(c1.latitude)) * cos(Math.toRadians(c2.latitude)) * sin(dLon / 2) * sin(dLon / 2)
    return R * 2 * atan2(sqrt(a), sqrt(1 - a))
}

suspend fun getLocationValidationError(context: Context, pickup: LocationCoordinate?, dropoff: LocationCoordinate?): String? {
    if (pickup == null) return "Please select a valid pickup location."
    if (dropoff == null) return "Please select a valid destination."
    if (calculateDistance(pickup, dropoff) < 0.05) return "Pickup and drop locations are same"
    val geo = Geocoder(context, Locale.getDefault())
    try {
        @Suppress("DEPRECATION")
        val pC = geo.getFromLocation(pickup.latitude, pickup.longitude, 1)?.firstOrNull()?.countryCode
        @Suppress("DEPRECATION")
        val dC = geo.getFromLocation(dropoff.latitude, dropoff.longitude, 1)?.firstOrNull()?.countryCode
        if (pC != null && dC != null && !pC.equals(dC, true)) return "International rides not supported ($pC vs $dC)."
    } catch (e: Exception) { Log.e("Val", "Geo fail", e) }
    if (calculateDistance(pickup, dropoff) > 1000.0) return "Distance too far."
    return null
}