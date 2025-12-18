package com.example.limouserapp.data.notification.converter

import com.example.limouserapp.data.notification.model.NotificationActionType
import com.example.limouserapp.data.notification.model.NotificationPayload
import com.example.limouserapp.data.notification.model.NotificationPriority
import com.example.limouserapp.data.notification.model.NotificationType
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber

/**
 * Converter for transforming RemoteMessage to NotificationPayload
 * Handles both data-only and notification messages
 */
object RemoteMessageConverter {
    
    /**
     * Convert RemoteMessage to NotificationPayload
     */
    fun convert(remoteMessage: RemoteMessage): NotificationPayload? {
        return try {
            // Priority from data or default
            val priority = remoteMessage.data["priority"]?.let {
                try {
                    NotificationPriority.valueOf(it.uppercase())
                } catch (e: Exception) {
                    NotificationPriority.DEFAULT
                }
            } ?: NotificationPriority.DEFAULT
            
            // Type from data or try to infer from notification
            val type = remoteMessage.data["type"]?.let { typeStr ->
                NotificationType.fromString(typeStr)
            } ?: inferTypeFromNotification(remoteMessage.notification)
            
            // Title and body
            val title = remoteMessage.data["title"]
                ?: remoteMessage.notification?.title
                ?: "1800Limo"
            val body = remoteMessage.data["body"]
                ?: remoteMessage.notification?.body
                ?: ""
            
            // Extract additional data
            val bookingId = remoteMessage.data["booking_id"]
                ?: remoteMessage.data["bookingId"]
            val driverId = remoteMessage.data["driver_id"]
                ?: remoteMessage.data["driverId"]
            
            // Action type
            val actionType = remoteMessage.data["action_type"]?.let {
                try {
                    NotificationActionType.valueOf(it)
                } catch (e: Exception) {
                    null
                }
            }
            
            // Deep link
            val deepLink = remoteMessage.data["deep_link"]
                ?: remoteMessage.data["deepLink"]
            
            NotificationPayload(
                type = type,
                title = title,
                body = body,
                bookingId = bookingId,
                driverId = driverId,
                data = remoteMessage.data,
                priority = priority,
                channelId = remoteMessage.data["channel_id"],
                sound = remoteMessage.notification?.sound?.toString(),
                imageUrl = remoteMessage.notification?.imageUrl?.toString(),
                actionType = actionType,
                deepLink = deepLink
            )
        } catch (e: Exception) {
            Timber.e(e, "Error converting RemoteMessage to NotificationPayload")
            null
        }
    }
    
    /**
     * Infer notification type from notification title/body if type not in data
     */
    private fun inferTypeFromNotification(notification: RemoteMessage.Notification?): NotificationType {
        if (notification == null) return NotificationType.UNKNOWN
        
        val title = notification.title?.lowercase() ?: ""
        val body = notification.body?.lowercase() ?: ""
        
        return when {
            title.contains("live ride") || body.contains("live ride") -> NotificationType.LIVE_RIDE
            title.contains("driver") && body.contains("arrived") -> NotificationType.DRIVER_ARRIVED
            title.contains("chat") || body.contains("message") -> NotificationType.CHAT_MESSAGE
            title.contains("emergency") || title.contains("urgent") -> NotificationType.EMERGENCY
            title.contains("booking") -> NotificationType.BOOKING_UPDATE
            title.contains("payment") -> NotificationType.PAYMENT_SUCCESS
            else -> NotificationType.UNKNOWN
        }
    }
}

