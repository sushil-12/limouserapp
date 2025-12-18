package com.example.limouserapp.data.notification.display

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.example.limouserapp.R
import com.example.limouserapp.data.notification.interfaces.NotificationDisplayManager
import com.example.limouserapp.data.notification.interfaces.NotificationIntentFactory
import com.example.limouserapp.data.notification.model.NotificationChannels
import com.example.limouserapp.data.notification.model.NotificationPayload
import com.example.limouserapp.data.notification.model.NotificationPriority
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of NotificationDisplayManager
 * Handles actual notification display logic
 */
@Singleton
class NotificationDisplayManagerImpl @Inject constructor(
    private val notificationManager: NotificationManager,
    private val intentFactory: NotificationIntentFactory
) : NotificationDisplayManager {

    // Track if user is currently viewing chat to suppress notifications
    private var currentChatBookingId: Int? = null

    fun setCurrentChatBookingId(bookingId: Int?) {
        currentChatBookingId = bookingId
    }
    
    override suspend fun displayNotification(
        context: Context,
        payload: NotificationPayload,
        notificationId: Int
    ): Boolean {
        // Suppress chat message notifications if user is currently viewing that chat
        if (payload.type == com.example.limouserapp.data.notification.model.NotificationType.CHAT_MESSAGE) {
            Timber.d("Chat notification detected - type: ${payload.type}, bookingId: ${payload.bookingId}, currentChatBookingId: $currentChatBookingId")
            if (currentChatBookingId.toString() == payload.bookingId) {
                Timber.d("Suppressing chat notification - user is viewing chat for booking ${payload.bookingId}")
                return true // Return true to indicate "handled" but don't show notification
            }
        }

        return try {
            val channelId = payload.channelId ?: getChannelIdForPayload(payload)
            val pendingIntent = intentFactory.createIntent(context, payload, notificationId)
            
            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(payload.title)
                .setContentText(payload.body)
                .setPriority(getPriority(payload.priority))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(getCategoryForType(payload.type))
            
            // Add specific features based on priority
            when (payload.priority) {
                NotificationPriority.MAX -> {
                    builder.setFullScreenIntent(pendingIntent, true)
                        .setVibrate(getVibrationPattern(payload.type))
                    if (payload.type == com.example.limouserapp.data.notification.model.NotificationType.EMERGENCY) {
                        builder.setLights(0xFF0000, 1000, 1000)
                    }
                }
                NotificationPriority.HIGH -> {
                    builder.setVibrate(getVibrationPattern(payload.type))
                }
                else -> {
                    // Default behavior
                }
            }
            
            // Add image if available
            payload.imageUrl?.let { imageUrl ->
                // TODO: Load and set large icon or big picture style
                // This would require image loading library integration
            }
            
            val notification = builder.build()
            notificationManager.notify(notificationId, notification)
            
            Timber.d("Notification displayed: type=${payload.type}, id=$notificationId")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error displaying notification")
            false
        }
    }
    
    override fun getNotificationId(payload: NotificationPayload): Int {
        // Generate unique ID based on booking ID if available, otherwise use hash of type+title
        return payload.bookingId?.hashCode()?.let {
            // Ensure positive ID
            if (it < 0) (-it).coerceAtMost(Int.MAX_VALUE - 1000) + 1000
            else it.coerceAtMost(Int.MAX_VALUE - 1000)
        } ?: run {
            // Fallback to hash of type + title
            (payload.type.name.hashCode() + payload.title.hashCode())
                .let { if (it < 0) (-it).coerceAtMost(Int.MAX_VALUE - 2000) + 2000 else it }
        }
    }
    
    override fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
        Timber.d("Notification cancelled: id=$notificationId")
    }
    
    override fun cancelAllNotifications() {
        notificationManager.cancelAll()
        Timber.d("All notifications cancelled")
    }
    
    private fun getChannelIdForPayload(payload: NotificationPayload): String {
        return when (payload.type) {
            com.example.limouserapp.data.notification.model.NotificationType.CHAT_MESSAGE -> NotificationChannels.CHAT
            com.example.limouserapp.data.notification.model.NotificationType.BOOKING_UPDATE,
            com.example.limouserapp.data.notification.model.NotificationType.DRIVER_ARRIVED,
            com.example.limouserapp.data.notification.model.NotificationType.RIDE_COMPLETED,
            com.example.limouserapp.data.notification.model.NotificationType.RIDE_CANCELLED -> NotificationChannels.BOOKING
            com.example.limouserapp.data.notification.model.NotificationType.EMERGENCY -> NotificationChannels.EMERGENCY
            com.example.limouserapp.data.notification.model.NotificationType.LIVE_RIDE,
            com.example.limouserapp.data.notification.model.NotificationType.LIVE_RIDE_DO -> NotificationChannels.HIGH_PRIORITY
            else -> NotificationChannels.DEFAULT
        }
    }
    
    private fun getPriority(priority: NotificationPriority): Int {
        return when (priority) {
            NotificationPriority.LOW -> NotificationCompat.PRIORITY_LOW
            NotificationPriority.DEFAULT -> NotificationCompat.PRIORITY_DEFAULT
            NotificationPriority.HIGH -> NotificationCompat.PRIORITY_HIGH
            NotificationPriority.MAX -> NotificationCompat.PRIORITY_MAX
        }
    }
    
    private fun getCategoryForType(type: com.example.limouserapp.data.notification.model.NotificationType): String {
        return when (type) {
            com.example.limouserapp.data.notification.model.NotificationType.CHAT_MESSAGE -> NotificationCompat.CATEGORY_MESSAGE
            com.example.limouserapp.data.notification.model.NotificationType.DRIVER_ARRIVED,
            com.example.limouserapp.data.notification.model.NotificationType.EMERGENCY -> NotificationCompat.CATEGORY_ALARM
            com.example.limouserapp.data.notification.model.NotificationType.PROMOTION -> NotificationCompat.CATEGORY_PROMO
            com.example.limouserapp.data.notification.model.NotificationType.BOOKING_UPDATE,
            com.example.limouserapp.data.notification.model.NotificationType.RIDE_COMPLETED,
            com.example.limouserapp.data.notification.model.NotificationType.RIDE_CANCELLED -> NotificationCompat.CATEGORY_STATUS
            else -> NotificationCompat.CATEGORY_STATUS
        }
    }
    
    private fun getVibrationPattern(type: com.example.limouserapp.data.notification.model.NotificationType): LongArray? {
        return when (type) {
            com.example.limouserapp.data.notification.model.NotificationType.DRIVER_ARRIVED -> longArrayOf(0, 1000, 500, 1000)
            com.example.limouserapp.data.notification.model.NotificationType.EMERGENCY -> longArrayOf(0, 1000, 500, 1000, 500, 1000)
            else -> null
        }
    }
}

