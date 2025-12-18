package com.example.limouserapp.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.limouserapp.data.network.error.ErrorHandler
import com.example.limouserapp.data.local.UserStateManager
import com.example.limouserapp.ui.navigation.AuthStateManager
import com.example.limouserapp.data.repository.LocationRepository
import com.example.limouserapp.domain.usecase.registration.SubmitCreditCardUseCase
import com.example.limouserapp.domain.validation.CreditCardValidationService
import com.example.limouserapp.domain.validation.ValidationResult
import com.example.limouserapp.ui.state.CreditCardUiEvent
import com.example.limouserapp.ui.state.CreditCardUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Credit Card Screen
 * Follows single responsibility principle and handles credit card submission logic
 */
@HiltViewModel
class CreditCardViewModel @Inject constructor(
    private val submitCreditCardUseCase: SubmitCreditCardUseCase,
    private val creditCardValidationService: CreditCardValidationService,
    private val locationRepository: LocationRepository,
    private val errorHandler: ErrorHandler,
    private val userStateManager: UserStateManager,
    private val authStateManager: AuthStateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreditCardUiState())
    val uiState: StateFlow<CreditCardUiState> = _uiState.asStateFlow()

    private var locationSearchJob: Job? = null

    /**
     * Handle UI events
     */
    fun onEvent(event: CreditCardUiEvent) {
        when (event) {
            is CreditCardUiEvent.CardNumberChanged -> {
                handleCardNumberChanged(event.cardNumber)
            }
            is CreditCardUiEvent.ExpiryMonthChanged -> {
                handleExpiryMonthChanged(event.month)
            }
            is CreditCardUiEvent.ExpiryYearChanged -> {
                handleExpiryYearChanged(event.year)
            }
            is CreditCardUiEvent.CvvChanged -> {
                handleCvvChanged(event.cvv)
            }
            is CreditCardUiEvent.CardHolderNameChanged -> {
                handleCardHolderNameChanged(event.name)
            }
            is CreditCardUiEvent.LocationChanged -> {
                handleLocationChanged(event.location)
            }
            is CreditCardUiEvent.LocationSelected -> {
                handleLocationSelected(event.fullAddress, event.city, event.state, event.zipCode, event.locationDisplay)
            }
            is CreditCardUiEvent.CityChanged -> {
                handleCityChanged(event.city)
            }
            is CreditCardUiEvent.StateChanged -> {
                handleStateChanged(event.state)
            }
            is CreditCardUiEvent.ZipCodeChanged -> {
                handleZipCodeChanged(event.zipCode)
            }
            is CreditCardUiEvent.SmsOptInChanged -> {
                handleSmsOptInChanged(event.smsOptIn)
            }
            is CreditCardUiEvent.LocationSuggestionSelected -> {
                handleLocationSuggestionSelected(event.suggestion)
            }
            is CreditCardUiEvent.SubmitCard -> {
                submitCreditCard()
            }
            is CreditCardUiEvent.ClearError -> {
                clearError()
            }
            is CreditCardUiEvent.DismissLocationSuggestions -> {
                dismissLocationSuggestions()
            }
        }
    }

    /**
     * Handle card number changes
     */
    private fun handleCardNumberChanged(cardNumber: String) {
        val formattedNumber = creditCardValidationService.formatCardNumber(cardNumber)
        val cardType = creditCardValidationService.getCardType(formattedNumber)
        
        _uiState.value = _uiState.value.copy(
            cardNumber = formattedNumber,
            cardType = cardType.displayName,
            isFormValid = isFormValid()
        )
    }

    /**
     * Handle expiry month changes
     */
    private fun handleExpiryMonthChanged(month: String) {
        _uiState.value = _uiState.value.copy(
            expiryMonth = month,
            isFormValid = isFormValid()
        )
    }

    /**
     * Handle expiry year changes
     */
    private fun handleExpiryYearChanged(year: String) {
        _uiState.value = _uiState.value.copy(
            expiryYear = year,
            isFormValid = isFormValid()
        )
    }

    /**
     * Handle CVV changes
     */
    private fun handleCvvChanged(cvv: String) {
        _uiState.value = _uiState.value.copy(
            cvv = cvv,
            isFormValid = isFormValid()
        )
    }

    /**
     * Handle card holder name changes
     */
    private fun handleCardHolderNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(
            cardHolderName = name,
            isFormValid = isFormValid()
        )
    }

    /**
     * Handle location changes with autocomplete
     */
    private fun handleLocationChanged(location: String) {
        val updatedState = _uiState.value.copy(
            locationDisplay = location,
            location = "", // Reset full address when user types
            isLocationSelected = false, // Reset selection flag when user types
            showLocationSuggestions = location.length >= 2
        )
        
        // Re-validate form (will be invalid since location is not selected)
        val validationResult = creditCardValidationService.validateCardForm(
            cardNumber = updatedState.cardNumber,
            cvv = updatedState.cvv,
            expMonth = updatedState.expiryMonth,
            expYear = updatedState.expiryYear,
            cardHolderName = updatedState.cardHolderName
        )
        
        _uiState.value = updatedState.copy(
            locationSuggestions = if (location.length >= 2) updatedState.locationSuggestions else emptyList(),
            showLocationSuggestions = location.length >= 2,
            isFormValid = validationResult is ValidationResult.Success && 
                         updatedState.isLocationSelected && 
                         updatedState.location.isNotEmpty()
        )
        
        if (location.length >= 2) {
            searchLocationSuggestions(location)
        }
    }
    
    /**
     * Handle location selection from autocomplete dropdown
     */
    private fun handleLocationSelected(
        fullAddress: String,
        city: String,
        state: String,
        zipCode: String,
        locationDisplay: String
    ) {
        Timber.d("handleLocationSelected - FullAddress: $fullAddress, City: $city, State: $state, ZipCode: $zipCode, Display: $locationDisplay")
        
        val updatedState = _uiState.value.copy(
            location = fullAddress,
            locationDisplay = locationDisplay,
            city = city,
            state = state,
            zipCode = zipCode,
            isLocationSelected = true,
            showLocationSuggestions = false,
            locationSuggestions = emptyList()
        )
        
        // Re-validate form with updated state
        val validationResult = creditCardValidationService.validateCardForm(
            cardNumber = updatedState.cardNumber,
            cvv = updatedState.cvv,
            expMonth = updatedState.expiryMonth,
            expYear = updatedState.expiryYear,
            cardHolderName = updatedState.cardHolderName
        )
        
        _uiState.value = updatedState.copy(
            isFormValid = validationResult is ValidationResult.Success && 
                         updatedState.isLocationSelected && 
                         updatedState.location.isNotEmpty()
        )
    }

    /**
     * Handle city changes
     */
    private fun handleCityChanged(city: String) {
        _uiState.value = _uiState.value.copy(city = city)
    }

    /**
     * Handle state changes
     */
    private fun handleStateChanged(state: String) {
        _uiState.value = _uiState.value.copy(state = state)
    }

    /**
     * Handle zip code changes
     */
    private fun handleZipCodeChanged(zipCode: String) {
        _uiState.value = _uiState.value.copy(zipCode = zipCode)
    }

    /**
     * Handle SMS opt-in changes
     */
    private fun handleSmsOptInChanged(smsOptIn: Boolean) {
        _uiState.value = _uiState.value.copy(smsOptIn = smsOptIn)
    }

    /**
     * Handle location suggestion selection
     */
    private fun handleLocationSuggestionSelected(suggestion: String) {
        _uiState.value = _uiState.value.copy(
            location = suggestion,
            showLocationSuggestions = false,
            locationSuggestions = emptyList()
        )
        
        // Fetch location details to populate city, state, zip code
        fetchLocationDetails(suggestion)
    }

    /**
     * Submit credit card
     */
    private fun submitCreditCard() {
        val currentState = _uiState.value
        if (!currentState.isReadyForSubmission()) return

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, error = null)
            
            try {
                val result = submitCreditCardUseCase(
                    cardNumber = currentState.cardNumber,
                    expMonth = currentState.expiryMonth,
                    expYear = currentState.expiryYear,
                    cvc = currentState.cvv,
                    cardHolderName = currentState.cardHolderName,
                    location = currentState.location,
                    city = currentState.city,
                    state = currentState.state,
                    zipCode = currentState.zipCode,
                    smsOptIn = currentState.smsOptIn
                )
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success) {
                            // Store next_step and account_id if available
                            response.data?.let { data ->
                                data.next_step?.let { nextStep ->
                                    userStateManager.setNextStep(nextStep)
                                }
                                data.account_id?.let { accountId ->
                                    userStateManager.setAccountId(accountId)
                                }
                            }
                            
                            // Persist profile completion and update centralized auth state
                            userStateManager.setProfileCompleted(true)
                            authStateManager.updateUserState(isProfileCompleted = true)

                            _uiState.value = currentState.copy(
                                isLoading = false,
                                success = true,
                                message = "Credit card added successfully"
                            )
                        } else {
                            _uiState.value = currentState.copy(
                                isLoading = false,
                                error = response.message
                            )
                        }
                    },
                    onFailure = { error ->
                        val errorMessage = errorHandler.handleApiError(error)
                        _uiState.value = currentState.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                        Timber.e(error, "Failed to submit credit card")
                    }
                )
            } catch (e: Exception) {
                val errorMessage = errorHandler.handleApiError(e)
                _uiState.value = currentState.copy(
                    isLoading = false,
                    error = errorMessage
                )
                Timber.e(e, "Unexpected error submitting credit card")

            }
        }
    }

    /**
     * Search location suggestions
     */
    private fun searchLocationSuggestions(query: String) {
        locationSearchJob?.cancel()
        locationSearchJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLocationLoading = true)
            
            try {
                val result = locationRepository.getPlaceSuggestions(query)
                if (result.isSuccess) {
                    val response = result.getOrThrow()
                    val suggestions = response.predictions.map { it.description }
                    _uiState.value = _uiState.value.copy(
                        locationSuggestions = suggestions,
                        isLocationLoading = false
                    )
                } else {
                    val error = result.exceptionOrNull()
                    Timber.e(error, "Failed to fetch location suggestions")
                    _uiState.value = _uiState.value.copy(
                        locationSuggestions = emptyList(),
                        isLocationLoading = false
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error fetching location suggestions")
                _uiState.value = _uiState.value.copy(
                    locationSuggestions = emptyList(),
                    isLocationLoading = false
                )
            }
        }
    }

    /**
     * Fetch location details
     */
    private fun fetchLocationDetails(placeId: String) {
        viewModelScope.launch {
            try {
                val result = locationRepository.getLocationInfo(placeId)
                if (result.isSuccess) {
                    val locationInfo = result.getOrThrow()
                    _uiState.value = _uiState.value.copy(
                        city = locationInfo.city ?: "",
                        state = locationInfo.state ?: "",
                        zipCode = locationInfo.postalCode ?: ""
                    )
                } else {
                    val error = result.exceptionOrNull()
                    Timber.e(error, "Failed to fetch location details")
                }
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error fetching location details")
            }
        }
    }

    /**
     * Check if form is valid
     */
    private fun isFormValid(): Boolean {
        val currentState = _uiState.value
        val validationResult = creditCardValidationService.validateCardForm(
            cardNumber = currentState.cardNumber,
            cvv = currentState.cvv,
            expMonth = currentState.expiryMonth,
            expYear = currentState.expiryYear,
            cardHolderName = currentState.cardHolderName
        )
        
        // Form is valid only if card validation passes AND location is selected from dropdown
        return validationResult is ValidationResult.Success && 
               currentState.isLocationSelected && 
               currentState.location.isNotEmpty()
    }

    /**
     * Dismiss location suggestions
     */
    private fun dismissLocationSuggestions() {
        _uiState.value = _uiState.value.copy(
            showLocationSuggestions = false,
            locationSuggestions = emptyList()
        )
    }

    /**
     * Clear error state
     */
    private fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        locationSearchJob?.cancel()
    }
}
