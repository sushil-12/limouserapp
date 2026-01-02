package com.example.limouserapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.limouserapp.data.network.error.ErrorHandler
import com.example.limouserapp.data.repository.AuthRepository
import com.example.limouserapp.domain.usecase.auth.VerifyOTPUseCase
import com.example.limouserapp.domain.validation.OTPValidationService
import com.example.limouserapp.domain.validation.ValidationResult
import com.example.limouserapp.ui.state.OtpUiEvent
import com.example.limouserapp.ui.state.OtpUiState
import com.example.limouserapp.data.local.SharedDataStore
import com.example.limouserapp.data.local.UserStateManager
import com.example.limouserapp.data.storage.TokenManager
import com.example.limouserapp.data.model.auth.User
import com.example.limouserapp.data.model.auth.DriverRegistrationState
import com.example.limouserapp.ui.navigation.AuthStateManager
import com.example.limouserapp.ui.navigation.AuthNavigationState
import com.example.limouserapp.ui.navigation.AuthNavigationEvent
import com.example.limouserapp.data.notification.FcmTopicManager
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
 * ViewModel for OTP Screen
 * Follows single responsibility principle and handles OTP verification logic
 */
@HiltViewModel
class OtpViewModel @Inject constructor(
    private val verifyOTPUseCase: VerifyOTPUseCase,
    private val authRepository: AuthRepository,
    private val otpValidationService: OTPValidationService,
    private val errorHandler: ErrorHandler,
    private val sharedDataStore: SharedDataStore,
    private val userStateManager: UserStateManager,
    private val tokenManager: TokenManager,
    private val authStateManager: AuthStateManager,
    private val fcmTopicManager: FcmTopicManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OtpUiState())
    val uiState: StateFlow<OtpUiState> = _uiState.asStateFlow()

    private var resendCooldownJob: Job? = null

    init {
        // Initialize with data from shared store
        val tempUserId = sharedDataStore.getTempUserId() ?: ""
        val phoneNumber = sharedDataStore.getPhoneNumber() ?: ""
        
        _uiState.value = _uiState.value.copy(
            tempUserId = tempUserId,
            phoneNumber = phoneNumber
        )
        
        // Set up resend cooldown timer
        startResendCooldown()
    }

    /**
     * Handle UI events
     */
    fun onEvent(event: OtpUiEvent) {
        when (event) {
            is OtpUiEvent.OtpChanged -> {
                handleOtpChanged(event.otp)
            }
            is OtpUiEvent.VerifyOtp -> {
                verifyOtp()
            }
            is OtpUiEvent.ResendOtp -> {
                resendOtp()
            }
            is OtpUiEvent.ClearError -> {
                clearError()
            }
            is OtpUiEvent.ClearSuccess -> {
                clearSuccess()
            }
        }
    }

    /**
     * Set initial data from previous screen
     */
    fun setInitialData(tempUserId: String, phoneNumber: String) {
        _uiState.value = _uiState.value.copy(
            tempUserId = tempUserId,
            phoneNumber = phoneNumber
        )
    }

    /**
     * Handle OTP input changes
     */
    private fun handleOtpChanged(otp: String) {
        // Clean the OTP (remove spaces) for validation and storage
        val cleanOtp = otp.replace(" ", "")
        val validationResult = otpValidationService.validateOTP(cleanOtp)
        
        _uiState.value = _uiState.value.copy(
            otp = cleanOtp, // Store clean OTP without spaces
            isFormValid = validationResult is ValidationResult.Success,
            error = if (validationResult is ValidationResult.Error) validationResult.message else null
        )
        
    }

    /**
     * Verify OTP
     */
    private fun verifyOtp() {
        val currentState = _uiState.value
        if (!currentState.isReadyForSubmission()) {
            return
        }

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, error = null)
            
            try {
                val result = verifyOTPUseCase(
                    tempUserId = currentState.tempUserId,
                    otp = currentState.otp
                )
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.data != null) {
                            // Store user data and determine navigation
                            val user = response.data.user
                            val driverRegistrationState = response.data.driverRegistrationState
                            val token = response.data.token
                            val tokenType = response.data.tokenType
                            val expiresIn = response.data.expiresIn
                            
                            // Save user data and authentication token persistently
                            saveUserData(user, driverRegistrationState)
                            saveAuthToken(token, tokenType, expiresIn)
                            
                            // Implement navigation decision tree
                            val nextAction = determineNextAction(user, driverRegistrationState)
                            
                            _uiState.value = currentState.copy(
                                isLoading = false,
                                success = true,
                                message = "OTP verified successfully",
                                nextAction = nextAction
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
                        Timber.e(error, "Failed to verify OTP")
                    }
                )
            } catch (e: Exception) {
                val errorMessage = errorHandler.handleError(e)
                _uiState.value = currentState.copy(
                    isLoading = false,
                    error = errorMessage
                )
                Timber.e(e, "Unexpected error verifying OTP")
            }
        }
    }

    /**
     * Save user data persistently
     */
    private fun saveUserData(user: User, driverRegistrationState: DriverRegistrationState?) {
        try {
            // Save user ID and phone number
            userStateManager.setUserId(user.id)
            userStateManager.setPhoneNumber(user.phone)
            
            // Save profile completion status
            userStateManager.setProfileCompleted(user.isProfileCompleted ?: false)
            
            // Save next_step from customer_registration_state if available
            user.customerRegistrationState?.nextStep?.let { nextStep ->
                userStateManager.setNextStep(nextStep)
            }
            
            // Save driver registration status
            if (driverRegistrationState != null) {
                userStateManager.setDriverRegistrationStatus(
                    hasRegistration = true,
                    isCompleted = driverRegistrationState.isCompleted
                )
            }
            
            // Subscribe to FCM topic based on userId (e.g., topic = "1146" for userId 1146)
            viewModelScope.launch {
                fcmTopicManager.subscribeToUserTopic(user.id)
                    .onSuccess {
                        Timber.i("✅ Subscribed to FCM topic for user ${user.id}")
                    }
                    .onFailure { error ->
                        Timber.w(error, "⚠️ Failed to subscribe to FCM topic (notifications may still work)")
                    }
            }
            
            // User data saved successfully
        } catch (e: Exception) {
            Timber.e(e, "Error saving user data")
        }
    }
    
    /**
     * Save authentication token persistently
     */
    private fun saveAuthToken(token: String, tokenType: String, expiresIn: Int) {
        try {
            tokenManager.saveTokens(
                accessToken = token,
                tokenType = tokenType,
                expiresIn = expiresIn
            )
            
            // Authentication token saved successfully
        } catch (e: Exception) {
            Timber.e(e, "Error saving authentication token")
        }
    }

    /**
     * Determine next action based on user data and registration state
     * Now uses centralized AuthStateManager for consistent navigation logic
     * Uses next_step from customer_registration_state if available
     */
    private fun determineNextAction(
        user: User,
        driverRegistrationState: DriverRegistrationState?
    ): String {
        // Update AuthStateManager with current user state
        // Get next_step from customer_registration_state or stored value
        val nextStep = user.customerRegistrationState?.nextStep ?: userStateManager.getNextStep()
        
        // Log the user state for debugging
        val isProfileCompleted = user.isProfileCompleted ?: false
        Timber.d("OTP Verification - isProfileCompleted: $isProfileCompleted, nextStep: $nextStep")
        
        authStateManager.updateUserState(
            isProfileCompleted = isProfileCompleted, // Use non-null value
            hasDriverRegistration = driverRegistrationState != null,
            isDriverRegistrationCompleted = driverRegistrationState?.isCompleted ?: false,
            nextStep = nextStep
        )
        
        // Calculate the next destination based on updated user state
        val updatedAuthState = authStateManager.userAuthState.value
        Timber.d("OTP Verification - Updated auth state: isAuthenticated=${updatedAuthState.isAuthenticated}, isProfileCompleted=${updatedAuthState.isProfileCompleted}, nextStep=${updatedAuthState.nextStep}")
        val nextDestination = updatedAuthState.getNextDestination()
        Timber.d("OTP Verification - Next destination: $nextDestination")
        
        // Update navigation state with the computed destination
        authStateManager.handleNavigationEvent(
            when (nextDestination) {
                AuthNavigationState.Onboarding -> AuthNavigationEvent.NavigateToOnboarding
                AuthNavigationState.PhoneEntry -> AuthNavigationEvent.NavigateToPhoneEntry
                AuthNavigationState.OtpVerification -> AuthNavigationEvent.NavigateToOtpVerification
                AuthNavigationState.BasicDetails -> AuthNavigationEvent.NavigateToBasicDetails
                AuthNavigationState.CreditCard -> AuthNavigationEvent.NavigateToCreditCard
                AuthNavigationState.Dashboard -> AuthNavigationEvent.NavigateToDashboard
                AuthNavigationState.Success -> AuthNavigationEvent.NavigateToSuccess
                else -> AuthNavigationEvent.NavigateToBasicDetails
            }
        )
        
        // Return the route string for navigation
        return when (nextDestination) {
            AuthNavigationState.Onboarding -> "onboarding"
            AuthNavigationState.PhoneEntry -> "phone"
            AuthNavigationState.OtpVerification -> "otp"
            AuthNavigationState.BasicDetails -> "basic_details"
            AuthNavigationState.CreditCard -> "credit_card"
            AuthNavigationState.Dashboard -> "dashboard"
            AuthNavigationState.Success -> "success"
            else -> "basic_details" // Default fallback
        }
    }

    /**
     * Resend OTP
     */
    private fun resendOtp() {
        val currentState = _uiState.value
        if (!currentState.canResend) return

        viewModelScope.launch {
            _uiState.value = currentState.copy(isLoading = true, error = null)
            
            try {
                val result = authRepository.resendOTP(currentState.tempUserId)
                
                result.fold(
                    onSuccess = { response ->
                        if (response.success) {
                            _uiState.value = currentState.copy(
                                isLoading = false,
                                success = true,
                                message = "OTP resent successfully",
                                resendCooldown = response.data?.cooldownRemaining ?: 60
                            )
                            startResendCooldown()
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
                        Timber.e(error, "Failed to resend OTP")
                    }
                )
            } catch (e: Exception) {
                val errorMessage = errorHandler.handleError(e)
                _uiState.value = currentState.copy(
                    isLoading = false,
                    error = errorMessage
                )
                Timber.e(e, "Unexpected error resending OTP")
            }
        }
    }

    /**
     * Start resend cooldown timer
     */
    private fun startResendCooldown() {
        resendCooldownJob?.cancel()
        resendCooldownJob = viewModelScope.launch {
            val initialCooldown = _uiState.value.resendCooldown
            if (initialCooldown <= 0) {
                _uiState.value = _uiState.value.copy(canResend = true, resendCooldown = 0)
                return@launch
            }

            for (i in initialCooldown downTo 0) {
                _uiState.value = _uiState.value.copy(
                    resendCooldown = i,
                    canResend = i == 0
                )
                if (i > 0) delay(1000)
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
        _uiState.value = _uiState.value.copy(success = false, nextAction = null)
        // Also clear shared data to ensure phone entry screen starts fresh
        sharedDataStore.clearData()
    }

    override fun onCleared() {
        super.onCleared()
        resendCooldownJob?.cancel()
    }
}
