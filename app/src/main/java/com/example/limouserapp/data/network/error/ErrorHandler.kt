package com.example.limouserapp.data.network.error

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AppErrorHandler"

@Singleton
class ErrorHandler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun handleError(throwable: Throwable): String {
        Timber.tag(TAG).e(throwable, ">> handleError triggered. Exception Type: ${throwable.javaClass.simpleName}")

        val userMessage = when (throwable) {
            is HttpException -> handleHttpException(throwable)
            is NetworkError -> handleNetworkError(throwable)
            is SocketTimeoutException -> "Request timed out. Please try again."
            is UnknownHostException -> "Unable to reach server. Check your connection."
            is IOException -> "Network connection error. Please try again."

            // --- NEW FIX START ---
            // If the error is a generic Exception (exactly java.lang.Exception),
            // it usually means we manually threw it with a message from the Repository.
            // We verify it's not a RuntimeException (like NullPointer) to be safe.
            is Exception -> {
                if (throwable.javaClass == Exception::class.java && !throwable.message.isNullOrEmpty()) {
                    throwable.message!!
                } else {
                    "An unexpected error occurred. Please try again."
                }
            }
            // --- NEW FIX END ---

            else -> "An unexpected error occurred. Please try again."
        }

        Timber.tag(TAG).d("<< Final User Message: '$userMessage'")
        return userMessage
    }

    private fun handleHttpException(exception: HttpException): String {
        val code = exception.code()

        // Log raw body for debugging
        val errorBody = try {
            exception.response()?.errorBody()?.string()
        } catch (e: Exception) {
            null
        }

        Timber.tag(TAG).d("Raw Error Body: '$errorBody'")

        if (!errorBody.isNullOrEmpty()) {
            val serverMessage = extractMessageFromJson(errorBody)
            if (serverMessage.isNotEmpty()) {
                return serverMessage
            }
        }

        return when (code) {
            401, 440 -> "User session has expired!" // Added 440 based on your logs
            403 -> "Access forbidden."
            404 -> "Resource not found."
            408 -> "Request timed out."
            422 -> "Invalid data provided."
            429 -> "Too many requests. Please wait a moment."
            in 500..599 -> "Server error. Please try again later."
            else -> "Server error ($code)."
        }
    }

    private fun extractMessageFromJson(jsonString: String): String {
        return try {
            val jsonObject = JSONObject(jsonString)

            if (jsonObject.has("message")) {
                val msg = jsonObject.getString("message")
                if (msg.isNotEmpty()) return msg
            }

            if (jsonObject.has("error")) {
                val err = jsonObject.getString("error")
                if (err.isNotEmpty()) return err
            }

            if (jsonObject.has("detail")) {
                val detail = jsonObject.getString("detail")
                if (detail.isNotEmpty()) return detail
            }

            ""
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "JSON Parsing Exception")
            ""
        }
    }

    private fun handleNetworkError(error: NetworkError): String {
        return when (error) {
            is NetworkError.NoInternetConnection -> "No internet connection."
            is NetworkError.Timeout -> "Request timed out."
            is NetworkError.Unauthorized -> "Session expired. Please login again."
            is NetworkError.RateLimitExceeded -> "Too many requests. Please try again."
            is NetworkError.ServerError -> "Server error. Please try again later."
            is NetworkError.NetworkIOException -> "Network error. Please try again."
            is NetworkError.ApiError -> error.message.ifEmpty { "API Error occurred." }
            else -> "An unexpected error occurred."
        }
    }

    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
