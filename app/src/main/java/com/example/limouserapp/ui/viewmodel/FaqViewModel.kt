package com.example.limouserapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.limouserapp.data.model.dashboard.FaqData
import com.example.limouserapp.data.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for FAQ Screen
 * Manages FAQ data fetching and state (same design as limodriverapp)
 */
@HiltViewModel
class FaqViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FaqUiState>(FaqUiState.Loading)
    val uiState: StateFlow<FaqUiState> = _uiState.asStateFlow()

    init {
        fetchFaq()
    }

    fun fetchFaq() {
        viewModelScope.launch {
            _uiState.value = FaqUiState.Loading
            dashboardRepository.getFaq()
                .onSuccess { faqData ->
                    _uiState.value = FaqUiState.Success(faqData)
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to fetch FAQs")
                    _uiState.value = FaqUiState.Error(error.message ?: "An error occurred")
                }
        }
    }

    fun retry() {
        fetchFaq()
    }
}

/**
 * UI State for FAQ Screen
 */
sealed class FaqUiState {
    data object Loading : FaqUiState()
    data class Success(val faqData: FaqData) : FaqUiState()
    data class Error(val message: String) : FaqUiState()
}
