package com.example.limouserapp.data.api

import com.example.limouserapp.data.model.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * API interface for notification-related endpoints
 */
interface NotificationApi {
    
    /**
     * Register FCM token
     */
    @POST("api/mobile/v1/notification/register-token")
    suspend fun registerToken(
        @Body request: FcmTokenRequest
    ): ApiResponse<Unit>
    
    /**
     * Unregister FCM token (on logout)
     */
    @POST("api/mobile/v1/notification/unregister-token")
    suspend fun unregisterToken(
        @Body request: FcmTokenRequest
    ): ApiResponse<Unit>
}

/**
 * Request model for FCM token operations
 */
data class FcmTokenRequest(
    val fcmToken: String,
    val deviceId: String,
    val platform: String = "android"
)

