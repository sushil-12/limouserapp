package com.example.limouserapp.data.model.booking

import com.google.gson.annotations.SerializedName

/**
 * Response model for generating ride OTP
 * Note: No request body is required for the generate OTP endpoint
 */
data class GenerateRideOTPResponse(
    @SerializedName("booking_id")
    val bookingId: Int,
    
    @SerializedName("otp")
    val otp: String,
    
    @SerializedName("generated_at")
    val generatedAt: String
)

/**
 * Request model for submitting driver feedback
 */
data class DriverFeedbackRequest(
    @SerializedName("rating")
    val rating: Int,
    
    @SerializedName("feedback")
    val feedback: String?
)

/**
 * Response model for submitting driver feedback
 */
data class DriverFeedbackResponse(
    @SerializedName("booking_id")
    val bookingId: Int,
    
    @SerializedName("rating")
    val rating: Int,
    
    @SerializedName("submitted_at")
    val submittedAt: String
)
