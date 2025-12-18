package com.example.limouserapp.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.limouserapp.R
import com.example.limouserapp.ui.liveride.LiveRideViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.google.android.gms.maps.GoogleMap as GmsMap
import kotlinx.coroutines.launch

// FIX: Define Converter as a top-level variable, NOT an extension on Companion
val LatLngVectorConverter = TwoWayConverter<LatLng, AnimationVector2D>(
    convertToVector = { AnimationVector2D(it.latitude.toFloat(), it.longitude.toFloat()) },
    convertFromVector = { LatLng(it.v1.toDouble(), it.v2.toDouble()) }
)

/**
 * Live Ride Map View Component
 * Displays real-time driver location with animated car marker, route polylines, and optimized camera controls
 * Matches the driver app's map implementation for consistent user experience
 */
@Composable
fun LiveRideMapView(
    viewModel: LiveRideViewModel,
    modifier: Modifier = Modifier,
    activeRoutePolyline: List<LatLng> = emptyList(),
    previewRoutePolyline: List<LatLng> = emptyList()
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()

    val uiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            compassEnabled = false,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false,
            rotationGesturesEnabled = true,
            tiltGesturesEnabled = true
        )
    }

    val cameraPositionState = rememberCameraPositionState()

    val mapProperties = remember(isDarkTheme) {
        MapProperties(
            isBuildingEnabled = false,
            isTrafficEnabled = false,
            mapStyleOptions = try {
                MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_minimal)
            } catch (e: Exception) { null }
        )
    }

    // Collect states
    val driverLocation by viewModel.driverLocation.collectAsState()
    val pickupLocation by viewModel.pickupLocation.collectAsState()
    val dropoffLocation by viewModel.dropoffLocation.collectAsState()
    val mapRegion by viewModel.mapRegion.collectAsState()
    val userHasInteracted by viewModel.userHasInteractedWithMap.collectAsState()

    // FIX: Use the top-level converter variable defined above
    val animatedDriverPosition = remember {
        Animatable(
            initialValue = driverLocation ?: LatLng(0.0, 0.0),
            typeConverter = LatLngVectorConverter
        )
    }
    val animatedDriverBearing = remember { Animatable(0f) }

    // Calculate bearing from driver location changes
    var previousDriverLocation by remember { mutableStateOf<LatLng?>(null) }

    LaunchedEffect(driverLocation) {
        driverLocation?.let { currentLocation ->
            previousDriverLocation?.let { previousLocation ->
                val bearing = calculateBearing(previousLocation, currentLocation)
                launch {
                    animatedDriverPosition.animateTo(
                        targetValue = currentLocation,
                        animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
                    )
                }
                launch {
                    val current = animatedDriverBearing.value
                    var diff = bearing - current
                    while (diff < -180) diff += 360
                    while (diff > 180) diff -= 360
                    animatedDriverBearing.animateTo(
                        targetValue = current + diff,
                        animationSpec = tween(durationMillis = 800, easing = LinearEasing)
                    )
                }
            } ?: run {
                // First location update
                animatedDriverPosition.snapTo(currentLocation)
                animatedDriverBearing.snapTo(0f)
            }
            previousDriverLocation = currentLocation
        }
    }

    var isUserInteracting by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(0L) }

    // Initial Fit
    LaunchedEffect(pickupLocation, dropoffLocation) {
        if (pickupLocation != null && dropoffLocation != null) {
            val bounds = LatLngBounds.Builder().include(pickupLocation!!).include(dropoffLocation!!).build()
            cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 150), 1000)
        }
    }

    // Auto-follow driver when not interacting
    LaunchedEffect(driverLocation, isUserInteracting) {
        if (driverLocation != null && !isUserInteracting) {
            if (System.currentTimeMillis() - lastInteractionTime > 3000) {
                val currentZoom = cameraPositionState.position.zoom.takeIf { it > 10f } ?: 16f
                val tilt = if (currentZoom > 15f) 45f else 0f

                val cameraUpdate = CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(driverLocation!!)
                        .zoom(currentZoom)
                        .bearing(0f)
                        .tilt(tilt)
                        .build()
                )
                cameraPositionState.animate(cameraUpdate, 1000)
            }
        }
    }

    // Update camera position when mapRegion changes and user hasn't interacted
    LaunchedEffect(mapRegion, userHasInteracted) {
        if (mapRegion != null && !userHasInteracted) {
            val position = CameraPosition.Builder()
                .target(mapRegion!!.center)
                .zoom(mapRegion!!.zoom)
                .bearing(0f)
                .tilt(0f)
                .build()

            cameraPositionState.animate(CameraUpdateFactory.newCameraPosition(position))
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = uiSettings,
        onMapLoaded = { }
    ) {
        MapEffect(Unit) { map ->
            // FIX: Use the GmsMap alias to refer to the class constants
            map.setOnCameraMoveStartedListener { reason ->
                if (reason == GmsMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                    isUserInteracting = true
                    lastInteractionTime = System.currentTimeMillis()
                }
            }
            map.setOnCameraIdleListener {
                isUserInteracting = false
            }
        }

        if (activeRoutePolyline.isNotEmpty()) {
            Polyline(
                points = activeRoutePolyline,
                color = Color.Black,
                width = 16f,
                zIndex = 1f,
                startCap = RoundCap(),
                endCap = RoundCap(),
                jointType = JointType.ROUND
            )
            Polyline(
                points = activeRoutePolyline,
                color = Color(0xFFFF9800),
                width = 10f,
                zIndex = 2f,
                startCap = RoundCap(),
                endCap = RoundCap(),
                jointType = JointType.ROUND
            )
        }

        if (previewRoutePolyline.isNotEmpty()) {
            Polyline(
                points = previewRoutePolyline,
                color = Color.Gray.copy(alpha = 0.6f),
                width = 10f,
                pattern = listOf(Dash(20f), Gap(10f)),
                jointType = JointType.ROUND
            )
        }

        pickupLocation?.let { location ->
            Marker(
                state = MarkerState(location),
                title = "Pickup",
                icon = bitmapDescriptorFromVector(context, R.drawable.ic_location_pin),
                anchor = androidx.compose.ui.geometry.Offset(0.5f, 1.0f),
                zIndex = 3f
            )
        }

        dropoffLocation?.let { location ->
            Marker(
                state = MarkerState(location),
                title = "Dropoff",
                icon = bitmapDescriptorFromVector(context, R.drawable.ic_location_pin),
                anchor = androidx.compose.ui.geometry.Offset(0.5f, 1.0f),
                zIndex = 3f
            )
        }

        if (driverLocation != null) {
            Marker(
                state = MarkerState(animatedDriverPosition.value),
                icon = bitmapDescriptorFromVector(context, R.drawable.ic_car),
                rotation = animatedDriverBearing.value,
                anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                zIndex = 5f,
                flat = true
            )
        }
    }
}

