package com.example.limouserapp.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import androidx.annotation.ColorInt
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.limouserapp.R
import com.example.limouserapp.ui.liveride.LiveRideViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import com.google.android.gms.maps.GoogleMap as GmsMap
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.max

// FIX: Define Converter as a top-level variable
val LatLngVectorConverter = TwoWayConverter<LatLng, AnimationVector2D>(
    convertToVector = { AnimationVector2D(it.latitude.toFloat(), it.longitude.toFloat()) },
    convertFromVector = { LatLng(it.v1.toDouble(), it.v2.toDouble()) }
)

/**
 * Live Ride Map View Component - Production Ready
 * * Features:
 * - Smart Camera: Zooms only on relevant points (e.g., Driver + Pickup) during approach.
 * - Visual Hierarchy: Driver on top > Pickup/Dropoff > Route Lines.
 * - Null-safety throughout.
 */
@Composable
fun LiveRideMapView(
    viewModel: LiveRideViewModel,
    modifier: Modifier = Modifier,
    activeRoutePolyline: List<LatLng> = emptyList(),
    previewRoutePolyline: List<LatLng> = emptyList(),
    rideStatus: String? = null // Critical: Passed to decide camera bounds logic
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
            } catch (e: Exception) {
                Timber.e(e, "Error loading map style")
                null
            }
        )
    }

    // Collect states
    val driverLocation by viewModel.driverLocation.collectAsState()
    val pickupLocation by viewModel.pickupLocation.collectAsState()
    val dropoffLocation by viewModel.dropoffLocation.collectAsState()

    // Animated driver position
    val animatedDriverPosition = remember {
        Animatable(
            initialValue = driverLocation ?: LatLng(0.0, 0.0),
            typeConverter = LatLngVectorConverter
        )
    }
    val animatedDriverBearing = remember { Animatable(0f) }
    var previousDriverLocation by remember { mutableStateOf<LatLng?>(null) }

    // Animate driver marker position
    LaunchedEffect(driverLocation) {
        driverLocation?.let { currentLocation ->
            if (currentLocation.latitude == 0.0 || currentLocation.longitude == 0.0) return@let

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
                animatedDriverPosition.snapTo(currentLocation)
                animatedDriverBearing.snapTo(0f)
            }
            previousDriverLocation = currentLocation
        }
    }

    // --- SMART CAMERA LOGIC ---
    var isUserInteracting by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(0L) }
    var hasInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(
        activeRoutePolyline,
        previewRoutePolyline,
        driverLocation,
        pickupLocation,
        dropoffLocation,
        rideStatus
    ) {
        // 1. If user is interacting, don't move camera
        if (isUserInteracting) return@LaunchedEffect

        // 2. Build bounds based on STATUS
        val boundsBuilder = LatLngBounds.Builder()
        var hasPoints = false

        // LOGIC FOR EN_ROUTE_PU (Driver approaching Pickup):
        // We ONLY care about Driver and Pickup. We ignore Dropoff here.
        if (rideStatus == "en_route_pu") {
            driverLocation?.let {
                if (isValidLatLng(it)) { boundsBuilder.include(it); hasPoints = true }
            }
            pickupLocation?.let {
                if (isValidLatLng(it)) { boundsBuilder.include(it); hasPoints = true }
            }
            // Include the active route (Driver -> Pickup path)
            activeRoutePolyline.forEach { boundsBuilder.include(it) }
        }
        // LOGIC FOR ALL OTHER STATES (On Trip, Arrived, etc.):
        else {
            driverLocation?.let { if (isValidLatLng(it)) { boundsBuilder.include(it); hasPoints = true } }
            dropoffLocation?.let { if (isValidLatLng(it)) { boundsBuilder.include(it); hasPoints = true } }

            // If we have a route, include it
            activeRoutePolyline.forEach { boundsBuilder.include(it) }

            // Fallback: If no route/driver, at least show pickup/dropoff
            if (!hasPoints) {
                pickupLocation?.let { if (isValidLatLng(it)) { boundsBuilder.include(it); hasPoints = true } }
            }
        }

        if (hasPoints) {
            try {
                val bounds = boundsBuilder.build()
                val padding = calculateAdaptivePadding(bounds)

                // Animate camera
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(bounds, padding),
                    1000
                )
                hasInitialized = true
            } catch (e: Exception) {
                Timber.w("Failed to update camera bounds: ${e.message}")
            }
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = uiSettings,
        onMapLoaded = { Timber.d("🗺️ Map loaded successfully") }
    ) {
        // Track user interactions
        MapEffect(Unit) { map ->
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

        // --- LAYER 1: PREVIEW ROUTE (Bottom) ---
        // Dashed line, usually for "Future" path (Pickup -> Dropoff)
        if (previewRoutePolyline.isNotEmpty()) {
            Polyline(
                points = previewRoutePolyline,
                color = if (isDarkTheme) Color.Gray.copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.5f),
                width = 10f,
                pattern = listOf(Dash(20f), Gap(10f)), // Dashed effect
                jointType = JointType.ROUND,
                zIndex = 0.5f
            )
        }

        // --- LAYER 2: ACTIVE ROUTE ---
        // Solid line, usually for "Current" path (Driver -> Pickup)
        if (activeRoutePolyline.isNotEmpty()) {
            // Shadow layer
            Polyline(
                points = activeRoutePolyline,
                color = if (isDarkTheme) Color(0xFFFFFFFF).copy(alpha = 0.2f) else Color(0xFF000000).copy(alpha = 0.3f),
                width = 18f,
                zIndex = 1f,
                startCap = RoundCap(),
                endCap = RoundCap(),
                jointType = JointType.ROUND
            )
            // Main Color layer (Orange)
            Polyline(
                points = activeRoutePolyline,
                color = Color(0xFFFF9800),
                width = 12f,
                zIndex = 2f,
                startCap = RoundCap(),
                endCap = RoundCap(),
                jointType = JointType.ROUND
            )
        }

        // --- LAYER 3: MARKERS ---

        // Pickup Marker (Green Tint)
        pickupLocation?.let { location ->
            if (isValidLatLng(location)) {
                // Use Tint to distinguish: Green for Pickup
                val icon = bitmapDescriptorFromVector(context, R.drawable.ic_location_pin, Color(0xFF4CAF50).toArgb())
                if (icon != null) {
                    Marker(
                        state = MarkerState(location),
                        title = "Pickup",
                        icon = icon,
                        anchor = androidx.compose.ui.geometry.Offset(0.5f, 1.0f),
                        zIndex = 3f
                    )
                }
            }
        }

        // Dropoff Marker (Red Tint)
        dropoffLocation?.let { location ->
            if (isValidLatLng(location)) {
                // Use Tint to distinguish: Red for Dropoff
                val icon = bitmapDescriptorFromVector(context, R.drawable.ic_location_pin, Color(0xFFE53935).toArgb())
                if (icon != null) {
                    Marker(
                        state = MarkerState(location),
                        title = "Dropoff",
                        icon = icon,
                        anchor = androidx.compose.ui.geometry.Offset(0.5f, 1.0f),
                        zIndex = 3f
                    )
                }
            }
        }

        // Driver Marker (Car - Top Layer)
        driverLocation?.let { location ->
            if (isValidLatLng(location)) {
                val icon = bitmapDescriptorFromVector(context, R.drawable.ic_car) // No tint for car
                if (icon != null) {
                    Marker(
                        state = MarkerState(animatedDriverPosition.value),
                        icon = icon,
                        rotation = animatedDriverBearing.value,
                        anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                        zIndex = 5f, // Highest Z-Index
                        flat = true
                    )
                }
            }
        }
    }
}

