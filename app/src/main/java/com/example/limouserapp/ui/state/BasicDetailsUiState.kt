package com.example.limouserapp.ui.state

/**
 * UI state for Basic Details Screen
 * Follows single responsibility principle
 */
data class BasicDetailsUiState(
    val name: String = "",
    val email: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val message: String? = null,
    val isFormValid: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null
) {
    /**
     * Check if the form is ready for submission
     */
    fun isReadyForSubmission(): Boolean = isFormValid && !isLoading
}

/**
 * UI events for Basic Details Screen
 */
sealed class BasicDetailsUiEvent {
    data class NameChanged(val name: String) : BasicDetailsUiEvent()
    data class EmailChanged(val email: String) : BasicDetailsUiEvent()
    object SubmitDetails : BasicDetailsUiEvent()
    object ClearError : BasicDetailsUiEvent()
}
