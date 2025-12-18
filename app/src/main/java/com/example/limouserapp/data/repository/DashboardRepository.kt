package com.example.limouserapp.data.repository

import com.example.limouserapp.data.api.DashboardApi
import com.example.limouserapp.data.local.UserStateManager
import com.example.limouserapp.data.model.ApiResponse
import com.example.limouserapp.data.model.dashboard.*
import com.example.limouserapp.data.model.notification.AuditRecordData
import com.example.limouserapp.data.storage.TokenManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for dashboard data management
 * Handles data fetching, caching, and business logic
 */
@Singleton
class DashboardRepository @Inject constructor(
    private val dashboardApi: DashboardApi,
    private val tokenManager: TokenManager,
    private val userStateManager: UserStateManager
) {
    
    /**
     * Get user bookings with optional filtering
     */
    suspend fun getUserBookings(
        status: BookingStatus? = null,
        limit: Int = 20,
        offset: Int = 0
    ): Result<List<UserBooking>> {
        return try {
            val response = dashboardApi.getUserBookings(
                status = status?.value,
                limit = limit,
                offset = offset
            )
            Result.success(response.data.data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get user profile information with local caching
     * Returns cached data if available, otherwise fetches from API and caches the result
     */
    suspend fun getUserProfile(): Result<UserProfile> {
        // First try to get cached profile data
        val cachedProfile = userStateManager.getCachedUserProfile()
        if (cachedProfile != null) {
            return Result.success(cachedProfile)
        }

        // No cached data, fetch from API
        return try {
            val response = dashboardApi.getUserProfile()
            val profile = response.data ?: throw Exception("Profile data not available")

            // Cache the profile data for future use
            userStateManager.saveUserProfile(profile)

            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refresh user profile from API and update cache
     * Use this when profile data needs to be updated (e.g., after account settings changes)
     */
    suspend fun refreshUserProfile(): Result<UserProfile> {
        return try {
            val response = dashboardApi.getUserProfile()
            val profile = response.data ?: throw Exception("Profile data not available")

            // Update cache with fresh data
            userStateManager.saveUserProfile(profile)

            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get upcoming bookings
     */
    suspend fun getUpcomingBookings(): Result<List<UserBooking>> {
        // Get all bookings without status filter to show recent bookings
        return getUserBookings(status = null, limit = 10)
    }
    
    /**
     * Get booking history
     */
    suspend fun getBookingHistory(
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<UserBooking>> {
        return getUserBookings(
            status = BookingStatus.COMPLETED,
            limit = limit,
            offset = offset
        )
    }
    
    /**
     * Get active booking (in progress)
     */
    suspend fun getActiveBooking(): Result<UserBooking?> {
        return try {
            val result = getUserBookings(status = BookingStatus.IN_PROGRESS, limit = 1)
            result.map { bookings -> bookings.firstOrNull() }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Cancel a booking
     */
    suspend fun cancelBooking(bookingId: Int): Result<Boolean> {
        return try {
            val request = com.example.limouserapp.data.api.CancelBookingRequest(bookingId)
            val response = dashboardApi.cancelBooking(request)
            Result.success(response.success)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get car locations for map display
     */
    suspend fun getCarLocations(
        userId: String
    ): Result<List<CarLocation>> {
        return try {
            val response = dashboardApi.getCarLocations(
                userId = userId
            )
            Result.success(response.data ?: emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get active ride for user
     */
    suspend fun getActiveRide(userId: String): Result<com.example.limouserapp.data.api.ActiveRideApiResponse?> {
        return try {
            val response = dashboardApi.getActiveRide(userId)
            if (response.isSuccessful) {
                response.body()?.let { 
                    Result.success(it)
                } ?: Result.success(null)
            } else {
                // No active ride found (this is normal)
                Result.success(null)
            }
        } catch (e: com.google.gson.JsonSyntaxException) {
            // API returns string "Account not found" when no active ride exists
            // This is expected behavior, treat as no active ride
            android.util.Log.d("DashboardRepository", "No active ride found (API returned string response)")
            Result.success(null)
        } catch (e: Exception) {
            // Handle other errors (network issues, etc.)
            android.util.Log.e("DashboardRepository", "Error fetching active ride", e)
            Result.failure(e)
        }
    }
    
    /**
     * Search bookings by query
     */
    suspend fun searchBookings(
        query: String,
        filters: SearchFilters? = null
    ): Result<List<UserBooking>> {
        return try {
            val response = dashboardApi.searchBookings(
                search = query,
                status = filters?.status?.value,
                vehicleType = filters?.vehicleType,
                paymentMethod = filters?.paymentMethod
            )
            Result.success(response.data.data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get dashboard statistics
     */
    suspend fun getDashboardStats(): Result<DashboardStats> {
        return try {
            val response = dashboardApi.getDashboardStats()
            Result.success(response.data ?: DashboardStats())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Update user profile
     */
    suspend fun updateUserProfile(profile: UserProfile): Result<UserProfile> {
        return try {
            val response = dashboardApi.updateUserProfile(profile)
            Result.success(response.data ?: profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get user profile as Flow for reactive updates
     */
    fun getUserProfileFlow(): Flow<UserProfile> = flow {
        val result = getUserProfile()
        result.fold(
            onSuccess = { profile -> emit(profile) },
            onFailure = { /* Handle error */ }
        )
    }
    
    /**
     * Get user bookings as Flow for reactive updates
     */
    fun getUserBookingsFlow(
        status: BookingStatus? = null
    ): Flow<List<UserBooking>> = flow {
        val result = getUserBookings(status = status)
        result.fold(
            onSuccess = { bookings -> emit(bookings) },
            onFailure = { /* Handle error */ }
        )
    }
    
    /**
     * Get booking audit records (notifications)
     */
    suspend fun getBookingAuditRecords(
        page: Int = 1,
        perPage: Int = 20,
        from: String? = null,
        to: String? = null,
        eventType: String? = null,
        eventCategory: String? = null,
        search: String? = null,
        bookingId: Int? = null
    ): Result<AuditRecordData> {
        return try {
            val response = dashboardApi.getBookingAuditRecords(
                page = page,
                perPage = perPage,
                from = from,
                to = to,
                eventType = eventType,
                eventCategory = eventCategory,
                search = search,
                bookingId = bookingId
            )
            Result.success(response.data ?: throw Exception("Audit records data not available"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

