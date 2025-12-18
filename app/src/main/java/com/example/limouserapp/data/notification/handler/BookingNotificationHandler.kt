package com.example.limouserapp.data.notification.handler

import com.example.limouserapp.data.notification.interfaces.NotificationDisplayManager
import com.example.limouserapp.data.notification.interfaces.NotificationHandler
import com.example.limouserapp.data.notification.model.NotificationPayload
import com.example.limouserapp.data.notification.model.NotificationType
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handler for booking-related notifications
 */
@Singleton
class BookingNotificationHandler @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val displayManager: NotificationDisplayManager
) : NotificationHandler {
    
    override fun canHandle(notificationType: NotificationType): Boolean {
        return notificationType == NotificationType.BOOKING_UPDATE ||
               notificationType == NotificationType.DRIVER_ARRIVED ||
               notificationType == NotificationType.RIDE_COMPLETED ||
               notificationType == NotificationType.RIDE_CANCELLED
    }
    
    override suspend fun handle(payload: NotificationPayload): Boolean {
        return try {
            Timber.d("Handling booking notification: ${payload.type}")
            
            val notificationId = displayManager.getNotificationId(payload)
            displayManager.displayNotification(context, payload, notificationId)
            
            Timber.d("Booking notification handled successfully")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error handling booking notification")
            false
        }
    }
    
    override fun getPriority(): Int = 8
}

