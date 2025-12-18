package com.example.limouserapp.data.service

import com.example.limouserapp.data.api.BookingApi
import com.example.limouserapp.data.model.booking.EditReservationRequest
import com.example.limouserapp.data.model.booking.EditReservationResponse
import com.example.limouserapp.data.model.booking.EditReservationUpdateResponse
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log
import com.example.limouserapp.ui.utils.DebugTags

/**
 * Edit Reservation Service - matches iOS EditReservationService
 * Handles fetching and updating edit reservation data
 */
@Singleton
class EditReservationService @Inject constructor(
    private val api: BookingApi
) {
    /**
     * Fetch edit reservation data for a given booking ID
     * Matches iOS fetchEditReservation function
     */
    suspend fun fetchEditReservation(bookingId: Int): Result<EditReservationResponse> = try {
        Log.d(DebugTags.BookingProcess, "===========================================")
        Log.d(DebugTags.BookingProcess, "🔄 FETCHING EDIT RESERVATION DATA")
        Log.d(DebugTags.BookingProcess, "===========================================")
        Log.d(DebugTags.BookingProcess, "API Endpoint: GET api/individual/get-reservation/$bookingId/edit")
        Log.d(DebugTags.BookingProcess, "Booking ID: $bookingId")
        Log.d(DebugTags.BookingProcess, "===========================================")
        
        val response = api.getEditReservation(bookingId)
        
        Log.d(DebugTags.BookingProcess, "===========================================")
        Log.d(DebugTags.BookingProcess, "✅ EDIT RESERVATION DATA FETCHED SUCCESSFULLY")
        Log.d(DebugTags.BookingProcess, "===========================================")
        Log.d(DebugTags.BookingProcess, "Response Success: ${response.success}")
        Log.d(DebugTags.BookingProcess, "Reservation ID: ${response.data.reservationId}")
        Log.d(DebugTags.BookingProcess, "Service Type: ${response.data.serviceType}")
        Log.d(DebugTags.BookingProcess, "Transfer Type: ${response.data.transferType}")
        Log.d(DebugTags.BookingProcess, "Vehicle ID: ${response.data.vehicleId}")
        Log.d(DebugTags.BookingProcess, "Driver ID: ${response.data.driverId}")
        Log.d(DebugTags.BookingProcess, "Total Passengers: ${response.data.totalPassengers}")
        Log.d(DebugTags.BookingProcess, "Luggage Count: ${response.data.luggageCount}")
        Log.d(DebugTags.BookingProcess, "===========================================")
        
        Result.success(response)
    } catch (e: Exception) {
        Log.e(DebugTags.BookingProcess, "===========================================")
        Log.e(DebugTags.BookingProcess, "❌ FAILED TO FETCH EDIT RESERVATION DATA")
        Log.e(DebugTags.BookingProcess, "===========================================")
        Log.e(DebugTags.BookingProcess, "API Endpoint: GET api/individual/get-reservation/$bookingId/edit")
        Log.e(DebugTags.BookingProcess, "Error: ${e.message}")
        Log.e(DebugTags.BookingProcess, "Exception Type: ${e.javaClass.simpleName}")
        e.printStackTrace()
        Log.e(DebugTags.BookingProcess, "===========================================")
        Result.failure(e)
    }
    
    /**
     * Update reservation with edit request
     * Matches iOS updateReservation function
     */
    suspend fun updateReservation(request: EditReservationRequest): Result<EditReservationUpdateResponse> = try {
        Log.d(DebugTags.BookingProcess, "===========================================")
        Log.d(DebugTags.BookingProcess, "🚀 UPDATE RESERVATION API CALL INITIATED")
        Log.d(DebugTags.BookingProcess, "===========================================")
        Log.d(DebugTags.BookingProcess, "API Endpoint: POST api/individual/edit-reservation")
        Log.d(DebugTags.BookingProcess, "Reservation ID: ${request.reservationId}")
        Log.d(DebugTags.BookingProcess, "Service Type: ${request.serviceType}")
        Log.d(DebugTags.BookingProcess, "Transfer Type: ${request.transferType}")
        Log.d(DebugTags.BookingProcess, "Vehicle ID: ${request.vehicleId}")
        Log.d(DebugTags.BookingProcess, "Driver ID: ${request.driverId}")
        Log.d(DebugTags.BookingProcess, "Grand Total: ${request.grandTotal}")
        Log.d(DebugTags.BookingProcess, "===========================================")
        
        val response = api.updateReservation(request)
        
        Log.d(DebugTags.BookingProcess, "===========================================")
        Log.d(DebugTags.BookingProcess, "✅ RESERVATION UPDATED SUCCESSFULLY")
        Log.d(DebugTags.BookingProcess, "===========================================")
        Log.d(DebugTags.BookingProcess, "Response Success: ${response.success}")
        Log.d(DebugTags.BookingProcess, "Response Message: ${response.message}")
        if (response.data != null) {
            Log.d(DebugTags.BookingProcess, "Reservation ID: ${response.data.reservationId}")
        }
        Log.d(DebugTags.BookingProcess, "===========================================")
        
        Result.success(response)
    } catch (e: Exception) {
        Log.e(DebugTags.BookingProcess, "===========================================")
        Log.e(DebugTags.BookingProcess, "❌ UPDATE RESERVATION API CALL FAILED")
        Log.e(DebugTags.BookingProcess, "===========================================")
        Log.e(DebugTags.BookingProcess, "API Endpoint: POST api/individual/edit-reservation")
        Log.e(DebugTags.BookingProcess, "Error: ${e.message}")
        Log.e(DebugTags.BookingProcess, "Exception Type: ${e.javaClass.simpleName}")
        e.printStackTrace()
        Log.e(DebugTags.BookingProcess, "===========================================")
        Result.failure(e)
    }
}

