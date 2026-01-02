package com.example.limouserapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.limouserapp.data.location.LocationManager
import com.example.limouserapp.data.service.AirportService
import com.example.limouserapp.data.service.RecentLocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ScheduleRideViewModel @Inject constructor(
    val airportService: AirportService,
    val recentLocationService: RecentLocationService,
    val locationManager: LocationManager
) : ViewModel() {
    
    // Auto-navigation guard - prevents multiple auto-navigations
    // Reset only when pickup or destination input changes
    private val _hasAutoNavigated = MutableStateFlow(false)
    val hasAutoNavigated: StateFlow<Boolean> = _hasAutoNavigated.asStateFlow()
    
    /**
     * Mark that auto-navigation has occurred (one-time guard)
     */
    fun markAutoNavigated() {
        _hasAutoNavigated.value = true
    }
    
    /**
     * Reset auto-navigation guard when pickup or destination input changes
     * This allows auto-navigation to work again after user changes locations
     */
    fun resetAutoNavigationGuard() {
        _hasAutoNavigated.value = false
    }
}

