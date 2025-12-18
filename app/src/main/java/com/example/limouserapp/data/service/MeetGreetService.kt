package com.example.limouserapp.data.service

import android.util.Log
import com.example.limouserapp.ui.utils.DebugTags
import com.example.limouserapp.data.api.MeetGreetApi
import com.example.limouserapp.data.model.booking.MeetGreetChoice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeetGreetService @Inject constructor(
    private val meetGreetApi: MeetGreetApi
) {
    private val _meetGreetChoices = MutableStateFlow<List<MeetGreetChoice>>(emptyList())
    val meetGreetChoices: StateFlow<List<MeetGreetChoice>> = _meetGreetChoices.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    /**
     * Fetch meet and greet choices
     */
    suspend fun fetchMeetGreetChoices() {
        withContext(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                
                val response = meetGreetApi.getMeetGreetChoices()
                if (response.success) {
                    _meetGreetChoices.value = response.data.meetGreets
                    Log.d(DebugTags.BookingProcess, "✅ Fetched ${_meetGreetChoices.value.size} meet and greet choices")
                } else {
                    Log.e(DebugTags.BookingProcess, "❌ Failed to fetch meet and greet choices: ${response.message}")
                    _errorMessage.value = response.message
                }
            } catch (e: Exception) {
                Log.e(DebugTags.BookingProcess, "Error fetching meet and greet choices", e)
                _errorMessage.value = "Failed to load meet and greet choices."
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Get meet and greet choice by ID
     */
    fun getMeetGreetChoiceById(id: Int): MeetGreetChoice? {
        return _meetGreetChoices.value.find { it.id == id }
    }
    
    /**
     * Get meet and greet choice by message
     */
    fun getMeetGreetChoiceByMessage(message: String): MeetGreetChoice? {
        return _meetGreetChoices.value.find { it.message.equals(message, ignoreCase = true) }
    }
}

