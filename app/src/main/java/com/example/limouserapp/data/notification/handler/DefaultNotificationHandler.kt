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
 * Default handler for notifications that don't match specific handlers
 * Acts as fallback handler
 */
@Singleton
class DefaultNotificationHandler @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val displayManager: NotificationDisplayManager
) : NotificationHandler {
    
    override fun canHandle(notificationType: NotificationType): Boolean {
        // Handles all notification types as fallback
        return true
    }
    
    override suspend fun handle(payload: NotificationPayload): Boolean {
        return try {
            Timber.d("Handling default notification: ${payload.type}")
            
            val notificationId = displayManager.getNotificationId(payload)
            displayManager.displayNotification(context, payload, notificationId)
            
            Timber.d("Default notification handled successfully")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error handling default notification")
            false
        }
    }
    
    override fun getPriority(): Int = 0 // Lowest priority - only used as fallback
}

