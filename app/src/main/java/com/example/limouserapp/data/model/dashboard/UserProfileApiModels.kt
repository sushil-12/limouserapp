package com.example.limouserapp.data.model.dashboard

import com.google.gson.annotations.SerializedName

/**
 * Response model for GET /api/mobile/v1/user/profile
 */
data class UserProfileApiResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String? = null,
    
    @SerializedName("data")
    val data: UserProfileData? = null,
    
    @SerializedName("timestamp")
    val timestamp: String? = null,
    
    @SerializedName("code")
    val code: Int? = null
)

/**
 * User profile data wrapper
 */
data class UserProfileData(
    @SerializedName("user")
    val user: UserProfileDetails
)

/**
 * User profile details
 */
data class UserProfileDetails(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("first_name")
    val firstName: String,
    
    @SerializedName("last_name")
    val lastName: String,
    
    @SerializedName("email")
    val email: String? = null,
    
    @SerializedName("phone")
    val phone: String? = null,
    
    @SerializedName("phone_isd")
    val phoneIsd: String? = null,
    
    @SerializedName("profile_image")
    val profileImage: String? = null,
    
    @SerializedName("gender")
    val gender: String? = null,
    
    @SerializedName("address")
    val address: String? = null,
    
    @SerializedName("city")
    val city: String? = null,
    
    @SerializedName("state")
    val state: String? = null,
    
    @SerializedName("country")
    val country: String? = null,
    
    @SerializedName("zip")
    val zip: String? = null,
    
    @SerializedName("latitude")
    val latitude: String? = null,
    
    @SerializedName("longitude")
    val longitude: String? = null,
    
    @SerializedName("dob")
    val dob: String? = null,
    
    @SerializedName("created_at")
    val createdAt: String? = null
)

/**
 * Request model for PUT /api/mobile/v1/user/profile
 */
data class UpdateUserProfileRequest(
    @SerializedName("first_name")
    val firstName: String,
    
    @SerializedName("last_name")
    val lastName: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("gender")
    val gender: String? = null,
    
    @SerializedName("date_of_birth")
    val dateOfBirth: String? = null,
    
    @SerializedName("address")
    val address: String? = null,
    
    @SerializedName("city")
    val city: String? = null,
    
    @SerializedName("state")
    val state: String? = null,
    
    @SerializedName("country")
    val country: String? = null,
    
    @SerializedName("zip")
    val zip: String? = null,
    
    @SerializedName("latitude")
    val latitude: Double? = null,
    
    @SerializedName("longitude")
    val longitude: Double? = null
)