// --- HELPER FUNCTIONS ---

private fun isValidLatLng(loc: LatLng): Boolean {
    return loc.latitude != 0.0 && loc.longitude != 0.0 &&
            loc.latitude.isFinite() && loc.longitude.isFinite()
}

/**
 * Calculate adaptive padding based on route bounds
 */
private fun calculateAdaptivePadding(bounds: LatLngBounds): Int {
    return try {
        val width = bounds.northeast.longitude - bounds.southwest.longitude
        val height = bounds.northeast.latitude - bounds.southwest.latitude
        val maxDimension = max(width, height)

        when {
            maxDimension > 0.1 -> 200 // Large route
            maxDimension > 0.05 -> 150 // Medium route
            else -> 150 // Default
        }
    } catch (e: Exception) {
        150
    }
}

/**
 * Calculate bearing between two LatLng points
 */
private fun calculateBearing(from: LatLng, to: LatLng): Float {
    return try {
        val lat1 = Math.toRadians(from.latitude)
        val lon1 = Math.toRadians(from.longitude)
        val lat2 = Math.toRadians(to.latitude)
        val lon2 = Math.toRadians(to.longitude)

        val dLon = lon2 - lon1
        val y = Math.sin(dLon) * Math.cos(lat2)
        val x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon)

        val bearing = Math.toDegrees(Math.atan2(y, x))
        ((bearing + 360) % 360).toFloat()
    } catch (e: Exception) {
        Timber.e(e, "Error calculating bearing")
        0f
    }
}

/**
 * Create bitmap descriptor from vector drawable with optional Tinting
 */
fun bitmapDescriptorFromVector(
    context: Context,
    @DrawableRes vectorResId: Int,
    @ColorInt tintColor: Int? = null, // Added tint support
    sizeDp: Int = 36
): BitmapDescriptor? {
    return try {
        val drawable = ContextCompat.getDrawable(context, vectorResId) ?: run {
            Timber.w("Drawable not found: $vectorResId")
            return BitmapDescriptorFactory.defaultMarker()
        }

        // Apply tint if provided
        if (tintColor != null) {
            drawable.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN)
        }

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
        Timber.e(e, "Error creating bitmap descriptor")
        BitmapDescriptorFactory.defaultMarker()
    }
}