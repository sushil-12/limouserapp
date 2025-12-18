package com.example.limouserapp.ui.booking

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.limouserapp.R
import com.example.limouserapp.data.model.booking.RideData
import com.example.limouserapp.ui.theme.LimoBlack
import com.example.limouserapp.ui.theme.LimoOrange
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Advanced Route Map View
 * Features:
 * - Interactive Zoom/Recenter controls
 * - Traffic Layer Toggle
 * - Map Type Toggle
 * - Smooth Polyline rendering
 * - Custom Marker logic
 */
@Composable
fun RouteMapView(
    rideData: RideData,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- State ---
    var isTrafficEnabled by remember { mutableStateOf(false) }
    var mapType by remember { mutableStateOf(MapType.NORMAL) }
    var routePolyline by remember { mutableStateOf<List<LatLng>?>(null) }
    var isLoadingRoute by remember { mutableStateOf(false) }

    // Coordinates
    val pickupLat = rideData.pickupLat
    val pickupLong = rideData.pickupLong
    val destLat = rideData.destinationLat
    val destLong = rideData.destinationLong

    // Camera State
    val cameraPositionState = rememberCameraPositionState()

    // --- Route Calculation ---
    LaunchedEffect(pickupLat, pickupLong, destLat, destLong) {
        if (pickupLat != null && pickupLong != null && destLat != null && destLong != null) {
            isLoadingRoute = true
            // In a real app, this would call Google Directions API
            routePolyline = withContext(Dispatchers.IO) {
                calculateAdvancedRoute(pickupLat, pickupLong, destLat, destLong)
            }
            isLoadingRoute = false

            // Auto-center once route is ready
            fitRouteBounds(cameraPositionState, pickupLat, pickupLong, destLat, destLong, routePolyline)
        }
    }

    // --- Map Settings ---
    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false, // We use custom buttons
            compassEnabled = false,
            myLocationButtonEnabled = false,
            rotationGesturesEnabled = true,
            tiltGesturesEnabled = true,
            zoomGesturesEnabled = true,
            scrollGesturesEnabled = true
        )
    }

    val mapProperties = remember(isTrafficEnabled, mapType) {
        MapProperties(
            mapType = mapType,
            isTrafficEnabled = isTrafficEnabled,
            // Try to load custom style, fallback if fails
            mapStyleOptions = try {
                MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_themed)
            } catch (e: Exception) { null }
        )
    }

    Box(modifier = modifier) {
        if (pickupLat != null && pickupLong != null && destLat != null && destLong != null) {

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                properties = mapProperties,
                uiSettings = mapUiSettings,
                cameraPositionState = cameraPositionState
            ) {
                // 1. Pickup Marker
                Marker(
                    state = MarkerState(position = LatLng(pickupLat, pickupLong)),
                    title = "Pickup",
                    snippet = getDisplayLocation(rideData.pickupType, rideData.pickupLocation, rideData.selectedPickupAirport),
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                    zIndex = 1f
                )

                // 2. Destination Marker
                Marker(
                    state = MarkerState(position = LatLng(destLat, destLong)),
                    title = "Destination",
                    snippet = getDisplayLocation(rideData.dropoffType, rideData.destinationLocation, rideData.selectedDestinationAirport),
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                    zIndex = 2f
                )

                // 3. The Route Line
                routePolyline?.let { points ->
                    Polyline(
                        points = points,
                        color = LimoOrange,
                        width = 12f,
                        jointType = JointType.ROUND, // Smooth corners
                        startCap = RoundCap(),
                        endCap = RoundCap(),
                        geodesic = true
                    )

                    // Optional: Inner white line for "Road" effect
                    Polyline(
                        points = points,
                        color = Color.White,
                        width = 4f,
                        zIndex = 1f,
                        jointType = JointType.ROUND,
                        startCap = RoundCap(),
                        endCap = RoundCap()
                    )
                }
            }

            // --- Floating Control Panel (Right Side) ---
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Map Layers Toggle
                MapControlButton(
                    icon = if (mapType == MapType.NORMAL) Icons.Default.Layers else Icons.Default.Layers,
                    isActive = mapType == MapType.HYBRID,
                    onClick = {
                        mapType = if (mapType == MapType.NORMAL) MapType.HYBRID else MapType.NORMAL
                    }
                )

                // 2. Traffic Toggle
                MapControlButton(
                    icon = Icons.Default.Traffic,
                    isActive = isTrafficEnabled,
                    onClick = { isTrafficEnabled = !isTrafficEnabled }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Zoom In
                MapControlButton(
                    icon = Icons.Default.Add,
                    onClick = {
                        scope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.zoomIn())
                        }
                    }
                )

                // 4. Zoom Out
                MapControlButton(
                    icon = Icons.Default.Remove,
                    onClick = {
                        scope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.zoomOut())
                        }
                    }
                )

                // 5. Recenter / Fit Bounds
                MapControlButton(
                    icon = Icons.Default.CenterFocusStrong,
                    onClick = {
                        scope.launch {
                            fitRouteBounds(
                                cameraPositionState,
                                pickupLat, pickupLong,
                                destLat, destLong,
                                routePolyline
                            )
                        }
                    }
                )
            }

            // Loading Overlay
            AnimatedVisibility(
                visible = isLoadingRoute,
                modifier = Modifier.align(Alignment.Center),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = LimoBlack.copy(alpha = 0.8f),
                    contentColor = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = LimoOrange,
                            strokeWidth = 2.dp
                        )
                        Text("Calculating best route...", style = TextStyle(fontSize = 12.sp))
                    }
                }
            }

        } else {
            // Error State
            Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Traffic, contentDescription = null, tint = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Route preview unavailable", color = Color.Gray)
                }
            }
        }
    }
}

