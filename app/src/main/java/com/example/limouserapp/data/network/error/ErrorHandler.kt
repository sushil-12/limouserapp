package com.example.limouserapp.data.network.error

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.limouserapp.R
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized error handling for network operations
 * Converts technical errors to user-friendly messages
 */
@Singleton
class ErrorHandler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    /**
     * Handle network errors and return user-friendly messages
     */
    fun handleError(throwable: Throwable): String {
        Timber.e(throwable, "Error occurred in API call")
        return when (throwable) {
            is NetworkError -> handleNetworkError(throwable)
            is HttpException -> handleHttpException(throwable)
            is SocketTimeoutException -> context.getString(R.string.error_timeout)
            is UnknownHostException -> context.getString(R.string.error_no_internet)
            is IOException -> context.getString(R.string.error_network_io)
            else -> context.getString(R.string.error_unknown)
        }
    }

    /**
     * Handle API response errors with detailed message extraction
     * This method should be used for all API calls to ensure consistent error handling
     */
    fun handleApiError(throwable: Throwable): String {
        Timber.e(throwable, "API Error occurred")
        return when (throwable) {
            is HttpException -> {
                val errorMessage = extractErrorMessageFromResponse(throwable)
                if (errorMessage.isNotEmpty()) {
                    errorMessage
                } else {
                    handleHttpException(throwable)
                }
            }
            is NetworkError -> handleNetworkError(throwable)
            is NetworkError.NetworkIOException -> {
                // Handle NetworkIOException specifically to extract the original error
                val originalException = throwable.originalException
                Timber.d("NetworkIOException with original exception: ${originalException.message}")
                when (originalException) {
                    is SocketTimeoutException -> context.getString(R.string.error_timeout)
                    is UnknownHostException -> context.getString(R.string.error_no_internet)
                    is IOException -> {
                        // Check if it's a server error by examining the message
                        val message = originalException.message ?: ""
                        if (message.contains("Unknown error occurred")) {
                            // This might be a server error that was wrapped
                            "Server error occurred. Please try again."
                        } else {
                            context.getString(R.string.error_network_io)
                        }
                    }
                    else -> context.getString(R.string.error_network_io)
                }
            }
            is SocketTimeoutException -> context.getString(R.string.error_timeout)
            is UnknownHostException -> context.getString(R.string.error_no_internet)
            is IOException -> {
                val message = throwable.message ?: ""
                if (message.contains("Unknown error occurred")) {
                    "Server error occurred. Please try again."
                } else {
                    context.getString(R.string.error_network_io)
                }
            }
            else -> context.getString(R.string.error_unknown)
        }
    }

    /**
     * Extract error message from HTTP response body
     * Handles various response formats and provides fallback messages
     */
    private fun extractErrorMessageFromResponse(exception: HttpException): String {
        return try {
            val errorBody = exception.response()?.errorBody()?.string()
            if (!errorBody.isNullOrEmpty()) {
                Timber.d("Error response body: $errorBody")
                extractMessageFromJson(errorBody)
            } else {
                ""
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract error message from response")
            ""
        }
    }

    /**
     * Extract message from JSON response
     * Handles different JSON structures commonly used in APIs
     */
    private fun extractMessageFromJson(jsonString: String): String {
        return try {
            val jsonObject = JSONObject(jsonString)

            // Try common message field names first
            val message = jsonObject.optString("message", "")
                .takeIf { it.isNotEmpty() }
                ?: jsonObject.optString("error", "")
                    .takeIf { it.isNotEmpty() }
                ?: jsonObject.optString("error_message", "")
                    .takeIf { it.isNotEmpty() }
                ?: jsonObject.optString("msg", "")
                    .takeIf { it.isNotEmpty() }
                ?: jsonObject.optString("description", "")
                    .takeIf { it.isNotEmpty() }
                ?: ""

            if (message.isNotEmpty()) {
                return message
            }

            // Fallback: find the first string field that looks like a message
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key is String) {
                    val value = jsonObject.optString(key, "")
                    if (value.isNotEmpty() && value.length > 3) {
                        return value
                    }
                }
            }

            "" // fallback if no valid message found
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse JSON error response")
            ""
        }
    }



    /**
     * Handle custom NetworkError types
     */
    private fun handleNetworkError(error: NetworkError): String {
        return when (error) {
            is NetworkError.NoInternetConnection -> context.getString(R.string.error_no_internet)
            is NetworkError.Timeout -> context.getString(R.string.error_timeout)
            is NetworkError.ServerError -> context.getString(R.string.error_server)
            is NetworkError.Unauthorized -> context.getString(R.string.error_unauthorized)
            is NetworkError.Forbidden -> context.getString(R.string.error_forbidden)
            is NetworkError.NotFound -> context.getString(R.string.error_not_found)
            is NetworkError.RateLimitExceeded -> context.getString(R.string.error_rate_limit)
            is NetworkError.ApiError -> error.message.ifEmpty { context.getString(R.string.error_api) }
            is NetworkError.NetworkIOException -> context.getString(R.string.error_network_io)
            is NetworkError.UnknownError -> {
                // Handle specific "closed" error
                if (error.throwable.message?.contains("closed") == true) {
                    context.getString(R.string.error_network_io)
                } else {
                    context.getString(R.string.error_unknown)
                }
            }
        }
    }
    
    /**
     * Handle HTTP exceptions
     */
    private fun handleHttpException(exception: HttpException): String {
        val errorCode = exception.code()
        val errorMessage = extractErrorMessageFromResponse(exception)
        
        return when (errorCode) {
            401 -> {
                if (errorMessage.isNotEmpty()) errorMessage
                else context.getString(R.string.error_unauthorized)
            }
            403 -> {
                if (errorMessage.isNotEmpty()) errorMessage
                else context.getString(R.string.error_forbidden)
            }
            404 -> {
                if (errorMessage.isNotEmpty()) errorMessage
                else context.getString(R.string.error_not_found)
            }
            429 -> {
                if (errorMessage.isNotEmpty()) errorMessage
                else context.getString(R.string.error_rate_limit)
            }
            in 500..599 -> {
                if (errorMessage.isNotEmpty()) errorMessage
                else context.getString(R.string.error_server)
            }
            else -> {
                if (errorMessage.isNotEmpty()) errorMessage
                else context.getString(R.string.error_http, errorCode)
            }
        }
    }
    
    /**
     * Check if device has internet connection
     */
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return when {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
    
    /**
     * Get retry suggestion based on error type
     */
    fun getRetrySuggestion(error: Throwable): String? {
        return when (error) {
            is NetworkError.NoInternetConnection -> context.getString(R.string.retry_check_internet)
            is NetworkError.Timeout -> context.getString(R.string.retry_timeout)
            is NetworkError.ServerError -> context.getString(R.string.retry_server_error)
            is NetworkError.RateLimitExceeded -> context.getString(R.string.retry_rate_limit)
            else -> null
        }
    }
}
