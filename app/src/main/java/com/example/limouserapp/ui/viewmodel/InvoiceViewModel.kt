package com.example.limouserapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.limouserapp.data.api.DashboardApi
import com.example.limouserapp.data.model.dashboard.Invoice
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
 * Time period enumeration for invoice filtering
 */
enum class TimePeriod(val displayName: String) {
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly"),
    CUSTOM("Custom")
}

/**
 * ViewModel for Invoices screen
 * Handles invoice list, filtering, search, and pagination
 */
@HiltViewModel
class InvoiceViewModel @Inject constructor(
    private val dashboardApi: DashboardApi
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(InvoiceUiState())
    val uiState: StateFlow<InvoiceUiState> = _uiState.asStateFlow()
    
    private val calendar = Calendar.getInstance()
    private var searchJob: Job? = null
    
    init {
        setupInitialDateRange()
        fetchInvoices()
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
            selectedWeekEnd = weekEnd
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
     * Fetch invoices from API
     */
    fun fetchInvoices(append: Boolean = false) {
        viewModelScope.launch {
            try {
                if (!append) {
                    _uiState.value = _uiState.value.copy(
                        currentPage = 1,
                        invoices = emptyList()
                    )
                }
                
                _uiState.value = _uiState.value.copy(isLoading = true, showError = false)
                
                val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val fromDate = dateFormatter.format(_uiState.value.selectedWeekStart)
                val toDate = dateFormatter.format(_uiState.value.selectedWeekEnd)
                
                val useDateFilter = if (_uiState.value.selectedTimePeriod == TimePeriod.CUSTOM) "true" else "false"
                
                val response = dashboardApi.getInvoices(
                    fromDate = fromDate,
                    toDate = toDate,
                    search = _uiState.value.searchText.takeIf { it.isNotEmpty() },
                    useDateFilter = useDateFilter,
                    page = _uiState.value.currentPage,
                    perPage = 20
                )
                
                if (response.success) {
                    _uiState.value = _uiState.value.copy(
                        invoices = if (append) {
                            _uiState.value.invoices + response.data.data
                        } else {
                            response.data.data
                        },
                        currentPage = response.data.currentPage,
                        totalPages = response.data.lastPage,
                        totalInvoices = response.data.total,
                        isLoading = false,
                        showError = false,
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        showError = true,
                        errorMessage = response.message
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Error fetching invoices")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showError = true,
                    errorMessage = e.message ?: "Failed to fetch invoices"
                )
            }
        }
    }
    
    /**
     * Refresh invoices
     */
    fun refreshInvoices() {
        _uiState.value = _uiState.value.copy(currentPage = 1)
        fetchInvoices()
    }
    
    /**
     * Load next page
     */
    fun loadNextPage() {
        if (!_uiState.value.isLoading && _uiState.value.currentPage < _uiState.value.totalPages) {
            _uiState.value = _uiState.value.copy(currentPage = _uiState.value.currentPage + 1)
            fetchInvoices(append = true)
        }
    }
    
    /**
     * Handle search with debounce
     */
    fun handleSearch(searchText: String) {
        _uiState.value = _uiState.value.copy(searchText = searchText)
        
        // Cancel previous search job
        searchJob?.cancel()
        
        // Create new search job with debounce
        searchJob = viewModelScope.launch {
            delay(500)
            if (_uiState.value.searchText == searchText) {
                refreshInvoices()
            }
        }
    }
    
    /**
     * Set search text (for UI binding)
     */
    fun setSearchText(text: String) {
        _uiState.value = _uiState.value.copy(searchText = text)
    }
    
    /**
     * Clear search
     */
    fun clearSearch() {
        _uiState.value = _uiState.value.copy(searchText = "")
        refreshInvoices()
    }
    
    // MARK: - Date Range Management
    
    /**
     * Get date range string for display
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
        refreshInvoices()
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
        refreshInvoices()
    }
    
    fun goToPreviousMonth() {
        calendar.time = _uiState.value.selectedWeekStart
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        
        val previousMonth = if (currentMonth == 0) 11 else currentMonth - 1
        val previousYear = if (currentMonth == 0) currentYear - 1 else currentYear
        
        calendar.set(previousYear, previousMonth, 1)
        val previousMonthStart = calendar.time
        
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val previousMonthEnd = calendar.time
        
        _uiState.value = _uiState.value.copy(
            selectedWeekStart = previousMonthStart,
            selectedWeekEnd = previousMonthEnd
        )
        refreshInvoices()
    }
    
    fun goToNextMonth() {
        calendar.time = _uiState.value.selectedWeekStart
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)
        
        val nextMonth = if (currentMonth == 11) 0 else currentMonth + 1
        val nextYear = if (currentMonth == 11) currentYear + 1 else currentYear
        
        calendar.set(nextYear, nextMonth, 1)
        val nextMonthStart = calendar.time
        
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        val nextMonthEnd = calendar.time
        
        _uiState.value = _uiState.value.copy(
            selectedWeekStart = nextMonthStart,
            selectedWeekEnd = nextMonthEnd
        )
        refreshInvoices()
    }
    
    fun goToPreviousYear() {
        calendar.time = _uiState.value.selectedWeekStart
        val currentYear = calendar.get(Calendar.YEAR)
        val previousYear = currentYear - 1
        
        calendar.set(previousYear, 0, 1)
        val previousYearStart = calendar.time
        
        calendar.set(previousYear, 11, 31)
        val previousYearEnd = calendar.time
        
        _uiState.value = _uiState.value.copy(
            selectedWeekStart = previousYearStart,
            selectedWeekEnd = previousYearEnd
        )
        refreshInvoices()
    }
    
    fun goToNextYear() {
        calendar.time = _uiState.value.selectedWeekStart
        val currentYear = calendar.get(Calendar.YEAR)
        val nextYear = currentYear + 1
        
        calendar.set(nextYear, 0, 1)
        val nextYearStart = calendar.time
        
        calendar.set(nextYear, 11, 31)
        val nextYearEnd = calendar.time
        
        _uiState.value = _uiState.value.copy(
            selectedWeekStart = nextYearStart,
            selectedWeekEnd = nextYearEnd
        )
        refreshInvoices()
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
        
        refreshInvoices()
    }
    
    fun handleDateRangeSelection(startDate: Date, endDate: Date) {
        _uiState.value = _uiState.value.copy(
            selectedWeekStart = startDate,
            selectedWeekEnd = endDate
        )
        refreshInvoices()
    }
    
    // MARK: - Computed Properties
    
    val totalSpent: Double
        get() = _uiState.value.invoices.sumOf { it.bookingTotal / 100.0 }
    
    val paidInvoices: Int
        get() = _uiState.value.invoices.count { 
            val status = it.status.lowercase()
            status == "paid" || status == "paid_cash"
        }
    
    val pendingInvoices: Int
        get() = _uiState.value.invoices.count { 
            it.status.lowercase() == "pending"
        }
}

/**
 * UI state for Invoices screen
 */
data class InvoiceUiState(
    val invoices: List<Invoice> = emptyList(),
    val isLoading: Boolean = false,
    val showError: Boolean = false,
    val errorMessage: String? = null,
    val searchText: String = "",
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val totalInvoices: Int = 0,
    val selectedWeekStart: Date = Date(),
    val selectedWeekEnd: Date = Date(),
    val selectedTimePeriod: TimePeriod = TimePeriod.WEEKLY
)

