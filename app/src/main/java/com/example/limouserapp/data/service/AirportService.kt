package com.example.limouserapp.data.service

import android.util.Log
import com.example.limouserapp.ui.utils.DebugTags
import com.example.limouserapp.data.api.AirportApi
import com.example.limouserapp.data.model.booking.Airport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
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
    private var currentSearchJob: Job? = null
    
    /**
     * Search airports with debouncing handled by component
     * Uses withContext which automatically cancels on new call
     * Cancels any ongoing search before starting a new one
     */
    suspend fun searchAirports(query: String) {
        val trimmedQuery = query.trimming()
        
        if (trimmedQuery.isEmpty() || trimmedQuery.length < 2) {
            // Cancel any ongoing search
            currentSearchJob?.cancel()
            currentSearchJob = null
            // For short/empty queries, just clear suggestions instead of fetching all airports
            // This prevents unnecessary API calls when user is still typing
            _suggestions.value = emptyList()
            _airports.value = emptyList()
            _errorMessage.value = null // Clear any previous errors
            currentSearchQuery = null
            return
        }
        
        // Skip if same query (prevents duplicate calls)
        if (currentSearchQuery == trimmedQuery) {
            return
        }
        
        // Cancel any ongoing search before starting a new one
        currentSearchJob?.cancel()
        currentSearchQuery = trimmedQuery
        
        // Use withContext to properly handle cancellation
        // Note: The debouncing in SearchableBottomSheet prevents rapid calls,
        // but we still cancel previous searches for safety
        try {
            withContext(Dispatchers.IO) {
                ensureActive() // Check if cancelled before starting
                _isLoading.value = true
                _errorMessage.value = null // Clear previous errors when starting new search
                
                Log.d(DebugTags.BookingProcess, "🔍 Searching airports for query: '$trimmedQuery'")
                
                val response = airportApi.searchAirports(
                    search = trimmedQuery,
                    page = 1,
                    limit = 50
                )
                
                ensureActive() // Check if cancelled after API call
                
                Log.d(DebugTags.BookingProcess, "✅ Airport API response received")
                Log.d(DebugTags.BookingProcess, "📦 Response data: ${response.data}")
                Log.d(DebugTags.BookingProcess, "📦 Airports data count: ${response.data?.airportsData?.size ?: 0}")
                
                val airportsList = response.data?.airportsData ?: emptyList()
                _airports.value = airportsList
                _suggestions.value = airportsList.map { it.displayName }
                
                Log.d(DebugTags.BookingProcess, "Airport suggestions count=${_suggestions.value.size} for query='$trimmedQuery'")
                
                if (_suggestions.value.isEmpty()) {
                    Log.w(DebugTags.BookingProcess, "⚠️ No airport suggestions found for query: '$trimmedQuery'")
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Job was cancelled, ignore
            Log.d(DebugTags.BookingProcess, "🚫 Airport search cancelled for query: '$trimmedQuery'")
            throw e
        } catch (e: Exception) {
            Log.e(DebugTags.BookingProcess, "❌ Error searching airports for query: '$trimmedQuery'", e)
            Log.e(DebugTags.BookingProcess, "❌ Exception type: ${e.javaClass.simpleName}")
            Log.e(DebugTags.BookingProcess, "❌ Exception message: ${e.message}")
            e.printStackTrace()
            _errorMessage.value = "Search failed. Please try again."
            _suggestions.value = emptyList()
            _airports.value = emptyList()
        } finally {
            _isLoading.value = false
            currentSearchJob = null
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
                
                Log.d(DebugTags.BookingProcess, "🔍 Fetching initial airports")
                
                val response = airportApi.searchAirports(
                    onlyAirports = true,
                    search = null,
                    page = 1,
                    limit = 20
                )
                
                Log.d(DebugTags.BookingProcess, "✅ Initial airports API response received")
                Log.d(DebugTags.BookingProcess, "📦 Response data: ${response.data}")
                Log.d(DebugTags.BookingProcess, "📦 Airports data count: ${response.data?.airportsData?.size ?: 0}")
                
                val airportsList = response.data?.airportsData ?: emptyList()
                _airports.value = airportsList
                _suggestions.value = airportsList.map { it.displayName }
                
                Log.d(DebugTags.BookingProcess, "✅ Initial airports loaded: ${airportsList.size} airports")
            } catch (e: Exception) {
                Log.e(DebugTags.BookingProcess, "❌ Error fetching initial airports", e)
                Log.e(DebugTags.BookingProcess, "❌ Exception type: ${e.javaClass.simpleName}")
                Log.e(DebugTags.BookingProcess, "❌ Exception message: ${e.message}")
                e.printStackTrace()
                _errorMessage.value = "Failed to load airports."
                _suggestions.value = emptyList()
                _airports.value = emptyList()
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
     * Clear suggestions and reset error state
     */
    fun clearSuggestions() {
        _suggestions.value = emptyList()
        _errorMessage.value = null
    }
    
    /**
     * Reset service state (clear all data and errors)
     */
    fun reset() {
        // Cancel any ongoing search
        currentSearchJob?.cancel()
        currentSearchJob = null
        _suggestions.value = emptyList()
        _airports.value = emptyList()
        _errorMessage.value = null
        _isLoading.value = false
        currentPage = 1
        currentSearchQuery = null
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

