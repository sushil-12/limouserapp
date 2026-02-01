package com.example.limouserapp.data.network

/**
 * Network configuration constants
 */
object NetworkConfig {
    const val BASE_URL = "https://1800limoapi.infodevbox.com"
//     const val BASE_URL = "https://api.1800limo.com"
    const val GOOGLE_PLACES_BASE_URL = "https://maps.googleapis.com"
    
    // Timeout configurations
    const val CONNECT_TIMEOUT = 30L
    const val READ_TIMEOUT = 30L
    const val WRITE_TIMEOUT = 30L
    
    // Retry configuration
    const val MAX_RETRIES = 3
    const val RETRY_DELAY_MS = 2000L
    
    // API Headers
    const val CONTENT_TYPE = "application/json"
    const val ACCEPT = "application/json"
    const val AUTHORIZATION = "Authorization"
    const val BEARER_PREFIX = "Bearer "
    
    // API Keys
    const val GOOGLE_PLACES_API_KEY = "AIzaSyDjV38fI9kDAaVJKqEq2sdgLAHXQPC3Up4"
}
