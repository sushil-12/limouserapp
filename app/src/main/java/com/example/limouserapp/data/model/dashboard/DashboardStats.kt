package com.example.limouserapp.data.model.dashboard

/**
 * Dashboard statistics data class
 */
data class DashboardStats(
    val totalRides: Int = 0,
    val totalSpent: Double = 0.0,
    val averageRating: Double = 0.0,
    val favoriteVehicleType: String? = null,
    val memberSince: String? = null
)
