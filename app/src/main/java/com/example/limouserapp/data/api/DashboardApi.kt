package com.example.limouserapp.data.api

import com.example.limouserapp.data.model.ApiResponse
import com.example.limouserapp.data.model.BaseResponse
import com.example.limouserapp.data.model.dashboard.*
import com.example.limouserapp.data.model.notification.AuditRecordData
import retrofit2.http.*

/**
 * API interface for dashboard-related endpoints
 */
interface DashboardApi {
    
    /**
     * Get user bookings with optional filtering
     */
    @GET("api/individual/get-all-reservation")
    suspend fun getUserBookings(
        @Query("status") status: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): PaginatedResponse<UserBooking>
    
    /**
     * Get user profile information
     */
    @GET("api/mobile/v1/user/registration/profile")
    suspend fun getUserProfile(): ApiResponse<UserProfile>
    
    /**
     * Get profile data with cards (matches iOS endpoint)
     */
    @GET("api/individual/get-profile-data")
    suspend fun getProfileData(): com.example.limouserapp.data.model.dashboard.ProfileDataResponse
    
    /**
     * Add credit card
     */
    @POST("api/individual/add-credit-card")
    suspend fun addCreditCard(
        @Body request: com.example.limouserapp.data.model.dashboard.AddCreditCardRequest
    ): com.example.limouserapp.data.model.dashboard.AddCreditCardResponse
    
    /**
     * Update user profile
     */
    @PUT("api/mobile/v1/user/registration/profile")
    suspend fun updateUserProfile(
        @Body profile: UserProfile
    ): ApiResponse<UserProfile>
    
    /**
     * Update profile data (Account Settings endpoint)
     * Uses same endpoint as getProfileData but with PUT method
     */
    @PUT("api/individual/get-profile-data")
    suspend fun updateProfileData(
        @Body request: com.example.limouserapp.data.model.dashboard.UpdateProfileRequest
    ): ProfileDataResponse
    
    /**
     * Cancel a booking
     */
    @POST("api/individual/cancel-reservation")
    suspend fun cancelBooking(
        @Body request: CancelBookingRequest
    ): ApiResponse<Boolean>
    
    /**
     * Get car locations for map display
     */
    @GET("api/mobile/v1/user/active-ride")
    suspend fun getCarLocations(
        @Query("userId") userId: String
    ): ApiResponse<List<CarLocation>>
    
    /**
     * Get active ride for current user
     */
    @GET("api/mobile/v1/user/active-ride")
    suspend fun getActiveRide(
        @Query("userId") userId: String
    ): retrofit2.Response<ActiveRideApiResponse>
    
    /**
     * Search bookings - matches iOS getAllReservations endpoint
     */
    @GET("api/individual/get-all-reservation")
    suspend fun searchBookings(
        @Query("search") search: String = "",
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("useDateFilter") useDateFilter: String? = null,
        @Query("current_date") currentDate: String? = null,
        @Query("current_time") currentTime: String? = null,
        @Query("page") page: Int = 1,
        @Query("status") status: String? = null,
        @Query("vehicle_type") vehicleType: String? = null,
        @Query("payment_method") paymentMethod: String? = null
    ): PaginatedResponse<UserBooking>
    
    /**
     * Get dashboard statistics
     */
    @GET("dashboard/stats")
    suspend fun getDashboardStats(): ApiResponse<DashboardStats>
    
    /**
     * Get upcoming bookings
     */
    @GET("bookings/upcoming")
    suspend fun getUpcomingBookings(
        @Query("limit") limit: Int = 10
    ): ApiResponse<List<UserBooking>>
    
    /**
     * Get booking history
     */
    @GET("bookings/history")
    suspend fun getBookingHistory(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): ApiResponse<List<UserBooking>>
    
    /**
     * Get active booking
     */
    @GET("bookings/active")
    suspend fun getActiveBooking(): ApiResponse<UserBooking?>
    
    /**
     * Rate a completed booking
     */
    @POST("bookings/{bookingId}/rate")
    suspend fun rateBooking(
        @Path("bookingId") bookingId: Int,
        @Body rating: BookingRating
    ): ApiResponse<Boolean>
    
    /**
     * Get booking details
     */
    @GET("bookings/{bookingId}")
    suspend fun getBookingDetails(
        @Path("bookingId") bookingId: Int
    ): ApiResponse<UserBooking>
    
    /**
     * Update booking preferences
     */
    @PUT("user/booking-preferences")
    suspend fun updateBookingPreferences(
        @Body preferences: BookingPreferences
    ): ApiResponse<Boolean>
    
