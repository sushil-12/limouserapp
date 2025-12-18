package com.example.limouserapp.data.network.error

import java.io.IOException

/**
 * Sealed class representing different types of network errors
 */
sealed class NetworkError : Exception() {
    
    /**
     * No internet connection available
     */
    object NoInternetConnection : NetworkError()
    
    /**
     * Request timeout
     */
    object Timeout : NetworkError()
    
    /**
     * Server error (5xx)
     */
    object ServerError : NetworkError()
    
    /**
     * Unauthorized access (401)
     */
    object Unauthorized : NetworkError()
    
    /**
     * Forbidden access (403)
     */
    object Forbidden : NetworkError()
    
    /**
     * Resource not found (404)
     */
    object NotFound : NetworkError()
    
    /**
     * Rate limit exceeded (429)
     */
    object RateLimitExceeded : NetworkError()
    
    /**
     * API error with specific code and message
     */
    data class ApiError(
        val code: Int,
        override val message: String,
        val errors: Map<String, List<String>>? = null
    ) : NetworkError()
    
    /**
     * Network I/O error
     */
    data class NetworkIOException(
        val originalException: IOException
    ) : NetworkError()
    
    /**
     * Unknown error
     */
    data class UnknownError(
        val throwable: Throwable
    ) : NetworkError()
}

/**
 * Extension function to convert HTTP status codes to NetworkError
 */
fun Int.toNetworkError(message: String? = null): NetworkError {
    return when (this) {
        401 -> NetworkError.Unauthorized
        403 -> NetworkError.Forbidden
        404 -> NetworkError.NotFound
        429 -> NetworkError.RateLimitExceeded
        in 500..599 -> NetworkError.ServerError
        else -> NetworkError.ApiError(this, message ?: "Unknown error")
    }
}
