package com.example.limouserapp.domain.usecase.dashboard

import com.example.limouserapp.data.api.ActiveRideApiResponse
import com.example.limouserapp.data.model.dashboard.BookingStatus
import com.example.limouserapp.data.model.dashboard.UserBooking
import com.example.limouserapp.data.repository.DashboardRepository
import javax.inject.Inject

/**
 * Use case for getting user bookings
 * Encapsulates business logic for fetching and processing user bookings
 */
class GetUserBookingsUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository
) {
    
    /**
     * Execute the use case to get user bookings
     */
    suspend operator fun invoke(
        status: BookingStatus? = null,
        limit: Int = 20,
        offset: Int = 0
    ): Result<List<UserBooking>> {
        return dashboardRepository.getUserBookings(
            status = status,
            limit = limit,
            offset = offset
        )
    }
    
    /**
     * Get active ride for user
     */
    suspend fun getActiveRide(userId: String): Result<ActiveRideApiResponse?> {
        return dashboardRepository.getActiveRide(userId)
    }
}
