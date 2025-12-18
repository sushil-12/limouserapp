package com.example.limouserapp.data.model.dashboard

import com.google.gson.annotations.SerializedName

/**
 * Request model for updating user profile
 * Matches iOS update profile structure
 */
data class UpdateProfileRequest(
    @SerializedName("first_name")
    val firstName: String,
    
    @SerializedName("last_name")
    val lastName: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("mobile")
    val mobile: String,
    
    @SerializedName("mobileIsd")
    val mobileIsd: String,
    
    @SerializedName("mobileCountry")
    val mobileCountry: String,
    
    @SerializedName("city")
    val city: String,
    
    @SerializedName("state")
    val state: String,
    
    @SerializedName("country")
    val country: String,
    
    @SerializedName("zip")
    val zip: String
)

