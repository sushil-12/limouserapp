package com.example.limouserapp.data.api

import com.example.limouserapp.data.model.booking.VehicleListingRequest
import com.example.limouserapp.data.model.booking.VehicleListingArrayResponse
import com.example.limouserapp.data.model.booking.VehicleMasterListingResponse
import com.example.limouserapp.data.model.booking.BookingRatesRequest
import com.example.limouserapp.data.model.booking.BookingRatesResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface QuoteApi {
    @POST("api/quote/master-vehicle-listing")
    suspend fun getMasterVehicleListing(@Body request: VehicleListingRequest): VehicleMasterListingResponse

    @POST("api/quote/vehicle-listing")
    suspend fun getVehicleListing(@Body request: VehicleListingRequest): VehicleListingArrayResponse
    
    /**
     * Get booking rates for a vehicle
     * Matches iOS endpoint: /api/admin/booking-rates-vehicle
     */
    @POST("api/admin/booking-rates-vehicle")
    suspend fun getBookingRates(@Body request: BookingRatesRequest): BookingRatesResponse
    
    /**
     * Get reservation rates for an existing booking
     * Matches iOS endpoint: GET /api/admin/reservation-rates/{bookingId}
     */
    @GET("api/admin/reservation-rates/{bookingId}")
    suspend fun getReservationRates(@retrofit2.http.Path("bookingId") bookingId: Int): BookingRatesResponse
}



