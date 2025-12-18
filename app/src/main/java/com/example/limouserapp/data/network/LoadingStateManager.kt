package com.example.limouserapp.data.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Global loading state manager
 * Tracks when any API call is in progress across the entire application
 * Provides a single source of truth for loading state
 * Uses a counter to handle multiple concurrent API calls
 */
@Singleton
class LoadingStateManager @Inject constructor() {
    
    // Counter to handle multiple concurrent API calls
    private val _loadingCount = MutableStateFlow(0)
    
    // Computed loading state - true if any API call is in progress
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    /**
     * Start loading - called when an API request begins
     */
    fun startLoading() {
        _loadingCount.value++
        _isLoading.value = _loadingCount.value > 0
    }
    
    /**
     * Stop loading - called when an API request completes
     */
    fun stopLoading() {
        if (_loadingCount.value > 0) {
            _loadingCount.value--
        }
        _isLoading.value = _loadingCount.value > 0
    }
    
    /**
     * Check if currently loading
     */
    fun isCurrentlyLoading(): Boolean {
        return _loadingCount.value > 0
    }
    
    /**
     * Force stop all loading (useful for error scenarios)
     */
    fun forceStopLoading() {
        _loadingCount.value = 0
        _isLoading.value = false
    }
}

