package com.example.limouserapp.data.notification.factory

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.TaskStackBuilder
import com.example.limouserapp.MainActivity
import com.example.limouserapp.data.notification.interfaces.NotificationIntentFactory
import com.example.limouserapp.data.notification.model.NotificationActionType
import com.example.limouserapp.data.notification.model.NotificationPayload
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory implementation for creating notification intents
 * Handles deep linking and navigation based on notification type
 */
@Singleton
class NotificationIntentFactoryImpl @Inject constructor() : NotificationIntentFactory {
    
    override fun createIntent(
        context: Context,
        payload: NotificationPayload,
        requestCode: Int
    ): PendingIntent {
        val intent = createNavigationIntent(context, payload)
        
        return TaskStackBuilder.create(context).run {
            // Add main activity as parent
            addNextIntentWithParentStack(Intent(context, MainActivity::class.java))
            // Add specific intent based on notification type
            addNextIntent(intent)
            getPendingIntent(
                requestCode,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } ?: run {
            // Fallback if TaskStackBuilder fails
            PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
    
    private fun createNavigationIntent(
        context: Context,
        payload: NotificationPayload
    ): Intent {
        // First check for deep link
        payload.deepLink?.let { deepLink ->
            return Intent(Intent.ACTION_VIEW, android.net.Uri.parse(deepLink)).apply {
                setPackage(context.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }
        
        // Then check action type
        payload.actionType?.let { actionType ->
            return when (actionType) {
                NotificationActionType.NAVIGATE_TO_LIVE_RIDE -> {
                    createLiveRideIntent(context, payload)
                }
                NotificationActionType.NAVIGATE_TO_BOOKING -> {
                    createBookingIntent(context, payload)
                }
                NotificationActionType.NAVIGATE_TO_CHAT -> {
                    createChatIntent(context, payload)
                }
                NotificationActionType.OPEN_PAYMENT -> {
                    createPaymentIntent(context, payload)
                }
                NotificationActionType.NAVIGATE_TO_DASHBOARD -> {
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                }
                NotificationActionType.NONE -> {
                    Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                }
            }
        }
        
        // Default: navigate based on notification type
        return when (payload.type) {
            com.example.limouserapp.data.notification.model.NotificationType.LIVE_RIDE,
            com.example.limouserapp.data.notification.model.NotificationType.LIVE_RIDE_DO -> {
                createLiveRideIntent(context, payload)
            }
            com.example.limouserapp.data.notification.model.NotificationType.CHAT_MESSAGE -> {
                createChatIntent(context, payload)
            }
            com.example.limouserapp.data.notification.model.NotificationType.BOOKING_UPDATE,
            com.example.limouserapp.data.notification.model.NotificationType.DRIVER_ARRIVED,
            com.example.limouserapp.data.notification.model.NotificationType.RIDE_COMPLETED,
            com.example.limouserapp.data.notification.model.NotificationType.RIDE_CANCELLED -> {
                createBookingIntent(context, payload)
            }
            else -> {
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
            }
        }
    }
    
    private fun createLiveRideIntent(context: Context, payload: NotificationPayload): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "live_ride")
            payload.bookingId?.let { putExtra("booking_id", it) }
        }
    }
    
    private fun createBookingIntent(context: Context, payload: NotificationPayload): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "booking")
            payload.bookingId?.let { putExtra("booking_id", it) }
        }
    }
    
    private fun createChatIntent(context: Context, payload: NotificationPayload): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "chat")
            payload.bookingId?.let { putExtra("booking_id", it.toString()) }
            Timber.d("Created chat intent with bookingId: ${payload.bookingId}")
        }
    }
    
    private fun createPaymentIntent(context: Context, payload: NotificationPayload): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", "payment")
            payload.bookingId?.let { putExtra("booking_id", it) }
        }
    }
}

