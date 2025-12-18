package com.example.limouserapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.limouserapp.data.api.DashboardApi
import com.example.limouserapp.data.model.dashboard.UserBooking
import com.example.limouserapp.domain.usecase.dashboard.GetUserBookingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for My Bookings screen
 * Handles booking list, filtering, search, and pagination
 */
@HiltViewModel
class MyBookingsViewModel @Inject constructor(
    private val getUserBookingsUseCase: GetUserBookingsUseCase,
    private val dashboardApi: DashboardApi
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MyBookingsUiState())
    val uiState: StateFlow<MyBookingsUiState> = _uiState.asStateFlow()
    
    private val calendar = Calendar.getInstance()
    private var searchJob: Job? = null
    
    init {
        setupInitialDateRange()
        loadBookings()
    }
    
    /**
     * Setup initial date range (current week)
     */
    private fun setupInitialDateRange() {
        val today = Date()
        val weekStart = getWeekStart(today)
        val weekEnd = getWeekEnd(weekStart)
        
        _uiState.value = _uiState.value.copy(
            selectedWeekStart = weekStart,
            selectedWeekEnd = weekEnd,
            selectedTimePeriod = TimePeriod.WEEKLY
        )
    }
    
    /**
     * Get week start date
     */
    private fun getWeekStart(date: Date): Date {
        calendar.time = date
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }
    
    /**
     * Get week end date
     */
    private fun getWeekEnd(weekStart: Date): Date {
        calendar.time = weekStart
        calendar.add(Calendar.DAY_OF_MONTH, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        return calendar.time
    }
    
    /**
     * Load bookings from API - matches iOS fetchBookings exactly
     */
    fun loadBookings(append: Boolean = false) {
        viewModelScope.launch {
            try {
                if (!append) {
                    _uiState.value = _uiState.value.copy(
                        currentPage = 1,
                        bookings = emptyList()
                    )
                }
                
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val fromDate = dateFormatter.format(_uiState.value.selectedWeekStart)
                val toDate = dateFormatter.format(_uiState.value.selectedWeekEnd)
                
                val searchText = _uiState.value.searchText
                // useDateFilter should be true for all time periods when not searching
                // but false when searching (when searchText is not empty)
                val useDateFilter = if (searchText.isEmpty()) "true" else "false"
                
                val currentDate = dateFormatter.format(Date())
                val currentTime = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
                
                Timber.d("🌐 API Call Parameters:")
                Timber.d("   from: $fromDate")
                Timber.d("   to: $toDate")
                Timber.d("   search: $searchText")
                Timber.d("   useDateFilter: $useDateFilter (true for date periods, false when searching)")
                Timber.d("   current_date: $currentDate")
                Timber.d("   current_time: $currentTime")
                Timber.d("   page: ${_uiState.value.currentPage}")
                Timber.d("   selectedTimePeriod: ${_uiState.value.selectedTimePeriod.displayName}")
                
                val response = dashboardApi.searchBookings(
                    search = searchText,
                    from = fromDate,
                    to = toDate,
                    useDateFilter = useDateFilter,
                    currentDate = currentDate,
                    currentTime = currentTime,
                    page = _uiState.value.currentPage,
                    status = null,
                    vehicleType = null,
                    paymentMethod = null
                )
                
                // Extract bookings and pagination info from response
                if (response.success && response.data != null) {
                    val bookings = response.data.data
                    val paginationData = response.data
                    
                    _uiState.value = _uiState.value.copy(
                        bookings = if (append) {
                            _uiState.value.bookings + bookings
                        } else {
                            bookings
                        },
                        currentPage = paginationData.currentPage,
                        totalPages = paginationData.lastPage,
                        totalBookings = paginationData.total,
                        isLoading = false,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = response.message ?: "Failed to load bookings"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading bookings")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load bookings"
                )
            }
        }
    }
    
    /**
     * Refresh bookings
     */
    fun refreshBookings() {
        _uiState.value = _uiState.value.copy(currentPage = 1)
        loadBookings()
    }
    
    /**
     * Load next page
     */
    fun loadNextPage() {
        if (!_uiState.value.isLoading && _uiState.value.currentPage < _uiState.value.totalPages) {
            _uiState.value = _uiState.value.copy(currentPage = _uiState.value.currentPage + 1)
            loadBookings(append = true)
        }
    }
    
    /**
     * Handle search - matches iOS onChange behavior
     */
    fun handleSearch(searchText: String) {
        // Cancel previous search job
        searchJob?.cancel()
        
        _uiState.value = _uiState.value.copy(searchText = searchText)
        
        // Debounce search
        searchJob = viewModelScope.launch {
            delay(500)
            if (_uiState.value.searchText == searchText) {
                refreshBookings()
            }
        }
    }
    
    /**
     * Clear search
     */
    fun clearSearch() {
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(
            searchText = ""
        )
        refreshBookings()
    }
    
    /**
     * Set search text (for TextField binding)
     */
    fun setSearchText(text: String) {
        _uiState.value = _uiState.value.copy(searchText = text)
        handleSearch(text)
    }
    
    /**
     * Handle date range selection
     */
    fun handleDateRangeSelection(startDate: Date, endDate: Date) {
        _uiState.value = _uiState.value.copy(
            selectedWeekStart = startDate,
            selectedWeekEnd = endDate
        )
        refreshBookings()
    }
    
    /**
     * Get date range string - matches iOS format
     */
    val dateRangeString: String
        get() {
            val start = _uiState.value.selectedWeekStart
            val end = _uiState.value.selectedWeekEnd
            val daysDifference = ((end.time - start.time) / (1000 * 60 * 60 * 24)).toInt()
            
            return when {
                // Full year (364+ days and same year)
                daysDifference >= 364 && isSameYear(start, end) -> {
                    SimpleDateFormat("yyyy", Locale.US).format(start)
                }
                // Full month (28-31 days and same month)
                daysDifference >= 28 && isSameMonth(start, end) -> {
                    SimpleDateFormat("MMMM yyyy", Locale.US).format(start)
                }
                // Default date range format
                else -> {
                    val startFormat = SimpleDateFormat("MMM dd", Locale.US).format(start)
                    val endFormat = SimpleDateFormat("MMM dd", Locale.US).format(end)
                    "$startFormat - $endFormat"
                }
            }
        }
    
    private fun isSameYear(date1: Date, date2: Date): Boolean {
        calendar.time = date1
        val year1 = calendar.get(Calendar.YEAR)
        calendar.time = date2
        val year2 = calendar.get(Calendar.YEAR)
        return year1 == year2
    }
    
    private fun isSameMonth(date1: Date, date2: Date): Boolean {
        calendar.time = date1
        val month1 = calendar.get(Calendar.MONTH)
        calendar.time = date2
        val month2 = calendar.get(Calendar.MONTH)
        return month1 == month2
    }
    
    // MARK: - Navigation Functions
    
    fun goToPreviousWeek() {
        calendar.time = _uiState.value.selectedWeekStart
        calendar.add(Calendar.DAY_OF_MONTH, -7)
        val newStart = calendar.time
        val newEnd = getWeekEnd(newStart)
        
        _uiState.value = _uiState.value.copy(
            selectedWeekStart = newStart,
            selectedWeekEnd = newEnd
        )
        refreshBookings()
    }
    
    fun goToNextWeek() {
        calendar.time = _uiState.value.selectedWeekStart
        calendar.add(Calendar.DAY_OF_MONTH, 7)
        val newStart = calendar.time
        val newEnd = getWeekEnd(newStart)
        
        _uiState.value = _uiState.value.copy(
            selectedWeekStart = newStart,
            selectedWeekEnd = newEnd
        )
        refreshBookings()
    }
    
    fun goToPreviousMonth() {
        calendar.time = _uiState.value.selectedWeekStart
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        
        val previousMonth = if (currentMonth == 0) 11 else currentMonth - 1
        val previousYear = if (currentMonth == 0) currentYear - 1 else currentYear
        
        calendar.set(previousYear, previousMonth, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val previousMonthStart = calendar.time
        
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        val previousMonthEnd = calendar.time
        
        _uiState.value = _uiState.value.copy(
            selectedWeekStart = previousMonthStart,
            selectedWeekEnd = previousMonthEnd
        )
        refreshBookings()
    }
    
    fun goToNextMonth() {
        calendar.time = _uiState.value.selectedWeekStart
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        
        val nextMonth = if (currentMonth == 11) 0 else currentMonth + 1
        val nextYear = if (currentMonth == 11) currentYear + 1 else currentYear
        
        calendar.set(nextYear, nextMonth, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val nextMonthStart = calendar.time
        
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        val nextMonthEnd = calendar.time
        
        _uiState.value = _uiState.value.copy(
            selectedWeekStart = nextMonthStart,
            selectedWeekEnd = nextMonthEnd
        )
        refreshBookings()
    }
    
    fun goToPreviousYear() {
        calendar.time = _uiState.value.selectedWeekStart
        val currentYear = calendar.get(Calendar.YEAR)
        val previousYear = currentYear - 1
        
        calendar.set(previousYear, 0, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val previousYearStart = calendar.time
        
        calendar.set(previousYear, 11, 31)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        val previousYearEnd = calendar.time
        
        _uiState.value = _uiState.value.copy(
            selectedWeekStart = previousYearStart,
            selectedWeekEnd = previousYearEnd
        )
        refreshBookings()
    }
    
    fun goToNextYear() {
        calendar.time = _uiState.value.selectedWeekStart
        val currentYear = calendar.get(Calendar.YEAR)
        val nextYear = currentYear + 1
        
        calendar.set(nextYear, 0, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val nextYearStart = calendar.time
        
        calendar.set(nextYear, 11, 31)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        val nextYearEnd = calendar.time
        
        _uiState.value = _uiState.value.copy(
            selectedWeekStart = nextYearStart,
            selectedWeekEnd = nextYearEnd
        )
        refreshBookings()
    }
    
    fun handleTimePeriodChange(newPeriod: TimePeriod) {
        _uiState.value = _uiState.value.copy(selectedTimePeriod = newPeriod)
        
        val today = Date()
        when (newPeriod) {
            TimePeriod.WEEKLY -> {
                val weekStart = getWeekStart(today)
                val weekEnd = getWeekEnd(weekStart)
                _uiState.value = _uiState.value.copy(
                    selectedWeekStart = weekStart,
                    selectedWeekEnd = weekEnd
                )
            }
            TimePeriod.MONTHLY -> {
                calendar.time = today
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val monthStart = calendar.time
                
                calendar.add(Calendar.MONTH, 1)
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                val monthEnd = calendar.time
                
                _uiState.value = _uiState.value.copy(
                    selectedWeekStart = monthStart,
                    selectedWeekEnd = monthEnd
                )
            }
            TimePeriod.YEARLY -> {
                calendar.time = today
                calendar.set(Calendar.MONTH, 0)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val yearStart = calendar.time
                
                calendar.set(Calendar.MONTH, 11)
                calendar.set(Calendar.DAY_OF_MONTH, 31)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                val yearEnd = calendar.time
                
                _uiState.value = _uiState.value.copy(
                    selectedWeekStart = yearStart,
                    selectedWeekEnd = yearEnd
                )
            }
            TimePeriod.CUSTOM -> {
                // Keep current selection for custom
            }
        }
        
        refreshBookings()
    }
    
    /**
     * Compute total spent
     */
    val totalSpent: Double
        get() = _uiState.value.bookings.sumOf { it.grandTotal ?: 0.0 }
    
    /**
     * Compute completed rides count
     */
    val completedRides: Int
        get() = _uiState.value.bookings.count { 
            it.bookingStatus.lowercase() == "completed" 
        }
    
    /**
     * Compute pending rides count
     */
    val pendingRides: Int
        get() = _uiState.value.bookings.count { 
            it.bookingStatus.lowercase() == "pending" 
        }
}

/**
 * UI state for My Bookings screen
 */
data class MyBookingsUiState(
    val bookings: List<UserBooking> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchText: String = "",
    val isSearching: Boolean = false,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val totalBookings: Int = 0,
    val selectedWeekStart: Date = Date(),
    val selectedWeekEnd: Date = Date(),
    val selectedTimePeriod: TimePeriod = TimePeriod.WEEKLY
)

