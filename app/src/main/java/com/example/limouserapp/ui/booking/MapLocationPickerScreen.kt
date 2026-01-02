package com.example.limouserapp.ui.booking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.limouserapp.data.PlacePrediction
import com.example.limouserapp.data.PlacesService
import com.example.limouserapp.data.model.booking.LocationCoordinate
import com.example.limouserapp.ui.booking.components.AddressSuggestionItem
import com.example.limouserapp.ui.theme.GoogleSansFamily
import com.example.limouserapp.ui.theme.LimoOrange
import com.example.limouserapp.ui.theme.LimoBlack
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

// --- Constants ---
private const val DEFAULT_ZOOM = 16.5f // Slightly closer for city feel
private const val DEBOUNCE_TIME_MS = 600L
private val DEFAULT_LOCATION = LatLng(30.7333, 76.7794)

// --- State ---
data class MapPickerUiState(
    val selectedLocation: LatLng,
    val selectedAddress: String?,
    val isAddressLoading: Boolean = false,
    val searchInput: String = "",
    val searchPredictions: List<PlacePrediction> = emptyList(),
    val isSearchLoading: Boolean = false,
    val isSearchFocused: Boolean = false,
    val userLocation: LatLng? = null,
    val isFirstLoad: Boolean = true
)

@Composable
fun MapLocationPickerScreen(
    navController: NavHostController,
    initialLat: Double? = null,
    initialLong: Double? = null,
    initialAddress: String? = null
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    
    val placesService = remember { PlacesService(context) }
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }

    // 1. Determine Start Position
    val hasInitialParams = initialLat != null && initialLong != null
    val startPos = if (hasInitialParams) LatLng(initialLat!!, initialLong!!) else DEFAULT_LOCATION

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(startPos, DEFAULT_ZOOM)
    }

    var uiState by remember {
        mutableStateOf(
            MapPickerUiState(
                selectedLocation = startPos,
                selectedAddress = initialAddress
            )
        )
    }

    var geocodingJob by remember { mutableStateOf<Job?>(null) }

    // --- FIX: Logic to get User Location on First Load ---
    // Only runs if no specific coordinates were passed arguments
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted && !hasInitialParams) {
            coroutineScope.launch {
                getCurrentLocation(context)?.let { loc ->
                    uiState = uiState.copy(userLocation = loc)
                    // ANIMATE TO CURRENT LOCATION
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(loc, DEFAULT_ZOOM))
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation(context)?.let { loc ->
                uiState = uiState.copy(userLocation = loc)
                if (!hasInitialParams) {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(loc, DEFAULT_ZOOM))
                }
            }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // --- Logic: Reverse Geocoding on Map Idle ---
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            val center = cameraPositionState.position.target
            
            // Optimization: Skip if distance is negligible (< 2 meters)
            if (distanceBetween(center, uiState.selectedLocation) < 2.0 && uiState.selectedAddress != null) {
                 return@LaunchedEffect
            }

            // Haptic "Click" when settled
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }

            uiState = uiState.copy(
                selectedLocation = center,
                isAddressLoading = true,
                selectedAddress = "Locating..."
            )

            geocodingJob?.cancel()
            geocodingJob = launch {
                delay(DEBOUNCE_TIME_MS) 
                val address = getAddressFromLocation(context, geocoder, center)
                uiState = uiState.copy(selectedAddress = address, isAddressLoading = false)
            }
        } else {
            geocodingJob?.cancel()
            if (!uiState.isAddressLoading) {
                 uiState = uiState.copy(isAddressLoading = true, selectedAddress = "Locating...")
            }
        }
    }

    // --- UI Layout ---
    Box(modifier = Modifier.fillMaxSize()) {
        
        // 1. Map Layer
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = uiState.userLocation != null,
                isBuildingEnabled = true,
                minZoomPreference = 5f,
                maxZoomPreference = 20f
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = false,
                rotationGesturesEnabled = false,
                tiltGesturesEnabled = false // Keep it flat for easier pickup selection
            )
        )

        // 2. Animated Center Pin
        // Scales up slightly when moving to look like it's "lifted"
        val pinScale by animateFloatAsState(if (cameraPositionState.isMoving) 1.1f else 1f, label = "pinScale")
        val shadowScale by animateFloatAsState(if (cameraPositionState.isMoving) 0.8f else 1f, label = "shadowScale")

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 35.dp), // Offset to put pin tip exactly at center
            contentAlignment = Alignment.BottomCenter
        ) {
            // Shadow on ground
            Box(
                modifier = Modifier
                    .width(12.dp)
                    .height(6.dp)
                    .scale(shadowScale)
                    .background(Color.Black.copy(alpha = 0.2f), CircleShape)
            )
            // Pin Icon
            Icon(
                imageVector = Icons.Default.Place, // Better filled icon
                contentDescription = null,
                tint = LimoBlack,
                modifier = Modifier
                    .size(48.dp)
                    .scale(pinScale)
                    .offset(y = (-4).dp)
            )
        }

        // 3. Floating Search Bar (Uber Style)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            FloatingSearchBar(
                query = uiState.searchInput,
                onQueryChange = { q -> 
                    uiState = uiState.copy(searchInput = q)
                    coroutineScope.launch {
                        uiState = uiState.copy(isSearchLoading = true)
                        val preds = placesService.getPlacePredictions(q)
                        uiState = uiState.copy(searchPredictions = preds, isSearchLoading = false)
                    }
                },
                onBack = { navController.popBackStack() },
                onClear = { 
                    uiState = uiState.copy(searchInput = "", isSearchFocused = false, searchPredictions = emptyList()) 
                },
                onFocus = { uiState = uiState.copy(isSearchFocused = true) }
            )

            // Search Results
            AnimatedVisibility(
                visible = uiState.isSearchFocused && uiState.searchInput.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(6.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                        items(uiState.searchPredictions) { prediction ->
                            AddressSuggestionItem(
                                prediction = prediction,
                                onClick = {
                                    coroutineScope.launch {
                                        uiState = uiState.copy(isSearchFocused = false, searchInput = "")
                                        placesService.getPlaceDetails(prediction.placeId)?.let { details ->
                                            val latLng = LatLng(details.latitude ?: 0.0, details.longitude ?: 0.0)
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM),
                                                1000 // smooth 1s flight
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // 4. Compact Bottom Sheet
        LocationConfirmSheet(
            modifier = Modifier.align(Alignment.BottomCenter),
            address = uiState.selectedAddress ?: "",
            isLoading = uiState.isAddressLoading,
            onRecenter = {
                uiState.userLocation?.let {
                    coroutineScope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, DEFAULT_ZOOM))
                    }
                }
            },
            showRecenter = uiState.userLocation != null,
            onConfirm = {
                // Return Result Logic
                val finalCoord = LocationCoordinate(
                    uiState.selectedLocation.latitude,
                    uiState.selectedLocation.longitude,
                    null, null
                )
                navController.previousBackStackEntry?.savedStateHandle?.apply {
                    set("map_picker_result_key", navController.previousBackStackEntry?.savedStateHandle?.get<String>("map_picker_result_key"))
                    set("selected_location", finalCoord)
                    set("selected_address", uiState.selectedAddress)
                }
                navController.popBackStack()
            }
        )
    }
}

// --- Component: Uber-like Floating Search Bar ---
@Composable
fun FloatingSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onFocus: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(6.dp, CircleShape), // High shadow for "floating" effect
        shape = CircleShape, // Fully rounded
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = LimoBlack)
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search for a building, street...",
                        style = TextStyle(fontSize = 15.sp, color = Color.Gray, fontFamily = GoogleSansFamily)
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = TextStyle(
                        fontSize = 16.sp, 
                        color = LimoBlack, 
                        fontFamily = GoogleSansFamily, 
                        fontWeight = FontWeight.Normal
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(LimoOrange),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) onFocus() }
                )
            }

            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                }
            } else {
                 Icon(
                     Icons.Default.Search, 
                     contentDescription = null, 
                     tint = LimoOrange,
                     modifier = Modifier.padding(end = 16.dp).size(24.dp)
                 )
            }
        }
    }
}

