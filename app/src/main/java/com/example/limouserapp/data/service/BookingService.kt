package com.example.limouserapp.data.service

import com.example.limouserapp.data.api.BookingApi
import com.example.limouserapp.data.model.booking.CreateReservationRequest
import com.example.limouserapp.data.model.booking.CreateReservationResponse
import com.example.limouserapp.data.model.booking.DriverFeedbackRequest
import com.example.limouserapp.data.model.booking.DriverFeedbackResponse
import com.example.limouserapp.data.model.booking.GenerateRideOTPResponse
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import com.example.limouserapp.ui.utils.DebugTags

@Singleton
class BookingService @Inject constructor(
    private val api: BookingApi
) {
    suspend fun createReservation(request: CreateReservationRequest): Result<CreateReservationResponse> = try {
        Log.d(DebugTags.BookingProcess, "===========================================")
        Log.d(DebugTags.BookingProcess, "🚀 BOOK NOW CLICKED - API CALL INITIATED")
        Log.d(DebugTags.BookingProcess, "===========================================")
        Log.d(DebugTags.BookingProcess, "API Endpoint: POST api/individual/create-reservation")
        Log.d(DebugTags.BookingProcess, "Service Type: ${request.serviceType}")
        Log.d(DebugTags.BookingProcess, "Vehicle ID: ${request.vehicleId}")
        Log.d(DebugTags.BookingProcess, "Pickup: ${request.pickup}")
        Log.d(DebugTags.BookingProcess, "Dropoff: ${request.dropoff}")
        Log.d(DebugTags.BookingProcess, "Grand Total: ${request.grandTotal}")
        Log.d(DebugTags.BookingProcess, "Sub Total: ${request.subTotal}")
        Log.d(DebugTags.BookingProcess, "Shares Array - Base Rate: ${request.sharesArray.baseRate}")
        Log.d(DebugTags.BookingProcess, "Shares Array - Grand Total: ${request.sharesArray.grandTotal}")
        Log.d(DebugTags.BookingProcess, "Shares Array - Affiliate Share: ${request.sharesArray.affiliateShare}")
        Log.d(DebugTags.BookingProcess, "===========================================")

        Log.d(DebugTags.BookingProcess, request.toString())


        val response = api.createReservation(request)

        Log.d(DebugTags.BookingProcess, "===========================================")
        Log.d(DebugTags.BookingProcess, "✅ API CALL SUCCESSFUL")
        Log.d(DebugTags.BookingProcess, "===========================================")
        Log.d(DebugTags.BookingProcess, "Response Success: ${response.success}")
        Log.d(DebugTags.BookingProcess, "Response Message: ${response.message}")
        Log.d(DebugTags.BookingProcess, "Booking ID: ${response.bookingId}")
        Log.d(DebugTags.BookingProcess, "===========================================")

        Result.success(response)
    } catch (e: Exception) {
        Log.e(DebugTags.BookingProcess, "===========================================")
        Log.e(DebugTags.BookingProcess, "❌ API CALL FAILED")
        Log.e(DebugTags.BookingProcess, "===========================================")
        Log.e(DebugTags.BookingProcess, "API Endpoint: POST api/individual/create-reservation")
        Log.e(DebugTags.BookingProcess, "Error: ${e.message}")
        Log.e(DebugTags.BookingProcess, "Exception Type: ${e.javaClass.simpleName}")
        e.printStackTrace()
        Log.e(DebugTags.BookingProcess, "===========================================")
        Result.failure(e)
    }
    
    suspend fun generateRideOTP(bookingId: Int): Result<GenerateRideOTPResponse> = try {
        val response = api.generateRideOTP(bookingId)
        if (response.success && response.data != null) {
            Result.success(response.data)
        } else {
            Result.failure(Exception(response.message ?: "Failed to generate OTP"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    suspend fun submitDriverFeedback(
        bookingId: Int,
        rating: Int,
        feedback: String?
    ): Result<DriverFeedbackResponse> = try {
        val request = DriverFeedbackRequest(
            rating = rating,
            feedback = feedback
        )
        val response = api.submitDriverFeedback(bookingId, request)
        if (response.success && response.data != null) {
            Result.success(response.data)
        } else {
            Result.failure(Exception(response.message ?: "Failed to submit feedback"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}



