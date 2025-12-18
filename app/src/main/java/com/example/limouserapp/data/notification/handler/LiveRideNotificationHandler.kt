package com.example.limouserapp.data.notification.handler

import com.example.limouserapp.data.notification.interfaces.NotificationDisplayManager
import com.example.limouserapp.data.notification.interfaces.NotificationHandler
import com.example.limouserapp.data.notification.model.NotificationPayload
import com.example.limouserapp.data.notification.model.NotificationType
import com.example.limouserapp.data.socket.SocketService
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handler for live ride notifications
 * Coordinates with SocketService for real-time updates
 */
@Singleton
class LiveRideNotificationHandler @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val displayManager: NotificationDisplayManager,
    private val socketService: SocketService
) : NotificationHandler {
    
    override fun canHandle(notificationType: NotificationType): Boolean {
        return notificationType == NotificationType.LIVE_RIDE || 
               notificationType == NotificationType.LIVE_RIDE_DO
    }
    
    override suspend fun handle(payload: NotificationPayload): Boolean {
        return try {
            Timber.d("Handling live ride notification: ${payload.type}")
            
            // The SocketService already handles live_ride notifications
            // This handler is mainly for displaying the notification
            // and ensuring socket connection is active
            if (!socketService.isConnected()) {
                Timber.d("Socket not connected, attempting to connect...")
                socketService.connect()
            }
            
            // Display notification
            val notificationId = displayManager.getNotificationId(payload)
            displayManager.displayNotification(context, payload, notificationId)
            
            Timber.d("Live ride notification handled successfully")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error handling live ride notification")
            false
        }
    }
    
    override fun getPriority(): Int = 10 // High priority for live ride notifications
}

