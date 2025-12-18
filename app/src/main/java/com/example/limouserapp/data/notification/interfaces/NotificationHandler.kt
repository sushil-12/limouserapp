package com.example.limouserapp.data.notification.interfaces

import com.example.limouserapp.data.notification.model.NotificationPayload

/**
 * Strategy interface for handling different types of notifications
 * Follows SOLID principles - Single Responsibility and Open/Closed
 * Each handler is responsible for a specific notification type
 */
interface NotificationHandler {
    /**
     * Checks if this handler can process the given notification type
     */
    fun canHandle(notificationType: com.example.limouserapp.data.notification.model.NotificationType): Boolean
    
    /**
     * Handles the notification payload
     * @param payload The notification payload to process
     * @return True if handled successfully, false otherwise
     */
    suspend fun handle(payload: NotificationPayload): Boolean
    
    /**
     * Get the priority of this handler (higher priority handlers are checked first)
     */
    fun getPriority(): Int = 0
}

