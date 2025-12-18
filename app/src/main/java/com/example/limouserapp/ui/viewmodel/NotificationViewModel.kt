package com.example.limouserapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.limouserapp.data.model.notification.AuditEvent
import com.example.limouserapp.data.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Notification screen
 * Handles audit records list, pagination, and filtering
 */
@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()
    
    init {
        loadAuditRecords()
    }
    
    /**
     * Load audit records from API
     */
    fun loadAuditRecords(append: Boolean = false) {
        viewModelScope.launch {
            try {
                if (!append) {
                    _uiState.value = _uiState.value.copy(
                        currentPage = 1,
                        events = emptyList()
                    )
                }
                
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val result = dashboardRepository.getBookingAuditRecords(
                    page = _uiState.value.currentPage,
                    perPage = 20,
                    from = _uiState.value.fromDate,
                    to = _uiState.value.toDate,
                    eventType = _uiState.value.eventType,
                    eventCategory = _uiState.value.eventCategory,
                    search = _uiState.value.searchText.ifEmpty { null },
                    bookingId = _uiState.value.bookingId
                )
                
                result.fold(
                    onSuccess = { data ->
                        _uiState.value = _uiState.value.copy(
                            events = if (append) {
                                _uiState.value.events + data.events
                            } else {
                                data.events
                            },
                            totalEvents = data.totalEvents,
                            totalBookings = data.totalBookings,
                            currentPage = data.pagination.currentPage,
                            totalPages = data.pagination.lastPage,
                            isLoading = false,
                            error = null
                        )
                    },
                    onFailure = { exception ->
                        Timber.e(exception, "Error loading audit records")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load notifications"
                        )
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Error loading audit records")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load notifications"
                )
            }
        }
    }
    
    /**
     * Refresh audit records
     */
    fun refreshAuditRecords() {
        _uiState.value = _uiState.value.copy(currentPage = 1)
        loadAuditRecords()
    }
    
    /**
     * Load next page
     */
    fun loadNextPage() {
        if (!_uiState.value.isLoading && _uiState.value.currentPage < _uiState.value.totalPages) {
            _uiState.value = _uiState.value.copy(currentPage = _uiState.value.currentPage + 1)
            loadAuditRecords(append = true)
        }
    }
    
    /**
     * Set search text
     */
    fun setSearchText(text: String) {
        _uiState.value = _uiState.value.copy(searchText = text)
    }
    
    /**
     * Clear search
     */
    fun clearSearch() {
        _uiState.value = _uiState.value.copy(searchText = "")
        refreshAuditRecords()
    }
    
    /**
     * Handle search with debounce
     */
    fun handleSearch(searchText: String) {
        _uiState.value = _uiState.value.copy(searchText = searchText)
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            if (_uiState.value.searchText == searchText) {
                refreshAuditRecords()
            }
        }
    }
}

/**
 * UI state for Notification screen
 */
data class NotificationUiState(
    val events: List<AuditEvent> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchText: String = "",
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val totalEvents: Int = 0,
    val totalBookings: Int = 0,
    val fromDate: String? = null,
    val toDate: String? = null,
    val eventType: String? = null,
    val eventCategory: String? = null,
    val bookingId: Int? = null
)