// --- Component: Sleek Bottom Sheet ---
@Composable
fun LocationConfirmSheet(
    modifier: Modifier = Modifier,
    address: String,
    isLoading: Boolean,
    showRecenter: Boolean,
    onRecenter: () -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalAlignment = Alignment.End // For the FAB
    ) {
        
        // Recenter FAB (Floating above sheet)
        if (showRecenter) {
            FloatingActionButton(
                onClick = onRecenter,
                modifier = Modifier.padding(end = 16.dp, bottom = 16.dp).size(48.dp),
                containerColor = Color.White,
                contentColor = LimoBlack,
                elevation = FloatingActionButtonDefaults.elevation(6.dp)
            ) {
                Icon(Icons.Default.LocationSearching, contentDescription = "Recenter", modifier = Modifier.size(24.dp))
            }
        }

        // The Sheet
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = Color.White,
            shadowElevation = 24.dp // Deep shadow
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Drag Handle
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.LightGray.copy(alpha = 0.6f))
                )
                
                Spacer(modifier = Modifier.height(20.dp))

                // Address Area
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small Pin Icon
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3F3F3)),
                        contentAlignment = Alignment.Center
                    ) {
                         Icon(Icons.Default.Place, null, tint = LimoBlack, modifier = Modifier.size(20.dp))
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isLoading) "Locating..." else "Selected Location",
                            style = TextStyle(fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        
                        if (isLoading) {
                            // Simple text shimmer substitute or loading state
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(0.5f).height(4.dp).padding(top = 4.dp),
                                color = LimoBlack,
                                trackColor = Color.LightGray.copy(alpha = 0.3f)
                            )
                        } else {
                            Text(
                                text = address.ifEmpty { "Unknown location" },
                                style = TextStyle(
                                    fontSize = 18.sp, 
                                    fontWeight = FontWeight.SemiBold, 
                                    fontFamily = GoogleSansFamily,
                                    color = LimoBlack
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            // Optional secondary line for detail
                            Text(
                                text = "Adjust pin to exact spot",
                                style = TextStyle(fontSize = 12.sp, color = Color.Gray)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Confirm Button
                Button(
                    onClick = onConfirm,
                    enabled = !isLoading && address.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(10.dp), // Slightly square for "pro" look
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LimoBlack, // High contrast
                        disabledContainerColor = Color.Gray
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp) // Flat is modern
                ) {
                    Text(
                        "Confirm Pickup",
                        style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    )
                }
            }
        }
    }
}

