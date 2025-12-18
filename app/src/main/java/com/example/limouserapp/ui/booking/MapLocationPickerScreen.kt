package com.example.limouserapp.ui.booking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.limouserapp.data.PlacePrediction
import com.example.limouserapp.data.PlacesService
import com.example.limouserapp.data.model.booking.LocationCoordinate
import com.example.limouserapp.ui.booking.components.AddressSuggestionItem
import com.example.limouserapp.ui.theme.AppTextStyles
import com.example.limouserapp.ui.theme.GoogleSansFamily
import com.example.limouserapp.ui.theme.LimoOrange
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Locale
import com.example.limouserapp.ui.booking.MapPickerState
import com.example.limouserapp.ui.booking.MapPickerEvents

// Constants
private const val DEFAULT_ZOOM = 15f
private val DEFAULT_LOCATION = LatLng(30.7333, 76.7794)

@Composable
fun MapLocationPickerScreen(
    navController: NavHostController,
    initialLat: Double? = null,
    initialLong: Double? = null,
    initialAddress: String? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val placesService = remember { PlacesService(context) }
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }

    val initialMapLocation = remember(initialLat, initialLong) {
        if (initialLat != null && initialLong != null) LatLng(initialLat, initialLong)
        else DEFAULT_LOCATION
    }

    // State management
    val _currentCameraPosition = remember { MutableStateFlow(initialMapLocation) }
    val _selectedLocation = remember { MutableStateFlow(initialMapLocation) }
    val _selectedAddress = remember { MutableStateFlow(initialAddress) }
    val _searchInput = remember { MutableStateFlow(initialAddress ?: "") }
    val _searchPredictions = remember { MutableStateFlow<List<PlacePrediction>>(emptyList()) }
    val _isLoadingSearch = remember { MutableStateFlow(false) }
    val _userLocation = remember { MutableStateFlow<LatLng?>(null) }
    val _focusedField = remember { mutableStateOf<String?>(null) }

    val currentCameraPosition by _currentCameraPosition.collectAsState()
    val selectedLocation by _selectedLocation.collectAsState()
    val selectedAddress by _selectedAddress.collectAsState()
    val searchInput by _searchInput.collectAsState()
    val searchPredictions by _searchPredictions.collectAsState()
    val isLoadingSearch by _isLoadingSearch.collectAsState()
    val userLocation by _userLocation.collectAsState()
    val focusedField by _focusedField

    val showUserLocationButton by remember {
        derivedStateOf { userLocation != null && currentCameraPosition != userLocation }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialMapLocation, DEFAULT_ZOOM)
    }

    // Permission Handling
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            coroutineScope.launch {
                getCurrentLocation(context)?.let { _userLocation.value = it }
            }
        }
    }

    // Lifecycle Observation
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    coroutineScope.launch { _userLocation.value = getCurrentLocation(context) }
                } else {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Reverse Geocoding Logic
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving && focusedField == null) {
            val center = cameraPositionState.position.target
            if (center != _selectedLocation.value) {
                _selectedLocation.value = center
                reverseGeocodeAndSetAddress(center, geocoder, _selectedAddress)
                _searchInput.value = _selectedAddress.value ?: ""
            }
        }
    }

    val events = remember {
        MapPickerEvents(
            onBack = { navController.popBackStack() },
            onCameraMove = { _currentCameraPosition.value = it },
            onConfirmLocation = { coord, addr ->
                navController.previousBackStackEntry?.savedStateHandle?.set("selected_location", coord)
                navController.previousBackStackEntry?.savedStateHandle?.set("selected_address", addr)
                navController.popBackStack()
            },
            onSearchInputChanged = { text ->
                _searchInput.value = text
                coroutineScope.launch {
                    _isLoadingSearch.value = true
                    _searchPredictions.value = placesService.getPlacePredictions(text)
                    _isLoadingSearch.value = false
                }
            },
            onSearchClear = {
                _searchInput.value = ""
                _searchPredictions.value = emptyList()
                _focusedField.value = null
            },
            onSearchSuggestionSelected = { pred ->
                coroutineScope.launch {
                    placesService.getPlaceDetails(pred.placeId)?.let { details ->
                        val pos = LatLng(details.latitude ?: 0.0, details.longitude ?: 0.0)
                        _currentCameraPosition.value = pos
                        _selectedLocation.value = pos
                        val fullAddress = details.address ?: pred.primaryText
                        _selectedAddress.value = fullAddress
                        _searchInput.value = fullAddress
                        _searchPredictions.value = emptyList()
                        _focusedField.value = null
                    }
                }
            },
            onRecenterMap = {
                coroutineScope.launch {
                    userLocation?.let {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, DEFAULT_ZOOM))
                    }
                }
            },
            onSearchFocusChanged = { _focusedField.value = if (it) "search" else null }
        )
    }

    MapLocationPickerScreenContent(
        state = MapPickerState(
            currentCameraPosition, selectedLocation, selectedAddress,
            searchInput.isNotEmpty(), searchInput, searchPredictions,
            isLoadingSearch, userLocation, showUserLocationButton, focusedField
        ),
        events = events,
        cameraPositionState = cameraPositionState
    )
}