    /**
     * Get invoices with optional filtering
     * Matches iOS endpoint: /api/individual/invoices
     */
    @GET("api/individual/invoices")
    suspend fun getInvoices(
        @Query("from") fromDate: String? = null,
        @Query("to") toDate: String? = null,
        @Query("search") search: String? = null,
        @Query("useDateFilter") useDateFilter: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20
    ): InvoiceResponse
    
    /**
     * Get recent locations for pickup or dropoff
     * Matches iOS endpoint: /api/mobile/v1/user/recent-locations?type={type}
     */
    @GET("api/mobile/v1/user/recent-locations")
    suspend fun getRecentLocations(
        @Query("type") type: String // "pickup" or "dropoff"
    ): com.example.limouserapp.data.model.booking.RecentLocationResponse
    
    /**
     * Get booking audit records (notifications)
     * Endpoint: /api/mobile/v1/user/bookings/audit-records
     */
    @GET("api/mobile/v1/user/bookings/audit-records")
    suspend fun getBookingAuditRecords(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("event_type") eventType: String? = null,
        @Query("event_category") eventCategory: String? = null,
        @Query("search") search: String? = null,
        @Query("booking_id") bookingId: Int? = null
    ): BaseResponse<AuditRecordData>
    
    /**
     * Get user profile
     * Endpoint: GET /api/mobile/v1/user/profile
     */
    @GET("api/mobile/v1/user/profile")
    suspend fun getUserProfileV1(): com.example.limouserapp.data.model.dashboard.UserProfileApiResponse
    
    /**
     * Update user profile
     * Endpoint: PUT /api/mobile/v1/user/profile
     */
    @PUT("api/mobile/v1/user/profile")
    suspend fun updateUserProfileV1(
        @Body request: com.example.limouserapp.data.model.dashboard.UpdateUserProfileRequest
    ): com.example.limouserapp.data.model.dashboard.UserProfileApiResponse

    /**
     * Get user FAQ
     * Endpoint: GET api/user-faq
     */
    @GET("api/user-faq")
    suspend fun getUserFaq(): ApiResponse<com.example.limouserapp.data.model.dashboard.FaqData>

    /**
     * Get user tutorials
     * Endpoint: GET api/tutorials?type=user_tutorials
     */
    @GET("api/tutorials")
    suspend fun getUserTutorials(
        @Query("type") type: String = "user_tutorials"
    ): ApiResponse<com.example.limouserapp.data.model.dashboard.TutorialData>
}

/**
 * Booking rating data class
 */
data class BookingRating(
    val rating: Int,
    val comment: String? = null,
    val driverRating: Int? = null,
    val vehicleRating: Int? = null
)

/**
 * Cancel booking request data class
 */
data class CancelBookingRequest(
    val bookingId: Int,
    val reason: String? = null
)

/**
 * Booking preferences data class
 */
data class BookingPreferences(
    val preferredVehicleType: String? = null,
    val preferredPaymentMethod: String? = null,
    val defaultPickupLocation: String? = null,
    val notificationPreferences: NotificationPreferences? = null
)

/**
 * Active ride API response
 */
data class ActiveRideApiResponse(
    val success: Boolean,
    val message: String?,
    val data: ActiveRideDetails?,
    val timestamp: String?,
    val code: Int?
)

data class ActiveRideDetails(
    val booking_id: Int,
    val status: String,
    val status_display: String?,
    val customer: CustomerInfo?,
    val driver: DriverInfo,
    val vehicle: VehicleInfo?,
    val locations: LocationData?,
    val trip_details: TripDetails?,
    val special_requirements: SpecialRequirements?,
    val amenities: List<String>?,
    val instructions: String?,
    val timestamps: TimestampInfo?
)

data class CustomerInfo(
    val id: Int,
    val name: String,
    val email: String?,
    val phone: String?
)

data class DriverInfo(
    val id: Int,
    val name: String,
    val phone: String,
    val rating: Int?
)

data class VehicleInfo(
    val id: Int,
    val type: String,
    val model: String?,
    val color: String?
)

data class LocationData(
    val pickup: LocationDetails,
    val dropoff: LocationDetails
)

data class LocationDetails(
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val datetime: String?
)

data class TripDetails(
    val passengers: Int?,
    val luggage: Int?,
    val distance: String?,
    val duration: String?,
    val estimated_fare: Double?,
    val actual_fare: Double?,
    val payment_status: String?,
    val payment_method: String?
)

data class SpecialRequirements(
    val child_certified: Boolean?,
    val baby_seat: Boolean?,
    val booster_seat: Boolean?,
    val pet_friendly: Boolean?,
    val handicap: Boolean?
)

data class TimestampInfo(
    val created_at: String,
    val updated_at: String,
    val pickup_time: String?
)
