package com.example.limouserapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.limouserapp.data.model.dashboard.*
import com.example.limouserapp.data.tracking.RealTimeTrackingService
import com.example.limouserapp.data.socket.SocketService
import com.example.limouserapp.data.socket.ActiveRide
import com.example.limouserapp.domain.usecase.dashboard.GetUpcomingBookingsUseCase
import com.example.limouserapp.domain.usecase.dashboard.GetUserBookingsUseCase
import com.example.limouserapp.domain.usecase.dashboard.GetUserProfileUseCase
import com.example.limouserapp.domain.usecase.dashboard.RefreshUserProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Dashboard screen
 * Manages dashboard state and business logic
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val refreshUserProfileUseCase: RefreshUserProfileUseCase,
    private val getUpcomingBookingsUseCase: GetUpcomingBookingsUseCase,
    private val getUserBookingsUseCase: GetUserBookingsUseCase,
    private val realTimeTrackingService: RealTimeTrackingService,
    private val socketService: SocketService,
    private val dashboardApi: com.example.limouserapp.data.api.DashboardApi
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    // Expose activeRide from SocketService for navigation
    val activeRide = socketService.activeRide
    
    // Track if we should trigger navigation to live ride
    private val _shouldNavigateToLiveRide = MutableStateFlow(false)
    val shouldNavigateToLiveRide: StateFlow<Boolean> = _shouldNavigateToLiveRide.asStateFlow()
    
    init {
        loadDashboardData()
        startRealTimeServices()
    }
    
    /**
     * Load all dashboard data
     */
    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // Check for active ride first (before loading profile/bookings)
            checkActiveRideOnStartup()
            
            // Load user profile
            loadUserProfile()
            
            // Load upcoming bookings
            loadUpcomingBookings()
            
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
    
    /**
     * Check for active ride on app startup via API
     * This ensures we catch active rides even if app was killed and restarted
     */
    private suspend fun checkActiveRideOnStartup() {
        try {
            // Get user ID from ProfileData (which has userId field, not just id)
            // The active ride API requires userId (from ProfileData), not id (from UserProfile)
            val profileDataResponse = try {
                dashboardApi.getProfileData()
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch profile data for active ride check")
                return
            }
            val userId = if (profileDataResponse.success) {
                profileDataResponse.data.userId.toString()
            } else {
                return // Skip if no user ID
            }
            
            // Call the dedicated active ride API
            getUserBookingsUseCase.getActiveRide(userId).fold(
                onSuccess = { activeRideResponse ->
                    // Check if we have an active ride
                    activeRideResponse?.data?.let { details ->
                        Timber.d("📱 Active ride detected on startup: ${details.booking_id}, status: ${details.status}")
                        
                        // Convert API response to ActiveRide format
                        val activeRide = ActiveRide(
                            bookingId = details.booking_id.toString(),
                            driverId = details.driver.id.toString(),
                            customerId = details.customer?.id.toString() ?: userId,
                            status = details.status,
                            driverLatitude = details.locations?.pickup?.latitude ?: 0.0,
                            driverLongitude = details.locations?.pickup?.longitude ?: 0.0,
                            pickupLatitude = details.locations?.pickup?.latitude ?: 0.0,
                            pickupLongitude = details.locations?.pickup?.longitude ?: 0.0,
                            dropoffLatitude = details.locations?.dropoff?.latitude ?: 0.0,
                            dropoffLongitude = details.locations?.dropoff?.longitude ?: 0.0,
                            pickupAddress = details.locations?.pickup?.address ?: "",
                            dropoffAddress = details.locations?.dropoff?.address ?: "",
                            driverName = details.driver.name,
                            driverPhone = details.driver.phone,
                            timestamp = details.timestamps?.updated_at ?: System.currentTimeMillis().toString()
                        )
                        
                        // Set active ride in SocketService
                        socketService.setActiveRide(activeRide)
                        
                        // Trigger navigation to live ride
                        _shouldNavigateToLiveRide.value = true
                        
                        // Ensure socket is connected for real-time updates
                        if (!socketService.isConnected()) {
                            socketService.connect()
                        }
                        
                        Timber.d("✅ Active ride initialized from API ${activeRide.toString()}" )
                    } ?: run {
                        // No active ride found (normal case)
                        Timber.d("📱 No active ride found for user: $userId")
                    }
                },
                onFailure = { error ->
                    // If it's a JSON parsing error (API returned string instead of JSON), 
                    // this is expected when no active ride exists - just log as debug
                    if (error is com.google.gson.JsonSyntaxException) {
                        Timber.d("No active ride found (API returned string response)")
                    } else {
                        // API error - log but don't crash
                        Timber.e(error, "Error fetching active ride")
                    }
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Error checking active ride on startup")
        }
    }
    
    /**
     * Load user profile
     */
    private suspend fun loadUserProfile() {
        getUserProfileUseCase().fold(
            onSuccess = { profile ->
                _uiState.value = _uiState.value.copy(
                    userProfile = profile,
                    profileLoading = false
                )
            },
            onFailure = { error ->
                Timber.e(error, "Error loading user profile")
                _uiState.value = _uiState.value.copy(
                    profileLoading = false,
                    error = error.message
                )
            }
        )
    }
    
    /**
     * Load upcoming bookings
     */
    private suspend fun loadUpcomingBookings() {
        getUpcomingBookingsUseCase().fold(
            onSuccess = { bookings ->
                _uiState.value = _uiState.value.copy(
                    upcomingBookings = bookings,
                    bookingsLoading = false
                )
            },
            onFailure = { error ->
                Timber.e(error, "Error loading upcoming bookings")
                _uiState.value = _uiState.value.copy(
                    bookingsLoading = false,
                    error = error.message
                )
            }
        )
    }
    
    /**
     * Refresh dashboard data
     */
    fun refreshDashboard() {
        loadDashboardData()
    }
    
    /**
     * Refresh user profile from API (forces fresh data, updates cache)
     * Use this when profile data needs to be updated (e.g., after account settings changes)
     */
    fun refreshUserProfile() {
        Timber.d("DashboardViewModel: refreshUserProfile() called")
        viewModelScope.launch {
            Timber.d("DashboardViewModel: Starting profile refresh from API...")
            refreshUserProfileUseCase().fold(
                onSuccess = { profile ->
                    Timber.d("DashboardViewModel: Profile refresh successful - fullName=${profile.fullName}, firstName=${profile.firstName}, lastName=${profile.lastName}")
                    _uiState.value = _uiState.value.copy(
                        userProfile = profile,
                        profileLoading = false
                    )
                    Timber.d("DashboardViewModel: UI state updated with new profile")
                },
                onFailure = { error ->
                    Timber.e(error, "DashboardViewModel: Error refreshing user profile")
                    _uiState.value = _uiState.value.copy(
                        profileLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }
    
    /**
     * Toggle bottom sheet expansion
     */
    fun toggleBottomSheet() {
        _uiState.value = _uiState.value.copy(
            bottomSheetState = _uiState.value.bottomSheetState.copy(
                isExpanded = !_uiState.value.bottomSheetState.isExpanded
            )
        )
    }
    
    /**
     * Set bottom sheet expansion state
     */
    fun setBottomSheetExpanded(isExpanded: Boolean) {
        _uiState.value = _uiState.value.copy(
            bottomSheetState = _uiState.value.bottomSheetState.copy(
                isExpanded = isExpanded
            )
        )
    }
    
    /**
     * Toggle navigation drawer
     */
    fun toggleNavigationDrawer() {
        _uiState.value = _uiState.value.copy(
            isNavigationDrawerOpen = !_uiState.value.isNavigationDrawerOpen
        )
    }
    
    /**
     * Close navigation drawer
     */
    fun closeNavigationDrawer() {
        _uiState.value = _uiState.value.copy(
            isNavigationDrawerOpen = false
        )
    }
    
    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            searchState = _uiState.value.searchState.copy(
                query = query
            )
        )
    }
    
    /**
     * Perform search
     */
    fun performSearch() {
        val query = _uiState.value.searchState.query
        if (query.isNotEmpty()) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(
                    searchState = _uiState.value.searchState.copy(
                        isSearching = true
                    )
                )
                
                // TODO: Implement search functionality
                // For now, just clear the search
                _uiState.value = _uiState.value.copy(
                    searchState = _uiState.value.searchState.copy(
                        isSearching = false,
                        query = ""
                    )
                )
            }
        }
    }
    
    /**
     * Clear search
     */
    fun clearSearch() {
        _uiState.value = _uiState.value.copy(
            searchState = _uiState.value.searchState.copy(
                query = "",
                isSearching = false,
                searchResults = emptyList()
            )
        )
    }
    
    /**
     * Update map region
     */
    fun updateMapRegion(region: MapRegion) {
        _uiState.value = _uiState.value.copy(
            mapRegion = region
        )
    }
    
    /**
     * Update user location
     */
    fun updateUserLocation(location: LocationData) {
        _uiState.value = _uiState.value.copy(
            userLocation = location
        )
    }
    
    /**
     * Update car locations
     */
    fun updateCarLocations(locations: List<CarLocation>) {
        _uiState.value = _uiState.value.copy(
            carLocations = locations
        )
    }
    
    /**
     * Update connection status
     */
    fun updateConnectionStatus(status: ConnectionStatus) {
        _uiState.value = _uiState.value.copy(
            connectionStatus = status
        )
    }
    
    /**
     * Clear error
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(
            error = null
        )
    }
    
    /**
     * Start real-time services
     */
    private fun startRealTimeServices() {
        viewModelScope.launch {
            // Start real-time tracking (will check permissions internally)
            realTimeTrackingService.startTracking()
            
            // Observe real-time updates
            observeRealTimeUpdates()
        }
    }
    
    /**
     * Observe real-time updates
     */
    private fun observeRealTimeUpdates() {
        viewModelScope.launch {
            // Observe user location updates
            realTimeTrackingService.userLocation.collect { location ->
                location?.let {
                    _uiState.value = _uiState.value.copy(
                        userLocation = it
                    )
                }
            }
        }
        
        viewModelScope.launch {
            // Observe driver location updates
            realTimeTrackingService.driverLocations.collect { locations ->
                _uiState.value = _uiState.value.copy(
                    carLocations = locations
                )
            }
        }
        
        viewModelScope.launch {
            // Observe socket connection status
            socketService.connectionStatus.collect { status ->
                _uiState.value = _uiState.value.copy(
                    connectionStatus = status
                )
            }
        }
        
        viewModelScope.launch {
            // Observe tracking errors
            realTimeTrackingService.trackingError.collect { error ->
                error?.let {
                    _uiState.value = _uiState.value.copy(
                        error = it
                    )
                }
            }
        }
    }
    
    /**
     * Stop real-time services
     */
    fun stopRealTimeServices() {
        viewModelScope.launch {
            realTimeTrackingService.stopTracking()
        }
    }
    
    /**
     * Request driver location for specific booking
     */
    fun requestDriverLocation(bookingId: Int) {
        realTimeTrackingService.requestDriverLocation(bookingId)
    }
    
    /**
     * Join booking room for real-time updates
     */
    fun joinBookingRoom(bookingId: Int) {
        realTimeTrackingService.joinBookingRoom(bookingId)
    }
    
    /**
     * Leave booking room
     */
    fun leaveBookingRoom(bookingId: Int) {
        realTimeTrackingService.leaveBookingRoom(bookingId)
    }
    
    /**
     * Get current user location
     */
    suspend fun getCurrentUserLocation(): LocationData? {
        return realTimeTrackingService.getCurrentUserLocation()
    }
    
    /**
     * Get tracking status
     */
    fun getTrackingStatus(): String {
        return realTimeTrackingService.getTrackingStatus()
    }
    
    override fun onCleared() {
        super.onCleared()
        stopRealTimeServices()
    }
}

/**
 * UI state for Dashboard screen
 */
data class DashboardUiState(
    val isLoading: Boolean = false,
    val profileLoading: Boolean = true,
    val bookingsLoading: Boolean = true,
    val userProfile: UserProfile? = null,
    val upcomingBookings: List<UserBooking> = emptyList(),
    val bottomSheetState: BottomSheetState = BottomSheetState(),
    val isNavigationDrawerOpen: Boolean = false,
    val searchState: SearchState = SearchState(),
    val mapRegion: MapRegion = MapRegion.DEFAULT_REGION,
    val userLocation: LocationData? = null,
    val carLocations: List<CarLocation> = emptyList(),
    val connectionStatus: ConnectionStatus = ConnectionStatus(isConnected = false),
    val error: String? = null
)
