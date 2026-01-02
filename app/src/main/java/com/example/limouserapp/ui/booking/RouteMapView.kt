package com.example.limouserapp.ui.booking

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.limouserapp.R
import com.example.limouserapp.data.model.booking.RideData
import com.example.limouserapp.data.network.NetworkConfig
import com.example.limouserapp.ui.theme.LimoBlack
import com.example.limouserapp.ui.theme.LimoOrange
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.PolyUtil
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

@Composable
fun RouteMapView(
    rideData: RideData,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- State ---
    var mapType by remember { mutableStateOf(MapType.NORMAL) }
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // --- Camera ---
    // Initialize centered on a default location or the pickup if available
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(rideData.pickupLat ?: 0.0, rideData.pickupLong ?: 0.0),
            12f
        )
    }

    // --- Map Style ---
    val mapProperties = remember(mapType) {
        MapProperties(
            mapType = mapType,
            isTrafficEnabled = false, // Keep false for cleaner look by default
            mapStyleOptions = try {
                MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_minimal)
            } catch (e: Exception) { null }
        )
    }

    val mapUiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            compassEnabled = false,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false // Hides the "Open in Google Maps" buttons
        )
    }

    // --- Fetch Route Logic ---
    LaunchedEffect(rideData) {
        if (rideData.pickupLat != null && rideData.destinationLat != null) {
            isLoading = true
            try {
                val points = DirectionsRepository.getRoute(
                    origin = LatLng(rideData.pickupLat, rideData.pickupLong!!),
                    dest = LatLng(rideData.destinationLat, rideData.destinationLong!!),
                    apiKey = NetworkConfig.GOOGLE_PLACES_API_KEY // Ensure this exists in strings.xml
                )
                routePoints = points

                // Auto-center camera to fit the new route
                if (points.isNotEmpty()) {
                    val bounds = LatLngBounds.builder().apply {
                        points.forEach { include(it) }
                    }.build()
                    // 100 padding for breathing room
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                }
            } catch (e: Exception) {
                Log.e("RouteMapView", "Error fetching route", e)
            } finally {
                isLoading = false
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            properties = mapProperties,
            uiSettings = mapUiSettings,
            cameraPositionState = cameraPositionState
        ) {
            // 1. The Route Polyline (Professional Double-Layer)
            if (routePoints.isNotEmpty()) {
                // Layer A: The Border (Thicker, Darker)
                Polyline(
                    points = routePoints,
                    color = Color(0xFF1A1A1A), // Almost black border
                    width = 20f, // 8dp equivalent roughly
                    zIndex = 1f,
                    jointType = JointType.ROUND,
                    startCap = RoundCap(),
                    endCap = RoundCap()
                )

                // Layer B: The Main Route (Thinner, Brand Color)
                Polyline(
                    points = routePoints,
                    color = LimoOrange,
                    width = 12f,
                    zIndex = 2f,
                    jointType = JointType.ROUND,
                    startCap = RoundCap(),
                    endCap = RoundCap()
                )
            }

            // 2. Pickup Marker
            if (rideData.pickupLat != null) {
                Marker(
                    state = MarkerState(position = LatLng(rideData.pickupLat, rideData.pickupLong!!)),
                    title = "Pickup",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                    zIndex = 3f
                )
            }

            // 3. Destination Marker
            if (rideData.destinationLat != null) {
                Marker(
                    state = MarkerState(position = LatLng(rideData.destinationLat, rideData.destinationLong!!)),
                    title = "Destination",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                    zIndex = 3f
                )
            }
        }

        // --- Clean Control Panel ---
        // We group controls in a Surface for a floating "Card" feel
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            shadowElevation = 4.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEEEEEE))
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MapIconButton(
                    icon = Icons.Default.Add,
                    onClick = { scope.launch { cameraPositionState.animate(CameraUpdateFactory.zoomIn()) } }
                )
                MapIconButton(
                    icon = Icons.Default.Remove,
                    onClick = { scope.launch { cameraPositionState.animate(CameraUpdateFactory.zoomOut()) } }
                )
                Divider(modifier = Modifier.width(24.dp).padding(vertical = 4.dp))
                MapIconButton(
                    icon = Icons.Default.CenterFocusStrong,
                    onClick = {
                        if (routePoints.isNotEmpty()) {
                            val bounds = LatLngBounds.builder().apply { routePoints.forEach { include(it) } }.build()
                            scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 100)) }
                        }
                    }
                )
                MapIconButton(
                    icon = Icons.Default.Layers,
                    isActive = mapType == MapType.HYBRID,
                    onClick = { mapType = if (mapType == MapType.NORMAL) MapType.HYBRID else MapType.NORMAL }
                )
            }
        }

        // --- Loading Indicator ---
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .size(32.dp)
                    .background(Color.White, androidx.compose.foundation.shape.CircleShape)
                    .padding(4.dp),
                color = LimoOrange,
                strokeWidth = 3.dp
            )
        }
    }
}

// --- Reusable UI Components ---

@Composable
private fun MapIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isActive) LimoOrange.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) LimoOrange else Color.Gray,
            modifier = Modifier.size(20.dp)
        )
    }
}

// --- Directions API Helper (Networking) ---

object DirectionsRepository {
    private val client = OkHttpClient()

    /**
     * Fetches route from Google Directions API and returns a list of LatLng.
     * Note: This must be called from a Coroutine (suspend).
     */
    suspend fun getRoute(origin: LatLng, dest: LatLng, apiKey: String): List<LatLng> {
        return withContext(Dispatchers.IO) {
            val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                    "origin=${origin.latitude},${origin.longitude}" +
                    "&destination=${dest.latitude},${dest.longitude}" +
                    "&mode=driving" +
                    "&key=$apiKey"

            val request = Request.Builder().url(url).build()

            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(body)

                val routes = json.getJSONArray("routes")
                if (routes.length() > 0) {
                    val points = routes.getJSONObject(0)
                        .getJSONObject("overview_polyline")
                        .getString("points")

                    // Decodes Google's compressed polyline string
                    return@withContext PolyUtil.decode(points)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext emptyList()
        }
    }
}