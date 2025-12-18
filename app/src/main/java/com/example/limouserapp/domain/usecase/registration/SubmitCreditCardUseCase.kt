package com.example.limouserapp.domain.usecase.registration

import com.example.limouserapp.data.repository.RegistrationRepository
import com.example.limouserapp.domain.validation.CreditCardValidationService
import com.example.limouserapp.domain.validation.ValidationResult
import javax.inject.Inject

/**
 * Use case for submitting credit card during registration
 * Combines validation and repository operations
 */
class SubmitCreditCardUseCase @Inject constructor(
    private val registrationRepository: RegistrationRepository,
    private val creditCardValidationService: CreditCardValidationService
) {
    
    /**
     * Execute the use case
     */
    suspend operator fun invoke(
        cardNumber: String,
        expMonth: String,
        expYear: String,
        cvc: String,
        cardHolderName: String,
        location: String? = null,
        city: String? = null,
        state: String? = null,
        zipCode: String? = null,
        smsOptIn: Boolean? = null
    ): Result<com.example.limouserapp.data.model.BaseResponse<com.example.limouserapp.data.model.registration.CreditCardData>> {
        
        // Validate credit card form
        val validationResult = creditCardValidationService.validateCardForm(
            cardNumber = cardNumber,
            cvv = cvc,
            expMonth = expMonth,
            expYear = expYear,
            cardHolderName = cardHolderName
        )
        
        if (validationResult is ValidationResult.Error) {
            return Result.failure(IllegalArgumentException(validationResult.message))
        }
        
        // Get card type
        val cardType = creditCardValidationService.getCardType(cardNumber)
        
        // Submit credit card
        return registrationRepository.submitCreditCard(
            cardNumber = cardNumber,
            expMonth = expMonth,
            expYear = expYear,
            cvc = cvc,
            cardHolderName = cardHolderName,
            location = location,
            city = city,
            state = state,
            zipCode = zipCode,
            cardType = cardType.displayName,
            smsOptIn = smsOptIn
        )
    }
}
