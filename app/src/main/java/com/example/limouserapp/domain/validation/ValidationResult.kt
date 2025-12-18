package com.example.limouserapp.domain.validation

/**
 * Sealed class representing validation results
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}

/**
 * Extension function to check if validation is successful
 */
fun ValidationResult.isSuccess(): Boolean = this is ValidationResult.Success

/**
 * Extension function to get error message
 */
fun ValidationResult.getErrorMessage(): String? = when (this) {
    is ValidationResult.Success -> null
    is ValidationResult.Error -> message
}
