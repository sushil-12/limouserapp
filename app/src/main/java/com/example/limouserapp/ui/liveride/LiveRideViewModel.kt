package com.example.limouserapp.ui.liveride

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.limouserapp.data.socket.ActiveRide
import com.example.limouserapp.data.socket.DriverLocationUpdate
import com.example.limouserapp.data.socket.SocketService
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import timber.log.Timber
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
    private val socketService: SocketService
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
    
    // Estimated time and distance
    private val _estimatedTime = MutableStateFlow("Calculating...")
    val estimatedTime: StateFlow<String> = _estimatedTime.asStateFlow()
    
    private val _distance = MutableStateFlow("...")
    val distance: StateFlow<String> = _distance.asStateFlow()
    
    // Status flags
    private val _isDriverOnLocation = MutableStateFlow(false)
    val isDriverOnLocation: StateFlow<Boolean> = _isDriverOnLocation.asStateFlow()

    // Proximity detection (soft geofence) for UI phase hints
    private val _pickupArrivalDetected = MutableStateFlow(false)
    val pickupArrivalDetected: StateFlow<Boolean> = _pickupArrivalDetected.asStateFlow()

    private val _dropoffArrivalDetected = MutableStateFlow(false)
    val dropoffArrivalDetected: StateFlow<Boolean> = _dropoffArrivalDetected.asStateFlow()

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
        dropoffArrivalDetected
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
            dropoffArrivalDetected = values[9] as Boolean
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
    
    // Debouncing mechanism to prevent excessive view updates
    private var lastViewUpdateTime = 0L
    private val viewUpdateThrottleInterval = 500L // 500ms throttle
    private var lastCarLocation: LatLng? = null
    
    // Throttle update job
    private var updateJob: Job? = null
    
    init {
        observeActiveRide()
        observeDriverLocations()
        startThrottledUpdates()
    }
    
    /**
     * Observe active ride from SocketService
     */
    private fun observeActiveRide() {
        viewModelScope.launch {
            socketService.activeRide
                .filterNotNull()
                .collect { ride ->
                    Timber.d("📱 Active ride received: ${ride.bookingId}, status: ${ride.status}")
                    handleActiveRideUpdate(ride)
                }
        }
    }
    
    /**
     * Observe driver location updates from SocketService
     */
    private fun observeDriverLocations() {
        viewModelScope.launch {
            socketService.driverLocations
                .debounce(500) // Throttle updates to 500ms
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
     * Throttled view update to prevent excessive refreshes
     */
    private fun throttledViewUpdate() {
        val now = System.currentTimeMillis()
        if (now - lastViewUpdateTime < viewUpdateThrottleInterval) {
            return
        }
        lastViewUpdateTime = now
        
        _activeRide.value?.let { ride ->
            updateStatusUI(ride.status)
            updateEstimatedMetrics(ride)
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
        
        // Update status message
        updateStatusUI(ride.status)

        // Proximity detection (if we already have driver location)
        _driverLocation.value?.let { driverLoc ->
            updateProximity(ride, driverLoc)
        }
        
        // Update map region if user hasn't interacted
        if (!_userHasInteractedWithMap.value) {
            updateMapRegion()
        }
        
        // Join booking room for real-time updates
        ride.bookingId.toIntOrNull()?.let { bookingId ->
            socketService.joinBookingRoom(bookingId)
        }
        
        Timber.d("🚗 Active ride processed: ${ride.bookingId}, Status: ${ride.status}")
    }
    
    /**
     * Process driver location updates from socket
     */
    private fun processDriverLocationUpdates(locations: List<DriverLocationUpdate>) {
        val currentRide = _activeRide.value ?: return
        
        locations
            .firstOrNull { it.bookingId == currentRide.bookingId }
            ?.let { update ->
                val newLocation = LatLng(update.latitude, update.longitude)
                
                // Only update if location changed significantly (> 10 meters)
                if (shouldUpdateLocation(newLocation)) {
                    _driverLocation.value = newLocation
                    lastCarLocation = newLocation
                    _isReceivingSocketUpdates.value = true

                    updateProximity(currentRide, newLocation)
                    
                    // Update map region if user hasn't interacted
                    if (!_userHasInteractedWithMap.value) {
                        updateMapRegion()
                    }
                    
                    Timber.d("📍 Driver location updated: ${update.latitude}, ${update.longitude}")
                }
            }
    }
    
    /**
     * Check if location should be updated (distance-based throttling)
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
        
        // Update if moved more than 10 meters
        return results[0] > 10
    }
    
    /**
     * Update map region to show driver and route
     */
    private fun updateMapRegion() {
        val driverLoc = _driverLocation.value ?: return
        val pickupLoc = _pickupLocation.value ?: return
        val dropoffLoc = _dropoffLocation.value ?: return
        
        val bounds = LatLngBounds.builder()
            .include(driverLoc)
            .include(pickupLoc)
            .include(dropoffLoc)
            .build()
        
        val width = bounds.northeast.longitude - bounds.southwest.longitude
        val height = bounds.northeast.latitude - bounds.southwest.latitude
        val maxDimension = maxOf(width, height)
        
        // Add padding for better view
        val scale = (maxDimension * 1.2).coerceIn(0.01, 0.1)
        
        _mapRegion.value = MapRegion(
            center = LatLng(driverLoc.latitude, driverLoc.longitude),
            zoom = calculateZoomLevel(scale)
        )
    }
    
    /**
     * Calculate zoom level from scale
     */
    private fun calculateZoomLevel(scale: Double): Float {
        val base = 2.0
        val zoom = -1.0 * (kotlin.math.log10(scale) / kotlin.math.log10(2.0) - 1.0)
        return zoom.toFloat().coerceIn(10f, 20f)
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
     * Update estimated time and distance
     */
    private fun updateEstimatedMetrics(ride: ActiveRide) {
        val driverLoc = _driverLocation.value
        val dropoffLoc = _dropoffLocation.value
        
        if (driverLoc != null && dropoffLoc != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                driverLoc.latitude,
                driverLoc.longitude,
                dropoffLoc.latitude,
                dropoffLoc.longitude,
                results
            )
            
            val distanceKm = results[0] / 1000
            _distance.value = String.format("%.1f km", distanceKm)
            
            // Estimate time (assuming 60 km/h average speed)
            val estimatedMinutes = (distanceKm / 60 * 60).toInt()
            _estimatedTime.value = "${estimatedMinutes} min"
        }
    }
    
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
            Timber.d("LiveRideViewModel: Joined booking room for booking $bookingId")
        }

        // If we already have active ride data for this booking, ensure it's properly initialized
        val currentRide = _activeRide.value
        if (currentRide != null && currentRide.bookingId == bookingId) {
            // Data is already available and matches, ensure proper initialization
            handleActiveRideUpdate(currentRide)
        } else {
            // No data or wrong booking, wait for socket data
            Timber.d("LiveRideViewModel: No active ride data for booking $bookingId, waiting for socket data")
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        updateJob?.cancel()
        
        // Leave booking room
        _activeRide.value?.bookingId?.toIntOrNull()?.let { bookingId ->
            socketService.leaveBookingRoom(bookingId)
        }
    }
}

/**
 * Data class for map region
 */
data class MapRegion(
    val center: LatLng,
    val zoom: Float
)
