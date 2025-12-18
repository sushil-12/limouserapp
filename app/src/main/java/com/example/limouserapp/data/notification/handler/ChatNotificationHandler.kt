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
 * Handler for chat message notifications
 */
@Singleton
class ChatNotificationHandler @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val displayManager: NotificationDisplayManager
) : NotificationHandler {
    
    override fun canHandle(notificationType: NotificationType): Boolean {
        return notificationType == NotificationType.CHAT_MESSAGE
    }
    
    override suspend fun handle(payload: NotificationPayload): Boolean {
        return try {
            Timber.d("Handling chat notification: ${payload.type}, bookingId: ${payload.bookingId}")

            val notificationId = displayManager.getNotificationId(payload)
            val displayResult = displayManager.displayNotification(context, payload, notificationId)

            Timber.d("Chat notification display result: $displayResult")
            displayResult
        } catch (e: Exception) {
            Timber.e(e, "Error handling chat notification")
            false
        }
    }
    
    override fun getPriority(): Int = 7
}

