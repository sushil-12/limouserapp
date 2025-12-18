package com.example.limouserapp.data.notification.handler

import com.example.limouserapp.data.notification.interfaces.NotificationHandler
import com.example.limouserapp.data.notification.model.NotificationPayload
import com.example.limouserapp.data.notification.model.NotificationType
import kotlin.jvm.JvmSuppressWildcards
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for coordinating notification handlers
 * Uses Chain of Responsibility pattern to route notifications to appropriate handlers
 */
@Singleton
class NotificationHandlerManager @Inject constructor(
    @JvmSuppressWildcards private val handlers: Set<NotificationHandler>
) {
    
    /**
     * Process notification using the appropriate handler
     */
    suspend fun processNotification(payload: NotificationPayload): Boolean {
        val handler = findHandler(payload.type)
        return if (handler != null) {
            Timber.d("Processing notification ${payload.type} with handler ${handler::class.simpleName}")
            handler.handle(payload)
        } else {
            Timber.e("No handler found for notification type: ${payload.type}")
            false
        }
    }
    
    /**
     * Find the appropriate handler for a notification type
     * Prioritizes handlers by their priority value (higher first)
     */
    private fun findHandler(type: NotificationType): NotificationHandler? {
        return handlers
            .filter { it.canHandle(type) }
            .sortedByDescending { it.getPriority() }
            .firstOrNull()
    }
}

