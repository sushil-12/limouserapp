package com.example.limouserapp.ui.navigation

import android.util.Log
import com.example.limouserapp.data.local.SharedDataStore
import com.example.limouserapp.data.local.UserStateManager
import com.example.limouserapp.data.model.auth.User
import com.example.limouserapp.data.model.auth.DriverRegistrationState
import com.example.limouserapp.data.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized authentication state manager
 * Handles user authentication state and navigation decisions
 * Matches iOS behavior exactly
 * This is a singleton service, not a ViewModel
 */
@Singleton
class AuthStateManager @Inject constructor(
    private val authRepository: AuthRepository,
    private val sharedDataStore: SharedDataStore,
    private val userStateManager: UserStateManager
) {

    private val _userAuthState = MutableStateFlow(UserAuthState())
    val userAuthState: StateFlow<UserAuthState> = _userAuthState.asStateFlow()

    private val _navigationState = MutableStateFlow<AuthNavigationState>(AuthNavigationState.Loading)
    val navigationState: StateFlow<AuthNavigationState> = _navigationState.asStateFlow()

    // Coroutine scope for this singleton service
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        serviceScope.launch {
            checkAuthenticationState()
        }
    }

    /**
     * Check current authentication state and determine navigation
     * Matches iOS SplashScreen logic exactly
     */
    private suspend fun checkAuthenticationState() {
        try {
            val isAuthenticated = authRepository.isAuthenticated()
            Log.e("AuthStateManager", isAuthenticated.toString())

            if (isAuthenticated) {
                // Get user data for detailed state checking
                val userData = getUserData()
                val driverRegistrationState = getDriverRegistrationState()
                val nextStep = userStateManager.getNextStep()
                Log.e("AuthStateManager", userData.toString())
                Log.e("AuthStateManager", driverRegistrationState.toString())
                Log.e("AuthStateManager", "nextStep: $nextStep")
                val authState = UserAuthState(
                    isAuthenticated = true,
                    isProfileCompleted = userData?.isProfileCompleted ?: false,
                    hasDriverRegistration = driverRegistrationState != null,
                    isDriverRegistrationCompleted = driverRegistrationState?.isCompleted ?: false,
                    tempUserId = sharedDataStore.getTempUserId(),
                    phoneNumber = sharedDataStore.getPhoneNumber(),
                    nextStep = nextStep
                )
                
                _userAuthState.value = authState
                
                // Determine next destination based on user state (uses next_step if available)
                val nextDestination = authState.getNextDestination()
                _navigationState.value = nextDestination
            } else {
                _userAuthState.value = UserAuthState(isAuthenticated = false)
                _navigationState.value = AuthNavigationState.Onboarding
            }
            
        } catch (e: Exception) {
            _userAuthState.value = UserAuthState(isAuthenticated = false)
            _navigationState.value = AuthNavigationState.Onboarding
        }
    }

    /**
     * Get user data from storage
     */
    private suspend fun getUserData(): User? {
        return try {
            val phone = userStateManager.getPhoneNumber() ?: sharedDataStore.getPhoneNumber()
            val userId = userStateManager.getUserId()
            
            if (phone != null && userId != null) {
                // Create user object with persisted data
                User(
                    id = userId,
                    phone = phone,
                    role = 1, // Customer role
                    isProfileCompleted = userStateManager.isProfileCompleted(),
                    lastLoginAt = "",
                    createdFrom = "mobile",
                    customerRegistrationState = null
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting user data")
            null
        }
    }

    /**
     * Get driver registration state from storage
     */
    private suspend fun getDriverRegistrationState(): DriverRegistrationState? {
        return try {
            if (userStateManager.hasDriverRegistration()) {
                // Create driver registration state from persisted data
                DriverRegistrationState(
                    currentStep = "completed", // Placeholder
                    progressPercentage = if (userStateManager.isDriverRegistrationCompleted()) 100 else 50,
                    isCompleted = userStateManager.isDriverRegistrationCompleted()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting driver registration state")
            null
        }
    }

    /**
     * Handle navigation events
     */
    fun handleNavigationEvent(event: AuthNavigationEvent) {
        when (event) {
            is AuthNavigationEvent.NavigateToOnboarding -> {
                _navigationState.value = AuthNavigationState.Onboarding
            }
            is AuthNavigationEvent.NavigateToPhoneEntry -> {
                _navigationState.value = AuthNavigationState.PhoneEntry
            }
            is AuthNavigationEvent.NavigateToOtpVerification -> {
                _navigationState.value = AuthNavigationState.OtpVerification
            }
            is AuthNavigationEvent.NavigateToBasicDetails -> {
                _navigationState.value = AuthNavigationState.BasicDetails
            }
            is AuthNavigationEvent.NavigateToCreditCard -> {
                _navigationState.value = AuthNavigationState.CreditCard
            }
            is AuthNavigationEvent.NavigateToDashboard -> {
                _navigationState.value = AuthNavigationState.Dashboard
            }
            is AuthNavigationEvent.NavigateToSuccess -> {
                _navigationState.value = AuthNavigationState.Success
            }
            is AuthNavigationEvent.NavigateBack -> {
                // Handle back navigation logic
                handleBackNavigation()
            }
        }
    }

    /**
     * Handle back navigation
     */
    private fun handleBackNavigation() {
        val currentState = _navigationState.value
        val nextState = when (currentState) {
            AuthNavigationState.PhoneEntry -> AuthNavigationState.Onboarding
            AuthNavigationState.OtpVerification -> AuthNavigationState.PhoneEntry
            AuthNavigationState.BasicDetails -> AuthNavigationState.OtpVerification
            AuthNavigationState.CreditCard -> AuthNavigationState.BasicDetails
            AuthNavigationState.Success -> AuthNavigationState.CreditCard
            else -> currentState
        }
        _navigationState.value = nextState
    }

    /**
     * Update user state after successful operations
     */
    fun updateUserState(
        isProfileCompleted: Boolean? = null,
        hasDriverRegistration: Boolean? = null,
        isDriverRegistrationCompleted: Boolean? = null,
        nextStep: String? = null
    ) {
        val currentState = _userAuthState.value
        
        // Persist the state changes
        isProfileCompleted?.let { userStateManager.setProfileCompleted(it) }
        if (hasDriverRegistration != null && isDriverRegistrationCompleted != null) {
            userStateManager.setDriverRegistrationStatus(hasDriverRegistration, isDriverRegistrationCompleted)
        }
        nextStep?.let { userStateManager.setNextStep(it) }
        
        // Update the current state
        // Ensure isAuthenticated is true when updating state after login
        _userAuthState.value = currentState.copy(
            isAuthenticated = true, // Always set to true when updating state after login
            isProfileCompleted = isProfileCompleted ?: currentState.isProfileCompleted,
            hasDriverRegistration = hasDriverRegistration ?: currentState.hasDriverRegistration,
            isDriverRegistrationCompleted = isDriverRegistrationCompleted ?: currentState.isDriverRegistrationCompleted,
            nextStep = nextStep ?: currentState.nextStep
        )
    }

    /**
     * Refresh authentication state
     * This is a suspend function so it can be awaited
     */
    suspend fun refreshAuthState() {
        checkAuthenticationState()
    }
}
