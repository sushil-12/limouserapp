package com.example.limouserapp.data.api

import com.example.limouserapp.data.model.ApiResponse
import com.example.limouserapp.data.model.booking.CreateReservationRequest
import com.example.limouserapp.data.model.booking.CreateReservationResponse
import com.example.limouserapp.data.model.booking.DriverFeedbackRequest
import com.example.limouserapp.data.model.booking.DriverFeedbackResponse
import com.example.limouserapp.data.model.booking.EditReservationRequest
import com.example.limouserapp.data.model.booking.EditReservationResponse
import com.example.limouserapp.data.model.booking.EditReservationUpdateResponse
import com.example.limouserapp.data.model.booking.GenerateRideOTPResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface BookingApi {
    @POST("api/individual/create-reservation")
    suspend fun createReservation(
        @Body request: CreateReservationRequest
    ): CreateReservationResponse
    
    @GET("api/individual/get-reservation/{bookingId}/edit")
    suspend fun getEditReservation(
        @Path("bookingId") bookingId: Int
    ): EditReservationResponse
    
    @POST("api/individual/edit-reservation")
    suspend fun updateReservation(
        @Body request: EditReservationRequest
    ): EditReservationUpdateResponse
    
    @POST("api/mobile/v1/bookings/{bookingId}/generate-ride-otp")
    suspend fun generateRideOTP(
        @Path("bookingId") bookingId: Int
    ): ApiResponse<GenerateRideOTPResponse>
    
    @POST("api/mobile/v1/bookings/{bookingId}/driver-feedback")
    suspend fun submitDriverFeedback(
        @Path("bookingId") bookingId: Int,
        @Body request: DriverFeedbackRequest
    ): ApiResponse<DriverFeedbackResponse>
}



