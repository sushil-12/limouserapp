package com.example.limouserapp.data.repository

import com.example.limouserapp.data.api.AuthApi
import com.example.limouserapp.data.model.BaseResponse
import com.example.limouserapp.data.model.auth.AuthData
import com.example.limouserapp.data.model.auth.LoginRegisterRequest
import com.example.limouserapp.data.model.auth.VerifyOTPData
import com.example.limouserapp.data.model.auth.VerifyOTPRequest
import com.example.limouserapp.data.network.error.ErrorHandler
import com.example.limouserapp.data.network.error.NetworkError
import com.example.limouserapp.data.storage.TokenManager
import com.example.limouserapp.domain.validation.CountryCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authentication repository
 * Handles all authentication-related data operations
 */
@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
    private val errorHandler: ErrorHandler
) {
    
    /**
     * Send verification code to phone number
     */
    suspend fun sendVerificationCode(
        phoneNumber: String,
        countryCode: CountryCode
    ): Result<BaseResponse<AuthData>> {
        return withContext(Dispatchers.IO) {
            try {
                val request = LoginRegisterRequest(
                    phoneIsd = countryCode.code,
                    phoneCountry = countryCode.shortCode,
                    phone = phoneNumber,
                    userType = "customer"
                )
                
                val response = authApi.sendVerificationCode(request)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }
    
    /**
     * Verify OTP code
     */
    suspend fun verifyOTP(
        tempUserId: String,
        otp: String
    ): Result<BaseResponse<VerifyOTPData>> {
        return withContext(Dispatchers.IO) {
            try {
                val request = VerifyOTPRequest(
                    tempUserId = tempUserId,
                    otp = otp
                )
                
                val response = authApi.verifyOTP(request)
                
                // Save tokens if verification is successful
                if (response.success && response.data != null) {
                    tokenManager.saveTokens(
                        accessToken = response.data.token,
                        tokenType = response.data.tokenType,
                        expiresIn = response.data.expiresIn
                    )
                }
                
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }
    
    /**
     * Resend OTP code
     */
    suspend fun resendOTP(tempUserId: String): Result<BaseResponse<AuthData>> {
        return withContext(Dispatchers.IO) {
            try {
                val request = mapOf("temp_user_id" to tempUserId)
                val response = authApi.resendOTP(request)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(Exception(errorHandler.handleApiError(e)))
            }
        }
    }
    
    /**
     * Check if user is authenticated
     */
    fun isAuthenticated(): Boolean {
        return tokenManager.isAuthenticated()
    }
    
    /**
     * Get current access token
     */
    fun getAccessToken(): String? {
        return tokenManager.getAccessToken()
    }
    
    /**
     * Clear authentication data
     */
    fun logout() {
        tokenManager.clearTokens()
    }
    
}
