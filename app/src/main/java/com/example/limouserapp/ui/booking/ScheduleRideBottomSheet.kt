package com.example.limouserapp.ui.booking

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.example.limouserapp.ui.utils.DebugTags
import com.example.limouserapp.ui.viewmodel.ScheduleRideViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

/**
 * Events for Schedule Ride Bottom Sheet
 */
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
// Main Screen Composable (Business Logic)
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
    val coroutineScope = rememberCoroutineScope()

    // Recent locations state
    val pickupRecentLocations by recentLocationService.pickupLocations.collectAsStateWithLifecycle()
    val dropoffRecentLocations by recentLocationService.dropoffLocations.collectAsStateWithLifecycle()
    val isLoadingRecentLocations by recentLocationService.isLoading.collectAsStateWithLifecycle()

    // Core states - restore from initialRideData if available
    var selectedRideType by remember(initialRideData) {
        mutableStateOf(initialRideData?.let { RideType.fromServiceType(it.serviceType) } ?: RideType.ONE_WAY)
    }
    var selectedBookingType by remember(initialRideData) {
        mutableStateOf(initialRideData?.let { BookingType.fromPickupType(it.pickupType) } ?: BookingType.CITY_FBO)
    }
    var selectedDestinationType by remember(initialRideData) {
        mutableStateOf(initialRideData?.let { BookingType.fromPickupType(it.dropoffType) } ?: BookingType.AIRPORT)
    }
    var selectedHours by remember(initialRideData) {
        mutableStateOf(initialRideData?.bookingHour?.let { "$it hours minimum" } ?: "2 hours minimum")
    }

    var pickupLocation by remember(initialRideData) {
        mutableStateOf(initialRideData?.pickupLocation ?: "")
    }
    var destinationLocation by remember(initialRideData) {
        mutableStateOf(initialRideData?.destinationLocation ?: "")
    }
    var selectedPickupAirport by remember(initialRideData) {
        mutableStateOf(initialRideData?.selectedPickupAirport ?: "")
    }
    var selectedDestinationAirport by remember(initialRideData) {
        mutableStateOf(initialRideData?.selectedDestinationAirport ?: "")
    }
    var pickupAirportSearch by remember(initialRideData) {
        mutableStateOf(initialRideData?.selectedPickupAirport ?: "")
    }
    var destinationAirportSearch by remember(initialRideData) {
        mutableStateOf(initialRideData?.selectedDestinationAirport ?: "")
    }

    var pickupCoordinate by remember(initialRideData) {
        mutableStateOf<LocationCoordinate?>(
            initialRideData?.let {
                if (it.pickupLat != null && it.pickupLong != null) {
                    LocationCoordinate(it.pickupLat, it.pickupLong)
                } else null
            } ?: null
        )
    }
    var destinationCoordinate by remember(initialRideData) {
        mutableStateOf<LocationCoordinate?>(
            initialRideData?.let {
                if (it.destinationLat != null && it.destinationLong != null) {
                    LocationCoordinate(it.destinationLat, it.destinationLong)
                } else null
            } ?: null
        )
    }

    // Suggestions
    var showPickupSuggestions by remember { mutableStateOf(false) }
    var showDestinationSuggestions by remember { mutableStateOf(false) }
    var pickupPredictions by remember { mutableStateOf<List<PlacePrediction>>(emptyList()) }
    var destinationPredictions by remember { mutableStateOf<List<PlacePrediction>>(emptyList()) }
    var focusedField by remember { mutableStateOf<String?>(null) }

    var pickupSearchJob by remember { mutableStateOf<Job?>(null) }
    var destinationSearchJob by remember { mutableStateOf<Job?>(null) }

    // Error state
    var showInvalidLocationDialog by remember { mutableStateOf(false) }
    var invalidLocationMessage by remember { mutableStateOf("") }

    val airportSuggestions by airportService.suggestions.collectAsStateWithLifecycle()
    val isLoadingAirports by airportService.isLoading.collectAsStateWithLifecycle()

    // Observe navigation back stack for results from MapLocationPickerScreen
    LaunchedEffect(navController.currentBackStackEntry) {
        val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
        val resultKey = savedStateHandle?.get<String>("map_picker_result_key")
        val selectedLocationFromMap = savedStateHandle?.get<LocationCoordinate>("selected_location")
        val selectedAddressFromMap = savedStateHandle?.get<String>("selected_address")

        if (resultKey != null && selectedLocationFromMap != null && selectedAddressFromMap != null) {
            when (resultKey) {
                "pickup" -> {
                    pickupLocation = selectedAddressFromMap
                    pickupCoordinate = selectedLocationFromMap
                    showPickupSuggestions = false
                    focusedField = null
                }
                "destination" -> {
                    destinationLocation = selectedAddressFromMap
                    destinationCoordinate = selectedLocationFromMap
                    showDestinationSuggestions = false
                    focusedField = null
                }
            }
            // Trigger validation after map selection
            val validationError = getLocationValidationError(
                pickupLocation,
                destinationLocation,
                selectedPickupAirport,
                selectedDestinationAirport,
                pickupCoordinate,
                destinationCoordinate,
                selectedBookingType,
                selectedDestinationType,
                placesService
            )
            if (validationError != null) {
                invalidLocationMessage = validationError
                showInvalidLocationDialog = true
            }

            savedStateHandle.remove<String>("map_picker_result_key")
            savedStateHandle.remove<LocationCoordinate>("selected_location")
            savedStateHandle.remove<String>("selected_address")
        }
    }

    // Computed values
    val pickupText = remember(selectedBookingType, selectedPickupAirport, pickupLocation) {
        derivedStateOf { if (selectedBookingType == BookingType.AIRPORT) selectedPickupAirport else pickupLocation }
    }.value

    val destinationText = remember(selectedDestinationType, selectedDestinationAirport, destinationLocation) {
        derivedStateOf { if (selectedDestinationType == BookingType.AIRPORT) selectedDestinationAirport else destinationLocation }
    }.value

    val pickupSearchValue = remember(selectedBookingType, pickupAirportSearch, pickupLocation) {
        derivedStateOf { if (selectedBookingType == BookingType.AIRPORT) pickupAirportSearch else pickupLocation }
    }.value

    val destinationSearchValue = remember(selectedDestinationType, destinationAirportSearch, destinationLocation) {
        derivedStateOf { if (selectedDestinationType == BookingType.AIRPORT) destinationAirportSearch else destinationLocation }
    }.value

    val bothLocationsFilled = remember(pickupLocation, selectedPickupAirport, destinationLocation, selectedDestinationAirport) {
        derivedStateOf {
            (pickupLocation.isNotEmpty() || selectedPickupAirport.isNotEmpty()) &&
                    (destinationLocation.isNotEmpty() || selectedDestinationAirport.isNotEmpty())
        }
    }.value

    val hasValidCoordinates = remember(pickupCoordinate, destinationCoordinate) {
        derivedStateOf { pickupCoordinate != null && destinationCoordinate != null }
    }.value

    // Callback functions
    val onPickupSuggestionSelected: (PlacePrediction) -> Unit = { prediction ->
        pickupLocation = prediction.primaryText
        pickupPredictions = emptyList()
        showPickupSuggestions = false
        coroutineScope.launch {
            val details = placesService.getPlaceDetails(prediction.placeId)
            details?.let {
                pickupCoordinate = LocationCoordinate(it.latitude ?: 0.0, it.longitude ?: 0.0, it.country, it.postalCode)
            }
        }
    }

    val onDestinationSuggestionSelected: (PlacePrediction) -> Unit = { prediction ->
        destinationLocation = prediction.primaryText
        destinationPredictions = emptyList()
        showDestinationSuggestions = false
        coroutineScope.launch {
            val details = placesService.getPlaceDetails(prediction.placeId)
            details?.let {
                destinationCoordinate = LocationCoordinate(it.latitude ?: 0.0, it.longitude ?: 0.0, it.country, it.postalCode)
                if (pickupCoordinate != null) {
                    createAndNavigateToTimeSelection(
                        selectedRideType, selectedBookingType, selectedDestinationType,
                        pickupLocation, destinationLocation, selectedPickupAirport, selectedDestinationAirport,
                        pickupCoordinate, destinationCoordinate, selectedHours, onNavigateToTimeSelection
                    )
                }
            }
        }
    }

    val onPickupAirportSelected: (String) -> Unit = { airportName ->
        selectedPickupAirport = airportName
        pickupAirportSearch = airportName
        val airport = airportService.selectAirportSuggestion(airportName)
        airport?.let {
            pickupCoordinate = LocationCoordinate(it.lat ?: 0.0, it.long ?: 0.0, null, null)
        }
        showPickupSuggestions = false
    }

    val onDestinationAirportSelected: (String) -> Unit = { airportName ->
        selectedDestinationAirport = airportName
        destinationAirportSearch = airportName
        val airport = airportService.selectAirportSuggestion(airportName)
        airport?.let {
            destinationCoordinate = LocationCoordinate(it.lat ?: 0.0, it.long ?: 0.0, null, null)
            if (pickupCoordinate != null) {
                createAndNavigateToTimeSelection(
                    selectedRideType, selectedBookingType, selectedDestinationType,
                    pickupLocation, destinationLocation, selectedPickupAirport, selectedDestinationAirport,
                    pickupCoordinate, destinationCoordinate, selectedHours, onNavigateToTimeSelection
                )
            }
        }
        showDestinationSuggestions = false
    }

    val onPickupRecentLocationSelected: (RecentLocation) -> Unit = { location ->
        if (location.isAirport) {
            selectedPickupAirport = location.airportName ?: location.address
            pickupAirportSearch = location.airportName ?: location.address
        } else {
            pickupLocation = location.address
        }
        pickupCoordinate = location.toLocationCoordinate()
        showPickupSuggestions = false
        recentLocationService.clearPickupLocations()
    }

    val onDestinationRecentLocationSelected: (RecentLocation) -> Unit = { location ->
        if (location.isAirport) {
            selectedDestinationAirport = location.airportName ?: location.address
            destinationAirportSearch = location.airportName ?: location.address
        } else {
            destinationLocation = location.address
        }
        destinationCoordinate = location.toLocationCoordinate()
        showDestinationSuggestions = false
        recentLocationService.clearDropoffLocations()
        if (pickupCoordinate != null) {
            createAndNavigateToTimeSelection(
                selectedRideType, selectedBookingType, selectedDestinationType,
                pickupLocation, destinationLocation, selectedPickupAirport, selectedDestinationAirport,
                pickupCoordinate, destinationCoordinate, selectedHours, onNavigateToTimeSelection
            )
        }
    }

    // Create state object
    val state = remember(
        selectedRideType, selectedBookingType, selectedDestinationType,
        pickupLocation, destinationLocation, selectedPickupAirport, selectedDestinationAirport,
        pickupAirportSearch, destinationAirportSearch, pickupCoordinate, destinationCoordinate,
        showPickupSuggestions, showDestinationSuggestions, pickupPredictions, destinationPredictions,
        airportSuggestions, isLoadingAirports, focusedField, selectedHours
    ) {
        ScheduleRideState(
            selectedRideType = selectedRideType,
            selectedBookingType = selectedBookingType,
            selectedDestinationType = selectedDestinationType,
            pickupLocation = pickupLocation,
            destinationLocation = destinationLocation,
            selectedPickupAirport = selectedPickupAirport,
            selectedDestinationAirport = selectedDestinationAirport,
            pickupAirportSearch = pickupAirportSearch,
            destinationAirportSearch = destinationAirportSearch,
            pickupCoordinate = pickupCoordinate,
            destinationCoordinate = destinationCoordinate,
            showPickupSuggestions = showPickupSuggestions,
            showDestinationSuggestions = showDestinationSuggestions,
            pickupPredictions = pickupPredictions,
            destinationPredictions = destinationPredictions,
            airportSuggestions = airportSuggestions,
            isLoadingAirports = isLoadingAirports,
            focusedField = focusedField,
            selectedHours = selectedHours
        )
    }

    // Create events object
    val events = remember(coroutineScope, placesService, airportService, onNavigateToTimeSelection, navController) {
        ScheduleRideEvents(
            onRideTypeSelected = { selectedRideType = it },
            onBookingTypeSelected = { selectedBookingType = it },
            onDestinationTypeSelected = { selectedDestinationType = it },
            onPickupValueChange = { newValue ->
                if (selectedBookingType == BookingType.AIRPORT) {
                    pickupAirportSearch = newValue
                    if (newValue.length >= 2) {
                        showPickupSuggestions = true
                        coroutineScope.launch { airportService.searchAirports(newValue) }
                    } else {
                        showPickupSuggestions = true
                        coroutineScope.launch { airportService.fetchInitialAirports() }
                    }
                } else {
                    pickupLocation = newValue
                    pickupSearchJob?.cancel()
                    pickupSearchJob = coroutineScope.launch {
                        delay(300)
                        if (newValue.length >= 2) {
                            pickupPredictions = placesService.getPlacePredictions(newValue)
                            showPickupSuggestions = true
                        } else {
                            pickupPredictions = emptyList()
                            showPickupSuggestions = false
                        }
                    }
                }
            },
            onDestinationValueChange = { newValue ->
                if (selectedDestinationType == BookingType.AIRPORT) {
                    destinationAirportSearch = newValue
                    if (newValue.length >= 2) {
                        showDestinationSuggestions = true
                        coroutineScope.launch { airportService.searchAirports(newValue) }
                    } else {
                        showDestinationSuggestions = true
                        coroutineScope.launch { airportService.fetchInitialAirports() }
                    }
                } else {
                    destinationLocation = newValue
                    destinationSearchJob?.cancel()
                    destinationSearchJob = coroutineScope.launch {
                        delay(300)
                        if (newValue.length >= 2) {
                            destinationPredictions = placesService.getPlacePredictions(newValue)
                            showDestinationSuggestions = true
                        } else {
                            destinationPredictions = emptyList()
                            showDestinationSuggestions = false
                        }
                    }
                }
            },
            onPickupFocusChanged = { isFocused ->
                if (isFocused) {
                    focusedField = "pickup"
                    showDestinationSuggestions = false
                    if (selectedBookingType == BookingType.AIRPORT) {
                        showPickupSuggestions = true
                        if (pickupAirportSearch.length < 2) coroutineScope.launch { airportService.fetchInitialAirports() }
                    } else {
                        if (pickupLocation.length >= 2 && pickupPredictions.isEmpty()) {
                            coroutineScope.launch {
                                pickupPredictions = placesService.getPlacePredictions(pickupLocation)
                                showPickupSuggestions = true
                            }
                        } else if (pickupLocation.isEmpty() || pickupLocation.length <= 2) {
                            coroutineScope.launch { recentLocationService.fetchRecentLocations("pickup") }
                        }
                    }
                }
            },
            onDestinationFocusChanged = { isFocused ->
                if (isFocused) {
                    focusedField = "destination"
                    showPickupSuggestions = false
                    if (selectedDestinationType == BookingType.AIRPORT) {
                        showDestinationSuggestions = true
                        if (destinationAirportSearch.length < 2) coroutineScope.launch { airportService.fetchInitialAirports() }
                    } else {
                        if (destinationLocation.length >= 2 && destinationPredictions.isEmpty()) {
                            coroutineScope.launch {
                                destinationPredictions = placesService.getPlacePredictions(destinationLocation)
                                showDestinationSuggestions = true
                            }
                        } else if (destinationLocation.isEmpty() || destinationLocation.length <= 2) {
                            coroutineScope.launch { recentLocationService.fetchRecentLocations("dropoff") }
                        }
                    }
                }
            },
            onPickupClear = {
                if (selectedBookingType == BookingType.AIRPORT) {
                    pickupAirportSearch = ""
                    selectedPickupAirport = ""
                } else {
                    pickupLocation = ""
                }
            },
            onDestinationClear = {
                if (selectedDestinationType == BookingType.AIRPORT) {
                    destinationAirportSearch = ""
                    selectedDestinationAirport = ""
                } else {
                    destinationLocation = ""
                }
            },
            onPickupSuggestionSelected = onPickupSuggestionSelected,
            onDestinationSuggestionSelected = onDestinationSuggestionSelected,
            onPickupAirportSelected = onPickupAirportSelected,
            onDestinationAirportSelected = onDestinationAirportSelected,
            onNext = {
                coroutineScope.launch {
                    val validationError = getLocationValidationError(
                        pickupLocation, destinationLocation, selectedPickupAirport, selectedDestinationAirport,
                        pickupCoordinate, destinationCoordinate, selectedBookingType, selectedDestinationType, placesService
                    )

                    if (validationError != null) {
                        invalidLocationMessage = validationError
                        showInvalidLocationDialog = true
                    } else {
                        createAndNavigateToTimeSelection(
                            selectedRideType, selectedBookingType, selectedDestinationType,
                            pickupLocation, destinationLocation, selectedPickupAirport, selectedDestinationAirport,
                            pickupCoordinate, destinationCoordinate, selectedHours, onNavigateToTimeSelection
                        )
                    }
                }
            },
            onDismiss = onDismiss,
            onPickupMapClick = {
                navController.currentBackStackEntry?.savedStateHandle?.set("map_picker_result_key", "pickup")
                navController.navigate("mapLocationPicker?initialLat=${pickupCoordinate?.latitude}&initialLong=${pickupCoordinate?.longitude}&initialAddress=${pickupLocation}")
            },
            onDestinationMapClick = {
                navController.currentBackStackEntry?.savedStateHandle?.set("map_picker_result_key", "destination")
                navController.navigate("mapLocationPicker?initialLat=${destinationCoordinate?.latitude}&initialLong=${destinationCoordinate?.longitude}&initialAddress=${destinationLocation}")
            }
        )
    }

    ScheduleRideScreenContent(
        state = state,
        events = events,
        pickupText = pickupText,
        destinationText = destinationText,
        pickupSearchValue = pickupSearchValue,
        destinationSearchValue = destinationSearchValue,
        bothLocationsFilled = bothLocationsFilled,
        hasValidCoordinates = hasValidCoordinates,
        pickupRecentLocations = pickupRecentLocations,
        dropoffRecentLocations = dropoffRecentLocations,
        isLoadingRecentLocations = isLoadingRecentLocations,
        isLoadingAirports = isLoadingAirports,
        onPickupRecentLocationSelected = onPickupRecentLocationSelected,
        onDestinationRecentLocationSelected = onDestinationRecentLocationSelected,
        showInvalidLocationDialog = showInvalidLocationDialog,
        invalidLocationMessage = invalidLocationMessage,
        onDismissError = { showInvalidLocationDialog = false },
        onHoursSelected = { selectedHours = it },
        onPickupMapClick = events.onPickupMapClick,
        onDestinationMapClick = events.onDestinationMapClick
    )
}

