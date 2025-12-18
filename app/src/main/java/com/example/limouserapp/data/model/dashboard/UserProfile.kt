package com.example.limouserapp.data.model.dashboard

import com.google.gson.annotations.SerializedName

/**
 * Represents user profile data for dashboard display
 */
data class UserProfile(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("first_name")
    val firstName: String,
    
    @SerializedName("last_name")
    val lastName: String,
    
    @SerializedName("email")
    val email: String? = null,
    
    @SerializedName("mobile")
    val phone: String,

    @SerializedName("mobileCountry")
    val phoneCountry: String? = null,
    
    @SerializedName("mobileIsd")
    val phoneCountryCode: String? = null,
    
    @SerializedName("profile_image_url")
    val profileImageUrl: String? = null,
    
    @SerializedName("rating")
    val rating: Double = 0.0,
    
    @SerializedName("total_rides")
    val totalRides: Int = 0,
    
    @SerializedName("member_since")
    val memberSince: String? = null,
    
    @SerializedName("is_verified")
    val isVerified: Boolean = false,
    
    @SerializedName("preferred_payment_method")
    val preferredPaymentMethod: String? = null,
    
    @SerializedName("notification_preferences")
    val notificationPreferences: NotificationPreferences? = null
) {
    /**
     * Get full name
     */
    val fullName: String
        get() = "$firstName $lastName"
    
    /**
     * Get formatted phone number
     */
    val formattedPhone: String
        get() = when {
            !phoneCountryCode.isNullOrEmpty() -> "$phoneCountryCode $phone"
            else -> phone
        }
    
    /**
     * Get display rating
     */
    val displayRating: String
        get() = String.format("%.1f", rating)
    
    /**
     * Check if user has profile image
     */
    val hasProfileImage: Boolean
        get() = !profileImageUrl.isNullOrEmpty()
}

/**
 * Notification preferences for user
 */
data class NotificationPreferences(
    @SerializedName("push_notifications")
    val pushNotifications: Boolean = true,
    
    @SerializedName("email_notifications")
    val emailNotifications: Boolean = true,
    
    @SerializedName("sms_notifications")
    val smsNotifications: Boolean = false,
    
    @SerializedName("ride_updates")
    val rideUpdates: Boolean = true,
    
    @SerializedName("promotional_offers")
    val promotionalOffers: Boolean = false,
    
    @SerializedName("driver_messages")
    val driverMessages: Boolean = true
)

/**
 * Dashboard menu item data
 */
data class DashboardMenuItem(
    val id: String,
    val title: String,
    val icon: String, // Resource name for icon
    val destination: String? = null,
    val badge: String? = null,
    val isEnabled: Boolean = true,
    val action: MenuAction? = null
)

/**
 * Menu action types
 */
enum class MenuAction {
    NAVIGATE,
    LOGOUT,
    SHOW_DIALOG,
    OPEN_URL
}

/**
 * Connection status for real-time services
 */
data class ConnectionStatus(
    val isConnected: Boolean,
    val lastConnected: Long? = null,
    val connectionAttempts: Int = 0,
    val isReconnecting: Boolean = false,
    val error: String? = null
) {
    val statusText: String
        get() = when {
            isConnected -> "Connected"
            isReconnecting -> "Reconnecting..."
            connectionAttempts > 0 -> "Connection Failed"
            else -> "Disconnected"
        }
}

/**
 * Bottom sheet state
 */
data class BottomSheetState(
    val isExpanded: Boolean = false,
    val isDragging: Boolean = false,
    val currentHeight: Float = 0f,
    val targetHeight: Float = 0f
)

/**
 * Search state for dashboard
 */
data class SearchState(
    val query: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<UserBooking> = emptyList(),
    val searchFilters: SearchFilters = SearchFilters()
)

/**
 * Search filters for bookings
 */
data class SearchFilters(
    val status: BookingStatus? = null,
    val dateRange: DateRange? = null,
    val vehicleType: String? = null,
    val paymentMethod: String? = null
)

/**
 * Date range for filtering
 */
data class DateRange(
    val startDate: Long,
    val endDate: Long
)
