package com.example.limouserapp.data.service

import android.util.Log
import com.example.limouserapp.ui.utils.DebugTags
import com.example.limouserapp.data.api.AirportApi
import com.example.limouserapp.data.model.booking.Airport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AirportService @Inject constructor(
    private val airportApi: AirportApi
) {
    private val _airports = MutableStateFlow<List<Airport>>(emptyList())
    val airports: StateFlow<List<Airport>> = _airports.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private var currentPage = 1
    private var currentSearchQuery: String? = null
    
    /**
     * Search airports with debouncing handled by ViewModel
     */
    suspend fun searchAirports(query: String) {
        val trimmedQuery = query.trimming()
        if (trimmedQuery.isEmpty() || trimmedQuery.length < 2) {
            // For short/empty queries, fetch initial airports instead of clearing
            fetchInitialAirports()
            return
        }
        
        withContext(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                val response = airportApi.searchAirports(
                    search = trimmedQuery,
                    page = 1,
                    limit = 50
                )
                
                _airports.value = response.data.airportsData
                _suggestions.value = response.data.airportsData.map { it.displayName }
                
                Log.d(DebugTags.BookingProcess, "Airport suggestions count=${'$'}{_suggestions.value.size} for query='${'$'}trimmedQuery'")
            } catch (e: Exception) {
                Log.e(DebugTags.BookingProcess, "Error searching airports", e)
                _errorMessage.value = "Search failed. Please try again."
                _suggestions.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Fetch an initial list of airports (popular/default) when query is empty
     */
    suspend fun fetchInitialAirports() {
        withContext(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                val response = airportApi.searchAirports(
                    onlyAirports = true,
                    search = null,
                    page = 1,
                    limit = 20
                )
                _airports.value = response.data.airportsData
                _suggestions.value = response.data.airportsData.map { it.displayName }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load airports."
                _suggestions.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Get airport data by display name
     */
    fun getAirportByDisplayName(displayName: String): Airport? {
        return _airports.value.find { it.displayName == displayName }
    }
    
    /**
     * Clear suggestions
     */
    fun clearSuggestions() {
        _suggestions.value = emptyList()
    }
    
    /**
     * Select airport suggestion and return airport data
     */
    fun selectAirportSuggestion(displayName: String): Airport? {
        return getAirportByDisplayName(displayName)
    }
    
    private fun String.trimming(): String {
        return this.trim()
    }
}

