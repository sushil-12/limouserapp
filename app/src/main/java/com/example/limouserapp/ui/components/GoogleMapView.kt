package com.example.limouserapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.limouserapp.R
import com.example.limouserapp.data.model.dashboard.CarLocation
import com.example.limouserapp.data.model.dashboard.LocationData
import com.example.limouserapp.data.model.dashboard.MapRegion
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

/**
 * Google Maps composable with location and car markers
 */
@Composable
fun GoogleMapView(
    mapRegion: MapRegion,
    userLocation: LocationData?,
    carLocations: List<CarLocation>,
    onLocationUpdate: (LocationData) -> Unit,
    onMapRegionUpdate: (MapRegion) -> Unit,
    hasLocationPermission: Boolean = false,
    onRequestLocationPermission: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    // Load map style
    val mapStyle = remember {
        try {
            MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_themed)
        } catch (e: Exception) {
            null
        }
    }
    
    // Map state
    var mapProperties by remember(hasLocationPermission) {
        mutableStateOf(
            MapProperties(
                isMyLocationEnabled = hasLocationPermission,
                mapType = MapType.NORMAL,
                mapStyleOptions = mapStyle
            )
        )
    }
    
    var mapUiSettings by remember {
        mutableStateOf(
            MapUiSettings(
                zoomControlsEnabled = false,
                compassEnabled = false,
                myLocationButtonEnabled = false,
                zoomGesturesEnabled = true,
                scrollGesturesEnabled = true,
                rotationGesturesEnabled = true,
                tiltGesturesEnabled = true
            )
        )
    }
    
    // Camera position
    val cameraPosition = remember(mapRegion) {
        CameraPosition.fromLatLngZoom(
            LatLng(mapRegion.centerLatitude, mapRegion.centerLongitude),
            getZoomLevel(mapRegion.latitudeDelta)
        )
    }
    
    // Handle lifecycle events
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    // Map started
                }
                Lifecycle.Event.ON_STOP -> {
                    // Map stopped
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = CameraPositionState(
                position = cameraPosition
            ),
            properties = mapProperties,
            uiSettings = mapUiSettings,
            onMapLoaded = {
                // Map loaded successfully
            }
        ) {
            // User location marker
            userLocation?.let { location ->
                Marker(
                    state = MarkerState(
                        position = LatLng(location.latitude, location.longitude)
                    ),
                    title = "Your Location",
                    snippet = location.address
                )
            }
            
            // Car location markers
            carLocations.forEach { carLocation ->
                Marker(
                    state = MarkerState(
                        position = LatLng(carLocation.latitude, carLocation.longitude)
                    ),
                    title = carLocation.driverName ?: "Available Car",
                    snippet = if (carLocation.isAvailable) "Available" else "Busy"
                )
            }
        }
        
        // Automatically request location permission if not granted (only once on first composition)
        LaunchedEffect(Unit) {
            if (!hasLocationPermission) {
                onRequestLocationPermission()
            }
        }
    }
}

/**
 * Get zoom level from latitude delta
 */
private fun getZoomLevel(latitudeDelta: Double): Float {
    return when {
        latitudeDelta >= 360 -> 1f
        latitudeDelta >= 180 -> 2f
        latitudeDelta >= 90 -> 3f
        latitudeDelta >= 45 -> 4f
        latitudeDelta >= 22.5 -> 5f
        latitudeDelta >= 11.25 -> 6f
        latitudeDelta >= 5.625 -> 7f
        latitudeDelta >= 2.813 -> 8f
        latitudeDelta >= 1.406 -> 9f
        latitudeDelta >= 0.703 -> 10f
        latitudeDelta >= 0.352 -> 11f
        latitudeDelta >= 0.176 -> 12f
        latitudeDelta >= 0.088 -> 13f
        latitudeDelta >= 0.044 -> 14f
        latitudeDelta >= 0.022 -> 15f
        latitudeDelta >= 0.011 -> 16f
        latitudeDelta >= 0.0055 -> 17f
        latitudeDelta >= 0.00275 -> 18f
        latitudeDelta >= 0.001375 -> 19f
        latitudeDelta >= 0.0006875 -> 20f
        else -> 21f
    }
}
