package com.example.limouserapp.domain.usecase.auth

import com.example.limouserapp.data.repository.AuthRepository
import com.example.limouserapp.domain.validation.CountryCode
import com.example.limouserapp.domain.validation.PhoneValidationService
import com.example.limouserapp.domain.validation.ValidationResult
import javax.inject.Inject

/**
 * Use case for sending verification code to phone number
 * Combines validation and repository operations
 */
class SendVerificationCodeUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val phoneValidationService: PhoneValidationService
) {
    
    /**
     * Execute the use case
     */
    suspend operator fun invoke(
        phoneNumber: String,
        countryCode: CountryCode
    ): Result<com.example.limouserapp.data.model.BaseResponse<com.example.limouserapp.data.model.auth.AuthData>> {
        
        // Validate phone number first
        val validationResult = phoneValidationService.validatePhoneNumber(phoneNumber, countryCode)
        if (validationResult is ValidationResult.Error) {
            return Result.failure(IllegalArgumentException(validationResult.message))
        }
        
        // Send verification code
        return authRepository.sendVerificationCode(phoneNumber, countryCode)
    }
}
