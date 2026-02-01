package com.example.limouserapp.ui.liveride

import com.example.limouserapp.data.socket.ActiveRide
import com.google.android.gms.maps.model.LatLng

/**
 * Single source of truth for the Live Ride screen (user app).
 *
 * Mirrors the optimized pattern used in the driver app:
 * - One uiState object collected by Compose
 * - Small derived flags for phase branching
 */
data class LiveRideUiState(
    val activeRide: ActiveRide? = null,
    val driverLocation: LatLng? = null,
    val pickupLocation: LatLng? = null,
    val dropoffLocation: LatLng? = null,
    val statusMessage: String = "",
    val estimatedTime: String = "",
    val distance: String = "",
    val rideOtp: String = "",
    val pickupArrivalDetected: Boolean = false,
    val dropoffArrivalDetected: Boolean = false,
    val routePolyline: List<LatLng> = emptyList(),
    val coveredPath: List<LatLng> = emptyList(),
    val driverHeading: Float? = null,
    val airportMessage: String? = null
) {
    val status: String get() = activeRide?.status.orEmpty()

    val isEnRouteToPickup: Boolean get() = status == "en_route_pu"
    val isArrivedAtPickup: Boolean get() = status == "on_location"
    val isRideStarted: Boolean get() = status == "en_route_do" || status == "started" || status == "ride_in_progress"
    val isRideEnded: Boolean get() = status == "ended"
}


