package com.example.limouserapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.limouserapp.data.network.error.ErrorHandler
import com.example.limouserapp.domain.usecase.auth.SendVerificationCodeUseCase
import com.example.limouserapp.domain.validation.CountryCode
import com.example.limouserapp.domain.validation.PhoneValidationService
import com.example.limouserapp.domain.validation.ValidationResult
import com.example.limouserapp.ui.state.PhoneEntryUiEvent
import com.example.limouserapp.ui.state.PhoneEntryUiState
import com.example.limouserapp.data.local.SharedDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Phone Entry Screen
 * Follows single responsibility principle and handles phone entry logic
 */
@HiltViewModel
class PhoneEntryViewModel @Inject constructor(
    private val sendVerificationCodeUseCase: SendVerificationCodeUseCase,
    private val phoneValidationService: PhoneValidationService,
    private val errorHandler: ErrorHandler,
    private val sharedDataStore: SharedDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhoneEntryUiState())
    val uiState: StateFlow<PhoneEntryUiState> = _uiState.asStateFlow()

    /**
     * Handle UI events
     */
    fun onEvent(event: PhoneEntryUiEvent) {
        when (event) {
            is PhoneEntryUiEvent.PhoneNumberChanged -> {
                handlePhoneNumberChanged(event.phoneNumber)
            }
            is PhoneEntryUiEvent.CountryCodeChanged -> {
                handleCountryCodeChanged(event.countryCode)
            }
            is PhoneEntryUiEvent.SendVerificationCode -> {
                sendVerificationCode()
            }
            is PhoneEntryUiEvent.ClearError -> {
                clearError()
            }
            is PhoneEntryUiEvent.ClearSuccess -> {
                clearSuccess()
            }
        }
    }

    /**
     * Handle phone number input changes
     * Smart validation: only show errors when user has entered something meaningful
     */
    private fun handlePhoneNumberChanged(phoneNumber: String) {
        // Clean the phone number to get raw digits only
        val rawNumber = phoneNumber.replace(Regex("[^0-9]"), "")
        
        val formattedNumber = phoneValidationService.formatPhoneNumber(
            rawNumber, 
            _uiState.value.selectedCountryCode
        )
        
        // Smart validation: only validate if user has started entering digits
        val validationResult = if (rawNumber.isNotEmpty()) {
            phoneValidationService.validatePhoneNumber(
                rawNumber, 
                _uiState.value.selectedCountryCode
            )
        } else {
            ValidationResult.Success // Don't show error for empty input
        }
        
        // Smart validation: show errors intelligently
        // - Never show errors for empty input
        // - Show format errors immediately (they indicate a real problem)
        // - Only show length errors when user has entered enough digits or too many
        val shouldShowError = when {
            rawNumber.isEmpty() -> false // Never show error for empty input
            validationResult is ValidationResult.Error -> {
                val errorMessage = (validationResult as ValidationResult.Error).message
                // Always show format/validation errors
                errorMessage.contains("Invalid", ignoreCase = true) ||
                // Show length errors only when user has entered enough digits or too many
                (rawNumber.length >= _uiState.value.selectedCountryCode.phoneLength ||
                 rawNumber.length > _uiState.value.selectedCountryCode.phoneLength)
            }
            else -> false
        }
        
        _uiState.value = _uiState.value.copy(
            phoneNumber = formattedNumber,
            rawPhoneNumber = rawNumber,
            isFormValid = validationResult is ValidationResult.Success,
            error = if (shouldShowError && validationResult is ValidationResult.Error) {
                validationResult.message
            } else null
        )
    }

    /**
     * Handle country code changes
     */
    private fun handleCountryCodeChanged(countryCode: CountryCode) {
        val currentRawPhoneNumber = _uiState.value.rawPhoneNumber
        val validationResult = phoneValidationService.validatePhoneNumber(
            currentRawPhoneNumber, 
            countryCode
        )
        
        _uiState.value = _uiState.value.copy(
            selectedCountryCode = countryCode,
            isFormValid = validationResult is ValidationResult.Success,
            error = if (validationResult is ValidationResult.Error) validationResult.message else null
        )
    }

    /**
     * Send verification code
     */
    private fun sendVerificationCode() {
        val currentState = _uiState.value
        
        // Re-validate before submission to show any final errors
        val finalValidation = phoneValidationService.validatePhoneNumber(
            currentState.rawPhoneNumber,
            currentState.selectedCountryCode
        )
        
        if (finalValidation is ValidationResult.Error) {
            _uiState.value = currentState.copy(
                error = finalValidation.message,
                isFormValid = false
            )
            return
        }
        
        if (!currentState.isReadyForSubmission()) return

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, error = null)
            
            try {
                val result = sendVerificationCodeUseCase(
                    phoneNumber = currentState.rawPhoneNumber,
                    countryCode = currentState.selectedCountryCode
                )
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.data != null) {
                            val fullPhoneNumber = "${currentState.selectedCountryCode.code}${currentState.rawPhoneNumber}"
                            
                            // Store data in shared store for OTP screen
                            sharedDataStore.setTempUserId(response.data.tempUserId)
                            sharedDataStore.setPhoneNumber(fullPhoneNumber)
                            
                            _uiState.value = currentState.copy(
                                isLoading = false,
                                success = true,
                                message = "OTP sent successfully",
                                tempUserId = response.data.tempUserId,
                                phoneNumberWithCountryCode = fullPhoneNumber
                            )
                        } else {
                            _uiState.value = currentState.copy(
                                isLoading = false,
                                error = response.message
                            )
                        }
                    },
                    onFailure = { error ->
                        val errorMessage = errorHandler.handleError(error)
                        _uiState.value = currentState.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                        Timber.e(error, "Failed to send verification code")
                    }
                )
            } catch (e: Exception) {
                val errorMessage = errorHandler.handleError(e)
                _uiState.value = currentState.copy(
                    isLoading = false,
                    error = errorMessage
                )
                Timber.e(e, "Unexpected error sending verification code")
            }
        }
    }

    /**
     * Clear error state
     */
    private fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun clearSuccess() {
        _uiState.value = _uiState.value.copy(success = false, tempUserId = "", phoneNumberWithCountryCode = "")
    }

    /**
     * Get formatted phone number for display
     */
    fun getFormattedPhoneNumber(): String {
        return phoneValidationService.formatPhoneNumber(
            _uiState.value.phoneNumber,
            _uiState.value.selectedCountryCode
        )
    }
}
