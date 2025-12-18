package com.example.limouserapp.data.notification.interfaces

import android.app.Notification
import android.content.Context
import com.example.limouserapp.data.notification.model.NotificationPayload

/**
 * Interface for displaying notifications
 * Abstraction layer for notification display logic
 * Follows SOLID - Dependency Inversion Principle
 */
interface NotificationDisplayManager {
    /**
     * Display a notification based on the payload
     */
    suspend fun displayNotification(
        context: Context,
        payload: NotificationPayload,
        notificationId: Int
    ): Boolean
    
    /**
     * Get the notification ID for a given payload
     */
    fun getNotificationId(payload: NotificationPayload): Int
    
    /**
     * Cancel a notification by ID
     */
    fun cancelNotification(notificationId: Int)
    
    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications()
}

