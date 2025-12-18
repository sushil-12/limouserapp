package com.example.limouserapp.data.notification.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Domain model for FCM notification payload
 * Represents the structured data received from Firebase Cloud Messaging
 */
@Parcelize
data class NotificationPayload(
    val type: NotificationType,
    val title: String,
    val body: String,
    val bookingId: String? = null,
    val driverId: String? = null,
    val data: Map<String, String> = emptyMap(),
    val priority: NotificationPriority = NotificationPriority.DEFAULT,
    val channelId: String? = null,
    val sound: String? = null,
    val imageUrl: String? = null,
    val actionType: NotificationActionType? = null,
    val deepLink: String? = null
) : Parcelable

/**
 * Notification types supported by the app
 */
enum class NotificationType(val value: String) {
    BOOKING_UPDATE("booking_update"),
    DRIVER_ARRIVED("driver_arrived"),
    CHAT_MESSAGE("chat_message"),
    LIVE_RIDE("live_ride"),
    LIVE_RIDE_DO("live_ride_do"),
    EMERGENCY("emergency"),
    PROMOTION("promotion"),
    RIDE_COMPLETED("ride_completed"),
    RIDE_CANCELLED("ride_cancelled"),
    PAYMENT_SUCCESS("payment_success"),
    PAYMENT_FAILED("payment_failed"),
    UNKNOWN("unknown");

    companion object {
        fun fromString(value: String): NotificationType {
            return values().find { it.value == value } ?: UNKNOWN
        }
    }
}

/**
 * Notification priority levels
 */
enum class NotificationPriority {
    LOW,
    DEFAULT,
    HIGH,
    MAX
}

/**
 * Notification action types for deep linking
 */
enum class NotificationActionType {
    NAVIGATE_TO_BOOKING,
    NAVIGATE_TO_LIVE_RIDE,
    NAVIGATE_TO_CHAT,
    NAVIGATE_TO_DASHBOARD,
    OPEN_PAYMENT,
    NONE
}

