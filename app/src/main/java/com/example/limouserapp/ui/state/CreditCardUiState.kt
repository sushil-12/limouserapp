package com.example.limouserapp.ui.state

/**
 * UI state for Credit Card Screen
 * Follows single responsibility principle
 */
data class CreditCardUiState(
    val cardNumber: String = "",
    val expiryMonth: String = "",
    val expiryYear: String = "",
    val cvv: String = "",
    val cardHolderName: String = "",
    val location: String = "", // Full address
    val locationDisplay: String = "", // Display text in input field
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val isLocationSelected: Boolean = false, // Track if location was selected from dropdown
    val smsOptIn: Boolean = true, // Default to true to match Swift implementation
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val message: String? = null,
    val isFormValid: Boolean = false,
    val cardType: String? = null,
    val isLocationLoading: Boolean = false,
    val locationSuggestions: List<String> = emptyList(),
    val showLocationSuggestions: Boolean = false
) {
    /**
     * Check if the form is ready for submission
     */
    fun isReadyForSubmission(): Boolean = isFormValid && !isLoading && error == null
    
    /**
     * Get formatted card number for display
     */
    fun getFormattedCardNumber(): String {
        val cleanNumber = cardNumber.replace(" ", "").replace("-", "")
        return cleanNumber.chunked(4).joinToString(" ")
    }
    
    /**
     * Get formatted expiry date
     */
    fun getFormattedExpiry(): String {
        return if (expiryMonth.isNotEmpty() && expiryYear.isNotEmpty()) {
            "$expiryMonth/$expiryYear"
        } else ""
    }
}

/**
 * UI events for Credit Card Screen
 */
sealed class CreditCardUiEvent {
    data class CardNumberChanged(val cardNumber: String) : CreditCardUiEvent()
    data class ExpiryMonthChanged(val month: String) : CreditCardUiEvent()
    data class ExpiryYearChanged(val year: String) : CreditCardUiEvent()
    data class CvvChanged(val cvv: String) : CreditCardUiEvent()
    data class CardHolderNameChanged(val name: String) : CreditCardUiEvent()
    data class LocationChanged(val location: String) : CreditCardUiEvent()
    data class LocationSelected(
        val fullAddress: String,
        val city: String,
        val state: String,
        val zipCode: String,
        val locationDisplay: String
    ) : CreditCardUiEvent()
    data class CityChanged(val city: String) : CreditCardUiEvent()
    data class StateChanged(val state: String) : CreditCardUiEvent()
    data class ZipCodeChanged(val zipCode: String) : CreditCardUiEvent()
    data class SmsOptInChanged(val smsOptIn: Boolean) : CreditCardUiEvent()
    data class LocationSuggestionSelected(val suggestion: String) : CreditCardUiEvent()
    object SubmitCard : CreditCardUiEvent()
    object ClearError : CreditCardUiEvent()
    object DismissLocationSuggestions : CreditCardUiEvent()
}
