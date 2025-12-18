package com.example.limouserapp.ui.navigation

/**
 * Centralized navigation state management for authentication flow
 * Follows clean architecture principles and matches iOS behavior
 */
sealed class AuthNavigationState {
    object Loading : AuthNavigationState()
    object Onboarding : AuthNavigationState()
    object PhoneEntry : AuthNavigationState()
    object OtpVerification : AuthNavigationState()
    object BasicDetails : AuthNavigationState()
    object CreditCard : AuthNavigationState()
    object Dashboard : AuthNavigationState()
    object Success : AuthNavigationState()
}

/**
 * Navigation events for authentication flow
 */
sealed class AuthNavigationEvent {
    object NavigateToOnboarding : AuthNavigationEvent()
    object NavigateToPhoneEntry : AuthNavigationEvent()
    object NavigateToOtpVerification : AuthNavigationEvent()
    object NavigateToBasicDetails : AuthNavigationEvent()
    object NavigateToCreditCard : AuthNavigationEvent()
    object NavigateToDashboard : AuthNavigationEvent()
    object NavigateToSuccess : AuthNavigationEvent()
    object NavigateBack : AuthNavigationEvent()
}

/**
 * User authentication state for navigation decisions
 */
data class UserAuthState(
    val isAuthenticated: Boolean = false,
    val isProfileCompleted: Boolean = false,
    val hasDriverRegistration: Boolean = false,
    val isDriverRegistrationCompleted: Boolean = false,
    val tempUserId: String? = null,
    val phoneNumber: String? = null,
    val nextStep: String? = null
) {
    /**
     * Determine the next navigation destination based on user state
     * Uses next_step from API if available, otherwise falls back to calculated logic
     */
    fun getNextDestination(): AuthNavigationState {
        // Not authenticated - go to onboarding
        if (!isAuthenticated) {
            return AuthNavigationState.Onboarding
        }
        
        // If profile is completed, always go to Dashboard (ignore API nextStep if it suggests credit_card)
        if (isProfileCompleted) {
            return AuthNavigationState.Dashboard
        }
        
        // If next_step is provided from API, use it for navigation
        nextStep?.let { step ->
            return when (step) {
                "basic_details" -> AuthNavigationState.BasicDetails
                "credit_card" -> AuthNavigationState.CreditCard
                "profile_complete", "dashboard", "completed" -> AuthNavigationState.Dashboard
                else -> {
                    // Fallback to calculated logic for unknown steps
                    getCalculatedDestination()
                }
            }
        }
        
        // Fallback to calculated logic if next_step is not available
        return getCalculatedDestination()
    }
    
    /**
     * Calculate destination based on user state (fallback logic)
     */
    private fun getCalculatedDestination(): AuthNavigationState {
        return when {
            // Has driver registration state
            hasDriverRegistration -> {
                if (isDriverRegistrationCompleted) {
                    AuthNavigationState.Dashboard
                } else {
                    if (!isProfileCompleted) {
                        AuthNavigationState.BasicDetails
                    } else {
                        AuthNavigationState.Dashboard
                    }
                }
            }
            
            // No driver registration state
            else -> {
                if (!isProfileCompleted) {
                    AuthNavigationState.BasicDetails
                } else {
                    AuthNavigationState.Dashboard
                }
            }
        }
    }
    
    /**
     * Check if user should skip onboarding and go directly to phone entry
     */
    fun shouldSkipOnboarding(): Boolean = isAuthenticated
}
