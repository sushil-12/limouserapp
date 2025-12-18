package com.example.limouserapp.data.service

import com.example.limouserapp.data.api.QuoteApi
import com.example.limouserapp.data.model.booking.BookingRatesRequest
import com.example.limouserapp.data.model.booking.BookingRatesResponse
import com.example.limouserapp.ui.utils.DebugTags
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Booking Rates Service - matches iOS BookingRatesService
 * Fetches detailed rate breakdown from booking-rates-vehicle API
 */
@Singleton
class BookingRatesService @Inject constructor(
    private val quoteApi: QuoteApi
) {
    /**
     * Fetch booking rates for a vehicle
     * Matches iOS fetchBookingRates implementation
     */
    suspend fun fetchBookingRates(request: BookingRatesRequest): Result<BookingRatesResponse> = try {
        Log.d(DebugTags.BookingProcess, "===========================================")
        Log.d(DebugTags.BookingProcess, "🚀 BOOKING RATES SERVICE - fetchBookingRates() CALLED")
        Log.d(DebugTags.BookingProcess, "===========================================")
        Log.d(DebugTags.BookingProcess, "API Endpoint: POST api/admin/booking-rates-vehicle")
        Log.d(DebugTags.BookingProcess, "QuoteApi instance: ${quoteApi.javaClass.simpleName}")
        Log.d(DebugTags.BookingProcess, "About to call quoteApi.getBookingRates()...")
        Log.d(DebugTags.BookingProcess, "===========================================")
        Log.d(DebugTags.BookingProcess, "Vehicle ID: ${request.vehicleId}")
        Log.d(DebugTags.BookingProcess, "Transfer Type: ${request.transferType}")
        Log.d(DebugTags.BookingProcess, "Service Type: ${request.serviceType}")
        Log.d(DebugTags.BookingProcess, "Number of Vehicles: ${request.numberOfVehicles}")
        Log.d(DebugTags.BookingProcess, "Distance: ${request.distance} meters")
        Log.d(DebugTags.BookingProcess, "Return Distance: ${request.returnDistance} meters")
        Log.d(DebugTags.BookingProcess, "No of Hours: ${request.noOfHours}")
        Log.d(DebugTags.BookingProcess, "Is Master Vehicle: ${request.isMasterVehicle}")
        Log.d(DebugTags.BookingProcess, "Pickup Time: ${request.pickupTime}")
        Log.d(DebugTags.BookingProcess, "Return Pickup Time: ${request.returnPickupTime}")
        Log.d(DebugTags.BookingProcess, "Return Vehicle ID: ${request.returnVehicleId}")
        Log.d(DebugTags.BookingProcess, "Return Affiliate Type: ${request.returnAffiliateType}")
        Log.d(DebugTags.BookingProcess, "Extra Stops: ${request.extraStops.size}")
        Log.d(DebugTags.BookingProcess, "Return Extra Stops: ${request.returnExtraStops.size}")
        Log.d(DebugTags.BookingProcess, "===========================================")
        Log.d(DebugTags.BookingProcess, "📡 MAKING NETWORK CALL: quoteApi.getBookingRates(request)")
        Log.d(DebugTags.BookingProcess, "===========================================")
        
        val response = quoteApi.getBookingRates(request)
        Log.d(DebugTags.BookingProcess, request.toString())
        Log.d(DebugTags.BookingProcess, "===========================================")
        Log.d(DebugTags.BookingProcess, "✅ NETWORK CALL SUCCEEDED - Got response")
        Log.d(DebugTags.BookingProcess, "===========================================")
        Log.d(DebugTags.BookingProcess, "✅ BOOKING RATES API RESPONSE")
        Log.d(DebugTags.BookingProcess, "===========================================")
        Log.d(DebugTags.BookingProcess, "Success: ${response.success}")
        Log.d(DebugTags.BookingProcess, "Message: ${response.message}")
        Log.d(DebugTags.BookingProcess, "Sub Total: ${response.data.subTotal}")
        Log.d(DebugTags.BookingProcess, "Grand Total: ${response.data.grandTotal}")
        Log.d(DebugTags.BookingProcess, "Min Rate Involved: ${response.data.minRateInvolved}")
        Log.d(DebugTags.BookingProcess, "Currency: ${response.currency?.symbol ?: "Unknown"}")
        
        // Log rate breakdown details
        val rateArray = response.data.rateArray
        Log.d(DebugTags.BookingProcess, "📊 RATE BREAKDOWN:")
        Log.d(DebugTags.BookingProcess, "Base Rate: ${rateArray.allInclusiveRates["Base_Rate"]?.amount ?: 0.0}")
        Log.d(DebugTags.BookingProcess, "Stops: ${rateArray.allInclusiveRates["Stops"]?.amount ?: 0.0}")
        Log.d(DebugTags.BookingProcess, "Wait: ${rateArray.allInclusiveRates["Wait"]?.amount ?: 0.0}")
        Log.d(DebugTags.BookingProcess, "ELH Charges: ${rateArray.allInclusiveRates["ELH_Charges"]?.amount ?: 0.0}")
        Log.d(DebugTags.BookingProcess, "===========================================")
        
        Result.success(response)
    } catch (e: Exception) {
        Log.e(DebugTags.BookingProcess, "===========================================")
        Log.e(DebugTags.BookingProcess, "❌ BOOKING RATES API FAILED")
        Log.e(DebugTags.BookingProcess, "===========================================")
        Log.e(DebugTags.BookingProcess, "API Endpoint: POST api/admin/booking-rates-vehicle")
        Log.e(DebugTags.BookingProcess, "Error: ${e.message}")
        Log.e(DebugTags.BookingProcess, "Exception Type: ${e.javaClass.simpleName}")
        e.printStackTrace()
        Log.e(DebugTags.BookingProcess, "===========================================")
        Result.failure(e)
    }
    
    /**
     * Fetch reservation rates for an existing booking
     * Matches iOS fetchReservationRates implementation
     * Used in edit mode for one-way trips without extra stops
     */
    suspend fun fetchReservationRates(bookingId: Int): Result<BookingRatesResponse> = try {
        Log.d(DebugTags.BookingProcess, "===========================================")
        Log.d(DebugTags.BookingProcess, "🚀 RESERVATION RATES SERVICE - fetchReservationRates() CALLED")
        Log.d(DebugTags.BookingProcess, "===========================================")
        Log.d(DebugTags.BookingProcess, "API Endpoint: GET api/admin/reservation-rates/$bookingId")
        Log.d(DebugTags.BookingProcess, "Booking ID: $bookingId")
        Log.d(DebugTags.BookingProcess, "===========================================")
        Log.d(DebugTags.BookingProcess, "📡 MAKING NETWORK CALL: quoteApi.getReservationRates($bookingId)")
        Log.d(DebugTags.BookingProcess, "===========================================")
        
        val response = quoteApi.getReservationRates(bookingId)
        
        Log.d(DebugTags.BookingProcess, "===========================================")
        Log.d(DebugTags.BookingProcess, "✅ NETWORK CALL SUCCEEDED - Got response")
        Log.d(DebugTags.BookingProcess, "===========================================")
        Log.d(DebugTags.BookingProcess, "✅ RESERVATION RATES API RESPONSE")
        Log.d(DebugTags.BookingProcess, "===========================================")
        Log.d(DebugTags.BookingProcess, "Success: ${response.success}")
        Log.d(DebugTags.BookingProcess, "Message: ${response.message}")
        Log.d(DebugTags.BookingProcess, "Sub Total: ${response.data.subTotal}")
        Log.d(DebugTags.BookingProcess, "Grand Total: ${response.data.grandTotal}")
        Log.d(DebugTags.BookingProcess, "Min Rate Involved: ${response.data.minRateInvolved}")
        Log.d(DebugTags.BookingProcess, "Currency: ${response.currency?.symbol ?: "Unknown"}")
        
        // Log rate breakdown details
        val rateArray = response.data.rateArray
        Log.d(DebugTags.BookingProcess, "📊 RATE BREAKDOWN:")
        Log.d(DebugTags.BookingProcess, "Base Rate: ${rateArray.allInclusiveRates["Base_Rate"]?.amount ?: 0.0}")
        Log.d(DebugTags.BookingProcess, "Stops: ${rateArray.allInclusiveRates["Stops"]?.amount ?: 0.0}")
        Log.d(DebugTags.BookingProcess, "Wait: ${rateArray.allInclusiveRates["Wait"]?.amount ?: 0.0}")
        Log.d(DebugTags.BookingProcess, "ELH Charges: ${rateArray.allInclusiveRates["ELH_Charges"]?.amount ?: 0.0}")
        Log.d(DebugTags.BookingProcess, "===========================================")
        
        Result.success(response)
    } catch (e: Exception) {
        Log.e(DebugTags.BookingProcess, "===========================================")
        Log.e(DebugTags.BookingProcess, "❌ RESERVATION RATES API FAILED")
        Log.e(DebugTags.BookingProcess, "===========================================")
        Log.e(DebugTags.BookingProcess, "API Endpoint: GET api/admin/reservation-rates/$bookingId")
        Log.e(DebugTags.BookingProcess, "Error: ${e.message}")
        Log.e(DebugTags.BookingProcess, "Exception Type: ${e.javaClass.simpleName}")
        e.printStackTrace()
        Log.e(DebugTags.BookingProcess, "===========================================")
        Result.failure(e)
    }
}

