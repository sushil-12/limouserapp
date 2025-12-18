package com.example.limouserapp.data.network.interceptors

import android.util.Log
import com.example.limouserapp.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Logging interceptor for debugging network requests and responses
 * Only logs in debug builds for security
 */
@Singleton
class LoggingInterceptor @Inject constructor() : Interceptor {
    
    companion object {
        private const val TAG = "NetworkLogging"
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        
        // Log request details
        logRequest(request)
        
        val startTime = System.currentTimeMillis()
        val response = try {
            chain.proceed(request)
        } catch (e: IOException) {
            Log.e(TAG, "Request failed: ${e.message}")
            throw e
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // Log response details
        return logResponse(response, duration)
    }
    
    private fun logRequest(request: okhttp3.Request) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "--> ${request.method} ${request.url}")
            Log.d(TAG, "Headers: ${request.headers}")
            
            request.body?.let { body ->
                val buffer = Buffer()
                body.writeTo(buffer)
                Log.d(TAG, "Request Body: ${buffer.readUtf8()}")
            }
        }
    }
    
    private fun logResponse(response: Response, duration: Long): Response {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "<-- ${response.code} ${response.message} (${duration}ms)")
            Log.d(TAG, "Response Headers: ${response.headers}")
            
            val responseBody = response.body
            val responseBodyString = responseBody?.string()
            
            if (responseBodyString != null) {
                Log.d(TAG, "Response Body: $responseBodyString")
                
                // Create new response body since we consumed the original
                val newResponseBody = responseBodyString.toResponseBody(responseBody.contentType())
                return response.newBuilder()
                    .body(newResponseBody)
                    .build()
            }
        }
        return response
    }
}
