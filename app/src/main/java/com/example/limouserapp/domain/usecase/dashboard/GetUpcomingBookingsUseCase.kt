package com.example.limouserapp.domain.usecase.dashboard

import com.example.limouserapp.data.model.dashboard.UserBooking
import com.example.limouserapp.data.repository.DashboardRepository
import javax.inject.Inject

/**
 * Use case for getting upcoming bookings
 * Encapsulates business logic for fetching upcoming user bookings
 */
class GetUpcomingBookingsUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository
) {
    
    /**
     * Execute the use case to get upcoming bookings
     */
    suspend operator fun invoke(): Result<List<UserBooking>> {
        return dashboardRepository.getUpcomingBookings()
    }
}
