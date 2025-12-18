package com.example.limouserapp.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.limouserapp.data.local.SharedDataStore
import com.example.limouserapp.data.local.UserStateManager
import com.example.limouserapp.data.network.error.ErrorHandler
import com.example.limouserapp.domain.usecase.registration.SubmitBasicDetailsUseCase
import com.example.limouserapp.domain.validation.EmailValidationService
import com.example.limouserapp.domain.validation.ValidationResult
import com.example.limouserapp.ui.state.BasicDetailsUiEvent
import com.example.limouserapp.ui.state.BasicDetailsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for Basic Details Screen
 * Follows single responsibility principle and handles basic details submission logic
 */
@HiltViewModel
class BasicDetailsViewModel @Inject constructor(
    private val submitBasicDetailsUseCase: SubmitBasicDetailsUseCase,
    private val emailValidationService: EmailValidationService,
    private val errorHandler: ErrorHandler,
    private val sharedDataStore: SharedDataStore,
    private val userStateManager: UserStateManager,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private const val KEY_NAME = "basic_details_name"
        private const val KEY_EMAIL = "basic_details_email"
    }

    private val _uiState = MutableStateFlow(
        BasicDetailsUiState(
            name = savedStateHandle.get<String>(KEY_NAME) 
                ?: userStateManager.getBasicDetailsName() 
                ?: "",
            email = savedStateHandle.get<String>(KEY_EMAIL) 
                ?: userStateManager.getBasicDetailsEmail() 
                ?: ""
        )
    )
    val uiState: StateFlow<BasicDetailsUiState> = _uiState.asStateFlow()

    init {
        // If we have data from SavedStateHandle or UserStateManager, ensure it's synced
        val savedName = savedStateHandle.get<String>(KEY_NAME) ?: userStateManager.getBasicDetailsName() ?: ""
        val savedEmail = savedStateHandle.get<String>(KEY_EMAIL) ?: userStateManager.getBasicDetailsEmail() ?: ""
        
        if (savedName.isNotEmpty() || savedEmail.isNotEmpty()) {
            savedStateHandle[KEY_NAME] = savedName
            savedStateHandle[KEY_EMAIL] = savedEmail
            
            // Validate the loaded data and update form validation state
            val nameValidation = validateName(savedName)
            val emailValidation = emailValidationService.validateEmail(savedEmail)
            val isValid = nameValidation is ValidationResult.Success && 
                         emailValidation is ValidationResult.Success
            
            _uiState.value = _uiState.value.copy(
                name = savedName,
                email = savedEmail,
                nameError = if (nameValidation is ValidationResult.Error) nameValidation.message else null,
                emailError = if (emailValidation is ValidationResult.Error) emailValidation.message else null,
                isFormValid = isValid
            )
            
            Timber.d("BasicDetailsViewModel: Loaded and validated data - name='$savedName', email='$savedEmail', isValid=$isValid")
        } else {
            Timber.d("BasicDetailsViewModel: Initialized with name='${_uiState.value.name}', email='${_uiState.value.email}'")
        }
    }

    /**
     * Handle UI events
     */
    fun onEvent(event: BasicDetailsUiEvent) {
        when (event) {
            is BasicDetailsUiEvent.NameChanged -> {
                handleNameChanged(event.name)
            }
            is BasicDetailsUiEvent.EmailChanged -> {
                handleEmailChanged(event.email)
            }
            is BasicDetailsUiEvent.SubmitDetails -> {
                submitBasicDetails()
            }
            is BasicDetailsUiEvent.ClearError -> {
                clearError()
            }
        }
    }

    /**
     * Handle name input changes
     */
    private fun handleNameChanged(name: String) {
        val validationResult = validateName(name)
        
        // Save to SavedStateHandle for navigation persistence
        savedStateHandle[KEY_NAME] = name
        
        // Save to UserStateManager for app restart persistence
        userStateManager.setBasicDetails(name, _uiState.value.email)
        
        _uiState.value = _uiState.value.copy(
            name = name,
            nameError = if (validationResult is ValidationResult.Error) validationResult.message else null,
            isFormValid = isFormValid(name, _uiState.value.email),
            error = null // Clear any previous errors when user starts typing
        )
        
        Timber.d("BasicDetailsViewModel: Name changed to '$name', saved to SavedStateHandle and UserStateManager")
    }

    /**
     * Handle email input changes
     */
    private fun handleEmailChanged(email: String) {
        val normalizedEmail = emailValidationService.normalizeEmail(email)
        val validationResult = emailValidationService.validateEmail(normalizedEmail)
        
        // Save to SavedStateHandle for navigation persistence
        savedStateHandle[KEY_EMAIL] = normalizedEmail
        
        // Save to UserStateManager for app restart persistence
        userStateManager.setBasicDetails(_uiState.value.name, normalizedEmail)
        
        _uiState.value = _uiState.value.copy(
            email = normalizedEmail,
            emailError = if (validationResult is ValidationResult.Error) validationResult.message else null,
            isFormValid = isFormValid(_uiState.value.name, normalizedEmail),
            error = null // Clear any previous errors when user starts typing
        )
        
        Timber.d("BasicDetailsViewModel: Email changed to '$normalizedEmail', saved to SavedStateHandle and UserStateManager")
    }

    /**
     * Submit basic details
     */
    private fun submitBasicDetails() {
        val currentState = _uiState.value
        if (!currentState.isReadyForSubmission()) return

        viewModelScope.launch {
            // Reset success state before submitting to ensure clean transition
            _uiState.value = currentState.copy(isLoading = true, error = null, success = false)

            try {
                val result = submitBasicDetailsUseCase(
                    name = currentState.name,
                    email = currentState.email
                )
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success) {
                            // Store account ID and next_step persistently for later steps (survives restarts)
                            response.data?.let { data ->
                                userStateManager.setAccountId(data.account_id)
                                userStateManager.setNextStep(data.next_step)
                                // Optional: keep in-memory copy if other screens expect it
                                sharedDataStore.setAccountId(data.account_id)
                            }
                            
                            _uiState.value = currentState.copy(
                                isLoading = false,
                                success = true,
                                message = "Basic details submitted successfully"
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
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                        Timber.e(error, "Failed to submit basic details")
                    }
                )
            } catch (e: Exception) {
                val errorMessage = errorHandler.handleApiError(e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = errorMessage
                )
                Timber.e(e, "Unexpected error submitting basic details")
            }
        }
    }

    /**
     * Validate name input
     */
    private fun validateName(name: String): ValidationResult {
        val trimmedName = name.trim()
        
        return when {
            trimmedName.isEmpty() -> ValidationResult.Error("Name is required")
            trimmedName.length < 2 -> ValidationResult.Error("Name must be at least 2 characters")
            trimmedName.length > 50 -> ValidationResult.Error("Name must be less than 50 characters")
            !trimmedName.all { it.isLetter() || it.isWhitespace() || it == '-' || it == '\'' } -> 
                ValidationResult.Error("Name can only contain letters, spaces, hyphens, and apostrophes")
            else -> ValidationResult.Success
        }
    }

    /**
     * Check if form is valid
     */
    private fun isFormValid(name: String, email: String): Boolean {
        val nameValidation = validateName(name)
        val emailValidation = emailValidationService.validateEmail(email)
        
        return nameValidation is ValidationResult.Success && 
               emailValidation is ValidationResult.Success
    }

    /**
     * Clear error state
     */
    private fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
