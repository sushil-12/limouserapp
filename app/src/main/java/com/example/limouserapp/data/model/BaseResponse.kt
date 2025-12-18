package com.example.limouserapp.data.model

import com.google.gson.annotations.SerializedName

/**
 * Base response wrapper for all API responses
 * Provides consistent structure across all endpoints
 */
data class BaseResponse<T>(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: T?,
    
    @SerializedName("timestamp")
    val timestamp: String,
    
    @SerializedName("code")
    val code: Int
)

/**
 * Generic error response for API failures
 */
data class ErrorResponse(
    @SerializedName("success")
    val success: Boolean = false,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("errors")
    val errors: Map<String, List<String>>? = null,
    
    @SerializedName("timestamp")
    val timestamp: String,
    
    @SerializedName("code")
    val code: Int
)
