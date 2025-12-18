package com.example.limouserapp.domain.validation

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Credit card validation service
 * Handles validation of credit card information
 */
@Singleton
class CreditCardValidationService @Inject constructor() {
    
    companion object {
        private const val MIN_CARD_LENGTH = 12
        private const val MAX_CARD_LENGTH = 19
        private const val MIN_CVV_LENGTH = 3
        private const val MAX_CVV_LENGTH = 4
        private const val MIN_NAME_LENGTH = 2
        private const val MAX_NAME_LENGTH = 50
    }
    
    /**
     * Validate complete credit card form
     */
    fun validateCardForm(
        cardNumber: String,
        cvv: String,
        expMonth: String,
        expYear: String,
        cardHolderName: String
    ): ValidationResult {
        val cardNumberResult = validateCardNumber(cardNumber)
        if (cardNumberResult is ValidationResult.Error) return cardNumberResult
        
        val cvvResult = validateCVV(cvv)
        if (cvvResult is ValidationResult.Error) return cvvResult
        
        val expiryResult = validateExpiryDate(expMonth, expYear)
        if (expiryResult is ValidationResult.Error) return expiryResult
        
        val nameResult = validateCardHolderName(cardHolderName)
        if (nameResult is ValidationResult.Error) return nameResult
        
        return ValidationResult.Success
    }
    
    /**
     * Validate credit card number
     */
    fun validateCardNumber(cardNumber: String): ValidationResult {
        val cleanNumber = cardNumber.replace(" ", "").replace("-", "")
        
        return when {
            cleanNumber.isEmpty() -> ValidationResult.Error("Card number is required")
            !cleanNumber.all { it.isDigit() } -> ValidationResult.Error("Card number must contain only digits")
            cleanNumber.length < MIN_CARD_LENGTH || cleanNumber.length > MAX_CARD_LENGTH -> 
                ValidationResult.Error("Card number must be between $MIN_CARD_LENGTH and $MAX_CARD_LENGTH digits")
            !isValidLuhnAlgorithm(cleanNumber) -> ValidationResult.Error("Invalid card number")
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Validate CVV/CVC
     */
    fun validateCVV(cvv: String): ValidationResult {
        val cleanCVV = cvv.trim()
        
        return when {
            cleanCVV.isEmpty() -> ValidationResult.Error("CVV is required")
            !cleanCVV.all { it.isDigit() } -> ValidationResult.Error("CVV must contain only digits")
            cleanCVV.length < MIN_CVV_LENGTH || cleanCVV.length > MAX_CVV_LENGTH -> 
                ValidationResult.Error("CVV must be between $MIN_CVV_LENGTH and $MAX_CVV_LENGTH digits")
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Validate expiry date
     */
    fun validateExpiryDate(month: String, year: String): ValidationResult {
        val cleanMonth = month.trim()
        val cleanYear = year.trim()
        
        return when {
            cleanMonth.isEmpty() || cleanYear.isEmpty() -> ValidationResult.Error("Expiry date is required")
            !cleanMonth.all { it.isDigit() } || !cleanYear.all { it.isDigit() } -> 
                ValidationResult.Error("Expiry date must contain only digits")
            cleanMonth.length != 2 || cleanYear.length != 2 -> 
                ValidationResult.Error("Expiry date must be in MM/YY format")
            !isValidExpiryDate(cleanMonth, cleanYear) -> 
                ValidationResult.Error("Please enter a valid expiry date")
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Validate card holder name
     */
    fun validateCardHolderName(name: String): ValidationResult {
        val cleanName = name.trim()
        
        return when {
            cleanName.isEmpty() -> ValidationResult.Error("Name on card is required")
            cleanName.length < MIN_NAME_LENGTH -> ValidationResult.Error("Name must be at least $MIN_NAME_LENGTH characters")
            cleanName.length > MAX_NAME_LENGTH -> ValidationResult.Error("Name must be less than $MAX_NAME_LENGTH characters")
            !cleanName.all { it.isLetter() || it.isWhitespace() || it == '-' || it == '\'' } -> 
                ValidationResult.Error("Name can only contain letters, spaces, hyphens, and apostrophes")
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Format card number with spaces
     */
    fun formatCardNumber(cardNumber: String): String {
        val cleanNumber = cardNumber.replace(" ", "").replace("-", "")
        return cleanNumber.chunked(4).joinToString(" ")
    }
    
    /**
     * Get card type based on number
     */
    fun getCardType(cardNumber: String): CardType {
        val cleanNumber = cardNumber.replace(" ", "").replace("-", "")
        
        return when {
            cleanNumber.startsWith("4") -> CardType.VISA
            cleanNumber.startsWith("5") && cleanNumber[1] in '1'..'5' -> CardType.MASTERCARD
            cleanNumber.startsWith("34") || cleanNumber.startsWith("37") -> CardType.AMERICAN_EXPRESS
            cleanNumber.startsWith("6") -> CardType.DISCOVER
            else -> CardType.UNKNOWN
        }
    }
    
    /**
     * Check if expiry date is valid and not expired
     */
    private fun isValidExpiryDate(month: String, year: String): Boolean {
        val monthInt = month.toIntOrNull() ?: return false
        val yearInt = year.toIntOrNull() ?: return false
        
        if (monthInt !in 1..12) return false
        
        val currentDate = java.util.Calendar.getInstance()
        val currentYear = currentDate.get(java.util.Calendar.YEAR) % 100
        val currentMonth = currentDate.get(java.util.Calendar.MONTH) + 1
        
        return yearInt > currentYear || (yearInt == currentYear && monthInt >= currentMonth)
    }
    
    /**
     * Validate using Luhn algorithm
     */
    private fun isValidLuhnAlgorithm(cardNumber: String): Boolean {
        var sum = 0
        var alternate = false
        
        for (i in cardNumber.length - 1 downTo 0) {
            var n = cardNumber[i].toString().toInt()
            
            if (alternate) {
                n *= 2
                if (n > 9) {
                    n = (n % 10) + 1
                }
            }
            
            sum += n
            alternate = !alternate
        }
        
        return sum % 10 == 0
    }
}

/**
 * Credit card types
 */
enum class CardType(val displayName: String) {
    VISA("Visa"),
    MASTERCARD("Mastercard"),
    AMERICAN_EXPRESS("American Express"),
    DISCOVER("Discover"),
    UNKNOWN("Unknown")
}
