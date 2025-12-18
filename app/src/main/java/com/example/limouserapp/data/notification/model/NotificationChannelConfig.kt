package com.example.limouserapp.data.notification.model

import android.app.NotificationChannel
import android.app.NotificationManager

/**
 * Configuration for notification channels
 * Provides a structured way to define channel properties
 */
data class NotificationChannelConfig(
    val id: String,
    val name: String,
    val description: String,
    val importance: Int,
    val enableLights: Boolean = true,
    val enableVibration: Boolean = true,
    val showBadge: Boolean = true,
    val sound: android.net.Uri? = null,
    val vibrationPattern: LongArray? = null
) {
    fun toNotificationChannel(): NotificationChannel {
        return NotificationChannel(id, name, importance).apply {
            this.description = this@NotificationChannelConfig.description
            enableLights(this@NotificationChannelConfig.enableLights)
            enableVibration(this@NotificationChannelConfig.enableVibration)
            setShowBadge(this@NotificationChannelConfig.showBadge)
            sound?.let { setSound(it, null) }
            vibrationPattern?.let { setVibrationPattern(it) }
        }
    }
}

/**
 * Predefined notification channels for the app
 */
object NotificationChannels {
    const val DEFAULT = "limo_notifications"
    const val HIGH_PRIORITY = "limo_high_priority"
    const val CHAT = "limo_chat"
    const val BOOKING = "limo_booking"
    const val EMERGENCY = "limo_emergency"
    
    fun getAllChannels(): List<NotificationChannelConfig> {
        return listOf(
            NotificationChannelConfig(
                id = DEFAULT,
                name = "1800Limo Notifications",
                description = "General notifications for ride updates and alerts",
                importance = NotificationManager.IMPORTANCE_HIGH
            ),
            NotificationChannelConfig(
                id = HIGH_PRIORITY,
                name = "1800Limo High Priority",
                description = "Important notifications requiring immediate attention",
                importance = NotificationManager.IMPORTANCE_MAX
            ),
            NotificationChannelConfig(
                id = CHAT,
                name = "1800Limo Chat",
                description = "Notifications for chat messages",
                importance = NotificationManager.IMPORTANCE_DEFAULT
            ),
            NotificationChannelConfig(
                id = BOOKING,
                name = "1800Limo Bookings",
                description = "Notifications for booking updates",
                importance = NotificationManager.IMPORTANCE_HIGH
            ),
            NotificationChannelConfig(
                id = EMERGENCY,
                name = "1800Limo Emergency",
                description = "Emergency and safety notifications",
                importance = NotificationManager.IMPORTANCE_MAX,
                vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
            )
        )
    }
}

