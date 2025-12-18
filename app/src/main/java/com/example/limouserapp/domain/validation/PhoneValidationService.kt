package com.example.limouserapp.domain.validation

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phone number validation service
 * Handles validation of phone numbers with country-specific rules
 */
@Singleton
class PhoneValidationService @Inject constructor() {
    
    /**
     * Validate phone number based on country code
     * Returns smart, contextual error messages
     */
    fun validatePhoneNumber(phoneNumber: String, countryCode: CountryCode): ValidationResult {
        // Clean the phone number - remove all non-digit characters
        val cleanNumber = phoneNumber.replace(Regex("[^0-9]"), "")
        
        return when {
            cleanNumber.isEmpty() -> ValidationResult.Error("Please enter your phone number")
            
            // Check for non-digit characters (shouldn't happen after cleaning, but double-check)
            !cleanNumber.all { it.isDigit() } -> ValidationResult.Error("Phone number should only contain numbers")
            
            // Check length - provide helpful messages based on how close they are
            cleanNumber.length < countryCode.phoneLength -> {
                val digitsNeeded = countryCode.phoneLength - cleanNumber.length
                ValidationResult.Error(
                    if (digitsNeeded == 1) {
                        "Please add 1 more digit"
                    } else {
                        "Please add $digitsNeeded more digits"
                    }
                )
            }
            
            cleanNumber.length > countryCode.phoneLength -> ValidationResult.Error(
                "Phone number should be ${countryCode.phoneLength} digits. Remove ${cleanNumber.length - countryCode.phoneLength} digit${if (cleanNumber.length - countryCode.phoneLength > 1) "s" else ""}"
            )
            
            // Check format validity
            !isValidPhoneFormat(cleanNumber, countryCode) -> {
                when (countryCode) {
                    CountryCode.US, CountryCode.CA -> ValidationResult.Error(
                        "Invalid phone number format. US/Canada numbers should start with 2-9"
                    )
                    CountryCode.UK -> ValidationResult.Error(
                        "Invalid phone number format. UK numbers should start with 1-9"
                    )
                    else -> ValidationResult.Error("Invalid phone number format")
                }
            }
            
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Format phone number for display
     */
    fun formatPhoneNumber(phoneNumber: String, countryCode: CountryCode): String {
        // Clean the phone number - remove all non-digit characters
        val cleanNumber = phoneNumber.replace(Regex("[^0-9]"), "")
        
        return when (countryCode) {
            CountryCode.US -> {
                when {
                    cleanNumber.length == 10 -> "(${cleanNumber.substring(0, 3)}) ${cleanNumber.substring(3, 6)}-${cleanNumber.substring(6)}"
                    cleanNumber.length > 10 -> cleanNumber.substring(0, 10).let { 
                        "(${it.substring(0, 3)}) ${it.substring(3, 6)}-${it.substring(6)}"
                    }
                    else -> cleanNumber
                }
            }
            CountryCode.UK -> {
                when {
                    cleanNumber.length == 10 -> "${cleanNumber.substring(0, 4)} ${cleanNumber.substring(4, 7)} ${cleanNumber.substring(7)}"
                    cleanNumber.length > 10 -> cleanNumber.substring(0, 10).let {
                        "${it.substring(0, 4)} ${it.substring(4, 7)} ${it.substring(7)}"
                    }
                    else -> cleanNumber
                }
            }
            else -> cleanNumber
        }
    }
    
    /**
     * Check if phone number format is valid for the country
     */
    private fun isValidPhoneFormat(phoneNumber: String, countryCode: CountryCode): Boolean {
        return when (countryCode) {
            CountryCode.US -> phoneNumber.matches(Regex("^[2-9]\\d{2}[2-9]\\d{2}\\d{4}$"))
            CountryCode.UK -> phoneNumber.matches(Regex("^[1-9]\\d{9}$"))
            CountryCode.CA -> phoneNumber.matches(Regex("^[2-9]\\d{2}[2-9]\\d{2}\\d{4}$"))
            else -> true // For other countries, basic length check is sufficient
        }
    }
}

/**
 * Country code enumeration with phone number specifications
 */
enum class CountryCode(
    val code: String,
    val shortCode: String,
    val displayName: String,
    val phoneLength: Int
) {
    US("+1", "us", "United States", 10),
    UK("+44", "uk", "United Kingdom", 10),
    CA("+1", "ca", "Canada", 10),
    AU("+61", "au", "Australia", 9),
    DE("+49", "de", "Germany", 11),
    FR("+33", "fr", "France", 10),
    IN("+91", "in", "India", 10)
}