/**
 * Calculate bearing between two LatLng points
 */
private fun calculateBearing(from: LatLng, to: LatLng): Float {
    val lat1 = Math.toRadians(from.latitude)
    val lon1 = Math.toRadians(from.longitude)
    val lat2 = Math.toRadians(to.latitude)
    val lon2 = Math.toRadians(to.longitude)

    val dLon = lon2 - lon1
    val y = Math.sin(dLon) * Math.cos(lat2)
    val x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon)

    val bearing = Math.toDegrees(Math.atan2(y, x))
    return ((bearing + 360) % 360).toFloat()
}

fun bitmapDescriptorFromVector(context: Context, @DrawableRes vectorResId: Int, sizeDp: Int = 32): BitmapDescriptor? {
    return try {
        val drawable = ContextCompat.getDrawable(context, vectorResId) ?: return null

        val density = context.resources.displayMetrics.density
        val sizePx = (sizeDp * density).toInt()

        drawable.setBounds(0, 0, sizePx, sizePx)

        val bitmap = Bitmap.createBitmap(
            sizePx,
            sizePx,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        drawable.draw(canvas)

        BitmapDescriptorFactory.fromBitmap(bitmap)
    } catch (e: Exception) {
        e.printStackTrace()
        BitmapDescriptorFactory.defaultMarker()
    }
}
