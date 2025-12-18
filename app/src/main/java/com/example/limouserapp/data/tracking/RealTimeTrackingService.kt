package com.example.limouserapp.data.tracking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.limouserapp.data.location.LocationManager
import com.example.limouserapp.data.model.dashboard.CarLocation
import com.example.limouserapp.data.model.dashboard.LocationData
import com.example.limouserapp.data.socket.SocketService
import com.example.limouserapp.data.storage.TokenManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real-time tracking service that combines location services and Socket.IO
 * Manages live tracking of user location and driver locations
 */
@Singleton
class RealTimeTrackingService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationManager: LocationManager,
    private val socketService: SocketService,
    private val tokenManager: TokenManager
) {
    
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()
    
    private val _userLocation = MutableStateFlow<LocationData?>(null)
    val userLocation: StateFlow<LocationData?> = _userLocation.asStateFlow()
    
    private val _driverLocations = MutableStateFlow<List<CarLocation>>(emptyList())
    val driverLocations: StateFlow<List<CarLocation>> = _driverLocations.asStateFlow()
    
    private val _trackingError = MutableStateFlow<String?>(null)
    val trackingError: StateFlow<String?> = _trackingError.asStateFlow()
    
    private var trackingJob: Job? = null
    private var locationUpdateJob: Job? = null
    private var driverLocationJob: Job? = null
    
    companion object {
        private const val LOCATION_UPDATE_INTERVAL = 5000L // 5 seconds
        private const val DRIVER_LOCATION_UPDATE_INTERVAL = 3000L // 3 seconds
    }
    
    /**
     * Start real-time tracking
     */
    fun startTracking() {
        if (_isTracking.value) {
            Timber.w("Tracking already started")
            return
        }
        
        if (!hasLocationPermission()) {
            _trackingError.value = "Location permission not granted"
            return
        }
        
        if (!locationManager.isLocationServiceAvailable()) {
            _trackingError.value = "Location services not available"
            return
        }
        
        try {
            _isTracking.value = true
            _trackingError.value = null
            
            // Connect to Socket.IO
            socketService.connect()
            
            // Start location tracking
//            startLocationTracking()
            
            // Start driver location updates
            startDriverLocationUpdates()
            
            Timber.d("Real-time tracking started successfully")
            
        } catch (e: Exception) {
            Timber.e(e, "Error starting real-time tracking")
            _trackingError.value = e.message ?: "Unknown error"
            _isTracking.value = false
        }
    }
    
    /**
     * Stop real-time tracking
     */
    fun stopTracking() {
        if (!_isTracking.value) {
            Timber.w("Tracking not started")
            return
        }
        
        try {
            _isTracking.value = false
            
            // Cancel all tracking jobs
            trackingJob?.cancel()
            locationUpdateJob?.cancel()
            driverLocationJob?.cancel()
            
            // Disconnect from Socket.IO
            socketService.disconnect()
            
            // Clear data
            _userLocation.value = null
            _driverLocations.value = emptyList()
            _trackingError.value = null
            
            Timber.d("Real-time tracking stopped")
            
        } catch (e: Exception) {
            Timber.e(e, "Error stopping real-time tracking")
        }
    }
    
    /**
     * Start location tracking
     */
    private fun startLocationTracking() {
        locationUpdateJob = CoroutineScope(Dispatchers.IO).launch {
            while (_isTracking.value) {
                try {
                    val locationResult = locationManager.getCurrentLocation()
                    locationResult.fold(
                        onSuccess = { location ->
                            _userLocation.value = location
                            
                            // Send location to server if connected
                            if (socketService.isConnected()) {
//                                sendUserLocationToServer(location)
                            }
                        },
                        onFailure = { error ->
                            Timber.e(error, "Error getting user location")
                            _trackingError.value = "Location update failed: ${error.message}"
                        }
                    )
                } catch (e: Exception) {
                    Timber.e(e, "Error in location tracking loop")
                }
                
                delay(LOCATION_UPDATE_INTERVAL)
            }
        }
    }
    
    /**
     * Start driver location updates
     */
    private fun startDriverLocationUpdates() {
        driverLocationJob = CoroutineScope(Dispatchers.IO).launch {
            // Observe driver location updates from Socket.IO
            socketService.driverLocations.collect { driverUpdates ->
                val carLocations = driverUpdates.map { update ->
                    CarLocation(
                        id = update.driverId,
                        latitude = update.latitude,
                        longitude = update.longitude,
                        driverId = update.driverId,
                        driverName = null, // Will be updated from booking info
                        vehicleType = null, // Will be updated from booking info
                        isAvailable = true,
                        lastUpdated = update.timestamp
                    )
                }
                _driverLocations.value = carLocations
            }
        }
    }
    
    /**
     * Send user location to server
     */
    private fun sendUserLocationToServer(location: LocationData) {
        try {
            // TODO: Implement sending user location to server
            // This would typically be done through Socket.IO or API
            Timber.d("Sending user location to server: ${location.latitude}, ${location.longitude}")
        } catch (e: Exception) {
            Timber.e(e, "Error sending user location to server")
        }
    }
    
    /**
     * Request driver location for specific booking
     */
    fun requestDriverLocation(bookingId: Int) {
        if (!_isTracking.value) {
            Timber.w("Tracking not started, cannot request driver location")
            return
        }
        
        try {
            socketService.requestDriverLocation(bookingId)
            Timber.d("Requested driver location for booking: $bookingId")
        } catch (e: Exception) {
            Timber.e(e, "Error requesting driver location")
            _trackingError.value = "Failed to request driver location"
        }
    }
    
    /**
     * Join booking room for real-time updates
     */
    fun joinBookingRoom(bookingId: Int) {
        if (!_isTracking.value) {
            Timber.w("Tracking not started, cannot join booking room")
            return
        }
        
        try {
            socketService.joinBookingRoom(bookingId)
            Timber.d("Joined booking room: $bookingId")
        } catch (e: Exception) {
            Timber.e(e, "Error joining booking room")
            _trackingError.value = "Failed to join booking room"
        }
    }
    
    /**
     * Leave booking room
     */
    fun leaveBookingRoom(bookingId: Int) {
        try {
            socketService.leaveBookingRoom(bookingId)
            Timber.d("Left booking room: $bookingId")
        } catch (e: Exception) {
            Timber.e(e, "Error leaving booking room")
        }
    }
    
    /**
     * Get current user location
     */
    suspend fun getCurrentUserLocation(): LocationData? {
        return try {
            val result = locationManager.getCurrentLocation()
            result.fold(
                onSuccess = { location ->
                    _userLocation.value = location
                    location
                },
                onFailure = { error ->
                    Timber.e(error, "Error getting current location")
                    _trackingError.value = error.message
                    null
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Error getting current location")
            _trackingError.value = e.message
            null
        }
    }
    
    /**
     * Get driver location for specific booking
     */
    fun getDriverLocationForBooking(bookingId: Int): CarLocation? {
        return _driverLocations.value.find { 
            it.driverId == bookingId.toString() 
        }
    }
    
    /**
     * Check if location permission is granted
     */
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Clear tracking error
     */
    fun clearError() {
        _trackingError.value = null
    }
    
    /**
     * Check if tracking is active
     */
    fun isTrackingActive(): Boolean {
        return _isTracking.value
    }
    
    /**
     * Get tracking status info
     */
    fun getTrackingStatus(): String {
        return buildString {
            append("Tracking: ${_isTracking.value}")
            append(", Socket: ${socketService.isConnected()}")
            append(", Location: ${_userLocation.value != null}")
            append(", Drivers: ${_driverLocations.value.size}")
            _trackingError.value?.let { error ->
                append(", Error: $error")
            }
        }
    }
}
