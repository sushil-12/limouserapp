package com.example.limouserapp.domain.usecase.auth

import com.example.limouserapp.data.repository.AuthRepository
import com.example.limouserapp.domain.validation.OTPValidationService
import com.example.limouserapp.domain.validation.ValidationResult
import javax.inject.Inject

/**
 * Use case for verifying OTP code
 * Combines validation and repository operations
 */
class VerifyOTPUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val otpValidationService: OTPValidationService
) {
    
    /**
     * Execute the use case
     */
    suspend operator fun invoke(
        tempUserId: String,
        otp: String
    ): Result<com.example.limouserapp.data.model.BaseResponse<com.example.limouserapp.data.model.auth.VerifyOTPData>> {
        
        // Validate OTP first
        val validationResult = otpValidationService.validateOTP(otp)
        if (validationResult is ValidationResult.Error) {
            return Result.failure(IllegalArgumentException(validationResult.message))
        }
        
        // Verify OTP
        return authRepository.verifyOTP(tempUserId, otp)
    }
}
