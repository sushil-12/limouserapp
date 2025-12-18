package com.example.limouserapp.data.network.interceptors

import com.example.limouserapp.data.network.NetworkConfig
import kotlinx.coroutines.delay
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retry interceptor for handling network failures
 * Implements exponential backoff strategy
 */
@Singleton
class RetryInterceptor @Inject constructor() : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        var lastException: IOException? = null
        var response: Response? = null
        
        repeat(NetworkConfig.MAX_RETRIES) { attempt ->
            try {
                response = chain.proceed(chain.request())
                
                // If response is successful or client error (4xx), don't retry
                if (response!!.isSuccessful || response!!.code in 400..499) {
                    return response!!
                }
                
                // For server errors (5xx), return the response immediately
                // This allows the repository to handle the error message properly
                if (response!!.code in 500..599) {
                    return response!!
                } else {
                    // For other cases, return the response without closing
                    return response!!
                }
                
            } catch (e: IOException) {
                lastException = e
                response = null
            }
            
            // Don't retry on the last attempt
            if (attempt < NetworkConfig.MAX_RETRIES - 1) {
                val delay = NetworkConfig.RETRY_DELAY_MS * (attempt + 1)
                try {
                    Thread.sleep(delay)
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw IOException("Retry interrupted", e)
                }
            }
        }
        
        // If we have a response, return it
        response?.let { return it }
        
        // Otherwise, throw the last exception with more context
        throw lastException ?: IOException("Network request failed after ${NetworkConfig.MAX_RETRIES} attempts")
    }
}
