package com.example.limouserapp.ui.state

import com.example.limouserapp.domain.validation.CountryCode

/**
 * UI state for Phone Entry Screen
 * Follows single responsibility principle
 */
data class PhoneEntryUiState(
    val phoneNumber: String = "",
    val rawPhoneNumber: String = "",
    val selectedCountryCode: CountryCode = CountryCode.US,
    val phoneLength: Int = CountryCode.US.phoneLength, // Store actual phone length from Country model
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val message: String? = null,
    val isFormValid: Boolean = false,
    val tempUserId: String = "",
    val phoneNumberWithCountryCode: String = ""
) {
    /**
     * Check if the form is ready for submission
     */
    fun isReadyForSubmission(): Boolean = isFormValid && !isLoading && error == null
}

/**
 * UI events for Phone Entry Screen
 */
sealed class PhoneEntryUiEvent {
    data class PhoneNumberChanged(val phoneNumber: String) : PhoneEntryUiEvent()
    data class CountryCodeChanged(val countryCode: CountryCode, val phoneLength: Int) : PhoneEntryUiEvent()
    object SendVerificationCode : PhoneEntryUiEvent()
    object ClearError : PhoneEntryUiEvent()
    object ClearSuccess : PhoneEntryUiEvent()
}
