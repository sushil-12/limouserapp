package com.example.limouserapp.domain.validation

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Email validation service
 * Handles validation of email addresses
 */
@Singleton
class EmailValidationService @Inject constructor() {
    
    companion object {
        private val EMAIL_REGEX = Regex(
            "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
        )
        private const val MAX_EMAIL_LENGTH = 254
        private const val MIN_EMAIL_LENGTH = 5
    }
    
    /**
     * Validate email address
     */
    fun validateEmail(email: String): ValidationResult {
        val trimmedEmail = email.trim()
        
        return when {
            trimmedEmail.isEmpty() -> ValidationResult.Error("Email is required")
            trimmedEmail.length < MIN_EMAIL_LENGTH -> ValidationResult.Error("Email is too short")
            trimmedEmail.length > MAX_EMAIL_LENGTH -> ValidationResult.Error("Email is too long")
            !EMAIL_REGEX.matches(trimmedEmail) -> ValidationResult.Error("Please enter a valid email address")
            hasConsecutiveDots(trimmedEmail) -> ValidationResult.Error("Email cannot have consecutive dots")
            startsOrEndsWithDot(trimmedEmail) -> ValidationResult.Error("Email cannot start or end with a dot")
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Check if email has consecutive dots
     */
    private fun hasConsecutiveDots(email: String): Boolean {
        return email.contains("..")
    }
    
    /**
     * Check if email starts or ends with dot
     */
    private fun startsOrEndsWithDot(email: String): Boolean {
        return email.startsWith(".") || email.endsWith(".")
    }
    
    /**
     * Normalize email address (convert to lowercase)
     */
    fun normalizeEmail(email: String): String {
        return email.trim().lowercase()
    }
    
    /**
     * Extract domain from email
     */
    fun extractDomain(email: String): String? {
        return if (EMAIL_REGEX.matches(email)) {
            email.substringAfter("@")
        } else null
    }
}
