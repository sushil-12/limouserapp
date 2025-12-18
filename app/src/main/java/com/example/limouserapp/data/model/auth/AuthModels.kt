package com.example.limouserapp.data.model.auth

import com.google.gson.annotations.SerializedName

/**
 * Request model for phone number validation and OTP request
 */
data class LoginRegisterRequest(
    @SerializedName("phone_isd")
    val phoneIsd: String,
    
    @SerializedName("phone_country")
    val phoneCountry: String,
    
    @SerializedName("phone")
    val phone: String,
    
    @SerializedName("user_type")
    val userType: String = "customer"
)

/**
 * Response data for phone number validation
 */
data class AuthData(
    @SerializedName("message")
    val message: String,
    
    @SerializedName("otp_type")
    val otpType: String,
    
    @SerializedName("temp_user_id")
    val tempUserId: String,
    
    @SerializedName("expires_in")
    val expiresIn: Int,
    
    @SerializedName("cooldown_remaining")
    val cooldownRemaining: Int
)

/**
 * Request model for OTP verification
 */
data class VerifyOTPRequest(
    @SerializedName("temp_user_id")
    val tempUserId: String,
    
    @SerializedName("otp")
    val otp: String
)

/**
 * User model from API response
 * Handles is_profile_completed as either Boolean or Int (1/0) to match iOS behavior
 */
data class User(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("phone")
    val phone: String,
    
    @SerializedName("role")
    val role: Int,
    
    @SerializedName("is_profile_completed")
    val isProfileCompleted: Boolean?,
    
    @SerializedName("last_login_at")
    val lastLoginAt: String,
    
    @SerializedName("created_from")
    val createdFrom: String?,
    
    @SerializedName("customer_registration_state")
    val customerRegistrationState: CustomerRegistrationState?
)

/**
 * Customer registration state tracking
 */
data class CustomerRegistrationState(
    @SerializedName("current_step")
    val currentStep: String,
    
    @SerializedName("progress_percentage")
    val progressPercentage: Int,
    
    @SerializedName("is_completed")
    val isCompleted: Boolean,
    
    @SerializedName("next_step")
    val nextStep: String,
    
    @SerializedName("steps")
    val steps: RegistrationSteps,
    
    @SerializedName("completed_steps")
    val completedSteps: List<String>,
    
    @SerializedName("total_steps")
    val totalSteps: Int,
    
    @SerializedName("completed_count")
    val completedCount: Int
)

/**
 * Registration steps tracking
 */
data class RegistrationSteps(
    @SerializedName("phone_verified")
    val phoneVerified: Boolean,
    
    @SerializedName("basic_details")
    val basicDetails: Boolean,
    
    @SerializedName("credit_card")
    val creditCard: Boolean,
    
    @SerializedName("profile_complete")
    val profileComplete: Boolean
)

/**
 * OTP verification response data
 */
data class VerifyOTPData(
    @SerializedName("user")
    val user: User,
    
    @SerializedName("token")
    val token: String,
    
    @SerializedName("token_type")
    val tokenType: String,
    
    @SerializedName("expires_in")
    val expiresIn: Int,
    
    @SerializedName("action")
    val action: String,
    
    @SerializedName("driver_registration_state")
    val driverRegistrationState: DriverRegistrationState?
)

/**
 * Driver registration state (for future use)
 */
data class DriverRegistrationState(
    @SerializedName("current_step")
    val currentStep: String,
    
    @SerializedName("progress_percentage")
    val progressPercentage: Int,
    
    @SerializedName("is_completed")
    val isCompleted: Boolean
)
