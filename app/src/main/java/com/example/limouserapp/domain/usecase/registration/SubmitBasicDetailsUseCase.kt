package com.example.limouserapp.domain.usecase.registration

import com.example.limouserapp.data.repository.RegistrationRepository
import com.example.limouserapp.domain.validation.EmailValidationService
import com.example.limouserapp.domain.validation.ValidationResult
import javax.inject.Inject

/**
 * Use case for submitting basic user details
 * Combines validation and repository operations
 */
class SubmitBasicDetailsUseCase @Inject constructor(
    private val registrationRepository: RegistrationRepository,
    private val emailValidationService: EmailValidationService
) {
    
    /**
     * Execute the use case
     */
    suspend operator fun invoke(
        name: String,
        email: String
    ): Result<com.example.limouserapp.data.model.BaseResponse<com.example.limouserapp.data.model.registration.BasicDetailsData>> {
        
        // Validate inputs
        val nameValidation = validateName(name)
        if (nameValidation is ValidationResult.Error) {
            return Result.failure(IllegalArgumentException(nameValidation.message))
        }
        
        val emailValidation = emailValidationService.validateEmail(email)
        if (emailValidation is ValidationResult.Error) {
            return Result.failure(IllegalArgumentException(emailValidation.message))
        }
        
        // Submit basic details
        return registrationRepository.submitBasicDetails(name.trim(), email.trim().lowercase())
    }
    
    /**
     * Validate name
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
}
