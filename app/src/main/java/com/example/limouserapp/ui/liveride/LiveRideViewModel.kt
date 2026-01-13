package com.example.limouserapp.ui.liveride

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.limouserapp.data.socket.ActiveRide
import com.example.limouserapp.data.socket.DriverLocationUpdate
import com.example.limouserapp.data.socket.SocketService
import com.example.limouserapp.data.service.DirectionsService
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.LinkedHashMap
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * ViewModel for Live Ride In Progress screen
 * Handles real-time driver location tracking, status updates, and map management
 * Optimized for production with efficient updates and memory management
 */
@HiltViewModel
class LiveRideViewModel @Inject constructor(
    private val socketService: SocketService,
    private val directionsService: DirectionsService
) : ViewModel() {

    companion object {
        // Configurable constants for production tuning
        private const val LOCATION_DEBOUNCE_MS = 1000L // Increased from 500ms to reduce updates
        private const val VIEW_UPDATE_THROTTLE_MS = 1000L // From 500ms
        private const val ROUTE_CACHE_SIZE = 10
        private const val ROUTE_CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes
        private const val ROUTE_API_TIMEOUT_MS = 10000L // 10s
        private const val PICKUP_PROXIMITY_M = 100f
        private const val DROPOFF_PROXIMITY_M = 50f
        private const val MIN_LOCATION_UPDATE_DISTANCE_M = 10f
        private const val FALLBACK_CITY_SPEED_MS = 13.89f // 50 km/h
        private const val FALLBACK_HIGHWAY_SPEED_MS = 22.22f // 80 km/h (for distances >5km)
        private const val COORD_PRECISION = 6 // Decimal places for cache keys
        private const val MAP_PADDING_PERCENT = 1.2f // 20% padding
        private const val MIN_ZOOM = 10f
        private const val MAX_ZOOM = 20f
    }

    // Live Ride Data
    private val _activeRide = MutableStateFlow<ActiveRide?>(null)
    val activeRide: StateFlow<ActiveRide?> = _activeRide.asStateFlow()

    // Driver Location
    private val _driverLocation = MutableStateFlow<LatLng?>(null)
    val driverLocation: StateFlow<LatLng?> = _driverLocation.asStateFlow() as StateFlow<LatLng?>

    // Pickup and Dropoff locations
    private val _pickupLocation = MutableStateFlow<LatLng?>(null)
    val pickupLocation: StateFlow<LatLng?> = _pickupLocation.asStateFlow() as StateFlow<LatLng?>

    private val _dropoffLocation = MutableStateFlow<LatLng?>(null)
    val dropoffLocation: StateFlow<LatLng?> = _dropoffLocation.asStateFlow() as StateFlow<LatLng?>

    // Map region and camera
    private val _mapRegion = MutableStateFlow<MapRegion?>(null)
    val mapRegion: StateFlow<MapRegion?> = _mapRegion.asStateFlow()

    // Route polylines
    private val _driverToPickupRoute = MutableStateFlow<List<LatLng>>(emptyList())
    val driverToPickupRoute: StateFlow<List<LatLng>> = _driverToPickupRoute.asStateFlow()

    private val _pickupToDropoffRoute = MutableStateFlow<List<LatLng>>(emptyList())
    val pickupToDropoffRoute: StateFlow<List<LatLng>> = _pickupToDropoffRoute.asStateFlow()

    // Route cache: LRU with TTL (key: rounded "lat1,lng1-lat2,lng2", value: (route, distance, duration, timestamp))
    // Use LinkedHashMap for LRU eviction
    private val routeCache = LinkedHashMap<String, Quadruple<List<LatLng>, Int, Int, Long>>(ROUTE_CACHE_SIZE, 0.75f, true)
    private val routeCacheLock = Mutex()

    // Ride Status
    private val _statusMessage = MutableStateFlow("Driver is on the way")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    // Estimated time and distance
    private val _estimatedTime = MutableStateFlow("Calculating...")
    val estimatedTime: StateFlow<String> = _estimatedTime.asStateFlow()

    private val _distance = MutableStateFlow("...")
    val distance: StateFlow<String> = _distance.asStateFlow()

    // Status flags
    private val _isDriverOnLocation = MutableStateFlow(false)
    val isDriverOnLocation: StateFlow<Boolean> = _isDriverOnLocation.asStateFlow()

    // Proximity detection
    private val _pickupArrivalDetected = MutableStateFlow(false)
    val pickupArrivalDetected: StateFlow<Boolean> = _pickupArrivalDetected.asStateFlow()

    private val _dropoffArrivalDetected = MutableStateFlow(false)
    val dropoffArrivalDetected: StateFlow<Boolean> = _dropoffArrivalDetected.asStateFlow()

    // Error state for UI (e.g., show toast)
    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState.asStateFlow()

    // Single uiState
    val uiState: StateFlow<LiveRideUiState> = combine(
        activeRide,
        driverLocation,
        pickupLocation,
        dropoffLocation,
        statusMessage,
        estimatedTime,
        distance,
        pickupArrivalDetected,
        dropoffArrivalDetected,
        driverToPickupRoute,
        pickupToDropoffRoute,
        errorState
    ) { values: Array<Any?> ->
        LiveRideUiState(
            activeRide = values[0] as ActiveRide?,
            driverLocation = values[1] as LatLng?,
            pickupLocation = values[2] as LatLng?,
            dropoffLocation = values[3] as LatLng?,
            statusMessage = values[4] as String,
            estimatedTime = values[5] as String,
            distance = values[6] as String,
            pickupArrivalDetected = values[7] as Boolean,
            dropoffArrivalDetected = values[8] as Boolean,
            driverToPickupRoute = values[9] as List<LatLng>,
            pickupToDropoffRoute = values[10] as List<LatLng>,
            error = values[11] as String?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LiveRideUiState()
    )

    // Map interaction tracking
    private val _userHasInteractedWithMap = MutableStateFlow(false)
    val userHasInteractedWithMap: StateFlow<Boolean> = _userHasInteractedWithMap.asStateFlow()

    // Track if receiving real-time updates
    private val _isReceivingSocketUpdates = MutableStateFlow(false)
    val isReceivingSocketUpdates: StateFlow<Boolean> = _isReceivingSocketUpdates.asStateFlow()

    // Debouncing mechanism
    private var lastViewUpdateTime = 0L
    private var lastCarLocation: LatLng? = null

    // Throttle update job
    private var updateJob: Job? = null

    // Route calculation jobs (cancellable)
    private var driverToPickupRouteJob: Job? = null
    private var pickupToDropoffRouteJob: Job? = null

    init {
        observeActiveRide()
        observeDriverLocations()
        startThrottledUpdates()
        monitorSocketConnection() // New: Monitor for disconnects
    }

    /**
     * Monitor socket connection for reliability
     */
    private fun monitorSocketConnection() {
        viewModelScope.launch {
            socketService.connectionStatus.collect { status ->
                if (!status.isConnected) {
                    _errorState.value = "Socket disconnected. Retrying..."
                    _isReceivingSocketUpdates.value = false
                } else {
                    _errorState.value = null
                }
            }
        }
    }

    private fun observeActiveRide() {
        viewModelScope.launch {
            socketService.activeRide
                .filterNotNull()
                .collect { ride ->
                    if (isValidRide(ride)) { // New: Validate ride data
                        Timber.d("📱 Active ride received: ${ride.bookingId}, status: ${ride.status}")
                        handleActiveRideUpdate(ride)
                    } else {
                        Timber.w("Invalid ride data received: $ride")
                        _errorState.value = "Invalid ride data received"
                    }
                }
        }
    }

    private fun observeDriverLocations() {
        viewModelScope.launch {
            socketService.driverLocations
                .debounce(LOCATION_DEBOUNCE_MS)
                .collect { locations ->
                    if (locations.isNotEmpty()) {
                        processDriverLocationUpdates(locations)
                    }
                }
        }
    }

    private fun startThrottledUpdates() {
        updateJob = viewModelScope.launch {
            while (true) {
                delay(VIEW_UPDATE_THROTTLE_MS)
                throttledViewUpdate()
            }
        }
    }

    private fun throttledViewUpdate() {
        val now = System.currentTimeMillis()
        if (now - lastViewUpdateTime < VIEW_UPDATE_THROTTLE_MS) return
        lastViewUpdateTime = now

        _activeRide.value?.let { ride ->
            updateStatusUI(ride.status)
            updateEstimatedMetrics(ride)
        }
    }

    private fun handleActiveRideUpdate(ride: ActiveRide) {
        val previousStatus = _activeRide.value?.status
        val statusChanged = previousStatus != ride.status
        
        Timber.d("🔄 [ACTIVE_RIDE_UPDATE] ════════════════════════════════════════════════════")
        Timber.d("🔄 [ACTIVE_RIDE_UPDATE] Handling active ride update - bookingId: ${ride.bookingId}")
        Timber.d("🔄 [ACTIVE_RIDE_UPDATE] Status: '${ride.status}' (previous: '$previousStatus', changed: $statusChanged)")
        
        _activeRide.value = ride
        val pickup = LatLng(ride.pickupLatitude, ride.pickupLongitude)
        val dropoff = LatLng(ride.dropoffLatitude, ride.dropoffLongitude)
        val driverFromRide = if (isValidLatLng(LatLng(ride.driverLatitude, ride.driverLongitude))) {
            LatLng(ride.driverLatitude, ride.driverLongitude)
        } else null

        if (isValidLatLng(pickup)) _pickupLocation.value = pickup else Timber.w("Invalid pickup location")
        if (isValidLatLng(dropoff)) _dropoffLocation.value = dropoff else Timber.w("Invalid dropoff location")

        if (driverFromRide != null && !locationsMatch(driverFromRide, pickup, MIN_LOCATION_UPDATE_DISTANCE_M)) {
            _driverLocation.value = driverFromRide
            lastCarLocation = driverFromRide
        } else {
            Timber.d("Ignoring driver location from ride (invalid or matches pickup)")
        }

        if (statusChanged) {
            Timber.d("🔄 [ACTIVE_RIDE_UPDATE] ⚠️ STATUS CHANGED - Updating UI from '$previousStatus' to '${ride.status}'")
        }
        updateStatusUI(ride.status)
        updateRoutes()
        _driverLocation.value?.let { updateProximity(ride, it) }
        if (!_userHasInteractedWithMap.value) updateMapRegion()
        Timber.d("🔄 [ACTIVE_RIDE_UPDATE] ════════════════════════════════════════════════════")
    }

    private suspend fun processDriverLocationUpdates(locations: List<DriverLocationUpdate>) {
        Timber.d("📍 [DRIVER_UPDATE] ════════════════════════════════════════════════════")
        Timber.d("📍 [DRIVER_UPDATE] Processing driver location updates: ${locations.size} updates")
        
        val currentRide = _activeRide.value ?: return Timber.w("📍 [DRIVER_UPDATE] No active ride - skipping location updates")

        val matchingUpdate = findMatchingUpdate(locations, currentRide) ?: return Timber.w("📍 [DRIVER_UPDATE] No matching driver update for booking ${currentRide.bookingId}")

        val newLocation = LatLng(matchingUpdate.latitude, matchingUpdate.longitude)
        if (!isValidLatLng(newLocation)) return Timber.w("📍 [DRIVER_UPDATE] Invalid driver location: $newLocation")

        val previousLoc = _driverLocation.value
        Timber.d("📍 [DRIVER_UPDATE] Previous driver location: $previousLoc")
        Timber.d("📍 [DRIVER_UPDATE] New driver location: lat=${newLocation.latitude}, lng=${newLocation.longitude}")
        
        if (shouldUpdateLocation(newLocation)) {
            val distanceMoved = previousLoc?.let { calculateDistance(it, newLocation) } ?: 0f
            Timber.d("📍 [DRIVER_UPDATE] Location changed by ${distanceMoved.toInt()}m - updating")
            
            _driverLocation.value = newLocation
            lastCarLocation = newLocation
            _isReceivingSocketUpdates.value = true

            if (previousLoc != null && distanceMoved > 50f) {
                Timber.d("📍 [DRIVER_UPDATE] Driver moved ${distanceMoved.toInt()}m (>50m) - clearing cache")
                clearDriverPickupCache() // Clear cache on significant move
            }

            Timber.d("📍 [DRIVER_UPDATE] Triggering route update...")
            updateRoutes()
            updateProximity(currentRide, newLocation)
            if (!_userHasInteractedWithMap.value) updateMapRegion()
            
            Timber.d("📍 [DRIVER_UPDATE] Driver location update processing complete")
        } else {
            Timber.d("📍 [DRIVER_UPDATE] Location unchanged (within threshold) - skipping update")
        }
        Timber.d("📍 [DRIVER_UPDATE] ════════════════════════════════════════════════════")
    }

    // Helper: Find matching update (refactored for readability)
    private fun findMatchingUpdate(locations: List<DriverLocationUpdate>, ride: ActiveRide): DriverLocationUpdate? {
        return locations.firstOrNull { update ->
            val updateBookingId = update.bookingId?.trim().orEmpty()
            val rideBookingId = ride.bookingId.trim()
            val updateDriverId = update.driverId.trim()
            val rideDriverId = ride.driverId.trim()

            updateBookingId == rideBookingId || (updateBookingId.isEmpty() && updateDriverId == rideDriverId)
        } ?: locations.firstOrNull { it.driverId.trim() == ride.driverId.trim() } // Fallback
    }

    private fun shouldUpdateLocation(newLocation: LatLng): Boolean {
        lastCarLocation?.let { last ->
            return calculateDistance(last, newLocation) > MIN_LOCATION_UPDATE_DISTANCE_M
        }
        return true // Always update if first
    }

    private fun updateMapRegion() {
        val driverLoc = _driverLocation.value ?: return
        val pickupLoc = _pickupLocation.value ?: return
        val dropoffLoc = _dropoffLocation.value ?: return

        val builder = LatLngBounds.builder()
            .include(driverLoc)
            .include(pickupLoc)
            .include(dropoffLoc)

        val status = _activeRide.value?.status
        addRouteToBounds(builder, _driverToPickupRoute.value, status == "en_route_pu")
        addRouteToBounds(builder, _pickupToDropoffRoute.value, status != "en_route_pu")

        try {
            val bounds = builder.build()
            val width = abs(bounds.northeast.longitude - bounds.southwest.longitude)
            val height = abs(bounds.northeast.latitude - bounds.southwest.latitude)
            val scale = max(width, height) * MAP_PADDING_PERCENT

            _mapRegion.value = MapRegion(
                center = bounds.center,
                zoom = calculateZoomLevel(scale).coerceIn(MIN_ZOOM, MAX_ZOOM)
            )
        } catch (e: IllegalStateException) {
            Timber.e(e, "Empty bounds - defaulting to wide view")
            _mapRegion.value = MapRegion(LatLng(0.0, 0.0), MIN_ZOOM) // Default
        }
    }

    // Helper: Sample large routes for performance
    private fun addRouteToBounds(builder: LatLngBounds.Builder, route: List<LatLng>, includeFull: Boolean) {
        if (route.isEmpty()) return
        if (route.size > 500 && !includeFull) {
            route.filterIndexed { index, _ -> index % 10 == 0 }.forEach { builder.include(it) }
        } else {
            route.forEach { builder.include(it) }
        }
    }

    // Improved zoom: Proper log2 for map scale
    private fun calculateZoomLevel(scale: Double): Float {
        return (18 - log2(scale * 256 / 360)).toFloat() // Adjusted for mercator projection
    }

    private fun updateRoutes() {
        Timber.d("🔄 [ROUTE_UPDATE] ════════════════════════════════════════════════════")
        Timber.d("🔄 [ROUTE_UPDATE] UPDATE ROUTES CALLED")
        
        cancelRouteJobs() // Cancel previous

        val driverLoc = _driverLocation.value ?: return Timber.d("🔄 [ROUTE_UPDATE] No driver loc - skipping routes")
        val pickupLoc = _pickupLocation.value ?: return Timber.d("🔄 [ROUTE_UPDATE] No pickup loc - skipping routes")
        val dropoffLoc = _dropoffLocation.value ?: return Timber.d("🔄 [ROUTE_UPDATE] No dropoff loc - skipping routes")
        val status = _activeRide.value?.status ?: return Timber.d("🔄 [ROUTE_UPDATE] No status - skipping routes")
        
        Timber.d("🔄 [ROUTE_UPDATE] Status: $status")
        Timber.d("🔄 [ROUTE_UPDATE] Driver: lat=${driverLoc.latitude}, lng=${driverLoc.longitude}")
        Timber.d("🔄 [ROUTE_UPDATE] Pickup: lat=${pickupLoc.latitude}, lng=${pickupLoc.longitude}")
        Timber.d("🔄 [ROUTE_UPDATE] Dropoff: lat=${dropoffLoc.latitude}, lng=${dropoffLoc.longitude}")
        Timber.d("🔄 [ROUTE_UPDATE] Current ETA: ${_estimatedTime.value}, Distance: ${_distance.value}")
        Timber.d("🔄 [ROUTE_UPDATE] ════════════════════════════════════════════════════")

        when (status) {
            "en_route_pu" -> {
                val distanceCheck = calculateDistance(driverLoc, pickupLoc)
                Timber.d("🛣️ [ROUTE] en_route_pu: Driver is ${distanceCheck.toInt()}m from pickup")
                
                if (!locationsMatch(driverLoc, pickupLoc, MIN_LOCATION_UPDATE_DISTANCE_M)) {
                    Timber.d("🛣️ [ROUTE] Driver not at pickup - calculating fallback first, then route")
                    calculateFallbackETAAndDistance(driverLoc, pickupLoc) // Immediate feedback
                    
                    Timber.d("🛣️ [ROUTE] Starting driver-to-pickup route calculation...")
                    driverToPickupRouteJob = launchRouteJob("driver-pickup", driverLoc, pickupLoc) { route, dist, dur ->
                        Timber.d("🛣️ [ROUTE_CALLBACK] ════════════════════════════════════════════════════")
                        Timber.d("🛣️ [ROUTE_CALLBACK] DRIVER-TO-PICKUP ROUTE CALCULATION COMPLETE")
                        Timber.d("🛣️ [ROUTE_CALLBACK] Route points: ${route.size}")
                        Timber.d("🛣️ [ROUTE_CALLBACK] Distance: ${dist}m (${dist/1000.0} km)")
                        Timber.d("🛣️ [ROUTE_CALLBACK] Duration: ${dur}s (${dur/60.0} min)")
                        Timber.d("🛣️ [ROUTE_CALLBACK] ⚠️ THIS WILL UPDATE ETA/DISTANCE")
                        Timber.d("🛣️ [ROUTE_CALLBACK] Current ETA before update: ${_estimatedTime.value}, Distance: ${_distance.value}")
                        Timber.d("🛣️ [ROUTE_CALLBACK] ════════════════════════════════════════════════════")
                        
                        _driverToPickupRoute.value = route
                        updateETAAndDistance(dist, dur)
                        
                        Timber.d("🛣️ [ROUTE_CALLBACK] ETA updated after route calculation")
                    }
                } else {
                    Timber.d("🛣️ [ROUTE] Driver is at pickup location - setting 'Arrived'")
                    _estimatedTime.value = "Arrived"
                    _distance.value = "0m"
                }

                Timber.d("🛣️ [ROUTE] Starting preview route (pickup-to-dropoff) calculation...")
                pickupToDropoffRouteJob = launchRouteJob("pickup-dropoff", pickupLoc, dropoffLoc) { route, _, _ ->
                    Timber.d("🛣️ [PREVIEW_ROUTE] Preview route calculated: ${route.size} points")
                    Timber.d("🛣️ [PREVIEW_ROUTE] NOTE: Preview route does NOT update ETA/distance")
                    _pickupToDropoffRoute.value = route // Preview only, no ETA update
                }
            }
            "on_location", "en_route_do" -> {
                Timber.d("🛣️ [ROUTE] Status is $status - calculating pickup-to-dropoff route")
                pickupToDropoffRouteJob = launchRouteJob("pickup-dropoff", pickupLoc, dropoffLoc) { route, dist, dur ->
                    Timber.d("🛣️ [ROUTE_CALLBACK] Pickup-to-dropoff route calculated")
                    Timber.d("🛣️ [ROUTE_CALLBACK] Distance: ${dist}m, Duration: ${dur}s")
                    Timber.d("🛣️ [ROUTE_CALLBACK] ⚠️ THIS WILL UPDATE ETA/DISTANCE")
                    _pickupToDropoffRoute.value = route
                    updateETAAndDistance(dist, dur)
                }
            }
        }
    }

    // Helper: Launch route job with timeout
    private fun launchRouteJob(cacheKey: String, from: LatLng, to: LatLng, onResult: (List<LatLng>, Int, Int) -> Unit): Job {
        Timber.d("🚀 [ROUTE_JOB] Launching route job: $cacheKey")
        Timber.d("🚀 [ROUTE_JOB] From: lat=${from.latitude}, lng=${from.longitude}")
        Timber.d("🚀 [ROUTE_JOB] To: lat=${to.latitude}, lng=${to.longitude}")
        Timber.d("🚀 [ROUTE_JOB] Timeout: ${ROUTE_API_TIMEOUT_MS}ms")
        
        return viewModelScope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            withTimeoutOrNull(ROUTE_API_TIMEOUT_MS) {
                Timber.d("🚀 [ROUTE_JOB] Starting route calculation...")
                calculateRouteWithDirections(from, to, cacheKey, onResult)
                val elapsed = System.currentTimeMillis() - startTime
                Timber.d("🚀 [ROUTE_JOB] Route calculation completed in ${elapsed}ms")
            } ?: run {
                val elapsed = System.currentTimeMillis() - startTime
                Timber.e("❌ [ROUTE_JOB] Route calculation timed out after ${elapsed}ms")
                _errorState.value = "Route calculation timed out"
                Timber.d("❌ [ROUTE_JOB] Using fallback calculation due to timeout")
                calculateFallbackETAAndDistance(from, to)
            }
        }
    }

    private suspend fun calculateRouteWithDirections(
        from: LatLng,
        to: LatLng,
        cacheKey: String,
        onResult: (List<LatLng>, Int, Int) -> Unit
    ) {
        val key = generateCacheKey(cacheKey, from, to)

        // TEMPORARILY DISABLED CACHE FOR DEBUGGING - Always calculate fresh routes
        // TODO: Re-enable cache after debugging
        Timber.d("🚫 [CACHE] CACHE TEMPORARILY DISABLED - Always calculating fresh route")
        Timber.d("🚫 [CACHE] Would have used cache key: $key")
        Timber.d("🚫 [CACHE] From: lat=${from.latitude}, lng=${from.longitude}")
        Timber.d("🚫 [CACHE] To: lat=${to.latitude}, lng=${to.longitude}")
        
        // DISABLED: Check cache first
        /*
        routeCacheLock.withLock {
            routeCache[key]?.let { (route, dist, dur, timestamp) ->
                if (System.currentTimeMillis() - timestamp < ROUTE_CACHE_TTL_MS) {
                    Timber.d("📦 [CACHE] CACHE HIT - Using cached route")
                    onResult(route, dist, dur)
                    return
                } else {
                    routeCache.remove(key) // Expire
                }
            }
        }
        */

        try {
            val apiStartTime = System.currentTimeMillis()
            Timber.d("🗺️ [DIRECTIONS_API] ════════════════════════════════════════════════════")
            Timber.d("🗺️ [DIRECTIONS_API] CALLING GOOGLE MAPS DIRECTIONS API")
            Timber.d("🗺️ [DIRECTIONS_API] Cache key: $cacheKey")
            Timber.d("🗺️ [DIRECTIONS_API] From: lat=${from.latitude}, lng=${from.longitude}")
            Timber.d("🗺️ [DIRECTIONS_API] To: lat=${to.latitude}, lng=${to.longitude}")
            
            val (route, dist, dur) = directionsService.getRoutePolyline(
                from.latitude, from.longitude, to.latitude, to.longitude
            )
            
            val apiElapsed = System.currentTimeMillis() - apiStartTime
            Timber.d("🗺️ [DIRECTIONS_API] API RESPONSE RECEIVED (took ${apiElapsed}ms)")
            Timber.d("🗺️ [DIRECTIONS_API] Route points: ${route.size}")
            Timber.d("🗺️ [DIRECTIONS_API] Distance: ${dist}m (${dist/1000.0} km)")
            Timber.d("🗺️ [DIRECTIONS_API] Duration: ${dur}s (${dur/60.0} min)")
            Timber.d("🗺️ [DIRECTIONS_API] ════════════════════════════════════════════════════")
            
            if (route.size >= 3) { // Validate
                // DISABLED: Cache the result
                /*
                routeCacheLock.withLock {
                    if (routeCache.size >= ROUTE_CACHE_SIZE) routeCache.remove(routeCache.keys.first())
                    routeCache[key] = Quadruple(route, dist, dur, System.currentTimeMillis())
                }
                */
                Timber.d("🚫 [CACHE] Caching disabled - not storing route result")
                Timber.d("🗺️ [DIRECTIONS_API] Calling onResult callback with route data...")
                onResult(route, dist, dur)
                Timber.d("🗺️ [DIRECTIONS_API] onResult callback completed")
            } else {
                Timber.w("⚠️ [DIRECTIONS_API] Invalid route: ${route.size} points (need at least 3)")
                throw IllegalStateException("Invalid route: ${route.size} points")
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ [DIRECTIONS_API] Route calc error: ${e.message}")
            Timber.e(e, "❌ [DIRECTIONS_API] Stack trace: ${e.stackTrace.take(5).joinToString("\n")}")
            _errorState.value = "Failed to calculate route"
            val fallbackRoute = listOf(from, to)
            val fallbackDist = calculateDistance(from, to).toInt()
            val fallbackDur = (fallbackDist / getFallbackSpeed(fallbackDist)).toInt()
            Timber.d("❌ [DIRECTIONS_API] Using fallback: distance=${fallbackDist}m, duration=${fallbackDur}s (${fallbackDur/60.0} min)")
            Timber.d("❌ [DIRECTIONS_API] Calling onResult with fallback values...")
            onResult(fallbackRoute, fallbackDist, fallbackDur)
        }
    }

    // Helper: Round coords for cache key
    private fun generateCacheKey(prefix: String, from: LatLng, to: LatLng): String {
        return "$prefix-${from.latitude.roundTo(COORD_PRECISION)},${from.longitude.roundTo(COORD_PRECISION)}-" +
                "${to.latitude.roundTo(COORD_PRECISION)},${to.longitude.roundTo(COORD_PRECISION)}"
    }

    private fun Double.roundTo(decimals: Int): Double {
        val factor = 10.0.pow(decimals.toDouble())
        return kotlin.math.round(this * factor) / factor
    }

    private fun updateETAAndDistance(distanceMeters: Int, durationSeconds: Int) {
        val previousDistance = _distance.value
        val previousTime = _estimatedTime.value
        
        val distText = if (distanceMeters >= 1000) "%.1f km".format(distanceMeters / 1000.0) else "${distanceMeters}m"
        val mins = (durationSeconds / 60).coerceAtLeast(1)
        val durText = if (mins >= 60) {
            val hrs = mins / 60
            val remMins = mins % 60
            if (remMins > 0) "$hrs hr $remMins min" else "$hrs hr"
        } else "$mins min"

        Timber.d("📊 [ETA_UPDATE] ════════════════════════════════════════════════════")
        Timber.d("📊 [ETA_UPDATE] UPDATE ETA/DISTANCE CALLED")
        Timber.d("📊 [ETA_UPDATE] Raw input: distance=${distanceMeters}m, duration=${durationSeconds}s (${durationSeconds/60.0} min)")
        Timber.d("📊 [ETA_UPDATE] Formatted: distance='$distText', time='$durText'")
        Timber.d("📊 [ETA_UPDATE] Previous values: distance='$previousDistance', time='$previousTime'")
        
        // Get caller info
        val stackTrace = Thread.currentThread().stackTrace
        val caller = if (stackTrace.size > 3) {
            val element = stackTrace[3]
            "${element.className}.${element.methodName}:${element.lineNumber}"
        } else "unknown"
        Timber.d("📊 [ETA_UPDATE] Called from: $caller")
        
        _distance.value = distText
        _estimatedTime.value = durText
        
        Timber.d("📊 [ETA_UPDATE] New values set: distance='${_distance.value}', time='${_estimatedTime.value}'")
        Timber.d("📊 [ETA_UPDATE] ════════════════════════════════════════════════════")
    }

    private fun calculateFallbackETAAndDistance(from: LatLng, to: LatLng) {
        Timber.d("📊 [FALLBACK] ════════════════════════════════════════════════════")
        Timber.d("📊 [FALLBACK] FALLBACK ETA/DISTANCE CALCULATION STARTED")
        Timber.d("📊 [FALLBACK] From: lat=${from.latitude}, lng=${from.longitude}")
        Timber.d("📊 [FALLBACK] To: lat=${to.latitude}, lng=${to.longitude}")
        
        val dist = calculateDistance(from, to).toInt()
        val speed = getFallbackSpeed(dist)
        val dur = (dist / speed).toInt()
        
        Timber.d("📊 [FALLBACK] Straight-line distance: ${dist}m (${dist/1000.0} km)")
        Timber.d("📊 [FALLBACK] Speed assumption: ${speed}m/s (${speed * 3.6} km/h)")
        Timber.d("📊 [FALLBACK] Calculated duration: ${dur}s (${dur/60.0} min)")
        Timber.d("📊 [FALLBACK] Calling updateETAAndDistance with: distance=${dist}m, duration=${dur}s")
        Timber.d("📊 [FALLBACK] ════════════════════════════════════════════════════")
        
        updateETAAndDistance(dist, dur)
    }

    // Helper: Variable speed based on distance
    private fun getFallbackSpeed(distanceMeters: Int): Float {
        return if (distanceMeters > 5000) FALLBACK_HIGHWAY_SPEED_MS else FALLBACK_CITY_SPEED_MS
    }

    private fun updateStatusUI(status: String) {
        val previousStatus = _activeRide.value?.status
        Timber.d("📱 [STATUS_UI] Updating status UI - status: '$status' (previous: '$previousStatus')")
        _isDriverOnLocation.value = status == "on_location"
        val message = when (status) {
            "en_route_pu" -> "Driver is on the way to pickup location"
            "on_location" -> "Driver has arrived at pickup location"
            "en_route_do" -> "En route to your destination"
            "ended" -> "Ride completed"
            else -> "Driver is on the way"
        }
        Timber.d("📱 [STATUS_UI] Status message: '$message', isDriverOnLocation: ${_isDriverOnLocation.value}")
        _statusMessage.value = message
    }

    private fun updateProximity(ride: ActiveRide, driverLoc: LatLng) {
        _pickupArrivalDetected.value = if (isValidLatLng(LatLng(ride.pickupLatitude, ride.pickupLongitude))) {
            calculateDistance(driverLoc, LatLng(ride.pickupLatitude, ride.pickupLongitude)) <= PICKUP_PROXIMITY_M
        } else false

        _dropoffArrivalDetected.value = if (isValidLatLng(LatLng(ride.dropoffLatitude, ride.dropoffLongitude))) {
            calculateDistance(driverLoc, LatLng(ride.dropoffLatitude, ride.dropoffLongitude)) <= DROPOFF_PROXIMITY_M
        } else false
    }

    private fun updateEstimatedMetrics(ride: ActiveRide) {
        Timber.d("📊 [ESTIMATED_METRICS] ════════════════════════════════════════════════════")
        Timber.d("📊 [ESTIMATED_METRICS] updateEstimatedMetrics called for status: ${ride.status}")
        Timber.d("📊 [ESTIMATED_METRICS] Current ETA: ${_estimatedTime.value}, Distance: ${_distance.value}")
        
        val driverLoc = _driverLocation.value
        val pickupLoc = _pickupLocation.value
        val dropoffLoc = _dropoffLocation.value
        
        // CRITICAL: Only update ETA/distance based on ride status
        // For en_route_pu: show driver-to-pickup (NOT driver-to-dropoff!)
        when (ride.status) {
            "en_route_pu" -> {
                if (driverLoc != null && pickupLoc != null) {
                    Timber.d("📊 [ESTIMATED_METRICS] Status is en_route_pu - calculating driver-to-pickup")
                    Timber.d("📊 [ESTIMATED_METRICS] Driver: lat=${driverLoc.latitude}, lng=${driverLoc.longitude}")
                    Timber.d("📊 [ESTIMATED_METRICS] Pickup: lat=${pickupLoc.latitude}, lng=${pickupLoc.longitude}")
                    // Only update if we don't already have a route-based calculation
                    // Don't overwrite route-based ETA with fallback
                    if (_driverToPickupRoute.value.isEmpty()) {
                        Timber.d("📊 [ESTIMATED_METRICS] No route yet - using fallback calculation")
                        calculateFallbackETAAndDistance(driverLoc, pickupLoc)
                    } else {
                        Timber.d("📊 [ESTIMATED_METRICS] Route exists - NOT overwriting with fallback")
                    }
                } else {
                    Timber.w("📊 [ESTIMATED_METRICS] Missing driver or pickup location")
                }
            }
            "on_location", "en_route_do" -> {
                if (driverLoc != null && dropoffLoc != null) {
                    Timber.d("📊 [ESTIMATED_METRICS] Status is ${ride.status} - calculating driver-to-dropoff")
                    // Only update if we don't already have a route-based calculation
                    if (_pickupToDropoffRoute.value.isEmpty()) {
                        Timber.d("📊 [ESTIMATED_METRICS] No route yet - using fallback calculation")
                        calculateFallbackETAAndDistance(driverLoc, dropoffLoc)
                    } else {
                        Timber.d("📊 [ESTIMATED_METRICS] Route exists - NOT overwriting with fallback")
                    }
                } else {
                    Timber.w("📊 [ESTIMATED_METRICS] Missing driver or dropoff location")
                }
            }
            else -> {
                Timber.d("📊 [ESTIMATED_METRICS] Status '${ride.status}' - no ETA calculation needed")
            }
        }
        Timber.d("📊 [ESTIMATED_METRICS] ════════════════════════════════════════════════════")
    }

    fun onMapInteraction() {
        _userHasInteractedWithMap.value = true
    }

    fun resetMapInteraction() {
        _userHasInteractedWithMap.value = false
        updateMapRegion()
    }

    fun initializeWithRide(ride: ActiveRide) {
        if (isValidRide(ride)) {
            _activeRide.value = ride
            handleActiveRideUpdate(ride)
        }
    }

    fun initializeWithBookingId(bookingId: String) {
        val currentRide = _activeRide.value
        if (currentRide != null && currentRide.bookingId == bookingId) {
            handleActiveRideUpdate(currentRide)
        } else {
            Timber.d("Waiting for socket data for booking $bookingId")
        }
    }

    override fun onCleared() {
        super.onCleared()
        updateJob?.cancel()
        cancelRouteJobs()
        _activeRide.value?.bookingId?.toIntOrNull()?.let { socketService.leaveBookingRoom(it) }
    }

    private fun cancelRouteJobs() {
        driverToPickupRouteJob?.cancel()
        pickupToDropoffRouteJob?.cancel()
    }

    private suspend fun clearDriverPickupCache() {
        routeCacheLock.withLock {
            val keysToRemove = routeCache.keys.filter { it.startsWith("driver-pickup-") }
            keysToRemove.forEach { routeCache.remove(it) }
        }
    }

    // Helpers
    private fun isValidLatLng(loc: LatLng): Boolean {
        return loc.latitude in -90.0..90.0 && loc.longitude in -180.0..180.0 && (loc.latitude != 0.0 || loc.longitude != 0.0)
    }

    private fun isValidRide(ride: ActiveRide): Boolean {
        return ride.bookingId.isNotBlank() && isValidLatLng(LatLng(ride.pickupLatitude, ride.pickupLongitude)) &&
                isValidLatLng(LatLng(ride.dropoffLatitude, ride.dropoffLongitude))
    }

    private fun calculateDistance(from: LatLng, to: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, results)
        return results[0]
    }

    private fun locationsMatch(loc1: LatLng, loc2: LatLng, toleranceM: Float): Boolean {
        return calculateDistance(loc1, loc2) < toleranceM
    }
}

/**
 * Data class for map region
 */
data class MapRegion(
    val center: LatLng,
    val zoom: Float
)


/**
 * Quadruple for cache (route, dist, dur, timestamp)
 */
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)