@Composable
private fun MapLocationPickerScreenContent(
    state: MapPickerState,
    events: MapPickerEvents,
    cameraPositionState: CameraPositionState
) {
    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = state.userLocation != null),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
            onMapClick = { events.onSearchClear() }
        ) {
            state.selectedLocation?.let {
                Marker(
                    state = MarkerState(position = it),
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)
                )
            }
        }

        // Search Overlay with Safe Area Padding
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    onClick = events.onBack,
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(50.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }

                OutlinedTextField(
                    value = state.searchInput,
                    onValueChange = events.onSearchInputChanged,
                    placeholder = { Text("Search location...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = LimoOrange) },
                    trailingIcon = {
                        if (state.searchInput.isNotEmpty()) {
                            IconButton(onClick = events.onSearchClear) {
                                Icon(Icons.Default.Close, null)
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .onFocusChanged { events.onSearchFocusChanged(it.isFocused) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp, fontFamily = GoogleSansFamily)
                )
            }

            AnimatedVisibility(
                visible = state.searchPredictions.isNotEmpty() && state.focusedField == "search",
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier.padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(state.searchPredictions) { prediction ->
                            AddressSuggestionItem(
                                prediction = prediction,
                                onClick = { events.onSearchSuggestionSelected(prediction) }
                            )
                        }
                    }
                }
            }
        }

        // Bottom Confirmation Panel
        AnimatedVisibility(
            visible = state.selectedAddress != null && state.focusedField == null,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color.White,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding() // Ensures confirm button is above gesture bar
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = state.selectedAddress ?: "",
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Button(
                        onClick = {
                            state.selectedLocation?.let { latLng ->
                                state.selectedAddress?.let { addr ->
                                    events.onConfirmLocation(
                                        LocationCoordinate(
                                            latLng.latitude,
                                            latLng.longitude,
                                            null,
                                            null
                                        ),
                                        addr
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LimoOrange),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirm Location", style = AppTextStyles.buttonLarge)
                    }
                }
            }
        }

        // Recenter Button
        if (state.showUserLocationButton) {
            FloatingActionButton(
                onClick = events.onRecenterMap,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 180.dp, end = 16.dp), // Adjusted padding to stay above bottom sheet
                containerColor = Color.White,
                contentColor = LimoOrange
            ) {
                Icon(Icons.Default.LocationSearching, null)
            }
        }
    }
}

// Helpers
private suspend fun getCurrentLocation(context: Context): LatLng? {
    val client = LocationServices.getFusedLocationProviderClient(context)
    return try {
        val location = client.lastLocation.await()
        location?.let { LatLng(it.latitude, it.longitude) }
    } catch (e: Exception) { null }
}

private suspend fun reverseGeocodeAndSetAddress(
    latLng: LatLng,
    geocoder: Geocoder,
    flow: MutableStateFlow<String?>
) {
    try {
        val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
        flow.value = addresses?.firstOrNull()?.getAddressLine(0) ?: "Unknown Location"
    } catch (e: Exception) { flow.value = "Error fetching address" }
}

// Extension function to convert LocationCoordinate to LatLng
fun LocationCoordinate.toLatLng(): LatLng {
    return LatLng(this.latitude, this.longitude)
}

// Extension function to convert LatLng to LocationCoordinate
fun LatLng.toLocationCoordinate(): LocationCoordinate {
    return LocationCoordinate(this.latitude, this.longitude, null, null)
}

@Preview(showBackground = true)
@Composable
fun MapLocationPickerScreenPreview() {
    MaterialTheme {
        MapLocationPickerScreen(
            navController = rememberNavController(),
            initialLat = 34.052235,
            initialLong = -118.243683,
            initialAddress = "Los Angeles, CA, USA"
        )
    }
}
