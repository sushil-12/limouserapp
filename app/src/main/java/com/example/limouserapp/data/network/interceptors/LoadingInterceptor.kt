package com.example.limouserapp.data.network.interceptors

import com.example.limouserapp.data.network.LoadingStateManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network loading interceptor
 * Automatically tracks loading state for all API calls
 * Shows/hides global loading overlay based on network activity
 */
@Singleton
class LoadingInterceptor @Inject constructor(
    private val loadingStateManager: LoadingStateManager
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        // Skip loading indicator for certain endpoints if needed
        // (e.g., WebSocket connections, background sync)
        if (shouldSkipLoading(request.url.toString())) {
            return chain.proceed(request)
        }
        
        // Start loading
        loadingStateManager.startLoading()
        
        return try {
            val response = chain.proceed(request)
            // Stop loading on success
            loadingStateManager.stopLoading()
            response
        } catch (e: Exception) {
            // Stop loading on error
            loadingStateManager.stopLoading()
            throw e
        }
    }
    
    /**
     * Determine if loading indicator should be skipped for this request
     */
    private fun shouldSkipLoading(url: String): Boolean {
        // Skip for WebSocket-like endpoints, health checks, etc.
        val skipPatterns = listOf(
            "ws://",
            "wss://",
            "/health",
            "/ping"
        )
        
        // Skip loading for airport API calls (they use shimmer instead)
        if (url.contains("/api/mobile-data", ignoreCase = true)) {
            if (url.contains("only_airports", ignoreCase = true) ||
                url.contains("onlyAirports", ignoreCase = true)) {
                return true
            }
        }

        // Skip global overlay for FAQ and Tutorials – screens use in-place shimmers
        if (url.contains("api/user-faq", ignoreCase = true) ||
            url.contains("api/tutorials", ignoreCase = true)) {
            return true
        }

        return skipPatterns.any { url.contains(it, ignoreCase = true) }
    }
}

