package com.example.limouserapp.data.service

import com.example.limouserapp.data.api.BookingApi
import com.example.limouserapp.data.model.booking.CreateReservationRequest
import com.example.limouserapp.data.model.booking.CreateReservationResponse
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
}