// --- Helpers ---
suspend fun getCurrentLocation(context: Context): LatLng? {
    // High accuracy request
    val client = LocationServices.getFusedLocationProviderClient(context)
    return try {
        // Try getting the last known location first (fast)
        var location = client.lastLocation.await()
        if (location == null) {
            // If null, request a fresh update (slower but accurate)
            val request = com.google.android.gms.location.CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build()
            location = client.getCurrentLocation(request, null).await()
        }
        location?.let { LatLng(it.latitude, it.longitude) }
    } catch (e: Exception) { 
        Log.e("MapPicker", "Error getting location", e)
        null 
    }
}

suspend fun getAddressFromLocation(context: Context, geocoder: Geocoder, latLng: LatLng): String = withContext(Dispatchers.IO) {
    try {
        val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
        if (!addresses.isNullOrEmpty()) {
            val address = addresses[0]
            // Uber-style: "Street Name, City" is usually enough
            val thoroughfare = address.thoroughfare ?: address.featureName
            val subLocality = address.subLocality ?: address.locality
            
            if (thoroughfare != null && subLocality != null) {
                "$thoroughfare, $subLocality"
            } else {
                address.getAddressLine(0) ?: "Unknown Location"
            }
        } else "Unknown Location"
    } catch (e: Exception) { "Error fetching address" }
}

fun distanceBetween(p1: LatLng, p2: LatLng): Float {
    val results = FloatArray(1)
    android.location.Location.distanceBetween(p1.latitude, p1.longitude, p2.latitude, p2.longitude, results)
    return results[0]
}