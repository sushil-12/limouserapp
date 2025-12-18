package com.example.limouserapp.data.api

import com.example.limouserapp.data.model.BaseResponse
import com.example.limouserapp.data.model.auth.AuthData
import com.example.limouserapp.data.model.auth.LoginRegisterRequest
import com.example.limouserapp.data.model.auth.VerifyOTPData
import com.example.limouserapp.data.model.auth.VerifyOTPRequest
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Authentication API service interface
 * Handles all authentication-related API calls
 */
interface AuthApi {
    
    /**
     * Send verification code to phone number
     * @param request Phone number and country information
     * @return Response containing temp user ID and OTP details
     */
    @POST("api/mobile/v1/auth/login-or-register")
    suspend fun sendVerificationCode(
        @Body request: LoginRegisterRequest
    ): BaseResponse<AuthData>
    
    /**
     * Verify OTP code
     * @param request OTP and temp user ID
     * @return Response containing user data and authentication token
     */
    @POST("api/mobile/v1/auth/verify-otp")
    suspend fun verifyOTP(
        @Body request: VerifyOTPRequest
    ): BaseResponse<VerifyOTPData>
    
    /**
     * Resend OTP code
     * @param request Temp user ID for resending OTP
     * @return Response containing new OTP details
     */
    @POST("api/mobile/v1/auth/resend-otp")
    suspend fun resendOTP(
        @Body request: Map<String, String>
    ): BaseResponse<AuthData>
}
