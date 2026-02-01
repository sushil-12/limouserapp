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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    
    // Job for debounced validation
    private var validationJob: Job? = null
    /**
     * Handle UI events
     */
    fun onEvent(event: PhoneEntryUiEvent) {
        when (event) {
            is PhoneEntryUiEvent.PhoneNumberChanged -> {
                handlePhoneNumberChanged(event.phoneNumber)
            }
            is PhoneEntryUiEvent.CountryCodeChanged -> {
                handleCountryCodeChanged(event.countryCode, event.phoneLength)
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
     * Debounced validation: only show errors after user stops typing for 800ms
     */
    private fun handlePhoneNumberChanged(phoneNumber: String) {
        // Cancel any existing validation job
        validationJob?.cancel()
        
        // Clean to digits only
        val rawNumber = phoneNumber.replace(Regex("[^0-9]"), "")
        
        // Limit to max 15 digits
        val limitedRawNumber = if (rawNumber.length > 15) rawNumber.substring(0, 15) else rawNumber
        
        val displayNumber = limitedRawNumber
        
        // Immediately update the phone number without showing errors
        _uiState.value = _uiState.value.copy(
            phoneNumber = displayNumber,
            rawPhoneNumber = limitedRawNumber,
            error = null // Clear any previous errors while typing
        )
        
        // Start debounced validation - only validate after user stops typing
        validationJob = viewModelScope.launch {
            // Wait for 800ms - if user types again, this job will be cancelled
            delay(800)
            
            val validationResult = if (limitedRawNumber.isNotEmpty()) {
                phoneValidationService.validatePhoneNumber(
                    limitedRawNumber,
                    _uiState.value.selectedCountryCode
                )
            } else {
                ValidationResult.Success
            }
            
            // Show error only if input is non-empty and invalid
            val shouldShowError = limitedRawNumber.isNotEmpty() && validationResult is ValidationResult.Error
            
            _uiState.value = _uiState.value.copy(
                isFormValid = validationResult is ValidationResult.Success && limitedRawNumber.isNotEmpty(),
                error = if (shouldShowError) {
                    (validationResult as ValidationResult.Error).message
                } else null
            )
        }
    }
    /**
     * Handle country code changes
     */
    private fun handleCountryCodeChanged(countryCode: CountryCode, phoneLength: Int) {
        val currentRawPhoneNumber = _uiState.value.rawPhoneNumber
        
        // If there's no phone number input, just update the country and phone length
        if (currentRawPhoneNumber.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                selectedCountryCode = countryCode,
                phoneLength = phoneLength,
                isFormValid = false,
                error = null
            )
            return
        }
        
        // Re-validate with new country code
        val validationResult = phoneValidationService.validatePhoneNumber(
            currentRawPhoneNumber,
            countryCode
        )
        
        val shouldShowError = currentRawPhoneNumber.isNotEmpty() && validationResult is ValidationResult.Error
        
        _uiState.value = _uiState.value.copy(
            selectedCountryCode = countryCode,
            phoneLength = phoneLength,
            isFormValid = (validationResult is ValidationResult.Success) && currentRawPhoneNumber.isNotEmpty(),
            error = if (shouldShowError) (validationResult as ValidationResult.Error).message else null
        )
    }
    /**
     * Send verification code
     */
    private fun sendVerificationCode() {
        val currentState = _uiState.value
        
        if (currentState.rawPhoneNumber.isEmpty()) {
            _uiState.value = currentState.copy(
                error = "Please enter your phone number",
                isFormValid = false
            )
            return
        }
        
        // Strict validation for submission
        val finalValidation = phoneValidationService.validatePhoneNumber(
            currentState.rawPhoneNumber,
            currentState.selectedCountryCode
        )
        
        if (finalValidation is ValidationResult.Error) {
            _uiState.value = currentState.copy(
                error = "Please enter a valid phone number",
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