// ==========================================
// UI Content Composable
// ==========================================

@Composable
fun ScheduleRideScreenContent(
    state: ScheduleRideState,
    events: ScheduleRideEvents,
    pickupText: String,
    destinationText: String,
    pickupSearchValue: String,
    destinationSearchValue: String,
    bothLocationsFilled: Boolean,
    hasValidCoordinates: Boolean,
    pickupRecentLocations: List<RecentLocation>,
    dropoffRecentLocations: List<RecentLocation>,
    isLoadingRecentLocations: Boolean,
    isLoadingAirports: Boolean,
    onPickupRecentLocationSelected: (RecentLocation) -> Unit,
    onDestinationRecentLocationSelected: (RecentLocation) -> Unit,
    showInvalidLocationDialog: Boolean,
    invalidLocationMessage: String,
    onDismissError: () -> Unit,
    onHoursSelected: (String) -> Unit,
    onPickupMapClick: () -> Unit,
    onDestinationMapClick: () -> Unit
) {
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
            item { ScheduleRideHeader(events.onDismiss) }

            item {
                RideTypeSelection(
                    selectedRideType = state.selectedRideType,
                    onRideTypeSelected = events.onRideTypeSelected
                )
            }

            if (state.selectedRideType == RideType.HOURLY) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    HoursDropdown(
                        selectedHours = state.selectedHours,
                        onHoursSelected = onHoursSelected,
                        modifier = Modifier.padding(horizontal = 0.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                BookingTypeSelection(
                    selectedBookingType = state.selectedBookingType,
                    selectedDestinationType = state.selectedDestinationType,
                    onBookingTypeSelected = events.onBookingTypeSelected,
                    onDestinationTypeSelected = events.onDestinationTypeSelected
                )
            }

            item {
                LocationInputCard(
                    pickupValue = pickupSearchValue,
                    destinationValue = destinationSearchValue,
                    onPickupValueChange = events.onPickupValueChange,
                    onDestinationValueChange = events.onDestinationValueChange,
                    onPickupFocusChanged = events.onPickupFocusChanged,
                    onDestinationFocusChanged = events.onDestinationFocusChanged,
                    showPickupClear = pickupText.isNotEmpty(),
                    showDestinationClear = destinationText.isNotEmpty(),
                    onPickupClear = events.onPickupClear,
                    onDestinationClear = events.onDestinationClear,
                    onPickupMapClick = onPickupMapClick,
                    onDestinationMapClick = onDestinationMapClick
                )
                Spacer(modifier = Modifier.width(16.dp))
            }

            if (state.focusedField == "pickup" || state.focusedField == "destination") {
                item {
                    AnimatedVisibility(
                        visible = state.focusedField == "pickup" || state.focusedField == "destination",
                        enter = fadeIn(animationSpec = tween(durationMillis = 300)) + expandVertically(animationSpec = tween(durationMillis = 300)),
                        exit = fadeOut(animationSpec = tween(durationMillis = 300)) + shrinkVertically(animationSpec = tween(durationMillis = 300))
                    ) {
                        MapSelectionRow(
                            onPickupMapClick = onPickupMapClick,
                            onDestinationMapClick = onDestinationMapClick,
                            showPickupMapButton = state.focusedField == "pickup",
                            showDestinationMapButton = state.focusedField == "destination"
                        )
                    }
                }
            }

            if (showInvalidLocationDialog && invalidLocationMessage.isNotEmpty()) {
                item {
                    LocationErrorBanner(message = invalidLocationMessage, onDismiss = onDismissError)
                }
            }

            val shouldShowPickupRecent = state.focusedField == "pickup" &&
                    !state.showPickupSuggestions &&
                    (pickupText.isEmpty() || pickupText.length <= 2) &&
                    state.selectedBookingType != BookingType.AIRPORT

            if (shouldShowPickupRecent) {
                if (isLoadingRecentLocations) {
                    items(3) { UberShimmerItem() }
                } else if (pickupRecentLocations.isNotEmpty()) {
                    items(pickupRecentLocations) { location ->
                        UberRecentLocationItem(
                            location = location,
                            onClick = { onPickupRecentLocationSelected(location) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (state.showPickupSuggestions && state.selectedBookingType != BookingType.AIRPORT && state.pickupPredictions.isNotEmpty()) {
                itemsIndexed(state.pickupPredictions) { _, prediction ->
                    AddressSuggestionItem(
                        prediction = prediction,
                        onClick = { events.onPickupSuggestionSelected(prediction) }
                    )
                }
            }

            if (state.showPickupSuggestions && state.selectedBookingType == BookingType.AIRPORT) {
                if (isLoadingAirports) {
                    items(3) { UberShimmerItem() }
                } else if (state.airportSuggestions.isNotEmpty()) {
                    itemsIndexed(state.airportSuggestions) { _, airportName ->
                        AirportSuggestionItem(
                            airportName = airportName,
                            onClick = { events.onPickupAirportSelected(airportName) }
                        )
                    }
                }
            }

            val shouldShowDropoffRecent = state.focusedField == "destination" &&
                    !state.showDestinationSuggestions &&
                    (destinationText.isEmpty() || destinationText.length <= 2) &&
                    state.selectedDestinationType != BookingType.AIRPORT

            if (shouldShowDropoffRecent) {
                if (isLoadingRecentLocations) {
                    items(3) { UberShimmerItem() }
                } else if (dropoffRecentLocations.isNotEmpty()) {
                    items(dropoffRecentLocations) { location ->
                        UberRecentLocationItem(
                            location = location,
                            onClick = { onDestinationRecentLocationSelected(location) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            if (state.showDestinationSuggestions && state.selectedDestinationType != BookingType.AIRPORT && state.destinationPredictions.isNotEmpty()) {
                itemsIndexed(state.destinationPredictions) { _, prediction ->
                    AddressSuggestionItem(
                        prediction = prediction,
                        onClick = { events.onDestinationSuggestionSelected(prediction) }
                    )
                }
            }

            if (state.showDestinationSuggestions && state.selectedDestinationType == BookingType.AIRPORT) {
                if (isLoadingAirports) {
                    items(3) { UberShimmerItem() }
                } else if (state.airportSuggestions.isNotEmpty()) {
                    itemsIndexed(state.airportSuggestions) { _, airportName ->
                        AirportSuggestionItem(
                            airportName = airportName,
                            onClick = { events.onDestinationAirportSelected(airportName) }
                        )
                    }
                }
            }

            if (bothLocationsFilled && hasValidCoordinates && !state.showPickupSuggestions && !state.showDestinationSuggestions) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 40.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = events.onNext,
                            colors = ButtonDefaults.buttonColors(containerColor = LimoOrange, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Next", style = AppTextStyles.buttonLarge)
                                Icon(
                                    painter = painterResource(id = R.drawable.right_arrow),
                                    contentDescription = "Arrow",
                                    tint = Color.White,
                                    modifier = Modifier.size(AppDimensions.iconSize)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// UI Components
// ==========================================

@Composable
fun MapSelectionRow(
    onPickupMapClick: () -> Unit,
    onDestinationMapClick: () -> Unit,
    showPickupMapButton: Boolean,
    showDestinationMapButton: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 0.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showPickupMapButton) {
            OutlinedButton(
                onClick = onPickupMapClick,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, LimoOrange),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LimoOrange)
            ) {
                Icon(painterResource(id = R.drawable.ic_location_pin), contentDescription = "Select Pickup on Map", modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Select Pickup on Map")
            }
        }

        if (showDestinationMapButton) {
            OutlinedButton(
                onClick = onDestinationMapClick,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, LimoOrange),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LimoOrange)
            ) {
                Icon(painterResource(id = R.drawable.ic_location_pin), contentDescription = "Select Destination on Map", modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Select Destination on Map")
            }
        }
    }
}

@Composable
fun UberRecentLocationItem(
    location: RecentLocation,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 24.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFFEEEEEE), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Recent",
                tint = Color.Black,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(verticalArrangement = Arrangement.Center) {
            val title = if (location.isAirport) {
                location.airportName ?: location.address
            } else {
                location.address.substringBefore(",")
            }

            Text(
                text = title,
                style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = location.address,
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun UberShimmerItem() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .lighterShimmer()
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .lighterShimmer()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .lighterShimmer()
            )
        }
    }
}

@Composable
fun Modifier.lighterShimmer(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val brush = Brush.linearGradient(
        colors = listOf(Color(0xFFF5F5F5), Color(0xFFFFFFFF), Color(0xFFF5F5F5)),
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )

    return this.background(brush)
}

// ==========================================
// Business Logic Helpers (Private)
// ==========================================

private val countrySynonyms = mapOf(
    "UNITED STATES OF AMERICA" to "US", "USA" to "US", "US" to "US",
    "U.S" to "US", "U.S.A" to "US", "U.S.A." to "US", "UNITED STATES" to "US",
    "AMERICA" to "US", "UNITED KINGDOM" to "GB", "UK" to "GB", "U.K" to "GB",
    "U.K." to "GB", "GREAT BRITAIN" to "GB", "BRITAIN" to "GB", "ENGLAND" to "GB",
    "SCOTLAND" to "GB", "WALES" to "GB", "NORTHERN IRELAND" to "GB",
    "UNITED ARAB EMIRATES" to "AE", "UAE" to "AE", "U.A.E" to "AE",
    "U.A.E." to "AE", "EMIRATES" to "AE", "CANADA" to "CA", "CA" to "CA",
    "C.A" to "CA", "C.A." to "CA", "MEXICO" to "MX", "AUSTRALIA" to "AU",
    "NEW ZEALAND" to "NZ", "GERMANY" to "DE", "FRANCE" to "FR", "SPAIN" to "ES",
    "ITALY" to "IT", "INDIA" to "IN", "CHINA" to "CN", "JAPAN" to "JP", "SINGAPORE" to "SG"
)

private fun normalizeCountry(country: String?): String? {
    if (country.isNullOrBlank()) return null
    var normalized = country.trim().replace(".", "").replace(",", "").replace(";", "").replace("  ", " ").uppercase()
    countrySynonyms[normalized]?.let { return it }
    val locale = Locale.getDefault()
    for (code in Locale.getISOCountries()) {
        val countryName = locale.getDisplayCountry(Locale("", code))
        if (countryName.uppercase() == normalized || code.uppercase() == normalized) return code.uppercase()
    }
    if (normalized.length <= 3) return null
    return null
}

private suspend fun extractCountryFromAddress(address: String, placesService: PlacesService): String? {
    val details = placesService.getPlaceDetails(address)
    return details?.country
}

private fun areLocationsSame(
    pickupText: String,
    destinationText: String,
    pickupCoordinate: LocationCoordinate?,
    destinationCoordinate: LocationCoordinate?
): Boolean {
    val normalizedPickup = pickupText.trim().replace("\\s+".toRegex(), " ").replace("[,;]".toRegex(), " ").uppercase()
    val normalizedDestination = destinationText.trim().replace("\\s+".toRegex(), " ").replace("[,;]".toRegex(), " ").uppercase()

    if (normalizedPickup.isNotEmpty() && normalizedDestination.isNotEmpty() && normalizedPickup == normalizedDestination) return true

    if (pickupCoordinate != null && destinationCoordinate != null) {
        val latDiff = abs(pickupCoordinate.latitude - destinationCoordinate.latitude)
        val longDiff = abs(pickupCoordinate.longitude - destinationCoordinate.longitude)
        if (latDiff < 0.002 && longDiff < 0.002) return true
    }
    return false
}

private suspend fun areCountriesDifferent(
    pickupLocation: String, destinationLocation: String,
    selectedPickupAirport: String, selectedDestinationAirport: String,
    selectedBookingType: BookingType, selectedDestinationType: BookingType,
    placesService: PlacesService
): Boolean {
    val pickupAddress = if (selectedBookingType == BookingType.AIRPORT) selectedPickupAirport else pickupLocation
    val destinationAddress = if (selectedDestinationType == BookingType.AIRPORT) selectedDestinationAirport else destinationLocation
    val pickupCountry = extractCountryFromAddress(pickupAddress, placesService)
    val destinationCountry = extractCountryFromAddress(destinationAddress, placesService)
    val normalizedPickup = normalizeCountry(pickupCountry)
    val normalizedDestination = normalizeCountry(destinationCountry)

    if (normalizedPickup != null && normalizedDestination != null) return normalizedPickup != normalizedDestination
    return false
}

suspend fun getLocationValidationError(
    pickupLocation: String, destinationLocation: String,
    selectedPickupAirport: String, selectedDestinationAirport: String,
    pickupCoordinate: LocationCoordinate?, destinationCoordinate: LocationCoordinate?,
    selectedBookingType: BookingType, selectedDestinationType: BookingType,
    placesService: PlacesService
): String? {
    val pickupText = if (selectedBookingType == BookingType.AIRPORT) selectedPickupAirport else pickupLocation
    val destinationText = if (selectedDestinationType == BookingType.AIRPORT) selectedDestinationAirport else destinationLocation

    if (areLocationsSame(pickupText, destinationText, pickupCoordinate, destinationCoordinate)) {
        return "Pickup and destination locations cannot be the same. Please select different locations."
    }
    if (areCountriesDifferent(
            pickupLocation, destinationLocation, selectedPickupAirport, selectedDestinationAirport,
            selectedBookingType, selectedDestinationType, placesService
        )
    ) {
        return "Pickup and destination must be in the same country. Please select valid locations."
    }
    return null
}

private fun createAndNavigateToTimeSelection(
    rideType: RideType, bookingType: BookingType, destinationType: BookingType,
    pickupLocation: String, destinationLocation: String,
    selectedPickupAirport: String, selectedDestinationAirport: String,
    pickupCoordinate: LocationCoordinate?, destinationCoordinate: LocationCoordinate?,
    selectedHours: String, onNavigate: (RideData) -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val now = Date()
    val hoursString = selectedHours.replace(" hours minimum", "").replace(" hours", "").trim()

    val rideData = RideData(
        serviceType = rideType.toServiceType(),
        bookingHour = hoursString,
        pickupType = bookingType.toPickupType(),
        dropoffType = destinationType.toPickupType(),
        pickupDate = dateFormat.format(now),
        pickupTime = timeFormat.format(now),
        pickupLocation = pickupLocation.ifEmpty { selectedPickupAirport },
        destinationLocation = destinationLocation.ifEmpty { selectedDestinationAirport },
        selectedPickupAirport = selectedPickupAirport,
        selectedDestinationAirport = selectedDestinationAirport,
        noOfPassenger = 1,
        noOfLuggage = 1,
        noOfVehicles = 1,
        pickupLat = pickupCoordinate?.latitude,
        pickupLong = pickupCoordinate?.longitude,
        pickupCountryCode = pickupCoordinate?.countryCode,
        pickupPostalCode = pickupCoordinate?.postalCode,
        destinationLat = destinationCoordinate?.latitude,
        destinationLong = destinationCoordinate?.longitude,
        destinationCountryCode = destinationCoordinate?.countryCode,
        destinationPostalCode = destinationCoordinate?.postalCode
    )
    onNavigate(rideData)
}