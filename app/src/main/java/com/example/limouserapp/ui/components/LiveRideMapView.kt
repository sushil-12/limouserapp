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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    modifier: Modifier = Modifier
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
    val uiState by viewModel.uiState.collectAsState()
    val driverLocation = uiState.driverLocation
    val pickupLocation = uiState.pickupLocation
    val dropoffLocation = uiState.dropoffLocation
    val routePolyline = uiState.routePolyline
    val coveredPath = uiState.coveredPath
    val driverHeading = uiState.driverHeading
    val rideStatus = uiState.status
    val airportMessage = uiState.airportMessage
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

    // Calculate bearing from driver location changes or use provided heading
    var previousDriverLocation by remember { mutableStateOf<LatLng?>(null) }

    LaunchedEffect(driverLocation, driverHeading) {
        driverLocation?.let { currentLocation ->
            val bearing = driverHeading ?: run {
                // Calculate bearing from previous location if heading not provided
                previousDriverLocation?.let { prevLoc ->
                    calculateBearing(prevLoc, currentLocation)
                } ?: 0f
            }

            previousDriverLocation?.let {
                // Animate to new position
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
                        targetValue = bearing,
                        animationSpec = tween(durationMillis = 800, easing = LinearEasing)
                    )
                }
            } ?: run {
                // First location update
                animatedDriverPosition.snapTo(currentLocation)
                animatedDriverBearing.snapTo(bearing)
            }
            previousDriverLocation = currentLocation
        }
    }

    var isUserInteracting by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(0L) }

    // Auto-adjust camera bounds based on ride status (only if user hasn't interacted)
    LaunchedEffect(mapRegion, userHasInteracted, driverLocation, pickupLocation, dropoffLocation, rideStatus) {
        if (!userHasInteracted && driverLocation != null) {
            // Determine which destination to show based on ride status
            val destination = when (rideStatus) {
                "en_route_pu" -> pickupLocation
                "en_route_do", "started", "ride_in_progress" -> dropoffLocation
                else -> null
            }

            if (destination != null) {
                // Create bounds with driver and destination
                val bounds = LatLngBounds.Builder()
                    .include(driverLocation)
                    .include(destination)
                    .build()

                // Smooth camera animation with tilt for navigation feel
                val cameraUpdate = CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(bounds.center)
                        .zoom(mapRegion?.zoom ?: 15f)
                        .bearing(driverHeading ?: 0f)
                        .tilt(45f)
                        .build()
                )
                cameraPositionState.animate(cameraUpdate, 1000)
            } else if (mapRegion != null) {
                val cameraUpdate = CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(mapRegion!!.center)
                        .zoom(mapRegion!!.zoom)
                        .bearing(driverHeading ?: 0f)
                        .tilt(45f)
                        .build()
                )
                cameraPositionState.animate(cameraUpdate, 1000)
            }
        }
    }

    // Auto-follow driver when not interacting (smooth following)
    LaunchedEffect(driverLocation, isUserInteracting, driverHeading) {
        if (driverLocation != null && !isUserInteracting) {
            if (System.currentTimeMillis() - lastInteractionTime > 3000) {
                val currentZoom = cameraPositionState.position.zoom.takeIf { it > 10f } ?: 16f

                val cameraUpdate = CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(driverLocation!!)
                        .zoom(currentZoom)
                        .bearing(driverHeading ?: 0f)
                        .tilt(45f) // Professional navigation tilt
                        .build()
                )
                cameraPositionState.animate(cameraUpdate, 1000)
            }
        }
    }

    Box(modifier = modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
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

        // Show covered path (traveled portion) - lighter gray
        if (coveredPath.isNotEmpty() && coveredPath.size > 1) {
            Polyline(
                points = coveredPath,
                color = Color(0xFFCCCCCC), // Light gray for traveled path
                width = 12f,
                zIndex = 1f,
                startCap = RoundCap(),
                endCap = RoundCap(),
                jointType = JointType.ROUND
            )
        }

        // Show primary route polyline (remaining/uncovered portion) - solid black
        if (routePolyline.isNotEmpty()) {
            Polyline(
                points = routePolyline,
                color = Color(0xFF000000), // Solid black for primary route
                width = 12f,
                zIndex = 2f,
                startCap = RoundCap(),
                endCap = RoundCap(),
                jointType = JointType.ROUND
            )
        }

        // Show pickup marker only when en_route_pu
        if (rideStatus == "en_route_pu" && pickupLocation != null) {
            Marker(
                state = MarkerState(pickupLocation),
                title = "Pickup",
                icon = bitmapDescriptorFromVector(context, R.drawable.ic_location_pin),
                anchor = androidx.compose.ui.geometry.Offset(0.5f, 1.0f),
                zIndex = 3f
            )
        }

        // Show dropoff marker when en_route_do, started, or ride_in_progress
        if ((rideStatus == "en_route_do" || rideStatus == "started" || rideStatus == "ride_in_progress") && dropoffLocation != null) {
            Marker(
                state = MarkerState(dropoffLocation),
                title = "Dropoff",
                icon = bitmapDescriptorFromVector(context, R.drawable.ic_location_pin),
                anchor = androidx.compose.ui.geometry.Offset(0.5f, 1.0f),
                zIndex = 3f
            )
        }

        // Driver marker with smooth animation and rotation
        if (driverLocation != null) {
            Marker(
                state = MarkerState(animatedDriverPosition.value),
                icon = bitmapDescriptorFromVector(context, R.drawable.ic_car),
                rotation = animatedDriverBearing.value,
                anchor = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                zIndex = 5f,
                flat = true // Flat marker rotates with map
            )
        }

        // Airport/Campus message banner (production-ready: clear UX message)
        if (airportMessage != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Text(
                    text = airportMessage,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
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


