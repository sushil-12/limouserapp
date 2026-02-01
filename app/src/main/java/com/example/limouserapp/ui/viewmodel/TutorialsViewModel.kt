package com.example.limouserapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.limouserapp.data.model.dashboard.TutorialContent
import com.example.limouserapp.data.model.dashboard.TutorialData
import com.example.limouserapp.data.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Tutorials Screen
 * Manages tutorial data fetching and state (same design as limodriverapp)
 */
@HiltViewModel
class TutorialsViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TutorialsUiState>(TutorialsUiState.Loading)
    val uiState: StateFlow<TutorialsUiState> = _uiState.asStateFlow()

    init {
        fetchTutorials()
    }

    fun fetchTutorials() {
        viewModelScope.launch {
            _uiState.value = TutorialsUiState.Loading
            dashboardRepository.getTutorials()
                .onSuccess { data ->
                    val groupedTutorials = data.contents.groupBy { it.category }
                    _uiState.value = TutorialsUiState.Success(
                        tutorialData = data,
                        groupedByCategory = groupedTutorials
                    )
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to fetch tutorials")
                    _uiState.value = TutorialsUiState.Error(error.message ?: "An error occurred")
                }
        }
    }

    fun retry() {
        fetchTutorials()
    }
}

/**
 * UI State for Tutorials Screen
 */
sealed class TutorialsUiState {
    data object Loading : TutorialsUiState()
    data class Success(
        val tutorialData: TutorialData,
        val groupedByCategory: Map<String, List<TutorialContent>>
    ) : TutorialsUiState()
    data class Error(val message: String) : TutorialsUiState()
}
