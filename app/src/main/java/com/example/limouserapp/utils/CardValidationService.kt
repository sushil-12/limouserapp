package com.example.limouserapp.utils

import java.util.Calendar

/**
 * Card validation service matching iOS CardValidationService
 * Provides formatting, validation, and card type detection
 */
object CardValidationService {
    
    /**
     * Format card number with spaces every 4 digits (e.g., "1234 5678 9012 3456")
     */
    fun formatCardNumber(cardNumber: String): String {
        // Remove all non-digit characters
        val digits = cardNumber.filter { it.isDigit() }
        
        // Limit to 16 digits
        val limitedDigits = digits.take(16)
        
        // Add spaces every 4 digits
        return limitedDigits.chunked(4).joinToString(" ")
    }
    
    /**
     * Validate card number (length, digits only, Luhn algorithm)
     */
    fun validateCardNumber(cardNumber: String): ValidationResult {
        val digits = cardNumber.filter { it.isDigit() }
        
        // Check length
        if (digits.length < 13 || digits.length > 19) {
            return ValidationResult(false, "Card number must be between 13 and 19 digits")
        }
        
        // Check if all non-space characters are digits
        val allowedChars = cardNumber.filter { it.isDigit() || it == ' ' }
        if (allowedChars.length != cardNumber.length) {
            return ValidationResult(false, "Card number can only contain numbers and spaces")
        }
        
        // Luhn algorithm validation
        if (!isValidLuhn(digits)) {
            return ValidationResult(false, "Invalid card number")
        }
        
        return ValidationResult(true, null)
    }
    
    /**
     * Format CVV (limit to 4 digits)
     */
    fun formatCVV(cvv: String): String {
        // Remove all non-digit characters and limit to 4 digits
        val digits = cvv.filter { it.isDigit() }
        return digits.take(4)
    }
    
    /**
     * Validate CVV (must be 3 or 4 digits)
     */
    fun validateCVV(cvv: String): ValidationResult {
        val digits = cvv.filter { it.isDigit() }
        
        // CVV should be 3 or 4 digits
        if (digits.length < 3 || digits.length > 4) {
            return ValidationResult(false, "CVV must be 3 or 4 digits")
        }
        
        // Check if all characters are digits
        if (digits.length != cvv.filter { it.isDigit() }.length) {
            return ValidationResult(false, "CVV can only contain numbers")
        }
        
        return ValidationResult(true, null)
    }
    
    /**
     * Validate expiry date
     */
    fun validateExpiryDate(month: String, year: String): ValidationResult {
        val monthInt = month.toIntOrNull()
        val yearInt = year.toIntOrNull()
        
        if (monthInt == null || yearInt == null) {
            return ValidationResult(false, "Invalid expiry date")
        }
        
        // Check month range
        if (monthInt < 1 || monthInt > 12) {
            return ValidationResult(false, "Invalid month")
        }
        
        // Check year (should be current year or later)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        if (yearInt < currentYear) {
            return ValidationResult(false, "Card has expired")
        }
        
        // Check if card is expired this month
        if (yearInt == currentYear) {
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1 // Calendar.MONTH is 0-based
            if (monthInt < currentMonth) {
                return ValidationResult(false, "Card has expired")
            }
        }
        
        return ValidationResult(true, null)
    }
    
    /**
     * Detect card type from card number
     */
    fun detectCardType(cardNumber: String): String {
        val digits = cardNumber.filter { it.isDigit() }
        
        return when {
            digits.startsWith("4") -> "visa"
            digits.startsWith("5") || digits.startsWith("2") -> "mastercard"
            digits.startsWith("3") -> "amex"
            digits.startsWith("6") -> "discover"
            else -> "unknown"
        }
    }
    
    /**
     * Complete form validation
     */
    fun validateCardForm(
        cardNumber: String,
        cvv: String,
        expMonth: String,
        expYear: String,
        cardHolderName: String
    ): ValidationResult {
        // Validate card holder name
        if (cardHolderName.trim().isEmpty()) {
            return ValidationResult(false, "Card holder name is required")
        }
        
        // Validate card number
        val cardValidation = validateCardNumber(cardNumber)
        if (!cardValidation.isValid) {
            return cardValidation
        }
        
        // Validate CVV
        val cvvValidation = validateCVV(cvv)
        if (!cvvValidation.isValid) {
            return cvvValidation
        }
        
        // Validate expiry date
        val expiryValidation = validateExpiryDate(expMonth, expYear)
        if (!expiryValidation.isValid) {
            return expiryValidation
        }
        
        return ValidationResult(true, null)
    }
    
    /**
     * Luhn algorithm validation
     */
    private fun isValidLuhn(cardNumber: String): Boolean {
        val digits = cardNumber.filter { it.isDigit() }
        var sum = 0
        var alternate = false
        
        // Process digits from right to left
        for (i in digits.length - 1 downTo 0) {
            val digit = digits[i].digitToIntOrNull() ?: return false
            
            if (alternate) {
                val doubled = digit * 2
                sum += if (doubled > 9) doubled - 9 else doubled
            } else {
                sum += digit
            }
            
            alternate = !alternate
        }
        
        return sum % 10 == 0
    }
    
    /**
     * Validation result
     */
    data class ValidationResult(
        val isValid: Boolean,
        val message: String?
    )
}

