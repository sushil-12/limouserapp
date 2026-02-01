package com.example.limouserapp.ui.liveride

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.limouserapp.data.socket.ActiveRide
import com.example.limouserapp.data.socket.DriverLocationUpdate
import com.example.limouserapp.data.socket.SocketService
import com.example.limouserapp.data.service.DirectionsService
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * ViewModel for Live Ride In Progress screen
 * Handles real-time driver location tracking, status updates, and map management
 * Optimized for production with efficient updates and memory management
 */
@HiltViewModel
class LiveRideViewModel @Inject constructor(
    private val socketService: SocketService,
    private val directionsService: DirectionsService,
    private val bookingService: com.example.limouserapp.data.service.BookingService,
    private val airportCampusService: com.example.limouserapp.data.service.AirportCampusService,
    private val roadsSnappingService: com.example.limouserapp.data.service.RoadsSnappingService
) : ViewModel() {
    
    // Live Ride Data
    private val _activeRide = MutableStateFlow<ActiveRide?>(null)
    val activeRide: StateFlow<ActiveRide?> = _activeRide.asStateFlow()
    
    // Driver Location
    private val _driverLocation = MutableStateFlow<LatLng?>(null)
    val driverLocation: StateFlow<LatLng?> = _driverLocation.asStateFlow()
    
    // Pickup and Dropoff locations
    private val _pickupLocation = MutableStateFlow<LatLng?>(null)
    val pickupLocation: StateFlow<LatLng?> = _pickupLocation.asStateFlow()
    
    private val _dropoffLocation = MutableStateFlow<LatLng?>(null)
    val dropoffLocation: StateFlow<LatLng?> = _dropoffLocation.asStateFlow()
    
    // Map region and camera
    private val _mapRegion = MutableStateFlow<MapRegion?>(null)
    val mapRegion: StateFlow<MapRegion?> = _mapRegion.asStateFlow()
    
    // Ride Status
    private val _statusMessage = MutableStateFlow("Driver is on the way")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()
    
    // Ride OTP
    private val _rideOTP = MutableStateFlow("")
    val rideOTP: StateFlow<String> = _rideOTP.asStateFlow()
    
    // OTP generation state
    private val _isGeneratingOTP = MutableStateFlow(false)
    val isGeneratingOTP: StateFlow<Boolean> = _isGeneratingOTP.asStateFlow()
    
    private val _otpError = MutableStateFlow<String?>(null)
    val otpError: StateFlow<String?> = _otpError.asStateFlow()
    
    // Track last status to detect changes
    private var lastStatus: String? = null
    private var otpGeneratedForBooking: Int? = null
    
    // Estimated time and distance (preserve last valid values)
    private val _estimatedTime = MutableStateFlow("Calculating...")
    val estimatedTime: StateFlow<String> = _estimatedTime.asStateFlow()
    
    private val _distance = MutableStateFlow("Calculating...")
    val distance: StateFlow<String> = _distance.asStateFlow()
    
    // Track if we have valid route data
    private var hasValidRouteData = false
    private var lastValidDistance: String? = null
    private var lastValidETA: String? = null
    
    // Status flags
    private val _isDriverOnLocation = MutableStateFlow(false)
    val isDriverOnLocation: StateFlow<Boolean> = _isDriverOnLocation.asStateFlow()

    // Proximity detection (soft geofence) for UI phase hints
    private val _pickupArrivalDetected = MutableStateFlow(false)
    val pickupArrivalDetected: StateFlow<Boolean> = _pickupArrivalDetected.asStateFlow()

    private val _dropoffArrivalDetected = MutableStateFlow(false)
    val dropoffArrivalDetected: StateFlow<Boolean> = _dropoffArrivalDetected.asStateFlow()
    
    // Route polyline for map display (full route)
    private val _routePolyline = MutableStateFlow<List<LatLng>>(emptyList())
    val routePolyline: StateFlow<List<LatLng>> = _routePolyline.asStateFlow()
    
    // Covered path (traveled portion of route)
    private val _coveredPath = MutableStateFlow<List<LatLng>>(emptyList())
    val coveredPath: StateFlow<List<LatLng>> = _coveredPath.asStateFlow()
    
    // Driver heading/bearing for marker rotation
    private val _driverHeading = MutableStateFlow<Float?>(null)
    val driverHeading: StateFlow<Float?> = _driverHeading.asStateFlow()
    
    // Route state persistence
    private var lastValidRoute: List<LatLng>? = null
    private var lastValidRouteOrigin: LatLng? = null
    private var lastValidRouteDestination: LatLng? = null
    private var lastValidRouteTimestamp: Long = 0L
    private var lastValidETASeconds: Int? = null
    private var lastValidDistanceMeters: Int? = null
    
    // ETA smoothing (Exponential Moving Average)
    private var smoothedETA: Double? = null
    private val etaSmoothingAlpha = 0.3 // EMA smoothing factor

    // Airport/Campus detection
    private val _airportMessage = MutableStateFlow<String?>(null)
    val airportMessage: StateFlow<String?> = _airportMessage.asStateFlow()
    private var currentAirportSite: com.example.limouserapp.data.model.location.Site? = null

    // Monotonic progress tracking
    private var lastProgressMeters: Double = 0.0
    private val progressBackstepThreshold = 50.0 // meters - allow large intentional reversals
    private var projectionBackstepCount = 0

    /**
     * Single source of truth (optimized): collect one uiState in Compose.
     */
    val uiState: StateFlow<LiveRideUiState> = combine(
        activeRide,
        driverLocation,
        pickupLocation,
        dropoffLocation,
        statusMessage,
        estimatedTime,
        distance,
        rideOTP,
        pickupArrivalDetected,
        dropoffArrivalDetected,
        routePolyline,
        coveredPath,
        driverHeading,
        airportMessage
    ) { values: Array<Any?> ->
        LiveRideUiState(
            activeRide = values[0] as ActiveRide?,
            driverLocation = values[1] as LatLng?,
            pickupLocation = values[2] as LatLng?,
            dropoffLocation = values[3] as LatLng?,
            statusMessage = values[4] as String,
            estimatedTime = values[5] as String,
            distance = values[6] as String,
            rideOtp = values[7] as String,
            pickupArrivalDetected = values[8] as Boolean,
            dropoffArrivalDetected = values[9] as Boolean,
            routePolyline = (values[10] as? List<LatLng>) ?: emptyList(),
            coveredPath = (values[11] as? List<LatLng>) ?: emptyList(),
            driverHeading = values[12] as Float?,
            airportMessage = values[13] as String?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LiveRideUiState()
    )
    
    // Map interaction tracking
    private val _userHasInteractedWithMap = MutableStateFlow(false)
    val userHasInteractedWithMap: StateFlow<Boolean> = _userHasInteractedWithMap.asStateFlow()
    
    // Should handle driver location updates
    private val _shouldHandleDriverLocationUpdates = MutableStateFlow(true)
    val shouldHandleDriverLocationUpdates: StateFlow<Boolean> = _shouldHandleDriverLocationUpdates.asStateFlow()
    
    // Track if receiving real-time updates
    private val _isReceivingSocketUpdates = MutableStateFlow(false)
    val isReceivingSocketUpdates: StateFlow<Boolean> = _isReceivingSocketUpdates.asStateFlow()
    
    // UI update throttling (1-2s for smoothness)
    private var lastViewUpdateTime = 0L
    private val viewUpdateThrottleInterval = 1500L // 1.5s throttle for UI updates
    private var lastCarLocation: LatLng? = null
    
    // Throttle update job
    private var updateJob: Job? = null
    
    // Route calculation throttling (avoid excessive API calls)
    private var lastRouteCalculationTime = 0L
    private val routeCalculationInterval = 5000L // Minimum 5s between route calculations
    private val routeRecalculationDistanceThreshold = 20.0 // 20 meters
    private var routeCalculationJob: Job? = null
    private var lastRouteOrigin: LatLng? = null
    private var lastRouteDestination: LatLng? = null
    private var lastRouteStatus: String? = null
    private var isRouteCalculationInProgress = false
    
    // GPS noise filtering (5-10m threshold)
    private val gpsNoiseThreshold = 7.5 // meters
    
    init {
        observeActiveRide()
        observeDriverLocations()
        startThrottledUpdates()
        startRouteCalculation()
    }
    
    /**
     * Observe active ride from SocketService
     */
    private fun observeActiveRide() {
        viewModelScope.launch {
            socketService.activeRide
                .filterNotNull()
                .collect { ride ->
                    handleActiveRideUpdate(ride)
                }
        }
    }
    
    /**
     * Observe driver location updates from SocketService
     * Throttled to 1-2s for UI updates, but accepts all updates for analytics
     */
    private fun observeDriverLocations() {
        viewModelScope.launch {
            socketService.driverLocations
                .collect { locations ->
                    processDriverLocationUpdates(locations)
                }
        }
    }
    
    /**
     * Start throttled updates for UI refresh
     */
    private fun startThrottledUpdates() {
        updateJob = viewModelScope.launch {
            while (true) {
                delay(500) // Check every 500ms
                throttledViewUpdate()
            }
        }
    }
    
    /**
     * Throttled view update to prevent excessive refreshes (1-2s interval)
     */
    private fun throttledViewUpdate() {
        val now = System.currentTimeMillis()
        if (now - lastViewUpdateTime < viewUpdateThrottleInterval) {
            return
        }
        lastViewUpdateTime = now
        
        _activeRide.value?.let { ride ->
            updateStatusUI(ride.status)
        }
        
        // Update map region if user hasn't interacted
        if (!_userHasInteractedWithMap.value) {
            updateMapRegion()
        }
    }
    
    /**
     * Handle active ride update from socket
     */
    private fun handleActiveRideUpdate(ride: ActiveRide) {
        _activeRide.value = ride
        
        // Update locations
        _pickupLocation.value = LatLng(ride.pickupLatitude, ride.pickupLongitude)
        _dropoffLocation.value = LatLng(ride.dropoffLatitude, ride.dropoffLongitude)
        
        // Update driver location if available
        if (ride.driverLatitude != 0.0 && ride.driverLongitude != 0.0) {
            val driverLoc = LatLng(ride.driverLatitude, ride.driverLongitude)
            _driverLocation.value = driverLoc
            lastCarLocation = driverLoc
        }
        
        // Check for status change and generate OTP if needed
        val statusChanged = lastStatus != ride.status
        if (statusChanged) {
            handleStatusChange(ride)
        }
        lastStatus = ride.status
        
        // Update status message
        updateStatusUI(ride.status)

        // Proximity detection (if we already have driver location)
        _driverLocation.value?.let { driverLoc ->
            updateProximity(ride, driverLoc)
        }
        
        // Trigger initial route calculation when active ride is received
        // This ensures ETA/distance is calculated immediately
        viewModelScope.launch {
            delay(500) // Small delay to ensure locations are set
            calculateRouteIfNeeded(force = true)
        }
        
        // Update map region if user hasn't interacted
        if (!_userHasInteractedWithMap.value) {
            updateMapRegion()
        }
        
        // Note: Room joining is now handled in SocketService when active ride is received
    }
    
    /**
     * Handle status change and generate OTP when appropriate
     */
    private fun handleStatusChange(ride: ActiveRide) {
        val bookingId = ride.bookingId.toIntOrNull() ?: return
        
        // Generate OTP when status changes to on_location (preferred) or en_route_pu (alternative)
        when (ride.status) {
            "on_location", "en_route_pu" -> {
                // Only generate if we haven't already generated for this booking
                if (otpGeneratedForBooking != bookingId && !_isGeneratingOTP.value) {
                    generateRideOTP(bookingId)
                }
            }
        }
    }
    
    /**
     * Generate ride OTP for the booking
     */
    private fun generateRideOTP(bookingId: Int) {
        viewModelScope.launch {
            _isGeneratingOTP.value = true
            _otpError.value = null
            
            val result = bookingService.generateRideOTP(bookingId)
            result.fold(
                onSuccess = { response ->
                    _rideOTP.value = response.otp
                    otpGeneratedForBooking = bookingId
                    _otpError.value = null
                },
                onFailure = { error ->
                    _otpError.value = error.message ?: "Failed to generate OTP"
                    // Don't clear existing OTP on failure - preserve last valid state
                }
            )
            
            _isGeneratingOTP.value = false
        }
    }
    
    /**
     * Process driver location updates from socket
     * Filters GPS noise and throttles UI updates
     */
    private fun processDriverLocationUpdates(locations: List<DriverLocationUpdate>) {
        val currentRide = _activeRide.value ?: return
        
        locations
            .firstOrNull { it.bookingId == currentRide.bookingId }
            ?.let { update ->
                val newLocation = LatLng(update.latitude, update.longitude)
                
                // Update driver heading if available
                if (update.heading > 0) {
                    _driverHeading.value = update.heading.toFloat()
                } else {
                    // Calculate heading from previous location
                    lastCarLocation?.let { prevLoc ->
                        val bearing = calculateBearing(prevLoc, newLocation)
                        _driverHeading.value = bearing
                    }
                }
                
                // Filter GPS noise: only update if moved > threshold
                if (shouldUpdateLocation(newLocation)) {
                    val prevLocation = lastCarLocation
                    _driverLocation.value = newLocation
                    lastCarLocation = newLocation
                    _isReceivingSocketUpdates.value = true

                    updateProximity(currentRide, newLocation)
                    
                    // Check for airport/campus detection
                    checkAirportCampus(newLocation)
                    
                    // Update covered path based on driver position (with monotonic progress)
                    updateCoveredPath(newLocation)
                    
                    // Trigger route recalculation if driver moved significantly or status changed
                    if (prevLocation != null) {
                        val distanceMoved = calculateDistance(prevLocation, newLocation)
                        if (distanceMoved > routeRecalculationDistanceThreshold) {
                            triggerRouteRecalculation()
                        }
                    } else {
                        // First location update - trigger initial route calculation
                        triggerRouteRecalculation()
                    }
                    
                    // Throttled UI update
                    throttledViewUpdate()
                }
            }
    }
    
    /**
     * Calculate bearing between two points
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
    
    /**
     * Calculate distance between two points in meters
     */
    private fun calculateDistance(from: LatLng, to: LatLng): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(
            from.latitude,
            from.longitude,
            to.latitude,
            to.longitude,
            results
        )
        return results[0]
    }
    
    /**
     * Check if location should be updated (GPS noise filtering: 7.5m threshold)
     */
    private fun shouldUpdateLocation(newLocation: LatLng): Boolean {
        val lastLocation = lastCarLocation ?: return true
        
        val results = FloatArray(1)
        Location.distanceBetween(
            lastLocation.latitude,
            lastLocation.longitude,
            newLocation.latitude,
            newLocation.longitude,
            results
        )
        
        // Filter GPS noise: only update if moved more than threshold (7.5m)
        return results[0] > gpsNoiseThreshold
    }
    
    /**
     * Update map region to show driver and destination based on ride status
     * Uber-like behavior: Show driver + pickup (en_route_pu) or driver + dropoff (en_route_do)
     */
    private fun updateMapRegion() {
        val driverLoc = _driverLocation.value ?: return
        val ride = _activeRide.value ?: return
        
        // Determine which destination to show based on ride status
        val destination = when (ride.status) {
            "en_route_pu" -> _pickupLocation.value
            "en_route_do", "started" -> _dropoffLocation.value
            else -> null
        }
        
        if (destination == null) {
            // Fallback: show all locations
            val pickupLoc = _pickupLocation.value
            val dropoffLoc = _dropoffLocation.value
            
            if (pickupLoc != null && dropoffLoc != null) {
                val bounds = LatLngBounds.builder()
                    .include(driverLoc)
                    .include(pickupLoc)
                    .include(dropoffLoc)
                    .build()
                
                _mapRegion.value = MapRegion(
                    center = bounds.center,
                    zoom = calculateZoomFromBounds(bounds)
                )
            } else {
                // Just center on driver
                _mapRegion.value = MapRegion(
                    center = driverLoc,
                    zoom = 16f
                )
            }
            return
        }
        
        // Create bounds with driver and destination
        val bounds = LatLngBounds.builder()
            .include(driverLoc)
            .include(destination)
            .build()
        
        _mapRegion.value = MapRegion(
            center = bounds.center,
            zoom = calculateZoomFromBounds(bounds)
        )
    }
    
    /**
     * Calculate zoom level from bounds
     */
    private fun calculateZoomFromBounds(bounds: LatLngBounds): Float {
        val width = bounds.northeast.longitude - bounds.southwest.longitude
        val height = bounds.northeast.latitude - bounds.southwest.latitude
        val maxDimension = maxOf(width, height)
        
        // Calculate zoom level based on bounds
        // Larger bounds = lower zoom, smaller bounds = higher zoom
        val zoom = when {
            maxDimension > 0.1 -> 11f // Very wide view
            maxDimension > 0.05 -> 13f // Wide view
            maxDimension > 0.01 -> 15f // Medium view
            else -> 17f // Close view
        }
        
        return zoom.coerceIn(11f, 18f)
    }
    
    
    /**
     * Update status UI based on ride status
     */
    private fun updateStatusUI(status: String) {
        // Keep this flag consistent (it was previously only set to true and never reset).
        _isDriverOnLocation.value = status == "on_location"
        val message = when (status) {
            "en_route_pu" -> "Driver is on the way to pickup location"
            "on_location" -> {
                "Driver has arrived at pickup location"
            }
            "en_route_do" -> "En route to your destination"
            "ended" -> "Ride completed"
            else -> "Driver is on the way"
        }
        
        _statusMessage.value = message
    }

    private fun updateProximity(ride: ActiveRide, driverLoc: LatLng) {
        // Pickup proximity (<=100m)
        if (ride.pickupLatitude != 0.0 && ride.pickupLongitude != 0.0) {
            val d = FloatArray(1)
            Location.distanceBetween(
                driverLoc.latitude,
                driverLoc.longitude,
                ride.pickupLatitude,
                ride.pickupLongitude,
                d
            )
            _pickupArrivalDetected.value = (d.firstOrNull() ?: Float.MAX_VALUE) <= 100f
        } else {
            _pickupArrivalDetected.value = false
        }

        // Dropoff proximity (<=50m)
        if (ride.dropoffLatitude != 0.0 && ride.dropoffLongitude != 0.0) {
            val d = FloatArray(1)
            Location.distanceBetween(
                driverLoc.latitude,
                driverLoc.longitude,
                ride.dropoffLatitude,
                ride.dropoffLongitude,
                d
            )
            _dropoffArrivalDetected.value = (d.firstOrNull() ?: Float.MAX_VALUE) <= 50f
        } else {
            _dropoffArrivalDetected.value = false
        }
    }
    
    /**
     * Start route calculation job
     */
    private fun startRouteCalculation() {
        routeCalculationJob = viewModelScope.launch {
            while (true) {
                delay(routeCalculationInterval)
                calculateRouteIfNeeded()
            }
        }
    }
    
    /**
     * Trigger route recalculation (called when driver moves significantly)
     */
    private fun triggerRouteRecalculation() {
        viewModelScope.launch {
            calculateRouteIfNeeded(force = true)
        }
    }
    
    /**
     * Calculate route based on ride status
     * en_route_pu: driver_current_location → pickup_location
     * en_route_do / ride_in_progress: driver_current_location → dropoff_location
     * 
     * Only recalculates when:
     * - Status changes
     * - Driver moves > 20m from last route origin
     * - Minimum time interval elapsed (5s)
     * 
     * Preserves last valid values if calculation fails
     */
    private suspend fun calculateRouteIfNeeded(force: Boolean = false) {
        val ride = _activeRide.value ?: return
        val driverLoc = _driverLocation.value ?: return
        
        // Determine destination based on ride status
        val destination = when (ride.status) {
            "en_route_pu" -> {
                val pickupLoc = _pickupLocation.value
                if (pickupLoc == null || (pickupLoc.latitude == 0.0 && pickupLoc.longitude == 0.0)) {
                    return
                }
                pickupLoc
            }
            "en_route_do", "started", "ride_in_progress" -> {
                val dropoffLoc = _dropoffLocation.value
                if (dropoffLoc == null || (dropoffLoc.latitude == 0.0 && dropoffLoc.longitude == 0.0)) {
                    return
                }
                dropoffLoc
            }
            else -> {
                return // No route needed for other statuses
            }
        }
        
        // Validate coordinates
        if (driverLoc.latitude == 0.0 && driverLoc.longitude == 0.0 ||
            destination.latitude == 0.0 && destination.longitude == 0.0) {
            return
        }
        
        // Prevent concurrent route calculations
        if (isRouteCalculationInProgress) {
            return
        }
        
        // Check if we need to recalculate
        val now = System.currentTimeMillis()
        val statusChanged = lastRouteStatus != ride.status
        val originMoved = lastRouteOrigin?.let { origin ->
            calculateDistance(origin, driverLoc) > routeRecalculationDistanceThreshold
        } ?: true
        val timeElapsed = now - lastRouteCalculationTime > routeCalculationInterval
        
        val shouldRecalculate = force || statusChanged || (originMoved && timeElapsed)
        
        if (!shouldRecalculate) {
            return
        }
        
        isRouteCalculationInProgress = true
        lastRouteCalculationTime = now
        lastRouteOrigin = driverLoc
        lastRouteDestination = destination
        lastRouteStatus = ride.status
        
        val startTime = System.currentTimeMillis()
        try {
            val routeResult = directionsService.getRouteWithPolyline(
                originLat = driverLoc.latitude,
                originLong = driverLoc.longitude,
                destLat = destination.latitude,
                destLong = destination.longitude
            )
            
            val calculationTime = System.currentTimeMillis() - startTime
            
            if (routeResult == null) {
                // Route calculation failed - preserve last valid values
                LiveRideMetrics.recordRouteFailure()
                return
            }
            
            val (distance, duration, polyline) = routeResult
            
            // Validate route data
            if (distance <= 0 || duration <= 0 || polyline.isEmpty()) {
                LiveRideMetrics.recordRouteFailure()
                return
            }
            
            // Update route polyline (road-snapped)
            _routePolyline.value = polyline
            lastValidRoute = polyline
            lastValidRouteOrigin = driverLoc
            lastValidRouteDestination = destination
            lastValidRouteTimestamp = now
            
            // Update ETA and distance with smoothing
            updateETAFromRoute(distance, duration)
            hasValidRouteData = true
            lastValidETASeconds = duration
            lastValidDistanceMeters = distance
            
            // Record successful route calculation
            LiveRideMetrics.recordRouteSuccess(calculationTime)
            
        } catch (e: Exception) {
            LiveRideMetrics.recordRouteFailure()
            // Preserve last valid values on error
        } finally {
            isRouteCalculationInProgress = false
        }
    }
    
    /**
     * Update ETA and distance from route calculation
     * Applies exponential moving average smoothing to ETA to avoid flicker
     */
    private fun updateETAFromRoute(distanceMeters: Int, durationSeconds: Int) {
        // Validate input
        if (distanceMeters <= 0 || durationSeconds <= 0) {
            return
        }
        
        // Format distance (no smoothing needed)
        val distanceKm = distanceMeters / 1000.0
        val formattedDistance = if (distanceKm >= 1.0) {
            String.format("%.1f km", distanceKm)
        } else {
            "${distanceMeters}m"
        }
        
        // Apply EMA smoothing to ETA
        val smoothedDuration = if (smoothedETA != null) {
            // Exponential Moving Average
            (etaSmoothingAlpha * durationSeconds + (1 - etaSmoothingAlpha) * smoothedETA!!).toInt()
        } else {
            smoothedETA = durationSeconds.toDouble()
            durationSeconds
        }
        smoothedETA = smoothedDuration.toDouble()
        
        // Format duration
        val minutes = smoothedDuration / 60
        val hours = minutes / 60
        val formattedETA = if (hours > 0) {
            "${hours}h ${minutes % 60}m"
        } else {
            "${minutes} min"
        }
        
        // Update values atomically
        _distance.value = formattedDistance
        _estimatedTime.value = formattedETA
        
        // Store as last valid values
        lastValidDistance = formattedDistance
        lastValidETA = formattedETA
    }
    
    /**
     * Update covered path by projecting driver position onto route
     * Slices the route polyline up to the projection point
     * Production-ready: enforces monotonic progress to prevent backwards movement
     */
    private fun updateCoveredPath(driverLocation: LatLng) {
        val fullRoute = _routePolyline.value
        if (fullRoute.isEmpty() || fullRoute.size < 2) {
            _coveredPath.value = emptyList()
            return
        }
        
        // Project driver onto route polyline (nearest segment + fraction)
        var bestSegmentIndex = 0
        var bestFraction = 0.0
        var minDistance = Double.MAX_VALUE
        
        // Find nearest segment and fraction along it
        for (i in 0 until fullRoute.lastIndex) {
            val segmentStart = fullRoute[i]
            val segmentEnd = fullRoute[i + 1]
            val projection = projectPointToSegment(driverLocation, segmentStart, segmentEnd)
            val d = FloatArray(1)
            Location.distanceBetween(
                driverLocation.latitude, driverLocation.longitude,
                projection.latitude, projection.longitude,
                d
            )
            val distance = d.firstOrNull()?.toDouble() ?: Double.MAX_VALUE
            if (distance < minDistance) {
                minDistance = distance
                bestSegmentIndex = i
                // Calculate fraction along segment
                val segmentLength = FloatArray(1)
                Location.distanceBetween(
                    segmentStart.latitude, segmentStart.longitude,
                    segmentEnd.latitude, segmentEnd.longitude,
                    segmentLength
                )
                val toStart = FloatArray(1)
                Location.distanceBetween(
                    segmentStart.latitude, segmentStart.longitude,
                    projection.latitude, projection.longitude,
                    toStart
                )
                bestFraction = if (segmentLength[0] > 0) {
                    (toStart[0] / segmentLength[0]).toDouble().coerceIn(0.0, 1.0)
                } else {
                    0.0
                }
            }
        }

        // Calculate progress in meters along route
        var progressMeters = 0.0
        for (i in 0 until bestSegmentIndex) {
            val d = FloatArray(1)
            Location.distanceBetween(
                fullRoute[i].latitude, fullRoute[i].longitude,
                fullRoute[i + 1].latitude, fullRoute[i + 1].longitude,
                d
            )
            progressMeters += d.firstOrNull()?.toDouble() ?: 0.0
        }
        // Add fraction of current segment
        if (bestSegmentIndex < fullRoute.lastIndex) {
            val d = FloatArray(1)
            Location.distanceBetween(
                fullRoute[bestSegmentIndex].latitude, fullRoute[bestSegmentIndex].longitude,
                fullRoute[bestSegmentIndex + 1].latitude, fullRoute[bestSegmentIndex + 1].longitude,
                d
            )
            progressMeters += (d.firstOrNull()?.toDouble() ?: 0.0) * bestFraction
        }

        // Enforce monotonic progress (ignore small backwards jumps, allow large reversals)
        val progressDelta = progressMeters - lastProgressMeters
        if (progressDelta < -progressBackstepThreshold) {
            // Large intentional reversal - allow it
            lastProgressMeters = progressMeters
            projectionBackstepCount++
        } else if (progressDelta < 0) {
            // Small backwards jump - ignore it (GPS noise or route recalculation)
            return
        } else {
            // Forward progress - update
            lastProgressMeters = progressMeters
        }

        // Slice the route up to projection point
        val sliceEndIndex = if (bestFraction > 0.5) {
            bestSegmentIndex + 1
        } else {
            bestSegmentIndex
        }.coerceAtMost(fullRoute.size - 1)
        
        _coveredPath.value = fullRoute.subList(0, sliceEndIndex + 1)
    }

    /**
     * Project a point onto a line segment
     */
    private fun projectPointToSegment(point: LatLng, segmentStart: LatLng, segmentEnd: LatLng): LatLng {
        val dx = segmentEnd.longitude - segmentStart.longitude
        val dy = segmentEnd.latitude - segmentStart.latitude
        val d2 = dx * dx + dy * dy
        
        if (d2 == 0.0) return segmentStart
        
        val t = ((point.longitude - segmentStart.longitude) * dx + 
                (point.latitude - segmentStart.latitude) * dy) / d2
        val clampedT = t.coerceIn(0.0, 1.0)
        
        return LatLng(
            segmentStart.latitude + clampedT * dy,
            segmentStart.longitude + clampedT * dx
        )
    }

    /**
     * Check if driver is inside airport/campus and update message
     */
    private fun checkAirportCampus(location: LatLng) {
        val site = airportCampusService.isInsideAirportOrCampus(location)
        if (site != null && site != currentAirportSite) {
            currentAirportSite = site
            val message = airportCampusService.getTerminalMessage(site)
            _airportMessage.value = message
        } else if (site == null && currentAirportSite != null) {
            currentAirportSite = null
            _airportMessage.value = null
        }
    }
    
    /**
     * REMOVED: updateEstimatedMetrics() - This method used straight-line calculation
     * which caused incorrect and unstable ETA/distance values.
     * 
     * ETA and distance should ONLY be calculated from route data via Google Directions API.
     * If route calculation fails, we preserve the last valid values instead of showing
     * incorrect straight-line estimates.
     */
    
    /**
     * User interacted with map (manual pan/zoom)
     */
    fun onMapInteraction() {
        _userHasInteractedWithMap.value = true
    }
    
    /**
     * Reset map interaction (allow auto-follow again)
     */
    fun resetMapInteraction() {
        _userHasInteractedWithMap.value = false
        updateMapRegion()
    }
    
    /**
     * Initialize with active ride data
     */
    fun initializeWithRide(ride: ActiveRide) {
        _activeRide.value = ride
        handleActiveRideUpdate(ride)
    }

    /**
     * Initialize with booking ID - ensures active ride data is available
     */
    fun initializeWithBookingId(bookingId: String) {
        // Join booking room for real-time updates
        bookingId.toIntOrNull()?.let { bookingIdInt ->
            socketService.joinBookingRoom(bookingIdInt)
        }

        // If we already have active ride data for this booking, ensure it's properly initialized
        val currentRide = _activeRide.value
        if (currentRide != null && currentRide.bookingId == bookingId) {
            // Data is already available and matches, ensure proper initialization
            handleActiveRideUpdate(currentRide)
        }
    }
    
    /**
     * Submit driver feedback
     */
    fun submitDriverFeedback(
        bookingId: Int,
        rating: Int,
        feedback: String?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = bookingService.submitDriverFeedback(bookingId, rating, feedback)
            result.fold(
                onSuccess = { onSuccess() },
                onFailure = { error ->
                    onFailure(error.message ?: "Failed to submit feedback")
                }
            )
        }
    }
    
    /**
     * Get instrumentation metrics for monitoring
     */
    fun getMetrics(): Map<String, Any> {
        val roadsMetrics = roadsSnappingService.getMetrics()
        return mapOf(
            "projectionBackstepCount" to projectionBackstepCount,
            "roadsSnapSuccessRate" to (roadsMetrics["snapSuccessRate"] ?: 0.0),
            "roadsSnapAvgLatencyMs" to (roadsMetrics["snapAvgLatencyMs"] ?: 0L)
        )
    }

    override fun onCleared() {
        super.onCleared()
        updateJob?.cancel()
        routeCalculationJob?.cancel()
        
        // NOTE: Do NOT leave booking room here - SocketService manages room membership
        // based on active ride state. Leaving the room prematurely would stop
        // driver.location.updates even when the ride is still active.
        // Room cleanup is handled by SocketService when the ride actually ends.
    }
}

/**
 * Data class for map region
 */
data class MapRegion(
    val center: LatLng,
    val zoom: Float
)
