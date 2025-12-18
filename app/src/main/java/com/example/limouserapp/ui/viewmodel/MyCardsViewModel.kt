package com.example.limouserapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.limouserapp.data.api.DashboardApi
import com.example.limouserapp.data.model.dashboard.*
import com.example.limouserapp.utils.CardValidationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for My Cards screen
 * Handles card management, validation, and API calls
 */
@HiltViewModel
class MyCardsViewModel @Inject constructor(
    private val dashboardApi: DashboardApi
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(MyCardsUiState())
    val uiState: StateFlow<MyCardsUiState> = _uiState.asStateFlow()
    
    init {
        loadProfileData()
    }
    
    /**
     * Load profile data from API
     */
    fun loadProfileData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = "")
            
            try {
                val response = dashboardApi.getProfileData()
                
                if (response.success) {
                    val profileData = response.data
                    val savedCards = profileData.cards
                        .sortedWith(compareByDescending<CardData> { it.isPrimary }
                            .thenByDescending { it.id })
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        savedCards = savedCards,
                        cardHolderName = "${profileData.firstName} ${profileData.lastName}"
                    )
                    Timber.d("Profile data loaded successfully. Cards count: ${savedCards.size}")
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = response.message
                    )
                    Timber.e("Failed to load profile data: ${response.message}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading profile data")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load profile data"
                )
            }
        }
    }
    
    /**
     * Update card holder name
     */
    fun updateCardHolderName(name: String) {
        _uiState.value = _uiState.value.copy(cardHolderName = name)
    }
    
    /**
     * Update card number with formatting
     */
    fun updateCardNumber(number: String) {
        val formatted = CardValidationService.formatCardNumber(number)
        _uiState.value = _uiState.value.copy(cardNumber = formatted)
    }
    
    /**
     * Update expiry month
     */
    fun updateExpiryMonth(month: String) {
        _uiState.value = _uiState.value.copy(expiryMonth = month)
    }
    
    /**
     * Update expiry year
     */
    fun updateExpiryYear(year: String) {
        _uiState.value = _uiState.value.copy(expiryYear = year)
    }
    
    /**
     * Update CVV with formatting
     */
    fun updateCVV(cvv: String) {
        val formatted = CardValidationService.formatCVV(cvv)
        _uiState.value = _uiState.value.copy(cvv = formatted)
    }
    
    /**
     * Toggle primary card status
     */
    fun toggleIsPrimary() {
        _uiState.value = _uiState.value.copy(isPrimary = !_uiState.value.isPrimary)
    }
    
    /**
     * Toggle edit mode
     */
    fun toggleEditMode() {
        _uiState.value = _uiState.value.copy(isEditing = !_uiState.value.isEditing)
    }
    
    /**
     * Reset form
     */
    fun resetForm() {
        _uiState.value = _uiState.value.copy(
            cardNumber = "",
            expiryMonth = "",
            expiryYear = "",
            cvv = "",
            isPrimary = false
        )
    }
    
    /**
     * Save card
     */
    fun saveCard() {
        val state = _uiState.value
        
        Timber.d("💳 MyCardsViewModel - Starting save card process...")
        Timber.d("📋 Form data:")
        Timber.d("   - Card Holder: ${state.cardHolderName}")
        Timber.d("   - Card Number: ${state.cardNumber}")
        Timber.d("   - CVV: ${state.cvv}")
        Timber.d("   - Exp Month: ${state.expiryMonth}")
        Timber.d("   - Exp Year: ${state.expiryYear}")
        Timber.d("   - Is Primary: ${state.isPrimary}")
        
        // Validate form
        val validation = CardValidationService.validateCardForm(
            cardNumber = state.cardNumber,
            cvv = state.cvv,
            expMonth = state.expiryMonth,
            expYear = state.expiryYear,
            cardHolderName = state.cardHolderName
        )
        
        if (!validation.isValid) {
            Timber.e("Validation failed: ${validation.message}")
            _uiState.value = state.copy(
                showErrorAlert = true,
                errorMessage = validation.message ?: "Please check your input"
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, errorMessage = "")
            
            try {
                // Prepare request
                val cleanCardNumber = state.cardNumber.filter { it.isDigit() }
                val request = AddCreditCardRequest(
                    cardType = "personal",
                    number = cleanCardNumber,
                    cvc = state.cvv,
                    expMonth = state.expiryMonth,
                    expYear = state.expiryYear,
                    name = state.cardHolderName
                )
                
                Timber.d("📤 Sending request: card_type=${request.cardType}, number length=${request.number.length}")
                
                val response = dashboardApi.addCreditCard(request)
                
                Timber.d("📥 Received response: success=${response.success}, message=${response.message}")
                
                if (response.success) {
                    _uiState.value = state.copy(
                        isLoading = false,
                        showSuccessAlert = true,
                        successMessage = response.message.ifEmpty { "Card added successfully" }
                    )
                    resetForm()
                    // Reload profile data to get updated cards
                    loadProfileData()
                    Timber.d("✅ Card added successfully")
                } else {
                    _uiState.value = state.copy(
                        isLoading = false,
                        showErrorAlert = true,
                        errorMessage = response.message.ifEmpty { "Failed to add card" }
                    )
                    Timber.e("❌ Card addition failed: ${response.message}")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error adding credit card")
                _uiState.value = state.copy(
                    isLoading = false,
                    showErrorAlert = true,
                    errorMessage = e.message ?: "Failed to add card. Please try again."
                )
            }
        }
    }
    
    /**
     * Dismiss success alert
     */
    fun dismissSuccessAlert() {
        _uiState.value = _uiState.value.copy(showSuccessAlert = false)
    }
    
    /**
     * Dismiss error alert
     */
    fun dismissErrorAlert() {
        _uiState.value = _uiState.value.copy(showErrorAlert = false)
    }
}

/**
 * UI state for My Cards screen
 */
data class MyCardsUiState(
    val isLoading: Boolean = false,
    val savedCards: List<CardData> = emptyList(),
    val cardHolderName: String = "",
    val cardNumber: String = "",
    val expiryMonth: String = "",
    val expiryYear: String = "",
    val cvv: String = "",
    val isPrimary: Boolean = false,
    val isEditing: Boolean = false,
    val showSuccessAlert: Boolean = false,
    val successMessage: String = "",
    val showErrorAlert: Boolean = false,
    val errorMessage: String = ""
) {
    /**
     * Get latest card (most recent or primary)
     */
    val latestCard: CardData?
        get() = savedCards.firstOrNull()
    
    /**
     * Check if form is valid
     */
    val isFormValid: Boolean
        get() = cardHolderName.isNotBlank() &&
                cardNumber.isNotBlank() &&
                expiryMonth.isNotBlank() &&
                expiryYear.isNotBlank() &&
                cvv.isNotBlank() &&
                expiryMonth != "MM" &&
                expiryYear != "YYYY"
}

