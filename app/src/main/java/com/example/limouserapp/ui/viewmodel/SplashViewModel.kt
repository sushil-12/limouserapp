package com.example.limouserapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.limouserapp.ui.navigation.AuthStateManager
import com.example.limouserapp.ui.navigation.AuthNavigationState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for Splash Screen
 * Follows single responsibility principle and handles splash screen logic
 * Now uses centralized AuthStateManager for navigation decisions
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authStateManager: AuthStateManager
) : ViewModel() {

    /**
     * Get next destination from AuthStateManager
     */
    fun getNextDestination(): String {
        return when (authStateManager.navigationState.value) {
            AuthNavigationState.Onboarding -> "onboarding"
            AuthNavigationState.PhoneEntry -> "phone"
            AuthNavigationState.OtpVerification -> "otp"
            AuthNavigationState.BasicDetails -> "basic_details"
            AuthNavigationState.CreditCard -> "credit_card"
            AuthNavigationState.Dashboard -> "dashboard"
            AuthNavigationState.Success -> "success"
            else -> "onboarding"
        }
    }
}
