package com.example.limouserapp.domain.usecase.dashboard

import com.example.limouserapp.data.model.dashboard.UserProfile
import com.example.limouserapp.data.repository.DashboardRepository
import javax.inject.Inject

/**
 * Use case for getting user profile with local caching
 * Returns cached data if available, otherwise fetches from API
 */
class GetUserProfileUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository
) {

    /**
     * Execute the use case to get user profile
     * Uses local cache when available to avoid unnecessary API calls
     */
    suspend operator fun invoke(): Result<UserProfile> {
        return dashboardRepository.getUserProfile()
    }
}

/**
 * Use case for refreshing user profile from API
 * Forces a fresh API call and updates the local cache
 */
class RefreshUserProfileUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository
) {

    /**
     * Execute the use case to refresh user profile from API
     * Updates local cache with fresh data
     */
    suspend operator fun invoke(): Result<UserProfile> {
        return dashboardRepository.refreshUserProfile()
    }
}
