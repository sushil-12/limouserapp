package com.example.limouserapp.data.service

import android.util.Log
import com.example.limouserapp.ui.utils.DebugTags
import com.example.limouserapp.data.api.AirlineApi
import com.example.limouserapp.data.model.booking.Airline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AirlineService @Inject constructor(
    private val airlineApi: AirlineApi
) {
    private val _airlines = MutableStateFlow<List<Airline>>(emptyList())
    val airlines: StateFlow<List<Airline>> = _airlines.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    /**
     * Fetch airlines with optional search query
     */
    suspend fun fetchAirlines(search: String? = null) {
        withContext(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                val response = airlineApi.searchAirlines(search = search)
                _airlines.value = response.data.airlinesData
                
                Log.d(DebugTags.BookingProcess, "Fetched ${_airlines.value.size} airlines")
            } catch (e: Exception) {
                Log.e(DebugTags.BookingProcess, "Error fetching airlines", e)
                _errorMessage.value = "Failed to load airlines."
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Search airlines (calls fetchAirlines with search query)
     */
    suspend fun searchAirlines(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            fetchAirlines() // Fetch all if query is empty
        } else {
            fetchAirlines(search = trimmedQuery)
        }
    }
    
    /**
     * Get airline by ID
     */
    fun getAirlineById(id: Int): Airline? {
        return _airlines.value.find { it.id == id }
    }
    
    /**
     * Get airline by code
     */
    fun getAirlineByCode(code: String): Airline? {
        return _airlines.value.find { it.code.equals(code, ignoreCase = true) }
    }
    
    /**
     * Get airline by display name (matches iOS getAirlineByDisplayName)
     */
    fun getAirlineByDisplayName(displayName: String): Airline? {
        return _airlines.value.find { 
            it.displayName.equals(displayName, ignoreCase = true) ||
            it.fullDisplayName.equals(displayName, ignoreCase = true)
        }
    }
}

