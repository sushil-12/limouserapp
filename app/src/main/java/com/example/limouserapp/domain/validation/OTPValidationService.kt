package com.example.limouserapp.domain.validation

import javax.inject.Inject
import javax.inject.Singleton

/**
 * OTP validation service
 * Handles validation of OTP codes
 */
@Singleton
class OTPValidationService @Inject constructor() {
    
    companion object {
        private const val OTP_LENGTH = 6
        private const val MAX_ATTEMPTS = 3
    }
    
    /**
     * Validate OTP code
     */
    fun validateOTP(otp: String): ValidationResult {
        return when {
            otp.isEmpty() -> ValidationResult.Error("Please enter the OTP")
            otp.length != OTP_LENGTH -> ValidationResult.Error("OTP must be $OTP_LENGTH digits")
            !otp.all { it.isDigit() } -> ValidationResult.Error("OTP must contain only digits")
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Format OTP for display (add spaces between digits)
     */
    fun formatOTP(otp: String): String {
        val cleanOTP = otp.replace(" ", "")
        return cleanOTP.chunked(1).joinToString(" ")
    }
    
    /**
     * Check if OTP is complete
     */
    fun isOTPComplete(otp: String): Boolean {
        return otp.replace(" ", "").length == OTP_LENGTH
    }
    
    /**
     * Get OTP length requirement
     */
    fun getOTPLength(): Int = OTP_LENGTH
    
    /**
     * Get maximum attempts allowed
     */
    fun getMaxAttempts(): Int = MAX_ATTEMPTS
}
