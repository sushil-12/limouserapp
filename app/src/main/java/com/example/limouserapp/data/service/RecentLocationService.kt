package com.example.limouserapp.data.service

import android.util.Log
import com.example.limouserapp.data.api.DashboardApi
import com.example.limouserapp.data.model.booking.RecentLocation
import com.example.limouserapp.ui.utils.DebugTags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecentLocationService @Inject constructor(
    private val dashboardApi: DashboardApi
) {
    private val _pickupLocations = MutableStateFlow<List<RecentLocation>>(emptyList())
    val pickupLocations: StateFlow<List<RecentLocation>> = _pickupLocations.asStateFlow()
    
    private val _dropoffLocations = MutableStateFlow<List<RecentLocation>>(emptyList())
    val dropoffLocations: StateFlow<List<RecentLocation>> = _dropoffLocations.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    /**
     * Fetch recent locations for pickup or dropoff
     */
    suspend fun fetchRecentLocations(type: String) {
        withContext(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                val response = dashboardApi.getRecentLocations(type)
                
                if (response.success) {
                    when (type) {
                        "pickup" -> {
                            _pickupLocations.value = response.data.locations
                            Log.d(DebugTags.BookingProcess, "Fetched ${response.data.locations.size} pickup locations")
                        }
                        "dropoff" -> {
                            _dropoffLocations.value = response.data.locations
                            Log.d(DebugTags.BookingProcess, "Fetched ${response.data.locations.size} dropoff locations")
                        }
                    }
                } else {
                    _errorMessage.value = response.message
                    Log.e(DebugTags.BookingProcess, "Failed to fetch recent locations: ${response.message}")
                }
            } catch (e: Exception) {
                Log.e(DebugTags.BookingProcess, "Error fetching recent locations", e)
                _errorMessage.value = "Failed to load recent locations. Please try again."
                when (type) {
                    "pickup" -> _pickupLocations.value = emptyList()
                    "dropoff" -> _dropoffLocations.value = emptyList()
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Clear locations cache
     */
    fun clearCache() {
        _pickupLocations.value = emptyList()
        _dropoffLocations.value = emptyList()
    }
    
    /**
     * Clear pickup locations
     */
    fun clearPickupLocations() {
        _pickupLocations.value = emptyList()
    }
    
    /**
     * Clear dropoff locations
     */
    fun clearDropoffLocations() {
        _dropoffLocations.value = emptyList()
    }
}

