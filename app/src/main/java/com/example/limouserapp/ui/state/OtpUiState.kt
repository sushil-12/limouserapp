package com.example.limouserapp.ui.state

/**
 * UI state for OTP Screen
 * Follows single responsibility principle
 */
data class OtpUiState(
    val otp: String = "",
    val tempUserId: String = "",
    val phoneNumber: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val message: String? = null,
    val isFormValid: Boolean = false,
    val resendCooldown: Int = 0,
    val canResend: Boolean = false,
    val nextAction: String? = null
) {
    /**
     * Check if the form is ready for submission
     */
    fun isReadyForSubmission(): Boolean = isFormValid && !isLoading && error == null
    
    /**
     * Check if OTP is complete
     */
    fun isOtpComplete(): Boolean = otp.length == 6
}

/**
 * UI events for OTP Screen
 */
sealed class OtpUiEvent {
    data class OtpChanged(val otp: String) : OtpUiEvent()
    object VerifyOtp : OtpUiEvent()
    object ResendOtp : OtpUiEvent()
    object ClearError : OtpUiEvent()
}
