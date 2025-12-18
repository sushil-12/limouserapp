package com.example.limouserapp.data.network.interceptors

import com.example.limouserapp.data.storage.TokenManager
import com.example.limouserapp.data.network.NetworkConfig
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authentication interceptor
 * Automatically adds Bearer token to requests that require authentication
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        // Skip auth for public endpoints
        if (isPublicEndpoint(originalRequest.url.toString())) {
            return chain.proceed(originalRequest)
        }
        
        // Add auth token if available
        val token = tokenManager.getAccessToken()
        val request = if (token != null) {
            android.util.Log.d("AuthInterceptor", "Adding Bearer token to request: ${token.take(20)}...")
            originalRequest.newBuilder()
                .addHeader(NetworkConfig.AUTHORIZATION, "${NetworkConfig.BEARER_PREFIX}$token")
                .build()
        } else {
            android.util.Log.w("AuthInterceptor", "No token available for request: ${originalRequest.url}")
            originalRequest
        }
        
        return chain.proceed(request)
    }
    
    /**
     * Check if the endpoint is public (doesn't require authentication)
     */
    private fun isPublicEndpoint(url: String): Boolean {
        val publicEndpoints = listOf(
            "auth/login-or-register",
            "auth/verify-otp",
            "auth/resend-otp",
            "maps.googleapis.com" // Google Places API
        )
        
        return publicEndpoints.any { url.contains(it) }
    }
}