// --- Helper Components ---

@Composable
private fun MapControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    SmallFloatingActionButton(
        onClick = onClick,
        containerColor = if (isActive) LimoOrange else Color.White,
        contentColor = if (isActive) Color.White else LimoBlack,
        shape = CircleShape,
        modifier = Modifier.size(44.dp),
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
    }
}

// --- Helper Logic ---

/**
 * Get the correct display location based on type
 * If type is "airport", use selectedAirport, otherwise use location
 */
private fun getDisplayLocation(
    type: String,
    location: String,
    selectedAirport: String
): String {
    return if (type.equals("airport", ignoreCase = true) && selectedAirport.isNotEmpty()) {
        selectedAirport
    } else {
        location.ifEmpty { selectedAirport }
    }
}

private suspend fun fitRouteBounds(
    cameraState: CameraPositionState,
    pLat: Double, pLong: Double,
    dLat: Double, dLong: Double,
    route: List<LatLng>?
) {
    val builder = LatLngBounds.Builder()
    builder.include(LatLng(pLat, pLong))
    builder.include(LatLng(dLat, dLong))
    route?.forEach { builder.include(it) }

    try {
        // Add decent padding (e.g., 100) so markers aren't on the screen edge
        cameraState.animate(CameraUpdateFactory.newLatLngBounds(builder.build(), 150))
    } catch (e: Exception) {
        // Fallback if layout not ready
        cameraState.move(CameraUpdateFactory.newLatLngZoom(LatLng(pLat, pLong), 10f))
    }
}

/**
 * Simulates a "Real" route for visualization.
 * In a real app, Replace this with Retrofit call to Google Directions API
 * and decode the 'overview_polyline' string.
 */
private fun calculateAdvancedRoute(
    startLat: Double, startLng: Double,
    endLat: Double, endLng: Double
): List<LatLng> {
    val points = mutableListOf<LatLng>()
    points.add(LatLng(startLat, startLng))

    // Create a quadratic Bezier curve to simulate a road route
    // (A straight line looks fake on a map)
    val midLat = (startLat + endLat) / 2
    val midLng = (startLng + endLng) / 2

    // Add a slight "curve" offset based on distance
    val curveOffset = 0.01 // Adjust for "curviness"
    val controlPoint = LatLng(midLat + curveOffset, midLng + curveOffset)

    val steps = 20
    for (i in 1 until steps) {
        val t = i.toDouble() / steps
        val lat = (1 - t) * (1 - t) * startLat + 2 * (1 - t) * t * controlPoint.latitude + t * t * endLat
        val lng = (1 - t) * (1 - t) * startLng + 2 * (1 - t) * t * controlPoint.longitude + t * t * endLng
        points.add(LatLng(lat, lng))
    }

    points.add(LatLng(endLat, endLng))
    return points
}